package br.com.byop.aegis.product.domain;

import br.com.byop.aegis.product.api.ModuleKey;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Catalogo fixo (nao entidade) dos modulos recomendados por {@link ProductTypeKey},
 * usado pelo scaffold de {@code POST /products} (ADR-0017). A ordem de cada lista
 * ja resolve a dependencia entre modulos (ex.: {@link ModuleKey#CONTENT} sempre
 * antes de {@link ModuleKey#KNOWLEDGE_GRAPH}), para que habilitar em sequencia
 * nunca seja rejeitado pela validacao de dependencia ja existente
 * ({@code ProductModuleService}). {@link ProductTypeKey#CUSTOM} nao recebe
 * nenhum modulo recomendado — produto nasce 100% em branco.
 *
 * <p>{@code Produto SaaS} nao inclui um modulo "Workflow": a ADR-0017/o catalogo
 * do frontend (`core/products/moduleDefaults.ts`) citam esse modulo, mas ele nao
 * existe no catalogo fechado {@link ModuleKey} ainda — decisao registrada em
 * {@code SPRINT-RESULTADO.md} (Etapa 26), mesmo tratamento ja dado a
 * {@code ECOMMERCE} (recomendado, mas nao implementado).
 */
public final class ProductModuleTemplateCatalog {

    private static final Map<ProductTypeKey, List<ModuleKey>> RECOMMENDED_MODULES = buildRecommendedModules();

    private ProductModuleTemplateCatalog() {
    }

    /**
     * Modulos recomendados para {@code type}, na ordem em que devem ser
     * habilitados (dependencias primeiro). Lista vazia para {@link ProductTypeKey#CUSTOM}.
     *
     * @param type tipo de produto
     * @return lista imutavel de modulos recomendados, nunca {@code null}
     */
    public static List<ModuleKey> recommendedModulesFor(ProductTypeKey type) {
        return RECOMMENDED_MODULES.getOrDefault(type, List.of());
    }

    private static Map<ProductTypeKey, List<ModuleKey>> buildRecommendedModules() {
        Map<ProductTypeKey, List<ModuleKey>> catalog = new EnumMap<>(ProductTypeKey.class);
        catalog.put(ProductTypeKey.SITE_INSTITUCIONAL, List.of(
                ModuleKey.PAGES, ModuleKey.CONTENT, ModuleKey.ASSETS, ModuleKey.FORMS,
                ModuleKey.SEO, ModuleKey.ANALYTICS
        ));
        catalog.put(ProductTypeKey.PORTAL, List.of(
                ModuleKey.PAGES, ModuleKey.CONTENT, ModuleKey.FORMS, ModuleKey.SEO, ModuleKey.ANALYTICS
        ));
        catalog.put(ProductTypeKey.KNOWLEDGE_BASE, List.of(
                ModuleKey.CONTENT, ModuleKey.ASSETS, ModuleKey.KNOWLEDGE_GRAPH, ModuleKey.SEO, ModuleKey.ANALYTICS
        ));
        catalog.put(ProductTypeKey.PORTFOLIO, List.of(
                ModuleKey.PORTFOLIO, ModuleKey.PAGES, ModuleKey.CONTENT, ModuleKey.ASSETS,
                ModuleKey.SEO, ModuleKey.ANALYTICS
        ));
        catalog.put(ProductTypeKey.LIBRARY_BOOKS_MUSIC, List.of(
                ModuleKey.LIBRARY, ModuleKey.BOOKS, ModuleKey.MUSIC, ModuleKey.CONTENT, ModuleKey.ASSETS,
                ModuleKey.KNOWLEDGE_GRAPH, ModuleKey.SEO, ModuleKey.ANALYTICS
        ));
        catalog.put(ProductTypeKey.PRODUTO_SAAS, List.of(
                ModuleKey.CONTENT, ModuleKey.ASSETS, ModuleKey.FORMS, ModuleKey.ANALYTICS, ModuleKey.SEO
        ));
        catalog.put(ProductTypeKey.CUSTOM, List.of());
        return Map.copyOf(catalog);
    }
}
