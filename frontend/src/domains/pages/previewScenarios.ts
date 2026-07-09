import type { Section } from "./contracts/responses";

function section(id: string, order: number, type: Section["type"], label: string, content: Record<string, unknown>): Section {
  return { id, order, type, label, content };
}

export const COMPONENT_LAB_PREVIEW_ID = "componentes";

export const componentLabSections: Section[] = [
  section("lab-hero", 1, "hero", "Hero", {
    title: "Preview completo de componentes",
    subtitle: "Cenário visual para validar todos os blocos, estados mistos e responsividade.",
    theme: "dark",
    image: { src: "", alt: "Imagem hero de exemplo" },
    ctas: [{ label: "Ação principal", href: "#" }, { label: "Ação secundária", href: "#" }],
  }),
  section("lab-text", 2, "text", "Texto", {
    title: "Texto editorial",
    body: "Bloco com **markdown**, links e conteúdo corrido para validar leitura em previews claros.",
  }),
  section("lab-rich", 3, "rich-text", "Rich text", {
    title: "Rich text",
    body: "Use este bloco para validar parágrafos longos, listas e _ênfases_ em contexto de página.",
  }),
  section("lab-two", 4, "two-column", "Composição mista", {
    left: [
      { type: "text", content: { title: "Coluna com texto", body: "Mini-bloco textual dentro de composição." } },
      { type: "cta", content: { label: "CTA interno", href: "#" } },
    ],
    right: [
      { type: "image", content: { src: "", alt: "Mini imagem em coluna" } },
      { type: "rich-text", content: { title: "Texto complementar", body: "Segundo mini-bloco para testar empilhamento." } },
    ],
  }),
  section("lab-image", 5, "image", "Imagem", { src: "", alt: "Imagem isolada" }),
  section("lab-image-text", 6, "image-text", "Imagem e texto", {
    title: "Imagem com texto",
    body: "Layout lado a lado com markdown e mídia autenticada.",
    image: { src: "", alt: "Imagem lateral" },
  }),
  section("lab-feature", 7, "feature-grid", "Feature grid", {
    title: "Recursos",
    items: [
      { title: "Governança", desc: "Permissões por papel e produto." },
      { title: "Conteúdo", desc: "Fluxo editorial com rastreio." },
      { title: "Assets", desc: "Mídia centralizada por produto." },
    ],
  }),
  section("lab-cards", 8, "card-list", "Card list", {
    title: "Lista de cards",
    items: [
      { title: "Card A", desc: "Resumo curto do primeiro card." },
      { title: "Card B", desc: "Resumo curto do segundo card." },
      { title: "Card C", desc: "Resumo curto do terceiro card." },
    ],
  }),
  section("lab-gallery", 9, "gallery", "Galeria", {
    title: "Galeria de mídia",
    items: [
      { src: "", alt: "Imagem destaque" },
      { src: "", alt: "Imagem secundária 1" },
      { src: "", alt: "Imagem secundária 2" },
      { src: "", alt: "Imagem secundária 3" },
      { src: "", alt: "Imagem secundária 4" },
    ],
  }),
  section("lab-timeline", 10, "timeline", "Timeline", {
    items: [
      { date: "08 jul", title: "Rascunho criado", desc: "Primeira versão da página." },
      { date: "09 jul", title: "Revisão", desc: "Ajustes editoriais e visuais." },
    ],
  }),
  section("lab-events", 11, "event-list", "Eventos", {
    title: "Agenda",
    source: "auto",
    selectedEventIds: ["lab-event-1", "lab-event-2"],
    selectedEvents: [
      { id: "lab-event-1", title: "Ensaio aberto", date: "2026-07-10T19:00:00", location: "Auditório principal", type: "public" },
      { id: "lab-event-2", title: "Apresentação corporativa", date: "2026-07-12T20:00:00", location: "Teatro central", type: "private" },
    ],
  }),
  section("lab-cta", 12, "cta-section", "CTA", {
    title: "Pronto para publicar?",
    ctaPrimary: { label: "Enviar para revisão", href: "#" },
  }),
  section("lab-faq", 13, "faq", "FAQ", {
    title: "Perguntas frequentes",
    items: [
      { q: "O preview usa dados reais?", a: "Quando há backend, sim. Este cenário usa dados controlados para QA visual." },
      { q: "A galeria aceita muitos itens?", a: "Sim, o layout se adapta e mantém foco visual." },
    ],
  }),
  section("lab-contact", 14, "contact", "Contato", { title: "Fale com a equipe", formId: "form-demo" }),
  section("lab-form", 15, "form", "Formulário", { formId: "form-demo" }),
  section("lab-download", 16, "download", "Downloads", {
    title: "Materiais",
    items: [{ title: "Baixar apresentação", fileAssetId: "", fileType: "pdf" }],
  }),
  section("lab-audio", 17, "audio", "Áudio", {
    title: "Ouça o resumo",
    source: "upload",
    fileAssetId: "",
    autoplay: false,
  }),
  section("lab-video", 18, "video", "Vídeo", {
    title: "Vídeo individual",
    source: "youtube",
    youtubeUrl: "https://youtu.be/opQ5NfaOKTQ?si=egJgbCBkOAyXGmAq",
    autoplay: false,
  }),
  section("lab-video-gallery", 19, "video-gallery", "Galeria de vídeos", {
    title: "Vídeos em destaque",
    items: [
      { title: "Share curto com query", source: "youtube", youtubeUrl: "https://youtu.be/opQ5NfaOKTQ?si=egJgbCBkOAyXGmAq", fileAssetId: "" },
      { title: "URL watch com parâmetros", source: "youtube", youtubeUrl: "https://www.youtube.com/watch?si=egJgbCBkOAyXGmAq&v=opQ5NfaOKTQ", fileAssetId: "" },
      { title: "Vídeo dos assets", source: "upload", fileAssetId: "", youtubeUrl: "" },
    ],
  }),
  section("lab-social", 20, "social-links", "Redes sociais", {
    title: "Siga o projeto",
    items: [
      { platform: "Instagram", href: "https://instagram.com" },
      { platform: "YouTube", href: "https://youtube.com" },
      { platform: "LinkedIn", href: "https://linkedin.com" },
    ],
  }),
];
