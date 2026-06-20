import { kgNodes, kgEdges, wikidevKgNodes, wikidevKgEdges, lokiKgNodes, lokiKgEdges, type KGEdge, type KGNode } from "../mocks/knowledge.mocks";
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
  async markInsightReviewed(_text: string): Promise<void> {
    void _text;
  },
  async resolveOrphan(_id: string, _action: string): Promise<void> {
    void _id;
    void _action;
  },
  async resolveOrphans(_ids: string[]): Promise<void> {
    void _ids;
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

  /** Cria a aresta RELATED_TO ao linkar uma referência inline (`kg-ref`) durante a autoria — Sprint 11, Tarefa C.3. */
  async createEdge(from: string, to: string, verb = "relacionado a"): Promise<void> {
    allEdges.push({ from, to, verb });
  },
};
