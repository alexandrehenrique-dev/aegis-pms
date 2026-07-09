package br.com.byop.aegis.pages.service;

import br.com.byop.aegis.pages.domain.BlockType;
import br.com.byop.aegis.pages.domain.Page;
import br.com.byop.aegis.pages.domain.PageSection;
import br.com.byop.aegis.pages.repository.PageRepository;
import br.com.byop.aegis.pages.repository.PageSectionRepository;
import br.com.byop.aegis.product.api.ProductCreatedEvent;
import br.com.byop.aegis.product.api.AssetStorageStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductPageScaffoldServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PRODUCT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private PageRepository pageRepository;

    @Mock
    private PageSectionRepository sectionRepository;

    private SectionContentValidationService validationService;

    private ProductPageScaffoldService service;

    @BeforeEach
    void setUp() {
        validationService = new SectionContentValidationService(
                mock(br.com.byop.aegis.form.api.FormReferenceService.class),
                mock(br.com.byop.aegis.asset.api.AssetReferenceService.class),
                new PageMarkdownSanitizer()
        );
        service = new ProductPageScaffoldService(pageRepository, sectionRepository, validationService, new ObjectMapper());
        lenient().when(pageRepository.save(any(Page.class))).thenAnswer(invocation -> {
            Page page = invocation.getArgument(0);
            ReflectionTestUtils.setField(page, "id", UUID.randomUUID());
            return page;
        });
    }

    @Test
    void shouldCreateSevenPagesForSiteInstitucional() {
        service.scaffoldFor(TENANT_ID, PRODUCT_ID, "SITE_INSTITUCIONAL", "pt-BR");

        ArgumentCaptor<Page> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(pageRepository, times(7)).save(pageCaptor.capture());
        assertThat(pageCaptor.getAllValues()).extracting(Page::getSlug)
                .containsExactly("home", "quem-somos", "historia", "agenda", "galeria", "apoie", "contato");
        assertThat(pageCaptor.getAllValues()).allSatisfy(page -> {
            assertThat(page.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(page.getProductId()).isEqualTo(PRODUCT_ID);
            assertThat(page.getStatus().contractValue()).isEqualTo("draft");
        });

        ArgumentCaptor<PageSection> sectionCaptor = ArgumentCaptor.forClass(PageSection.class);
        verify(sectionRepository, org.mockito.Mockito.atLeast(1)).save(sectionCaptor.capture());
        assertThat(sectionCaptor.getAllValues()).extracting(PageSection::getType)
                .contains(BlockType.HERO, BlockType.GALLERY, BlockType.CONTACT);
    }

    @Test
    void shouldCreateOnePageWithLabeledCardListsForPortal() {
        service.scaffoldFor(TENANT_ID, PRODUCT_ID, "PORTAL", "pt-BR");

        verify(pageRepository, times(1)).save(any(Page.class));
        ArgumentCaptor<PageSection> sectionCaptor = ArgumentCaptor.forClass(PageSection.class);
        verify(sectionRepository, times(4)).save(sectionCaptor.capture());
        assertThat(sectionCaptor.getAllValues()).extracting(PageSection::getVariant)
                .containsExactly(null, null, "Vagas", "Blog");
    }

    @Test
    void shouldNotCreateAnyPageForCustomOrTypesWithoutSkeleton() {
        service.scaffoldFor(TENANT_ID, PRODUCT_ID, "CUSTOM", "pt-BR");
        service.scaffoldFor(TENANT_ID, PRODUCT_ID, "KNOWLEDGE_BASE", "pt-BR");
        service.scaffoldFor(TENANT_ID, PRODUCT_ID, "LIBRARY_BOOKS_MUSIC", "pt-BR");
        service.scaffoldFor(TENANT_ID, PRODUCT_ID, "PRODUTO_SAAS", "pt-BR");

        verify(pageRepository, never()).save(any(Page.class));
        verify(sectionRepository, never()).save(any(PageSection.class));
    }

    @Test
    void shouldPropagateFailureWithoutPersistingAnySection() {
        doThrow(new RuntimeException("db down")).when(pageRepository).save(any(Page.class));

        assertThatThrownBy(() -> service.scaffoldFor(TENANT_ID, PRODUCT_ID, "SITE_INSTITUCIONAL", "pt-BR"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db down");

        verify(sectionRepository, never()).save(any(PageSection.class));
    }

    @Test
    void shouldScaffoldOnProductCreatedEvent() {
        service.onProductCreated(new ProductCreatedEvent(TENANT_ID, PRODUCT_ID, AssetStorageStrategy.LOCAL,
                "PORTFOLIO", "pt-BR"));

        verify(pageRepository, times(1)).save(any(Page.class));
        verify(sectionRepository, times(5)).save(any(PageSection.class));
    }

    @Test
    void shouldWriteSanitizedDefaultContentAsJson() {
        service.scaffoldFor(TENANT_ID, PRODUCT_ID, "PORTAL", "pt-BR");

        ArgumentCaptor<PageSection> sectionCaptor = ArgumentCaptor.forClass(PageSection.class);
        verify(sectionRepository, times(4)).save(sectionCaptor.capture());
        PageSection hero = sectionCaptor.getAllValues().get(0);
        assertThat(hero.getContentJson()).contains("Novo título");
    }

    @Test
    void shouldWrapSectionContentSerializationFailure() {
        ObjectMapper brokenObjectMapper = mock(ObjectMapper.class);
        ProductPageScaffoldService brokenService = new ProductPageScaffoldService(pageRepository, sectionRepository,
                validationService, brokenObjectMapper);
        when(brokenObjectMapper.writeValueAsString(any())).thenThrow(mock(JacksonException.class));

        assertThatThrownBy(() -> brokenService.scaffoldFor(TENANT_ID, PRODUCT_ID, "PORTAL", "pt-BR"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("serialize");
    }
}
