import type { KGEdge, KGNode } from "../mocks/knowledge.mocks";

export type ListNodesResponse = KGNode[];
export type ListEdgesResponse = KGEdge[];

export type GraphEdgeApiResponse = {
  id: string;
  tenantId: string;
  productId: string;
  sourceNodeId: string;
  targetNodeId: string;
  edgeType: EdgeType;
  weight?: number | string | null;
  createdAt?: string;
  updatedAt?: string;
};

/**
 * Catálogo fechado de `edgeType` (Sprint 02-gpt, etapa 07) — qualquer valor
 * fora desta lista é rejeitado (400) pelo backend real. `KGEdge.verb` (mock,
 * texto livre em PT-BR para exibição) é um conceito de apresentação distinto
 * deste enum; só arestas criadas via `knowledgeService.createEdge` (fluxo de
 * referência inline `kg-ref`) precisam respeitar este catálogo.
 */
export const EDGE_TYPES = [
  "CONTAINS", "BELONGS_TO", "REFERENCES", "RELATED_TO", "INSPIRED_BY", "USES",
  "IMPLEMENTS", "PUBLISHED_AS", "SUBMITTED_TO", "TAGGED_WITH", "PART_OF", "DEPENDS_ON",
] as const;
export type EdgeType = (typeof EDGE_TYPES)[number];

/**
 * Preview leve de um nó do grafo (Sprint 11, Tarefa C.1) — shape fixo pensado
 * para um tooltip/popover ao passar o mouse, não para a tela de detalhe
 * completa (`GET /graph/nodes/{nodeId}` continua retornando o nó inteiro).
 */
export type GraphNodePreview = {
  id: string;
  label: string;
  type: string;
  summary: string;
  difficulty?: "beginner" | "intermediate" | "advanced";
  thumbnail?: string;
};

export type RelatedNode = { node: KGNode; verb: string; weight: number };
