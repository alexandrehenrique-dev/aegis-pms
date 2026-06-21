/**
 * Domínio Page/Section/Block (Sprint 11, Tarefa B) — composição de páginas
 * institucionais a partir de seções tipadas. Convive com `content` (artigo
 * editorial linear); não o substitui. Espelha o backend `21_dominio_pages_secoes_e_blocos.md`.
 */
export const BLOCK_TYPES = [
  "hero",
  "text",
  "rich-text",
  "two-column",
  "image",
  "image-text",
  "feature-grid",
  "card-list",
  "gallery",
  "timeline",
  "event-list",
  "cta-section",
  "faq",
  "contact",
  "form",
  "download",
  "audio",
  "video",
  "video-gallery",
  "social-links",
] as const;

export type BlockType = (typeof BLOCK_TYPES)[number];

/**
 * Allowlist de mini-blocos permitidos dentro de `left`/`right` do bloco
 * `two-column` — espelha o backend (`21_dominio_pages_secoes_e_blocos.md`):
 * cada coluna é uma lista de mini-blocos tipados, não um objeto fixo.
 */
export const MINI_BLOCK_TYPES = ["text", "rich-text", "image", "cta"] as const;
export type MiniBlockType = (typeof MINI_BLOCK_TYPES)[number];
export type MiniBlock = { type: MiniBlockType; content: Record<string, unknown> };

/**
 * Catálogo de composição (Sprint 13, Decisão 3 / Tarefa E.1) — qualquer
 * `BlockType` presente aqui aceita filhos, renderizados pelo motor genérico
 * `SubBlockEditor`. Não é mais exclusividade do `two-column`: um bloco novo
 * (`tabs`, `accordion`...) só precisa de uma entrada aqui para ganhar CRUD de
 * sub-blocos de graça. A allowlist de filhos usa `MiniBlockType` (o catálogo
 * de blocos compostos existente), não o `BlockType` completo — `text`,
 * `rich-text`, `image` e `cta` são os únicos tipos pensados para aninhar.
 */
export const BLOCK_ACCEPTS_CHILDREN: Partial<Record<BlockType, MiniBlockType[]>> = {
  "two-column": ["text", "rich-text", "image", "cta"],
};

export type PageStatus = "draft" | "review" | "published" | "archived";

export type PageSeo = {
  title?: string;
  description?: string;
  keywords?: string;
};

/** `source` permite que um Section (ex.: event-list, card-list) referencie itens de outro domínio em vez de conteúdo inline — igual ao EventListBlock da auditoria CMSS. */
export type SectionSource = {
  type: "contentType" | "jobPosting";
  contentType?: string;
  filter?: Record<string, unknown>;
};

export type Section = {
  id: string;
  type: BlockType;
  label: string;
  order: number;
  content: Record<string, unknown>;
  settings?: Record<string, unknown>;
  source?: SectionSource;
};

export type Page = {
  id: string;
  productSlug: string;
  slug: string;
  title: string;
  locale: string;
  status: PageStatus;
  version: number;
  seo: PageSeo;
  sections: Section[];
};

export type ListPagesResponse = Page[];
