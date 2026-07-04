package br.com.byop.aegis.pages.service;

import br.com.byop.aegis.pages.domain.BlockDefaults;
import br.com.byop.aegis.pages.domain.Page;
import br.com.byop.aegis.pages.domain.PageSection;
import br.com.byop.aegis.pages.domain.PageSkeletonTemplate;
import br.com.byop.aegis.pages.domain.PageTemplateCatalog;
import br.com.byop.aegis.pages.domain.SectionSkeletonTemplate;
import br.com.byop.aegis.pages.repository.PageRepository;
import br.com.byop.aegis.pages.repository.PageSectionRepository;
import br.com.byop.aegis.product.api.ProductCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

/**
 * Cria o esqueleto de {@link Page}/{@link PageSection} de um produto recem-criado,
 * quando o {@code type} tiver um esqueleto definido em {@link PageTemplateCatalog}
 * (ADR-0017, Etapa 26). Escuta {@link ProductCreatedEvent} (publicado pelo modulo
 * {@code product}) para nao criar uma dependencia de {@code product} para
 * {@code pages} — {@code pages} ja depende de {@code product.api} (ver {@link
 * br.com.byop.aegis.product.api.ProductReferenceService}), e uma chamada direta no
 * sentido contrario criaria um ciclo entre os dois modulos, rejeitado por
 * {@code ApplicationModules.verify()}.
 *
 * <p>Ao contrario do listener equivalente em {@code asset}
 * ({@code AssetStorageProvisioningService}, fase {@code AFTER_COMMIT} — side
 * effect em disco, sem necessidade de atomicidade com o banco), este listener
 * roda em {@link TransactionPhase#BEFORE_COMMIT}: criar Page/PageSection é
 * escrita no mesmo banco do {@code Product}, e a Etapa 26 exige que a operacao
 * inteira (produto + modulos + paginas + secoes) seja uma unica transacao — se
 * qualquer secao falhar a validacao, nada deve persistir, produto incluido.
 */
@Slf4j
@Service
public class ProductPageScaffoldService {

    private final PageRepository pageRepository;
    private final PageSectionRepository sectionRepository;
    private final SectionContentValidationService validationService;
    private final ObjectMapper objectMapper;

    public ProductPageScaffoldService(PageRepository pageRepository, PageSectionRepository sectionRepository,
                                      SectionContentValidationService validationService, ObjectMapper objectMapper) {
        this.pageRepository = pageRepository;
        this.sectionRepository = sectionRepository;
        this.validationService = validationService;
        this.objectMapper = objectMapper;
    }

    /**
     * Cria o esqueleto de paginas/secoes de {@code productType} para o produto
     * informado. Nao faz nada quando {@code productType} nao tiver esqueleto
     * definido (ex. {@code Custom}, {@code Knowledge Base}).
     *
     * @param tenantId tenant proprietario do produto
     * @param productId produto recem-criado
     * @param productType {@code name()} do enum {@code ProductTypeKey}
     * @param defaultLocale locale default do produto, usado em toda pagina criada
     */
    public void scaffoldFor(UUID tenantId, UUID productId, String productType, String defaultLocale) {
        log.debug("scaffoldFor: productId='{}', productType='{}'", productId, productType);
        int pagesCreated = 0;
        for (PageSkeletonTemplate pageTemplate : PageTemplateCatalog.skeletonFor(productType)) {
            Page page = new Page(tenantId, productId, pageTemplate.slug(), pageTemplate.title(), defaultLocale);
            pageRepository.save(page);
            for (SectionSkeletonTemplate sectionTemplate : pageTemplate.sections()) {
                createSection(page, sectionTemplate, productId);
            }
            pagesCreated++;
        }
        log.info("scaffoldFor: esqueleto criado productId='{}', pagesCreated='{}'", productId, pagesCreated);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onProductCreated(ProductCreatedEvent event) {
        log.debug("onProductCreated: productId='{}', productType='{}'", event.productId(), event.productType());
        scaffoldFor(event.tenantId(), event.productId(), event.productType(), event.defaultLocale());
    }

    private void createSection(Page page, SectionSkeletonTemplate sectionTemplate, UUID productId) {
        Map<String, Object> content = validationService.sanitizeContent(BlockDefaults.defaultFor(sectionTemplate.type()));
        validationService.validateSectionContent(productId, sectionTemplate.type(), content);
        PageSection section = new PageSection(page.getId(), sectionTemplate.type(), sectionTemplate.variant(),
                sectionTemplate.order(), writeJson(content), null);
        sectionRepository.save(section);
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Unable to serialize section JSON", ex);
        }
    }
}
