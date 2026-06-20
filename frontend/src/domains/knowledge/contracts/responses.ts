import type { KGEdge, KGNode } from "../mocks/knowledge.mocks";

export type ListNodesResponse = KGNode[];
export type ListEdgesResponse = KGEdge[];

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
