package br.com.byop.aegis.pages.service;

import br.com.byop.aegis.audit.api.AuditRecordCommand;
import br.com.byop.aegis.audit.api.AuditService;
import br.com.byop.aegis.pages.contract.UpdateProductGlobalsRequest;
import br.com.byop.aegis.pages.domain.ProductGlobals;
import br.com.byop.aegis.pages.dto.FloatingWhatsappResponse;
import br.com.byop.aegis.pages.dto.FooterResponse;
import br.com.byop.aegis.pages.dto.NavbarResponse;
import br.com.byop.aegis.pages.dto.ProductGlobalsResponse;
import br.com.byop.aegis.pages.dto.SocialLinkResponse;
import br.com.byop.aegis.pages.mapper.ProductGlobalsMapper;
import br.com.byop.aegis.pages.repository.ProductGlobalsRepository;
import br.com.byop.aegis.product.api.ProductReference;
import br.com.byop.aegis.product.api.ProductReferenceService;
import br.com.byop.aegis.security.AuthenticatedUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Navbar, footer e redes sociais de um produto (ADR-0013). {@code GET} nunca
 * retorna 404 — objeto vazio quando o produto ainda nao tem {@link
 * ProductGlobals} configurado; {@code PUT} faz upsert (Secao F da Sprint 23).
 */
@Slf4j
@Service
public class ProductGlobalsService {

    private static final String TARGET_TYPE_PRODUCT_GLOBALS = "ProductGlobals";
    private static final String MODULE_PAGES = "PAGES";
    private static final String EMPTY_LINKS_JSON = "{\"links\":[]}";
    private static final String EMPTY_SOCIAL_LINKS_JSON = "[]";
    private static final TypeReference<List<SocialLinkResponse>> SOCIAL_LINKS_TYPE = new TypeReference<>() {
    };

    private final ProductGlobalsRepository globalsRepository;
    private final ProductGlobalsMapper globalsMapper;
    private final ProductReferenceService productReferenceService;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    public ProductGlobalsService(ProductGlobalsRepository globalsRepository, ProductGlobalsMapper globalsMapper,
                                 ProductReferenceService productReferenceService, ObjectMapper objectMapper,
                                 AuditService auditService) {
        this.globalsRepository = globalsRepository;
        this.globalsMapper = globalsMapper;
        this.productReferenceService = productReferenceService;
        this.objectMapper = objectMapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public ProductGlobalsResponse getGlobals(UUID productId) {
        log.debug("getGlobals: productId='{}'", productId);
        return globalsRepository.findByProductId(productId)
                .map(this::toResponse)
                .orElseGet(this::emptyResponse);
    }

    @Transactional
    public ProductGlobalsResponse updateGlobals(UUID productId, UpdateProductGlobalsRequest request, AuthenticatedUser caller) {
        log.debug("updateGlobals: productId='{}'", productId);
        String navbarJson = writeJson(request.navbar());
        String footerJson = writeJson(request.footer());
        String socialLinksJson = writeJson(request.socialLinks());
        String floatingWhatsappJson = request.floatingWhatsapp() == null ? null : writeJson(request.floatingWhatsapp());

        Optional<ProductGlobals> existing = globalsRepository.findByProductId(productId);
        ProductGlobals globals = existing
                .map(current -> {
                    current.apply(navbarJson, footerJson, socialLinksJson, floatingWhatsappJson);
                    return current;
                })
                .orElseGet(() -> new ProductGlobals(productId, navbarJson, footerJson, socialLinksJson, floatingWhatsappJson));
        globalsRepository.save(globals);

        recordAudit(productId, caller.subject(), existing.isEmpty());
        log.info("updateGlobals: globals atualizados productId='{}', created='{}'", productId, existing.isEmpty());

        return globalsMapper.toResponse(request);
    }

    private ProductGlobalsResponse toResponse(ProductGlobals globals) {
        return new ProductGlobalsResponse(
                readValue(globals.getNavbarJson(), NavbarResponse.class),
                readValue(globals.getFooterJson(), FooterResponse.class),
                readList(globals.getSocialLinksJson()),
                globals.getFloatingWhatsappJson() == null ? null : readValue(globals.getFloatingWhatsappJson(), FloatingWhatsappResponse.class)
        );
    }

    private ProductGlobalsResponse emptyResponse() {
        return new ProductGlobalsResponse(
                readValue(EMPTY_LINKS_JSON, NavbarResponse.class),
                readValue(EMPTY_LINKS_JSON, FooterResponse.class),
                readList(EMPTY_SOCIAL_LINKS_JSON),
                null
        );
    }

    private void recordAudit(UUID productId, String actorSubject, boolean created) {
        ProductReference product = productReferenceService.getRequiredReference(productId);
        auditService.recordEvent(new AuditRecordCommand(product.tenantId(), productId, actorSubject,
                created ? "PRODUCT_GLOBALS_CREATED" : "PRODUCT_GLOBALS_UPDATED", TARGET_TYPE_PRODUCT_GLOBALS,
                productId.toString(), TARGET_TYPE_PRODUCT_GLOBALS, MODULE_PAGES, null, null));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Unable to serialize product globals JSON", ex);
        }
    }

    private <T> T readValue(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Unable to deserialize product globals JSON", ex);
        }
    }

    private List<SocialLinkResponse> readList(String json) {
        try {
            return objectMapper.readValue(json, SOCIAL_LINKS_TYPE);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Unable to deserialize product globals JSON", ex);
        }
    }
}
