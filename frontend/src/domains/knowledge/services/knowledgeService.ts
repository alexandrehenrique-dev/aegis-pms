import { kgNodes, kgEdges, wikidevKgNodes, wikidevKgEdges, lokiKgNodes, lokiKgEdges, type KGEdge, type KGNode } from "../mocks/knowledge.mocks";
import { logApiCall } from "../../../shared/services/devLog";
import type { GraphNodePreview, ListEdgesResponse, ListNodesResponse, RelatedNode } from "../contracts/responses";

// Store em memória só para a sessão do navegador — ver nota equivalente em
// domains/products/services/productsService.ts. Os 3 conjuntos (Maestro
// Beton, WikiDev, Loki) convivem aqui porque `getNodePreview`/`createEdge`
// precisam resolver nós de qualquer produto (caso WikiDev, Tarefa C).
const allNodes: KGNode[] = [...kgNodes, ...wikidevKgNodes, ...lokiKgNodes];
const allEdges: KGEdge[] = [...kgEdges, ...wikidevKgEdges, ...lokiKgEdges];

export const knowledgeService = {
  async listNodes(): Promise<ListNodesResponse> {
    return kgNodes;
  },
  async listEdges(): Promise<ListEdgesResponse> {
    return kgEdges;
  },
  // Pontos de integração real (Sprint 07) — sem endpoint formalizado ainda em
  // docs/trace/00_endpoints_esperados.md (só o GET de orphans existe, Seção B.5);
  // path inferido por convenção REST sobre o recurso já documentado.
  async markInsightReviewed(text: string): Promise<void> {
    logApiCall("POST", "/api/v1/products/{productId}/graph/insights/review", { text });
  },
  async resolveOrphan(id: string, action: string): Promise<void> {
    logApiCall("POST", `/api/v1/products/{productId}/graph/orphans/${id}/resolve`, { action });
  },
  async resolveOrphans(ids: string[]): Promise<void> {
    logApiCall("POST", "/api/v1/products/{productId}/graph/orphans/resolve", { ids });
  },

  /** Preview leve para tooltip de referência inline (`kg-ref`) — Sprint 11, Tarefa C.1/C.4. */
  async getNodePreview(nodeId: string): Promise<GraphNodePreview | undefined> {
    const node = allNodes.find((n) => n.id === nodeId);
    if (!node) return undefined;
    return { id: node.id, label: node.label, type: node.type, summary: node.summary ?? "", difficulty: node.difficulty, thumbnail: node.thumbnail };
  },

  /** Nós relacionados a um nó, ordenados por `weight` desc — Sprint 11, Tarefa C.5 (já é `GET /graph/nodes/{id}/related` no backend, etapa 07). */
  async listRelated(nodeId: string): Promise<RelatedNode[]> {
    return allEdges
      .filter((e) => e.from === nodeId || e.to === nodeId)
      .map((e) => {
        const otherId = e.from === nodeId ? e.to : e.from;
        const node = allNodes.find((n) => n.id === otherId);
        return node ? { node, verb: e.verb, weight: e.weight ?? 0 } : null;
      })
      .filter((x): x is RelatedNode => x !== null)
      .sort((a, b) => b.weight - a.weight);
  },

  async searchNodes(query: string): Promise<KGNode[]> {
    const q = query.toLowerCase();
    return allNodes.filter((n) => !q || (n.label + n.type).toLowerCase().includes(q));
  },

  /** Cria a aresta RELATED_TO ao linkar uma referência inline (`kg-ref`) durante a autoria — Sprint 11, Tarefa C.3 (docs/trace, Seção A: `POST .../graph/edges`). */
  async createEdge(from: string, to: string, verb = "relacionado a"): Promise<void> {
    console.log("[mock→backend] POST /api/v1/products/{productId}/graph/edges", { from, to, verb });
    allEdges.push({ from, to, verb });
  },
};
