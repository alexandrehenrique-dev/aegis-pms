package br.com.byop.aegis.pages.service;

import br.com.byop.aegis.audit.api.AuditRecordCommand;
import br.com.byop.aegis.audit.api.AuditService;
import br.com.byop.aegis.pages.contract.CreatePageRequest;
import br.com.byop.aegis.pages.contract.CreateSectionRequest;
import br.com.byop.aegis.pages.contract.PageSeoRequest;
import br.com.byop.aegis.pages.contract.ReorderSectionsRequest;
import br.com.byop.aegis.pages.contract.UpdatePageRequest;
import br.com.byop.aegis.pages.contract.UpdateSectionRequest;
import br.com.byop.aegis.pages.domain.BlockType;
import br.com.byop.aegis.pages.domain.Page;
import br.com.byop.aegis.pages.domain.PageSection;
import br.com.byop.aegis.pages.domain.PageStatus;
import br.com.byop.aegis.pages.dto.PageDetail;
import br.com.byop.aegis.pages.dto.PageSectionResponse;
import br.com.byop.aegis.pages.dto.PageSummary;
import br.com.byop.aegis.pages.exception.DuplicatePageSlugException;
import br.com.byop.aegis.pages.exception.InvalidPageStatusException;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PageService {

    private static final String TARGET_TYPE_PAGE = "Page";
    private static final String TARGET_TYPE_PAGE_SECTION = "PageSection";
    private static final String MODULE_PAGES = "PAGES";
    private static final String DIFF_KEY_SLUG = "slug";
    private static final String DIFF_KEY_TITLE = "title";
    private static final String DIFF_KEY_STATUS = "status";
    private static final TypeReference<Map<String, Object>> CONTENT_TYPE = new TypeReference<>() {
    };

    private final PageRepository pageRepository;
    private final PageSectionRepository sectionRepository;
    private final PageMapper pageMapper;
    private final PageSectionMapper sectionMapper;
    private final SectionContentValidationService validationService;
    private final ProductReferenceService productReferenceService;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    public PageService(PageRepository pageRepository, PageSectionRepository sectionRepository, PageMapper pageMapper,
                       PageSectionMapper sectionMapper, SectionContentValidationService validationService,
                       ProductReferenceService productReferenceService, ObjectMapper objectMapper,
                       AuditService auditService) {
        this.pageRepository = pageRepository;
        this.sectionRepository = sectionRepository;
        this.pageMapper = pageMapper;
        this.sectionMapper = sectionMapper;
        this.validationService = validationService;
        this.productReferenceService = productReferenceService;
        this.objectMapper = objectMapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<PageSummary> listPages(UUID productId) {
        log.debug("listPages: productId='{}'", productId);
        return pageRepository.findAllByProductId(productId).stream().map(pageMapper::toSummary).toList();
    }

    @Transactional
    public PageSummary createPage(UUID productId, CreatePageRequest request, AuthenticatedUser caller) {
        log.debug("createPage: productId='{}', slug='{}'", productId, request.slug());
        ProductReference product = productReferenceService.getRequiredReference(productId);
        assertSlugAvailable(productId, request.slug(), null);

        Page page = new Page(product.tenantId(), productId, request.slug(), request.title(), request.locale());
        pageRepository.save(page);
        recordPageAudit("PAGE_CREATED", page, caller.subject(), null, currentPageState(page));
        log.info("createPage: pagina criada id='{}', slug='{}'", page.getId(), page.getSlug());

        return pageMapper.toSummary(page);
    }

    @Transactional(readOnly = true)
    public PageDetail getPage(UUID productId, UUID pageId) {
        log.debug("getPage: productId='{}', pageId='{}'", productId, pageId);
        return toDetail(findPageInProduct(productId, pageId));
    }

    @Transactional
    public PageDetail updatePage(UUID productId, UUID pageId, UpdatePageRequest request, AuthenticatedUser caller) {
        log.debug("updatePage: productId='{}', pageId='{}', slug='{}'", productId, pageId, request.slug());
        Page page = findPageInProduct(productId, pageId);
        assertSlugAvailable(productId, request.slug(), pageId);
        Map<String, Object> before = currentPageState(page);

        PageStatus status = parseStatus(request.status());
        PageSeoRequest seo = request.seo();
        page.applyEdit(new Page.Edit(request.slug(), request.title(), request.locale(), status,
                seo == null ? null : seo.title(), seo == null ? null : seo.description(),
                seo == null ? null : seo.canonical(), seo == null ? null : seo.ogImageAssetId(),
                seo != null && seo.noIndex()));
        pageRepository.save(page);
        recordPageAudit("PAGE_UPDATED", page, caller.subject(), before, currentPageState(page));
        log.info("updatePage: pagina atualizada id='{}', status='{}'", page.getId(), page.getStatus());

        return toDetail(page);
    }

    @Transactional
    public void deletePage(UUID productId, UUID pageId, AuthenticatedUser caller) {
        log.debug("deletePage: productId='{}', pageId='{}'", productId, pageId);
        Page page = findPageInProduct(productId, pageId);
        recordPageAudit("PAGE_DELETED", page, caller.subject(), currentPageState(page), null);
        pageRepository.delete(page);
        log.info("deletePage: pagina removida id='{}'", pageId);
    }

    @Transactional
    public PageSectionResponse createSection(UUID productId, UUID pageId, CreateSectionRequest request,
                                             AuthenticatedUser caller) {
        log.debug("createSection: productId='{}', pageId='{}', type='{}'", productId, pageId, request.type());
        Page page = findPageInProduct(productId, pageId);
        BlockType type = parseBlockType(request.type());
        Map<String, Object> sanitizedContent = validationService.sanitizeContent(request.content());
        validationService.validateSectionContent(productId, type, sanitizedContent);
        Map<String, Object> sanitizedSettings = request.settings() == null ? null : validationService.sanitizeContent(request.settings());

        PageSection section = new PageSection(page.getId(), type, request.variant(), request.order(),
                writeJson(sanitizedContent), writeJson(sanitizedSettings));
        sectionRepository.save(section);
        recordSectionAudit("PAGE_SECTION_CREATED", page, section, caller.subject());
        log.info("createSection: secao criada id='{}', pageId='{}', type='{}'", section.getId(), pageId, type);

        return sectionMapper.toResponse(section, sanitizedContent, sanitizedSettings);
    }

    @Transactional
    public PageSectionResponse updateSection(UUID productId, UUID pageId, UUID sectionId, UpdateSectionRequest request,
                                             AuthenticatedUser caller) {
        log.debug("updateSection: productId='{}', pageId='{}', sectionId='{}'", productId, pageId, sectionId);
        Page page = findPageInProduct(productId, pageId);
        PageSection section = findSectionInPage(pageId, sectionId);
        BlockType type = parseBlockType(request.type());
        Map<String, Object> sanitizedContent = validationService.sanitizeContent(request.content());
        validationService.validateSectionContent(productId, type, sanitizedContent);
        Map<String, Object> sanitizedSettings = request.settings() == null ? null : validationService.sanitizeContent(request.settings());

        section.applyEdit(new PageSection.Edit(type, request.variant(), request.order(),
                writeJson(sanitizedContent), writeJson(sanitizedSettings)));
        sectionRepository.save(section);
        recordSectionAudit("PAGE_SECTION_UPDATED", page, section, caller.subject());
        log.info("updateSection: secao atualizada id='{}'", section.getId());

        return sectionMapper.toResponse(section, sanitizedContent, sanitizedSettings);
    }

    @Transactional
    public void deleteSection(UUID productId, UUID pageId, UUID sectionId, AuthenticatedUser caller) {
        log.debug("deleteSection: productId='{}', pageId='{}', sectionId='{}'", productId, pageId, sectionId);
        Page page = findPageInProduct(productId, pageId);
        PageSection section = findSectionInPage(pageId, sectionId);
        recordSectionAudit("PAGE_SECTION_DELETED", page, section, caller.subject());
        sectionRepository.delete(section);
        log.info("deleteSection: secao removida id='{}'", sectionId);
    }

    @Transactional
    public List<PageSectionResponse> reorderSections(UUID productId, UUID pageId, ReorderSectionsRequest request,
                                                      AuthenticatedUser caller) {
        log.debug("reorderSections: productId='{}', pageId='{}', count='{}'", productId, pageId,
                request.sectionIds().size());
        Page page = findPageInProduct(productId, pageId);
        List<PageSection> sections = sectionRepository.findAllByPageIdOrderByOrderAsc(pageId);
        assertReorderMatchesCurrentSections(sections, request.sectionIds());

        Map<UUID, PageSection> sectionsById = new HashMap<>();
        sections.forEach(section -> sectionsById.put(section.getId(), section));
        for (int index = 0; index < request.sectionIds().size(); index++) {
            sectionsById.get(request.sectionIds().get(index)).reorderTo(index);
        }
        sectionRepository.saveAll(sections);

        auditService.recordEvent(new AuditRecordCommand(page.getTenantId(), productId, caller.subject(),
                "PAGE_SECTIONS_REORDERED", TARGET_TYPE_PAGE, page.getId().toString(), page.getTitle(), MODULE_PAGES,
                null, Map.of("sectionIds", request.sectionIds())));
        log.info("reorderSections: secoes reordenadas pageId='{}'", pageId);

        return loadSectionResponses(pageId);
    }

    private PageDetail toDetail(Page page) {
        return pageMapper.toDetail(page, loadSectionResponses(page.getId()));
    }

    private List<PageSectionResponse> loadSectionResponses(UUID pageId) {
        return sectionRepository.findAllByPageIdOrderByOrderAsc(pageId).stream()
                .map(this::toSectionResponse)
                .toList();
    }

    private PageSectionResponse toSectionResponse(PageSection section) {
        Map<String, Object> content = readMap(section.getContentJson());
        String settingsJson = section.getSettingsJson();
        Map<String, Object> settings = settingsJson == null ? null : readMap(settingsJson);
        return sectionMapper.toResponse(section, content, settings);
    }

    private Page findPageInProduct(UUID productId, UUID pageId) {
        return pageRepository.findByProductIdAndId(productId, pageId)
                .orElseThrow(() -> new PageNotFoundException(pageId));
    }

    private PageSection findSectionInPage(UUID pageId, UUID sectionId) {
        return sectionRepository.findByPageIdAndId(pageId, sectionId)
                .orElseThrow(() -> new PageSectionNotFoundException(sectionId));
    }

    private void assertSlugAvailable(UUID productId, String slug, UUID pageIdBeingEdited) {
        pageRepository.findByProductIdAndSlug(productId, slug).ifPresent(existing -> {
            if (!existing.getId().equals(pageIdBeingEdited)) {
                log.warn("assertSlugAvailable: slug ja em uso productId='{}', slug='{}'", productId, slug);
                throw new DuplicatePageSlugException(slug);
            }
        });
    }

    private void assertReorderMatchesCurrentSections(List<PageSection> sections, List<UUID> sectionIds) {
        Set<UUID> currentIds = sections.stream().map(PageSection::getId).collect(Collectors.toSet());
        Set<UUID> requestedIds = new HashSet<>(sectionIds);
        if (requestedIds.size() != sectionIds.size() || !currentIds.equals(requestedIds)) {
            log.warn("assertReorderMatchesCurrentSections: reorder invalido, ids nao correspondem as secoes atuais");
            throw new InvalidSectionReorderException();
        }
    }

    private BlockType parseBlockType(String type) {
        try {
            return BlockType.fromContractValue(type);
        } catch (IllegalArgumentException _) {
            throw new UnknownBlockTypeException(type);
        }
    }

    private PageStatus parseStatus(String status) {
        try {
            return PageStatus.fromContractValue(status);
        } catch (IllegalArgumentException _) {
            throw new InvalidPageStatusException(status);
        }
    }

    private void recordPageAudit(String action, Page page, String actorSubject, Map<String, Object> before,
                                 Map<String, Object> after) {
        auditService.recordEvent(new AuditRecordCommand(page.getTenantId(), page.getProductId(),
                actorSubject, action, TARGET_TYPE_PAGE, page.getId().toString(), page.getTitle(),
                MODULE_PAGES, before, after));
    }

    private void recordSectionAudit(String action, Page page, PageSection section, String actorSubject) {
        auditService.recordEvent(new AuditRecordCommand(page.getTenantId(), page.getProductId(),
                actorSubject, action, TARGET_TYPE_PAGE_SECTION, section.getId().toString(),
                page.getTitle() + " / " + section.getType().contractValue(), MODULE_PAGES, null,
                Map.of("type", section.getType().contractValue(), "order", section.getOrder())));
    }

    private Map<String, Object> currentPageState(Page page) {
        return Map.of(DIFF_KEY_SLUG, page.getSlug(), DIFF_KEY_TITLE, page.getTitle(),
                DIFF_KEY_STATUS, page.getStatus().contractValue());
    }

    private String writeJson(Map<String, Object> value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Unable to serialize section JSON", ex);
        }
    }

    /**
     * Decodifica {@code json} para um mapa; nunca retorna {@code null}
     * (java:S1168) — o chamador ({@link #toSectionResponse}) decide, antes de
     * invocar este metodo, se a coluna nula deve virar {@code null} na
     * resposta (preserva a distincao "sem settings" vs "settings vazio").
     * Entrada em branco (nao nula, mas sem conteudo) e tratada como mapa vazio.
     *
     * @param json JSON bruto ja garantido nao nulo pelo chamador
     * @return mapa decodificado, ou vazio se {@code json} estiver em branco
     */
    private Map<String, Object> readMap(String json) {
        if (json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, CONTENT_TYPE);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Unable to deserialize section JSON", ex);
        }
    }
}
