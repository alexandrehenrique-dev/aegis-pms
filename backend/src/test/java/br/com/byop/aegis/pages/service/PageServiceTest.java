package br.com.byop.aegis.pages.service;

import br.com.byop.aegis.audit.api.AuditRecordCommand;
import br.com.byop.aegis.audit.api.AuditService;
import br.com.byop.aegis.asset.api.AssetReferenceService;
import br.com.byop.aegis.form.api.FormReferenceService;
import br.com.byop.aegis.pages.contract.CreatePageRequest;
import br.com.byop.aegis.pages.contract.CreateSectionRequest;
import br.com.byop.aegis.pages.contract.PageSeoRequest;
import br.com.byop.aegis.pages.contract.ReorderSectionsRequest;
import br.com.byop.aegis.pages.contract.UpdatePageRequest;
import br.com.byop.aegis.pages.contract.UpdateSectionRequest;
import br.com.byop.aegis.pages.domain.BlockType;
import br.com.byop.aegis.pages.domain.Page;
import br.com.byop.aegis.pages.domain.PageSection;
import br.com.byop.aegis.pages.dto.PageDetail;
import br.com.byop.aegis.pages.dto.PageSectionResponse;
import br.com.byop.aegis.pages.dto.PageSeoResponse;
import br.com.byop.aegis.pages.dto.PageSummary;
import br.com.byop.aegis.pages.exception.DuplicatePageSlugException;
import br.com.byop.aegis.pages.exception.InvalidPageStatusException;
import br.com.byop.aegis.pages.exception.InvalidSectionContentException;
import br.com.byop.aegis.pages.exception.InvalidSectionReorderException;
import br.com.byop.aegis.pages.exception.PageNotFoundException;
import br.com.byop.aegis.pages.exception.PageSectionNotFoundException;
import br.com.byop.aegis.pages.exception.UnknownBlockTypeException;
import br.com.byop.aegis.pages.mapper.PageMapper;
import br.com.byop.aegis.pages.mapper.PageSectionMapper;
import br.com.byop.aegis.pages.repository.PageRepository;
import br.com.byop.aegis.pages.repository.PageSectionRepository;
import br.com.byop.aegis.product.api.ProductReference;
import br.com.byop.aegis.product.api.ProductReferenceService;
import br.com.byop.aegis.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PageServiceTest {

    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final AuthenticatedUser CALLER = new AuthenticatedUser("user-1", "user@example.com", "user", "User", Set.of("ROLE_EDITOR"));

    @Mock
    private PageRepository pageRepository;

    @Mock
    private PageSectionRepository sectionRepository;

    @Mock
    private PageMapper pageMapper;

    @Mock
    private PageSectionMapper sectionMapper;

    @Mock
    private FormReferenceService formReferenceService;

    @Mock
    private AssetReferenceService assetReferenceService;

    @Mock
    private ProductReferenceService productReferenceService;

    @Mock
    private AuditService auditService;

    private PageService service;

    @BeforeEach
    void setUp() {
        SectionContentValidationService validationService = new SectionContentValidationService(
                formReferenceService, assetReferenceService, new PageMarkdownSanitizer());
        service = new PageService(pageRepository, sectionRepository, pageMapper, sectionMapper, validationService,
                productReferenceService, new ObjectMapper(), auditService);
    }

    @Test
    void shouldListPages() {
        Page page = page(UUID.randomUUID(), "home");
        when(pageRepository.findAllByProductId(PRODUCT_ID)).thenReturn(List.of(page));
        PageSummary summary = summary(page);
        when(pageMapper.toSummary(page)).thenReturn(summary);

        assertThat(service.listPages(PRODUCT_ID)).containsExactly(summary);
    }

    @Test
    void shouldCreatePage() {
        when(productReferenceService.getRequiredReference(PRODUCT_ID)).thenReturn(new ProductReference(PRODUCT_ID, TENANT_ID));
        when(pageRepository.findByProductIdAndSlug(PRODUCT_ID, "home")).thenReturn(Optional.empty());
        when(pageRepository.save(any(Page.class))).thenAnswer(invocation -> assignId(invocation.getArgument(0)));
        when(pageMapper.toSummary(any(Page.class))).thenAnswer(invocation -> summary(invocation.getArgument(0)));

        PageSummary result = service.createPage(PRODUCT_ID, new CreatePageRequest("home", "Home", "pt-BR"), CALLER);

        assertThat(result.slug()).isEqualTo("home");
        verify(pageRepository).save(any(Page.class));
        ArgumentCaptor<AuditRecordCommand> captor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService).recordEvent(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo("PAGE_CREATED");
        assertThat(captor.getValue().tenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    void shouldRejectCreatePageWithDuplicateSlug() {
        when(productReferenceService.getRequiredReference(PRODUCT_ID)).thenReturn(new ProductReference(PRODUCT_ID, TENANT_ID));
        when(pageRepository.findByProductIdAndSlug(PRODUCT_ID, "home")).thenReturn(Optional.of(page(UUID.randomUUID(), "home")));
        CreatePageRequest request = new CreatePageRequest("home", "Home", "pt-BR");

        assertThatThrownBy(() -> service.createPage(PRODUCT_ID, request, CALLER))
                .isInstanceOf(DuplicatePageSlugException.class);
        verify(pageRepository, never()).save(any(Page.class));
    }

    @Test
    void shouldGetPageWithSections() {
        UUID pageId = UUID.randomUUID();
        Page page = page(pageId, "home");
        PageSection section = section(page.getId(), BlockType.HERO, "{\"title\":\"Ola\"}", null);
        when(pageRepository.findByProductIdAndId(PRODUCT_ID, pageId)).thenReturn(Optional.of(page));
        when(sectionRepository.findAllByPageIdOrderByOrderAsc(pageId)).thenReturn(List.of(section));
        PageSectionResponse sectionResponse = new PageSectionResponse(section.getId(), "hero", null, 0, Map.of("title", "Ola"), null);
        when(sectionMapper.toResponse(section, Map.of("title", "Ola"), null)).thenReturn(sectionResponse);
        PageDetail detail = detail(page, List.of(sectionResponse));
        when(pageMapper.toDetail(page, List.of(sectionResponse))).thenReturn(detail);

        assertThat(service.getPage(PRODUCT_ID, pageId)).isEqualTo(detail);
    }

    @Test
    void shouldTreatBlankSettingsJsonAsEmptyMap() {
        UUID pageId = UUID.randomUUID();
        Page page = page(pageId, "home");
        PageSection section = section(page.getId(), BlockType.TEXT, "{\"body\":\"a\"}", "   ");
        when(pageRepository.findByProductIdAndId(PRODUCT_ID, pageId)).thenReturn(Optional.of(page));
        when(sectionRepository.findAllByPageIdOrderByOrderAsc(pageId)).thenReturn(List.of(section));
        when(sectionMapper.toResponse(section, Map.of("body", "a"), Map.of()))
                .thenReturn(new PageSectionResponse(section.getId(), "text", null, 0, Map.of("body", "a"), Map.of()));
        when(pageMapper.toDetail(any(Page.class), any())).thenAnswer(invocation -> detail(invocation.getArgument(0), invocation.getArgument(1)));

        service.getPage(PRODUCT_ID, pageId);

        verify(sectionMapper).toResponse(section, Map.of("body", "a"), Map.of());
    }

    @Test
    void shouldRejectGetPageWhenNotFound() {
        UUID pageId = UUID.randomUUID();
        when(pageRepository.findByProductIdAndId(PRODUCT_ID, pageId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPage(PRODUCT_ID, pageId)).isInstanceOf(PageNotFoundException.class);
    }

    @Test
    void shouldUpdatePage() {
        UUID pageId = UUID.randomUUID();
        Page page = page(pageId, "home");
        UUID ogImageAssetId = UUID.randomUUID();
        when(pageRepository.findByProductIdAndId(PRODUCT_ID, pageId)).thenReturn(Optional.of(page));
        when(pageRepository.findByProductIdAndSlug(PRODUCT_ID, "home")).thenReturn(Optional.of(page));
        when(sectionRepository.findAllByPageIdOrderByOrderAsc(pageId)).thenReturn(List.of());
        when(pageMapper.toDetail(any(Page.class), any())).thenAnswer(invocation -> detail(invocation.getArgument(0), List.of()));

        UpdatePageRequest request = new UpdatePageRequest("home", "Home editada", "en-US", "published",
                new PageSeoRequest("SEO title", "SEO desc", "https://example.com", ogImageAssetId, true));
        PageDetail result = service.updatePage(PRODUCT_ID, pageId, request, CALLER);

        assertThat(result.title()).isEqualTo("Home editada");
        assertThat(page.getSeoOgImageAssetId()).isEqualTo(ogImageAssetId);
        ArgumentCaptor<AuditRecordCommand> captor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService).recordEvent(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo("PAGE_UPDATED");
    }

    @Test
    void shouldUpdatePageWithoutSeo() {
        UUID pageId = UUID.randomUUID();
        Page page = page(pageId, "home");
        when(pageRepository.findByProductIdAndId(PRODUCT_ID, pageId)).thenReturn(Optional.of(page));
        when(pageRepository.findByProductIdAndSlug(PRODUCT_ID, "home")).thenReturn(Optional.of(page));
        when(sectionRepository.findAllByPageIdOrderByOrderAsc(pageId)).thenReturn(List.of());
        when(pageMapper.toDetail(any(Page.class), any())).thenAnswer(invocation -> detail(invocation.getArgument(0), List.of()));

        UpdatePageRequest request = new UpdatePageRequest("home", "Home", "pt-BR", "draft", null);
        service.updatePage(PRODUCT_ID, pageId, request, CALLER);

        assertThat(page.getSeoTitle()).isNull();
        assertThat(page.isSeoNoIndex()).isFalse();
    }

    @Test
    void shouldUpdatePageWithSeoButNoIndexFalse() {
        UUID pageId = UUID.randomUUID();
        Page page = page(pageId, "home");
        when(pageRepository.findByProductIdAndId(PRODUCT_ID, pageId)).thenReturn(Optional.of(page));
        when(pageRepository.findByProductIdAndSlug(PRODUCT_ID, "home")).thenReturn(Optional.of(page));
        when(sectionRepository.findAllByPageIdOrderByOrderAsc(pageId)).thenReturn(List.of());
        when(pageMapper.toDetail(any(Page.class), any())).thenAnswer(invocation -> detail(invocation.getArgument(0), List.of()));

        UpdatePageRequest request = new UpdatePageRequest("home", "Home", "pt-BR", "draft",
                new PageSeoRequest("SEO title", null, null, null, false));
        service.updatePage(PRODUCT_ID, pageId, request, CALLER);

        assertThat(page.isSeoNoIndex()).isFalse();
    }

    @Test
    void shouldRejectUpdatePageWithSlugTakenByAnotherPage() {
        UUID pageId = UUID.randomUUID();
        Page page = page(pageId, "home");
        Page otherPage = page(UUID.randomUUID(), "sobre");
        when(pageRepository.findByProductIdAndId(PRODUCT_ID, pageId)).thenReturn(Optional.of(page));
        when(pageRepository.findByProductIdAndSlug(PRODUCT_ID, "sobre")).thenReturn(Optional.of(otherPage));

        UpdatePageRequest request = new UpdatePageRequest("sobre", "Home", "pt-BR", "draft", null);

        assertThatThrownBy(() -> service.updatePage(PRODUCT_ID, pageId, request, CALLER))
                .isInstanceOf(DuplicatePageSlugException.class);
    }

    @Test
    void shouldRejectUpdatePageWithInvalidStatus() {
        UUID pageId = UUID.randomUUID();
        Page page = page(pageId, "home");
        when(pageRepository.findByProductIdAndId(PRODUCT_ID, pageId)).thenReturn(Optional.of(page));
        when(pageRepository.findByProductIdAndSlug(PRODUCT_ID, "home")).thenReturn(Optional.of(page));

        UpdatePageRequest request = new UpdatePageRequest("home", "Home", "pt-BR", "unknown-status", null);

        assertThatThrownBy(() -> service.updatePage(PRODUCT_ID, pageId, request, CALLER))
                .isInstanceOf(InvalidPageStatusException.class);
    }

    @Test
    void shouldDeletePage() {
        UUID pageId = UUID.randomUUID();
        Page page = page(pageId, "home");
        when(pageRepository.findByProductIdAndId(PRODUCT_ID, pageId)).thenReturn(Optional.of(page));

        service.deletePage(PRODUCT_ID, pageId, CALLER);

        verify(pageRepository).delete(page);
        ArgumentCaptor<AuditRecordCommand> captor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService).recordEvent(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo("PAGE_DELETED");
    }

    @Test
    void shouldCreateSection() {
        UUID pageId = UUID.randomUUID();
        Page page = page(pageId, "home");
        when(pageRepository.findByProductIdAndId(PRODUCT_ID, pageId)).thenReturn(Optional.of(page));
        when(sectionRepository.save(any(PageSection.class))).thenAnswer(invocation -> assignId(invocation.getArgument(0)));
        PageSectionResponse response = new PageSectionResponse(UUID.randomUUID(), "hero", null, 0, Map.of("title", "Ola"), Map.of("align", "center"));
        when(sectionMapper.toResponse(any(PageSection.class), any(), any())).thenReturn(response);

        CreateSectionRequest request = new CreateSectionRequest("hero", null, 0, Map.of("title", "Ola"), Map.of("align", "center"));
        PageSectionResponse result = service.createSection(PRODUCT_ID, pageId, request, CALLER);

        assertThat(result).isEqualTo(response);
        verify(sectionRepository).save(any(PageSection.class));
        ArgumentCaptor<AuditRecordCommand> captor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService).recordEvent(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo("PAGE_SECTION_CREATED");
    }

    @Test
    void shouldRejectCreateSectionWithUnknownType() {
        UUID pageId = UUID.randomUUID();
        when(pageRepository.findByProductIdAndId(PRODUCT_ID, pageId)).thenReturn(Optional.of(page(pageId, "home")));

        CreateSectionRequest request = new CreateSectionRequest("carousel-3d", null, 0, Map.of(), null);

        assertThatThrownBy(() -> service.createSection(PRODUCT_ID, pageId, request, CALLER))
                .isInstanceOf(UnknownBlockTypeException.class);
    }

    @Test
    void shouldRejectCreateSectionWithInvalidContent() {
        UUID pageId = UUID.randomUUID();
        when(pageRepository.findByProductIdAndId(PRODUCT_ID, pageId)).thenReturn(Optional.of(page(pageId, "home")));

        CreateSectionRequest request = new CreateSectionRequest("hero", null, 0, Map.of(), null);

        assertThatThrownBy(() -> service.createSection(PRODUCT_ID, pageId, request, CALLER))
                .isInstanceOf(InvalidSectionContentException.class);
        verify(sectionRepository, never()).save(any(PageSection.class));
    }

    @Test
    void shouldUpdateSection() {
        UUID pageId = UUID.randomUUID();
        Page page = page(pageId, "home");
        PageSection section = section(pageId, BlockType.TEXT, "{\"body\":\"a\"}", null);
        when(pageRepository.findByProductIdAndId(PRODUCT_ID, pageId)).thenReturn(Optional.of(page));
        when(sectionRepository.findByPageIdAndId(pageId, section.getId())).thenReturn(Optional.of(section));
        PageSectionResponse response = new PageSectionResponse(section.getId(), "text", null, 1, Map.of("body", "b"), Map.of("columns", 2));
        when(sectionMapper.toResponse(any(PageSection.class), any(), any())).thenReturn(response);

        UpdateSectionRequest request = new UpdateSectionRequest("text", null, 1, Map.of("body", "b"), Map.of("columns", 2));
        PageSectionResponse result = service.updateSection(PRODUCT_ID, pageId, section.getId(), request, CALLER);

        assertThat(result).isEqualTo(response);
        ArgumentCaptor<AuditRecordCommand> captor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService).recordEvent(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo("PAGE_SECTION_UPDATED");
    }

    @Test
    void shouldUpdateSectionWithoutSettings() {
        UUID pageId = UUID.randomUUID();
        Page page = page(pageId, "home");
        PageSection section = section(pageId, BlockType.TEXT, "{\"body\":\"a\"}", "{\"columns\":2}");
        when(pageRepository.findByProductIdAndId(PRODUCT_ID, pageId)).thenReturn(Optional.of(page));
        when(sectionRepository.findByPageIdAndId(pageId, section.getId())).thenReturn(Optional.of(section));
        PageSectionResponse response = new PageSectionResponse(section.getId(), "text", null, 1, Map.of("body", "b"), null);
        when(sectionMapper.toResponse(any(PageSection.class), any(), any())).thenReturn(response);

        UpdateSectionRequest request = new UpdateSectionRequest("text", null, 1, Map.of("body", "b"), null);
        service.updateSection(PRODUCT_ID, pageId, section.getId(), request, CALLER);

        assertThat(section.getSettingsJson()).isNull();
    }

    @Test
    void shouldRejectUpdateSectionWhenNotFound() {
        UUID pageId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        when(pageRepository.findByProductIdAndId(PRODUCT_ID, pageId)).thenReturn(Optional.of(page(pageId, "home")));
        when(sectionRepository.findByPageIdAndId(pageId, sectionId)).thenReturn(Optional.empty());

        UpdateSectionRequest request = new UpdateSectionRequest("text", null, 0, Map.of("body", "b"), null);

        assertThatThrownBy(() -> service.updateSection(PRODUCT_ID, pageId, sectionId, request, CALLER))
                .isInstanceOf(PageSectionNotFoundException.class);
    }

    @Test
    void shouldDeleteSection() {
        UUID pageId = UUID.randomUUID();
        Page page = page(pageId, "home");
        PageSection section = section(pageId, BlockType.TEXT, "{\"body\":\"a\"}", null);
        when(pageRepository.findByProductIdAndId(PRODUCT_ID, pageId)).thenReturn(Optional.of(page));
        when(sectionRepository.findByPageIdAndId(pageId, section.getId())).thenReturn(Optional.of(section));

        service.deleteSection(PRODUCT_ID, pageId, section.getId(), CALLER);

        verify(sectionRepository).delete(section);
        ArgumentCaptor<AuditRecordCommand> captor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService).recordEvent(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo("PAGE_SECTION_DELETED");
    }

    @Test
    void shouldReorderSections() {
        UUID pageId = UUID.randomUUID();
        Page page = page(pageId, "home");
        PageSection first = section(pageId, BlockType.HERO, "{\"title\":\"A\"}", null);
        PageSection second = section(pageId, BlockType.TEXT, "{\"body\":\"B\"}", null);
        when(pageRepository.findByProductIdAndId(PRODUCT_ID, pageId)).thenReturn(Optional.of(page));
        when(sectionRepository.findAllByPageIdOrderByOrderAsc(pageId)).thenReturn(List.of(first, second));
        when(sectionMapper.toResponse(any(PageSection.class), any(), any()))
                .thenReturn(new PageSectionResponse(UUID.randomUUID(), "hero", null, 0, Map.of(), null));

        List<UUID> newOrder = List.of(second.getId(), first.getId());
        service.reorderSections(PRODUCT_ID, pageId, new ReorderSectionsRequest(newOrder), CALLER);

        assertThat(second.getOrder()).isZero();
        assertThat(first.getOrder()).isEqualTo(1);
        verify(sectionRepository).saveAll(List.of(first, second));
        ArgumentCaptor<AuditRecordCommand> captor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService).recordEvent(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo("PAGE_SECTIONS_REORDERED");
    }

    @Test
    void shouldRejectReorderWithMismatchedSectionIds() {
        UUID pageId = UUID.randomUUID();
        Page page = page(pageId, "home");
        PageSection first = section(pageId, BlockType.HERO, "{\"title\":\"A\"}", null);
        when(pageRepository.findByProductIdAndId(PRODUCT_ID, pageId)).thenReturn(Optional.of(page));
        when(sectionRepository.findAllByPageIdOrderByOrderAsc(pageId)).thenReturn(List.of(first));

        ReorderSectionsRequest request = new ReorderSectionsRequest(List.of(UUID.randomUUID()));

        assertThatThrownBy(() -> service.reorderSections(PRODUCT_ID, pageId, request, CALLER))
                .isInstanceOf(InvalidSectionReorderException.class);
    }

    @Test
    void shouldRejectReorderWithDuplicatedSectionIds() {
        UUID pageId = UUID.randomUUID();
        Page page = page(pageId, "home");
        PageSection first = section(pageId, BlockType.HERO, "{\"title\":\"A\"}", null);
        PageSection second = section(pageId, BlockType.TEXT, "{\"body\":\"B\"}", null);
        when(pageRepository.findByProductIdAndId(PRODUCT_ID, pageId)).thenReturn(Optional.of(page));
        when(sectionRepository.findAllByPageIdOrderByOrderAsc(pageId)).thenReturn(List.of(first, second));

        ReorderSectionsRequest request = new ReorderSectionsRequest(List.of(first.getId(), first.getId()));

        assertThatThrownBy(() -> service.reorderSections(PRODUCT_ID, pageId, request, CALLER))
                .isInstanceOf(InvalidSectionReorderException.class);
    }

    @Test
    void shouldWrapSectionContentSerializationFailure() {
        ObjectMapper brokenObjectMapper = mock(ObjectMapper.class);
        PageService brokenService = new PageService(pageRepository, sectionRepository, pageMapper, sectionMapper,
                new SectionContentValidationService(formReferenceService, assetReferenceService, new PageMarkdownSanitizer()),
                productReferenceService, brokenObjectMapper, auditService);
        UUID pageId = UUID.randomUUID();
        when(pageRepository.findByProductIdAndId(PRODUCT_ID, pageId)).thenReturn(Optional.of(page(pageId, "home")));
        when(brokenObjectMapper.writeValueAsString(any())).thenThrow(mock(JacksonException.class));

        CreateSectionRequest request = new CreateSectionRequest("hero", null, 0, Map.of("title", "Ola"), null);

        assertThatThrownBy(() -> brokenService.createSection(PRODUCT_ID, pageId, request, CALLER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("serialize");
    }

    @Test
    void shouldWrapSectionContentDeserializationFailure() {
        ObjectMapper brokenObjectMapper = mock(ObjectMapper.class);
        PageService brokenService = new PageService(pageRepository, sectionRepository, pageMapper, sectionMapper,
                new SectionContentValidationService(formReferenceService, assetReferenceService, new PageMarkdownSanitizer()),
                productReferenceService, brokenObjectMapper, auditService);
        UUID pageId = UUID.randomUUID();
        Page page = page(pageId, "home");
        PageSection section = section(pageId, BlockType.HERO, "{\"title\":\"Ola\"}", null);
        when(pageRepository.findByProductIdAndId(PRODUCT_ID, pageId)).thenReturn(Optional.of(page));
        when(sectionRepository.findAllByPageIdOrderByOrderAsc(pageId)).thenReturn(List.of(section));
        when(brokenObjectMapper.readValue(anyString(), any(TypeReference.class)))
                .thenThrow(mock(JacksonException.class));

        assertThatThrownBy(() -> brokenService.getPage(PRODUCT_ID, pageId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deserialize");
    }

    private <T> T assignId(T entity) {
        ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
        return entity;
    }

    private Page page(UUID id, String slug) {
        Page page = new Page(TENANT_ID, PRODUCT_ID, slug, "Home", "pt-BR");
        ReflectionTestUtils.setField(page, "id", id);
        return page;
    }

    private PageSection section(UUID pageId, BlockType type, String contentJson, String settingsJson) {
        PageSection section = new PageSection(pageId, type, null, 0, contentJson, settingsJson);
        ReflectionTestUtils.setField(section, "id", UUID.randomUUID());
        return section;
    }

    private PageSummary summary(Page page) {
        return new PageSummary(page.getId(), page.getSlug(), page.getTitle(), page.getLocale(), page.getStatus().contractValue(), page.getVersion());
    }

    private PageDetail detail(Page page, List<PageSectionResponse> sections) {
        return new PageDetail(page.getId(), page.getSlug(), page.getTitle(), page.getLocale(), page.getStatus().contractValue(),
                page.getVersion(), new PageSeoResponse(page.getSeoTitle(), page.getSeoDescription(), page.getSeoCanonical(),
                page.getSeoOgImageAssetId(), page.isSeoNoIndex()), sections);
    }
}
