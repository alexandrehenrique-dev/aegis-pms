import type { BlockType } from "./contracts/responses";

export type ItemCrudRule = {
  /** Campos que precisam estar não-vazios em cada item antes de considerar o item válido (ex.: `alt` em `gallery`). */
  requiredFields?: string[];
  maxItems?: number;
};

export type ItemCrudConfig = {
  /** Chave do array dentro de `content` (quase sempre `items`; `footer` usa `links`). */
  key: string;
  /** Shape de um item novo, criado vazio ao clicar "+ Adicionar item". */
  newItem: Record<string, unknown>;
  rules?: ItemCrudRule;
};

/**
 * CRUD de itens (Sprint 12, Tarefa D) — catálogo único de qual campo de
 * `content` é gerenciado pelo `ItemsCrudEditor` por `BlockType`, com o shape
 * de item novo e as regras de validação client-side (auditoria CMS-first).
 * Substitui o `ArrayFieldEditor` genérico, que só editava itens já
 * existentes e não tinha botão de adicionar/remover.
 */
export const ITEMS_CRUD_CONFIG: Partial<Record<BlockType, ItemCrudConfig>> = {
  gallery: { key: "items", newItem: { src: "", alt: "" }, rules: { requiredFields: ["alt"], maxItems: 50 } },
  "card-list": { key: "items", newItem: { title: "", desc: "" } },
  "feature-grid": { key: "items", newItem: { title: "", desc: "", icon: "" } },
  timeline: { key: "items", newItem: { date: "", title: "", desc: "" } },
  faq: { key: "items", newItem: { q: "", a: "" }, rules: { requiredFields: ["q", "a"] } },
  navbar: { key: "items", newItem: { label: "", href: "" } },
  footer: { key: "links", newItem: { label: "", href: "" } },
  download: { key: "items", newItem: { title: "", fileAssetId: "", fileType: "pdf" }, rules: { requiredFields: ["title", "fileAssetId"] } },
  "social-links": { key: "items", newItem: { platform: "", href: "" }, rules: { requiredFields: ["platform", "href"] } },
};
