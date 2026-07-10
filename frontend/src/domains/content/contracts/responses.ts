import type { WFItem } from "../mocks/content.mocks";

export type DifficultyLevel = "beginner" | "intermediate" | "advanced";

export type ContentRow = {
  /** Identidade real do `Content` (Sprint 15, Tarefa A) — usado para roteamento ao editor dedicado, nunca o `title` em slug improvisado. */
  id: string;
  title: string; type: string; lang: string; author: string;
  status: string; updatedAt: string; publication: string; version: string;
  /** Campos de primeira classe adicionados na Sprint 11 (caso WikiDev) — não são metadata solta, pois a Knowledge Base precisa filtrar/ordenar por dificuldade e mostrar resumo em previews leves (ver `domains/knowledge` `GraphNodePreview`). */
  summary?: string;
  difficultyLevel?: DifficultyLevel;
  /** Corpo em texto rico, podendo conter marcas inline `{{kg-ref:nodeId:Label}}` (Sprint 11, Tarefa C.3). */
  body?: string;
  /** Hierarquia categoria→tópico→artigo da WikiDev (Sprint 11, Tarefa E.2) — não existe como entidade nova, é navegação sobre `Content`. */
  category?: string;
  topic?: string;
  /** Campos extras por `type` de conteúdo (Loki: `isbn`/`pdfUrl`/`musicReferenceId`/... — registrado como Sprint 02 etapa 25, guardado em `metadataJson`). */
  metadata?: Record<string, unknown>;
};

export type ListContentResponse = ContentRow[];
export type ListEditEventsResponse = string[];
export type ListWorkflowItemsResponse = WFItem[];

export type ContentVersionRow = {
  id: string;
  versionLabel: string;
  createdByName: string;
  createdAt: string;
  snapshotJson?: string | null;
};
