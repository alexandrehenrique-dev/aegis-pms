package br.com.byop.aegis.core.product;

/**
 * Catalogo fechado de tipos de produto usados para templates e modulos recomendados.
 */
public enum ProductTypeKey {
    SITE_INSTITUCIONAL("Site Institucional"),
    PORTAL("Portal"),
    KNOWLEDGE_BASE("Knowledge Base"),
    PORTFOLIO("Portfolio"),
    LIBRARY_BOOKS_MUSIC("Library/Books/Music"),
    PRODUTO_SAAS("Produto SaaS"),
    CUSTOM("Custom");

    private final String label;

    ProductTypeKey(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
