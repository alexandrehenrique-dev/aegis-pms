package br.com.byop.aegis.pages.domain;

import java.util.List;
import java.util.Map;

/**
 * Catalogo fixo (nao entidade) do esqueleto de paginas/secoes por tipo de
 * produto (ADR-0017, Etapa 26) — usado pelo listener que reage a
 * {@code ProductCreatedEvent} para criar o scaffold. Só os tipos cujo modulo
 * {@code PAGES} e recomendado por padrao (Site Institucional, Portal,
 * Portfolio — ver {@code product.domain.ProductModuleTemplateCatalog}) tem
 * entrada aqui; os demais (Knowledge Base, Library/Books/Music, Produto SaaS,
 * Custom) nao criam nenhuma pagina.
 *
 * <p>A chave e o {@code name()} do enum {@code ProductTypeKey} (ex.
 * {@code "SITE_INSTITUCIONAL"}), nunca o proprio tipo — o modulo {@code pages}
 * nao pode depender de {@code product.domain} (so de {@code product.api}),
 * entao o tipo de produto chega aqui como {@code String} via o campo
 * {@code productType} de {@code ProductCreatedEvent}.
 */
public final class PageTemplateCatalog {

    private static final String SLUG_HOME = "home";
    private static final String TITLE_HOME = "Home";
    private static final Map<String, List<PageSkeletonTemplate>> SKELETONS = buildSkeletons();

    private PageTemplateCatalog() {
    }

    /**
     * Esqueleto de paginas para {@code productType}.
     *
     * @param productType {@code name()} do enum {@code ProductTypeKey}
     * @return lista imutavel de paginas a criar, vazia se o tipo nao tiver esqueleto
     */
    public static List<PageSkeletonTemplate> skeletonFor(String productType) {
        return SKELETONS.getOrDefault(productType, List.of());
    }

    private static Map<String, List<PageSkeletonTemplate>> buildSkeletons() {
        return Map.of(
                "SITE_INSTITUCIONAL", siteInstitucionalSkeleton(),
                "PORTAL", portalSkeleton(),
                "PORTFOLIO", portfolioSkeleton()
        );
    }

    private static List<PageSkeletonTemplate> siteInstitucionalSkeleton() {
        return List.of(
                new PageSkeletonTemplate(SLUG_HOME, TITLE_HOME, List.of(
                        new SectionSkeletonTemplate(BlockType.HERO, 0),
                        new SectionSkeletonTemplate(BlockType.FEATURE_GRID, 1),
                        new SectionSkeletonTemplate(BlockType.EVENT_LIST, 2),
                        new SectionSkeletonTemplate(BlockType.CTA_SECTION, 3)
                )),
                new PageSkeletonTemplate("quem-somos", "Quem Somos", List.of(
                        new SectionSkeletonTemplate(BlockType.IMAGE_TEXT, 0),
                        new SectionSkeletonTemplate(BlockType.TWO_COLUMN, 1)
                )),
                new PageSkeletonTemplate("historia", "História", List.of(
                        new SectionSkeletonTemplate(BlockType.TIMELINE, 0)
                )),
                new PageSkeletonTemplate("agenda", "Agenda", List.of(
                        new SectionSkeletonTemplate(BlockType.EVENT_LIST, 0)
                )),
                new PageSkeletonTemplate("galeria", "Galeria", List.of(
                        new SectionSkeletonTemplate(BlockType.GALLERY, 0)
                )),
                new PageSkeletonTemplate("apoie", "Apoie", List.of(
                        new SectionSkeletonTemplate(BlockType.RICH_TEXT, 0),
                        new SectionSkeletonTemplate(BlockType.FAQ, 1)
                )),
                new PageSkeletonTemplate("contato", "Contato", List.of(
                        new SectionSkeletonTemplate(BlockType.CONTACT, 0)
                ))
        );
    }

    private static List<PageSkeletonTemplate> portalSkeleton() {
        return List.of(
                new PageSkeletonTemplate(SLUG_HOME, TITLE_HOME, List.of(
                        new SectionSkeletonTemplate(BlockType.HERO, 0),
                        new SectionSkeletonTemplate(BlockType.TEXT, 1),
                        new SectionSkeletonTemplate(BlockType.CARD_LIST, "Vagas", 2),
                        new SectionSkeletonTemplate(BlockType.CARD_LIST, "Blog", 3)
                ))
        );
    }

    private static List<PageSkeletonTemplate> portfolioSkeleton() {
        return List.of(
                new PageSkeletonTemplate(SLUG_HOME, TITLE_HOME, List.of(
                        new SectionSkeletonTemplate(BlockType.HERO, 0),
                        new SectionSkeletonTemplate(BlockType.CARD_LIST, "Projetos", 1),
                        new SectionSkeletonTemplate(BlockType.FEATURE_GRID, "Skills", 2),
                        new SectionSkeletonTemplate(BlockType.TIMELINE, "Experiência", 3),
                        new SectionSkeletonTemplate(BlockType.DOWNLOAD, "Downloads", 4)
                ))
        );
    }
}
