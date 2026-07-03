package br.com.byop.aegis.product.domain;

import br.com.byop.aegis.product.api.ModuleKey;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductModuleTemplateCatalogTest {

    @Test
    void shouldRecommendModulesForSiteInstitucionalWithPagesFirst() {
        assertThat(ProductModuleTemplateCatalog.recommendedModulesFor(ProductTypeKey.SITE_INSTITUCIONAL))
                .containsExactly(ModuleKey.PAGES, ModuleKey.CONTENT, ModuleKey.ASSETS, ModuleKey.FORMS,
                        ModuleKey.SEO, ModuleKey.ANALYTICS);
    }

    @Test
    void shouldRecommendModulesForPortal() {
        assertThat(ProductModuleTemplateCatalog.recommendedModulesFor(ProductTypeKey.PORTAL))
                .containsExactly(ModuleKey.PAGES, ModuleKey.CONTENT, ModuleKey.FORMS, ModuleKey.SEO,
                        ModuleKey.ANALYTICS);
    }

    @Test
    void shouldRecommendContentBeforeKnowledgeGraphForKnowledgeBase() {
        assertThat(ProductModuleTemplateCatalog.recommendedModulesFor(ProductTypeKey.KNOWLEDGE_BASE))
                .containsExactly(ModuleKey.CONTENT, ModuleKey.KNOWLEDGE_GRAPH, ModuleKey.SEO, ModuleKey.ANALYTICS);
    }

    @Test
    void shouldRecommendModulesForPortfolio() {
        assertThat(ProductModuleTemplateCatalog.recommendedModulesFor(ProductTypeKey.PORTFOLIO))
                .containsExactly(ModuleKey.PORTFOLIO, ModuleKey.PAGES, ModuleKey.CONTENT, ModuleKey.ASSETS,
                        ModuleKey.SEO, ModuleKey.ANALYTICS);
    }

    @Test
    void shouldRecommendContentBeforeKnowledgeGraphForLibraryBooksMusic() {
        assertThat(ProductModuleTemplateCatalog.recommendedModulesFor(ProductTypeKey.LIBRARY_BOOKS_MUSIC))
                .containsExactly(ModuleKey.LIBRARY, ModuleKey.BOOKS, ModuleKey.MUSIC, ModuleKey.CONTENT,
                        ModuleKey.KNOWLEDGE_GRAPH, ModuleKey.SEO, ModuleKey.ANALYTICS);
    }

    @Test
    void shouldRecommendModulesForProdutoSaasWithoutUnmodeledWorkflowModule() {
        assertThat(ProductModuleTemplateCatalog.recommendedModulesFor(ProductTypeKey.PRODUTO_SAAS))
                .containsExactly(ModuleKey.CONTENT, ModuleKey.ASSETS, ModuleKey.FORMS, ModuleKey.ANALYTICS,
                        ModuleKey.SEO);
    }

    @Test
    void shouldRecommendNoModulesForCustom() {
        assertThat(ProductModuleTemplateCatalog.recommendedModulesFor(ProductTypeKey.CUSTOM)).isEmpty();
    }
}
