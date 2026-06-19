export const contents = [
  ["Home", "Página", "PT-BR", "Marina Costa", "Published", "há 12 min", "Publicado", "v18"],
  ["Sobre o Maestro", "Página", "PT-BR", "Rafael Lima", "In Review", "há 1 h", "Agendado", "v7"],
  ["Apresentações", "Seção", "EN-US", "Ana Nunes", "Draft", "há 3 h", "—", "v3"],
  ["Galeria", "Bloco reutilizável", "PT-BR", "Marina Costa", "In Review", "ontem", "—", "v11"],
  ["Depoimentos", "Seção", "ES-ES", "João Alves", "Published", "2 dias", "Publicado", "v5"],
  ["Disponibilidade", "Landing", "PT-BR", "Rafael Lima", "Draft", "3 dias", "—", "v2"],
  ["Contato", "Página", "PT-BR", "Ana Nunes", "Archived", "12 dias", "Arquivado", "v14"],
  ["Agenda", "Artigo", "PT-BR", "Marina Costa", "Draft", "agora", "—", "v1"],
  ["Orçamento", "Página", "PT-BR", "João Alves", "Published", "5 dias", "Publicado", "v9"],
];

export const editEvents = [
  "Editor criou rascunho da página Agenda.",
  "Product Manager aprovou página Home.",
  "Sistema gerou nova versão do conteúdo Sobre.",
  "Editor enviou Galeria para revisão.",
];

export type WFStatus = "Draft" | "In Review" | "Published" | "Archived";
export type WFItem = { id: string; title: string; type: string; lang: string; author: string; status: WFStatus; version: string };
export type WFEvent = { id: string; text: string; from: WFStatus; to: WFStatus; time: string; comment: string };
export type PendingDrop = { item: WFItem; from: WFStatus; to: WFStatus };

export const WF_DRAG = "WF_CARD";

export const wfInitialItems: WFItem[] = [
  { id: "w1", title: "Home", type: "Página", lang: "PT-BR", author: "Marina Costa", status: "Published", version: "v18" },
  { id: "w2", title: "Sobre o Maestro", type: "Página", lang: "PT-BR", author: "Rafael Lima", status: "In Review", version: "v7" },
  { id: "w3", title: "Apresentações", type: "Seção", lang: "EN-US", author: "Ana Nunes", status: "Draft", version: "v3" },
  { id: "w4", title: "Galeria", type: "Bloco", lang: "PT-BR", author: "Marina Costa", status: "In Review", version: "v11" },
  { id: "w5", title: "Depoimentos", type: "Seção", lang: "ES-ES", author: "João Alves", status: "Published", version: "v5" },
  { id: "w6", title: "Disponibilidade", type: "Landing", lang: "PT-BR", author: "Rafael Lima", status: "Draft", version: "v2" },
  { id: "w7", title: "Contato", type: "Página", lang: "PT-BR", author: "Ana Nunes", status: "Archived", version: "v14" },
  { id: "w8", title: "Agenda", type: "Artigo", lang: "PT-BR", author: "Marina Costa", status: "Draft", version: "v1" },
];

export const wfLaneBorder: Record<WFStatus, string> = { Draft: "border-t-muted-foreground/25", "In Review": "border-t-[#b45309]", Published: "border-t-primary", Archived: "border-t-muted-foreground/15" };
export const wfBadgeTone: Record<WFStatus, "neutral" | "amber" | "violet"> = { Draft: "neutral", "In Review": "amber", Published: "violet", Archived: "neutral" };

export const wfAllowed: Record<WFStatus, WFStatus[]> = {
  Draft: ["In Review"],
  "In Review": ["Draft", "Published"],
  Published: ["Archived"],
  Archived: ["Draft"],
};

export const wfImpact: Partial<Record<WFStatus, string>> = {
  "In Review": "O conteúdo aguardará revisão de um Product Manager antes de ser publicado.",
  Published: "O conteúdo ficará visível publicamente. Esta ação envia notificações à equipe.",
  Archived: "O conteúdo será arquivado e removido da listagem pública. Gera evento de auditoria.",
  Draft: "O conteúdo retorna como rascunho com nova versão. Publicação anterior é mantida no histórico.",
};
