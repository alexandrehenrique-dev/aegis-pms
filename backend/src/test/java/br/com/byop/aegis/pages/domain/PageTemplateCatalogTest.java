package br.com.byop.aegis.pages.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PageTemplateCatalogTest {

    @Test
    void shouldReturnSevenPagesForSiteInstitucional() {
        var pages = PageTemplateCatalog.skeletonFor("SITE_INSTITUCIONAL");

        assertThat(pages).extracting(PageSkeletonTemplate::slug)
                .containsExactly("home", "quem-somos", "historia", "agenda", "galeria", "apoie", "contato");
        assertThat(pages.get(0).sections()).extracting(SectionSkeletonTemplate::type)
                .containsExactly(BlockType.HERO, BlockType.FEATURE_GRID, BlockType.EVENT_LIST, BlockType.CTA_SECTION);
    }

    @Test
    void shouldReturnOnePageWithLabeledCardListsForPortal() {
        var pages = PageTemplateCatalog.skeletonFor("PORTAL");

        assertThat(pages).extracting(PageSkeletonTemplate::slug).containsExactly("home");
        assertThat(pages.get(0).sections()).extracting(SectionSkeletonTemplate::variant)
                .containsExactly(null, null, "Vagas", "Blog");
    }

    @Test
    void shouldReturnOnePageWithLabeledSectionsForPortfolio() {
        var pages = PageTemplateCatalog.skeletonFor("PORTFOLIO");

        assertThat(pages).extracting(PageSkeletonTemplate::slug).containsExactly("home");
        assertThat(pages.get(0).sections()).extracting(SectionSkeletonTemplate::type)
                .containsExactly(BlockType.HERO, BlockType.CARD_LIST, BlockType.FEATURE_GRID, BlockType.TIMELINE,
                        BlockType.DOWNLOAD);
    }

    @Test
    void shouldReturnNoPagesForTypesWithoutSkeleton() {
        assertThat(PageTemplateCatalog.skeletonFor("KNOWLEDGE_BASE")).isEmpty();
        assertThat(PageTemplateCatalog.skeletonFor("LIBRARY_BOOKS_MUSIC")).isEmpty();
        assertThat(PageTemplateCatalog.skeletonFor("PRODUTO_SAAS")).isEmpty();
        assertThat(PageTemplateCatalog.skeletonFor("CUSTOM")).isEmpty();
    }

    @Test
    void sectionOrdersShouldBeSequentialWithinEachPage() {
        PageTemplateCatalog.skeletonFor("SITE_INSTITUCIONAL").forEach(page -> {
            for (int i = 0; i < page.sections().size(); i++) {
                assertThat(page.sections().get(i).order()).isEqualTo(i);
            }
        });
    }
}
