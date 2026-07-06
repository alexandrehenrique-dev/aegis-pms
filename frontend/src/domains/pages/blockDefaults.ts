import type { BlockType, MiniBlockType } from "./contracts/responses";

/** Conteúdo inicial por `MiniBlockType` ao adicionar um mini-bloco numa coluna de `two-column`. */
export const MINI_BLOCK_DEFAULT_CONTENT: Record<MiniBlockType, Record<string, unknown>> = {
  text: { title: "Novo título", body: "Escreva o conteúdo aqui." },
  "rich-text": { title: "Novo título", body: "Escreva o conteúdo aqui." },
  image: { src: "", alt: "Descrição da imagem" },
  cta: { label: "Saiba mais", href: "#" },
};

/**
 * Conteúdo inicial por `BlockType` ao adicionar um bloco novo no editor
 * (`ContentStructureTree`/`ContentEditor`). Sem isto, um bloco nascia com
 * `content: {}` e o canvas não tinha nenhum campo para mostrar — o usuário
 * via o bloco na árvore, mas não conseguia editar nada dentro dele.
 */
export const DEFAULT_BLOCK_CONTENT: Record<BlockType, Record<string, unknown>> = {
  hero: {
    title: "Novo título",
    subtitle: "Novo subtítulo",
    /**
     * "dark"/"light" (E.10.3, BUG-SPRINT consolidado) — variante visual do
     * hero. Guardado em `content.theme`, não em `PageSection.variant`: esse
     * campo já é usado como `label` do bloco na árvore de estrutura
     * (`pagesService.ts` mapeia `variant` <-> `label`), então reaproveitá-lo
     * para estilo quebraria os labels já existentes.
     */
    theme: "dark",
    image: { src: "", alt: "Descrição da imagem" },
    ctas: [{ label: "Saiba mais", href: "#" }],
  },
  text: { title: "Novo título", body: "Escreva o conteúdo aqui." },
  "rich-text": { title: "Novo título", body: "Escreva o conteúdo aqui." },
  "two-column": {
    left: [{ type: "text", content: { title: "Coluna 1", body: "Conteúdo da primeira coluna." } }],
    right: [{ type: "text", content: { title: "Coluna 2", body: "Conteúdo da segunda coluna." } }],
  },
  image: { alt: "Descrição da imagem", src: "" },
  "image-text": {
    title: "Novo título",
    body: "Conteúdo ao lado da imagem.",
    image: { src: "", alt: "Descrição da imagem" },
  },
  "feature-grid": { title: "Novo título", items: [{ title: "Item 1", desc: "Descrição do item." }] },
  "card-list": { title: "Novo título", items: [{ title: "Item 1", desc: "Descrição do item." }] },
  gallery: { items: [{ src: "", alt: "Descrição da imagem" }] },
  timeline: { items: [{ date: "2026", title: "Marco", desc: "Descrição do marco." }] },
  "event-list": { title: "Agenda", source: {}, selectedEventIds: [] },
  "cta-section": { title: "Novo título", ctaPrimary: { label: "Call to action", href: "#" } },
  faq: { items: [{ q: "Pergunta frequente?", a: "Resposta para a pergunta." }] },
  contact: {
    title: "Fale com a gente",
    infoItems: [],
    formId: "",
  },
  form: { formId: "" },
  download: { title: "Downloads", items: [] },
  audio: { title: "Faixa", source: "upload", fileAssetId: "", spotifyUrl: "", autoplay: false },
  video: { title: "", source: "upload", fileAssetId: "", youtubeUrl: "", autoplay: false },
  "video-gallery": { items: [{ title: "Vídeo 1", source: "youtube", youtubeUrl: "" }] },
  "social-links": { items: [{ platform: "instagram", href: "" }] },
};
