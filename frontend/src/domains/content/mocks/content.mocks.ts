import type { ContentRow } from "../contracts/responses";

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

/**
 * Conteúdo fiel aos contratos de negócio reais (Sprint 11, Tarefa E.2) para
 * os produtos cujo gap principal é `content` (artigo/post linear), não
 * `pages` — ver `domains/pages/mocks/pages.mocks.ts` para os demais.
 */
export const contentByProduct: Record<string, ContentRow[]> = {
  "conecta-talentos": [
    { id: "conecta-processo-seletivo-enxuto", title: "Como montar um processo seletivo enxuto", type: "Artigo", lang: "PT-BR", author: "Conecta Talentos", status: "Published", updatedAt: "há 3 dias", publication: "Publicado", version: "v2" },
    { id: "conecta-sinais-consultoria-rh", title: "5 sinais de que sua empresa precisa de consultoria de RH", type: "Artigo", lang: "PT-BR", author: "Conecta Talentos", status: "Published", updatedAt: "há 9 dias", publication: "Publicado", version: "v1" },
    { id: "conecta-diversidade-vantagem-competitiva", title: "Diversidade como vantagem competitiva", type: "Artigo", lang: "PT-BR", author: "Conecta Talentos", status: "Draft", updatedAt: "ontem", publication: "—", version: "v1" },
  ],

  // WikiDev: estrutura categoria → tópico → artigo (Sprint 11, Tarefa E.2).
  // O artigo "Introdução ao Spring Boot" tem summary+difficultyLevel
  // preenchidos e uma referência inline kg-ref no corpo — prova de conceito
  // da Tarefa C (ver domains/knowledge/mocks/knowledge.mocks.ts:wikidevKgNodes).
  wikidev: [
    {
      id: "wikidev-introducao-spring-boot",
      title: "Introdução ao Spring Boot", type: "Artigo", lang: "PT-BR", author: "Equipe WikiDev",
      status: "Published", updatedAt: "há 2 dias", publication: "Publicado", version: "v3",
      category: "Programação", topic: "Java",
      summary: "Visão geral do Spring Boot: convenção sobre configuração, autoconfiguração e starters.",
      difficultyLevel: "beginner",
      body: "Spring Boot é um framework que simplifica a criação de aplicações Spring prontas para produção. Ele se integra com {{kg-ref:node-jpa:JPA}} para persistência de dados e pode ser facilmente empacotado em containers com {{kg-ref:node-docker:Docker}} para deploy em qualquer ambiente.",
    },
    {
      id: "wikidev-jpa-na-pratica",
      title: "JPA na prática", type: "Artigo", lang: "PT-BR", author: "Equipe WikiDev",
      status: "Published", updatedAt: "há 5 dias", publication: "Publicado", version: "v2",
      category: "Programação", topic: "Java",
      summary: "Mapeamento objeto-relacional (ORM) com JPA e Hibernate: entidades, repositórios e consultas.",
      difficultyLevel: "intermediate",
    },
    {
      id: "wikidev-docker-para-desenvolvedores-java",
      title: "Docker para desenvolvedores Java", type: "Artigo", lang: "PT-BR", author: "Equipe WikiDev",
      status: "Draft", updatedAt: "hoje", publication: "—", version: "v1",
      category: "Programação", topic: "Java",
      summary: "Containerização de aplicações Spring Boot: Dockerfile, multi-stage build e boas práticas.",
      difficultyLevel: "beginner",
    },
  ],

  loki: [
    { id: "loki-manifesto-do-silencio", title: "Manifesto do Silêncio", type: "Manifesto", lang: "PT-BR", author: "Loki", status: "Published", updatedAt: "há 1 semana", publication: "Publicado", version: "v1", metadata: { playlistId: "node-playlist-introspeccao" } },
    { id: "loki-manifesto-da-errancia", title: "Manifesto da Errância", type: "Manifesto", lang: "PT-BR", author: "Loki", status: "Published", updatedAt: "há 2 semanas", publication: "Publicado", version: "v1" },
    { id: "loki-sobre-o-esquecimento", title: "Sobre o esquecimento", type: "Reflexão", lang: "PT-BR", author: "Loki", status: "Published", updatedAt: "há 4 dias", publication: "Publicado", version: "v2" },
    { id: "loki-sobre-o-tempo-que-insiste", title: "Sobre o tempo que insiste", type: "Reflexão", lang: "PT-BR", author: "Loki", status: "Draft", updatedAt: "ontem", publication: "—", version: "v1" },
    { id: "loki-vigilia", title: "Vigília", type: "Poema", lang: "PT-BR", author: "Loki", status: "Published", updatedAt: "há 3 dias", publication: "Publicado", version: "v1", metadata: { musicReferenceId: "node-musica-clair" } },
    { id: "loki-fragmento-noturno", title: "Fragmento noturno", type: "Poema", lang: "PT-BR", author: "Loki", status: "Published", updatedAt: "há 6 dias", publication: "Publicado", version: "v1" },
    { id: "loki-insonia", title: "Insônia", type: "Poema", lang: "PT-BR", author: "Loki", status: "Draft", updatedAt: "hoje", publication: "—", version: "v1" },
    {
      id: "loki-fragmentos",
      title: "Fragmentos", type: "Livro", lang: "PT-BR", author: "Loki", status: "Published", updatedAt: "há 1 mês", publication: "Publicado", version: "v1",
      metadata: { isbn: "978-65-00-00000-0", pdfUrl: "/downloads/fragmentos.pdf", epubUrl: "/downloads/fragmentos.epub", physicalAvailable: false },
    },
    {
      id: "loki-introspeccao",
      title: "Introspecção", type: "Playlist", lang: "PT-BR", author: "Loki", status: "Published", updatedAt: "há 2 dias", publication: "Publicado", version: "v1",
      metadata: { trackCount: 14, provider: "Spotify", url: "https://open.spotify.com/playlist/introspeccao" },
    },
  ],
};
