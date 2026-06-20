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
  "footer",
  "navbar",
] as const;

export type BlockType = (typeof BLOCK_TYPES)[number];

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
