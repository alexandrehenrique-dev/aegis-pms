/**
 * Conjunto padrão de módulos por tipo de produto (Sprint 11, Tarefa A.2).
 * Usado por CreateProductForm para pré-marcar módulos editáveis na criação.
 * Reaproveitável pelo backend quando a etapa 06 da Sprint 02 for revisitada.
 */
export type ProductTypeKey =
  | "Site Institucional"
  | "Portal"
  | "Knowledge Base"
  | "Portfolio"
  | "Library/Books/Music"
  | "Produto SaaS"
  | "Custom";

export const PRODUCT_TYPES: ProductTypeKey[] = [
  "Site Institucional",
  "Portal",
  "Knowledge Base",
  "Portfolio",
  "Library/Books/Music",
  "Produto SaaS",
  "Custom",
];

export type ModuleOption = {
  key: string;
  /** Pré-marcado por padrão para este tipo de produto. */
  default: boolean;
  /** Módulo ainda não implementado no backend (etapa pendente — ver Sprint 11, Tarefa D). */
  comingSoon?: boolean;
};

/** Knowledge Graph exige o módulo Conteúdo habilitado (Sprint 11, Tarefa A.4). */
export const KNOWLEDGE_GRAPH_DEPENDENCY = "Conteúdo";

/**
 * "E-commerce" (Sprint 13, Decisão 5/Tarefa H.2) — carrinho, checkout e
 * pagamento não são implementados nesta sprint; o módulo é registrado como
 * `comingSoon` para já existir como conceito no catálogo quando chegar a
 * hora, sem exigir outro refactor de modelo. Espelha `ECOMMERCE` no enum de
 * `moduleKey` do backend (`06_modelo_core_tenant_product_modulos.md`).
 */
export const ECOMMERCE_MODULE_KEY = "E-commerce";

/**
 * Produtos seedados nos mocks (`core/auth/mocks/users.ts`) não carregam
 * `modulesList` — só produtos criados via `CreateProductModal`/`CreateProductForm`
 * nesta sessão têm essa lista explícita. Sem isto, qualquer gate por módulo
 * (sidebar, FAQ por módulo, campos condicionais do editor de conteúdo) ficaria
 * vazio para todo produto de demonstração pré-existente. Fallback: módulos
 * padrão do tipo de produto (`PRODUCT_TYPE_MODULE_DEFAULTS`) quando
 * `modulesList` não existir (Sprint 13, Tarefa O; reaproveitado na Sprint 15,
 * Tarefas B e A).
 */
export function resolveEnabledModules(product: { type: string; modulesList?: string[] } | null): string[] {
  if (!product) return [];
  if (product.modulesList) return product.modulesList;
  const defaults = PRODUCT_TYPE_MODULE_DEFAULTS[product.type as ProductTypeKey];
  return defaults ? defaults.filter((m) => m.default).map((m) => m.key) : [];
}

export const PRODUCT_TYPE_MODULE_DEFAULTS: Partial<Record<ProductTypeKey, ModuleOption[]>> = {
  "Site Institucional": [
    { key: "Páginas", default: true },
    { key: "Conteúdo", default: true },
    { key: "Assets", default: true },
    { key: "Forms", default: true },
    { key: "SEO", default: true },
    { key: "Analytics", default: true },
    { key: "Knowledge Graph", default: false },
    { key: ECOMMERCE_MODULE_KEY, default: false, comingSoon: true },
  ],
  Portal: [
    { key: "Páginas", default: true },
    { key: "Conteúdo", default: true },
    { key: "Vagas", default: false, comingSoon: true },
    { key: "Submissions", default: false, comingSoon: true },
    { key: "Forms", default: true },
    { key: "SEO", default: true },
    { key: "Analytics", default: true },
    { key: "Knowledge Graph", default: false },
  ],
  "Knowledge Base": [
    { key: "Conteúdo", default: true },
    { key: "Knowledge Graph", default: true },
    { key: "Comentários", default: false, comingSoon: true },
    { key: "Contribuidores", default: false, comingSoon: true },
    { key: "SEO", default: true },
    { key: "Analytics", default: true },
  ],
  Portfolio: [
    { key: "Portfolio", default: true },
    { key: "Páginas", default: true },
    { key: "Conteúdo", default: true },
    { key: "Assets", default: true },
    { key: "SEO", default: true },
    { key: "Analytics", default: true },
    { key: "Knowledge Graph", default: false },
  ],
  "Library/Books/Music": [
    { key: "Library", default: true },
    { key: "Books", default: true },
    { key: "Music", default: true },
    { key: "Conteúdo", default: true },
    { key: "Knowledge Graph", default: true },
    { key: "SEO", default: true },
    { key: "Analytics", default: true },
  ],
  "Produto SaaS": [
    { key: "Conteúdo", default: true },
    { key: "Assets", default: true },
    { key: "Forms", default: true },
    { key: "Analytics", default: true },
    { key: "SEO", default: true },
    { key: "Workflow", default: true },
    { key: "Knowledge Graph", default: false },
    { key: ECOMMERCE_MODULE_KEY, default: false, comingSoon: true },
  ],
};
