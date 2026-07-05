import { kgNodes, kgEdges, wikidevKgNodes, wikidevKgEdges, lokiKgNodes, lokiKgEdges, type KGEdge, type KGEntityType, type KGNode } from "../mocks/knowledge.mocks";
import { products as productsStore } from "../../products/mocks/products.mocks";
import { logApiCall } from "../../../shared/services/devLog";
import { IS_API_MODE } from "../../../infra/apiMode";
import { apiClient } from "../../../shared/services/apiClient";
import { slugify } from "../../../shared/utils/slugify";
import type { EdgeType, GraphNodePreview, ListEdgesResponse, ListNodesResponse, RelatedNode } from "../contracts/responses";

// Store em memória só para a sessão do navegador — ver nota equivalente em
// domains/products/services/productsService.ts. Os 3 conjuntos (Maestro
// Beton, WikiDev, Loki) convivem aqui porque `getNodePreview`/`createEdge`
// precisam resolver nós de qualquer produto (caso WikiDev, Tarefa C).
const PRODUCT_NODE_SEEDS: { slug: string; nodes: KGNode[]; edges: KGEdge[] }[] = [
  { slug: "maestro-beton", nodes: kgNodes, edges: kgEdges },
  { slug: "wikidev", nodes: wikidevKgNodes, edges: wikidevKgEdges },
  { slug: "loki", nodes: lokiKgNodes, edges: lokiKgEdges },
];

const allNodes: KGNode[] = PRODUCT_NODE_SEEDS.flatMap((g) => g.nodes);
const allEdges: KGEdge[] = PRODUCT_NODE_SEEDS.flatMap((g) => g.edges);

/**
 * Produto-dono de cada nó (ADR-0016: uma edge só conecta nós do mesmo
 * produto, nunca cross-produto) — populado a partir dos 3 conjuntos de seed
 * acima e atualizado por `ensureNodeForContent` para nós criados em runtime.
 */
const nodeProductSlug = new Map<string, string>(PRODUCT_NODE_SEEDS.flatMap((g) => g.nodes.map((n) => [n.id, g.slug] as const)));

function productSlugFromId(productId: string): string | undefined {
  const product = productsStore.find((p) => p.id === productId);
  return product ? slugify(product.name) : undefined;
}

function productNodes(productId: string): KGNode[] {
  const productSlug = productSlugFromId(productId);
  return allNodes.filter((n) => !productSlug || nodeProductSlug.get(n.id) === productSlug);
}

function productEdges(productId: string): KGEdge[] {
  const nodeIds = new Set(productNodes(productId).map((n) => n.id));
  return allEdges.filter((e) => nodeIds.has(e.from) && nodeIds.has(e.to));
}

function warnMissingEndpoint(method: string, path: string) {
  if (!import.meta.env.PROD) console.warn(`knowledgeService: backend ainda não expõe ${method} ${path}; usando mock local.`);
}

type GraphRelatedApiResponse = {
  node: KGNode;
  edge: { edgeType?: string; weight?: number | string | null };
};

function mapRelatedApiResponse(row: GraphRelatedApiResponse): RelatedNode {
  return {
    node: row.node,
    verb: row.edge.edgeType ?? "RELATED_TO",
    weight: Number(row.edge.weight ?? 0),
  };
}

export const knowledgeService = {
  async listNodes(productId: string): Promise<ListNodesResponse> {
    if (IS_API_MODE) return apiClient.get<ListNodesResponse>(`/products/${productId}/graph/nodes`);
    return productNodes(productId);
  },
  async listEdges(productId: string): Promise<ListEdgesResponse> {
    if (IS_API_MODE) warnMissingEndpoint("GET", `/api/v1/products/${productId}/graph/edges`);
    return productEdges(productId);
  },
  // Pontos de integração real — sem endpoint formalizado ainda em
  // docs/trace/00_endpoints_esperados.md (só o GET de orphans existe, Seção B.5);
  // path inferido por convenção REST sobre o recurso já documentado.
  async markInsightReviewed(productId: string, text: string): Promise<void> {
    if (IS_API_MODE) return apiClient.post(`/products/${productId}/graph/insights/review`, { text });
    logApiCall("POST", `/api/v1/products/${productId}/graph/insights/review`, { text });
  },
  async resolveOrphan(productId: string, id: string, action: string): Promise<void> {
    if (IS_API_MODE) return apiClient.post(`/products/${productId}/graph/orphans/${id}/resolve`, { action });
    logApiCall("POST", `/api/v1/products/${productId}/graph/orphans/${id}/resolve`, { action });
  },
  async resolveOrphans(productId: string, ids: string[]): Promise<void> {
    if (IS_API_MODE) return apiClient.post(`/products/${productId}/graph/orphans/resolve`, { ids });
    logApiCall("POST", `/api/v1/products/${productId}/graph/orphans/resolve`, { ids });
  },

  /** Preview leve para tooltip de referência inline (`kg-ref`) — Sprint 11, Tarefa C.1/C.4. */
  async getNodePreview(productId: string, nodeId: string): Promise<GraphNodePreview | undefined> {
    if (IS_API_MODE) return apiClient.get<GraphNodePreview>(`/products/${productId}/graph/nodes/${nodeId}/preview`);
    const node = allNodes.find((n) => n.id === nodeId);
    if (!node) return undefined;
    return { id: node.id, label: node.label, type: node.type, summary: node.summary ?? "", difficulty: node.difficulty, thumbnail: node.thumbnail };
  },

  /** Nós relacionados a um nó, ordenados por `weight` desc — Sprint 11, Tarefa C.5 (já é `GET /graph/nodes/{id}/related` no backend, etapa 07). */
  async listRelated(productId: string, nodeId: string): Promise<RelatedNode[]> {
    if (IS_API_MODE) {
      const rows = await apiClient.get<GraphRelatedApiResponse[]>(`/products/${productId}/graph/nodes/${nodeId}/related`);
      return rows.map(mapRelatedApiResponse).sort((a, b) => b.weight - a.weight);
    }
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

  /** Busca por label entre os nós de um produto (ADR-0016: uma edge nunca conecta nós de produtos diferentes). */
  async searchNodes(query: string, productId: string): Promise<KGNode[]> {
    if (IS_API_MODE) return apiClient.get<KGNode[]>(`/products/${productId}/graph/nodes?q=${encodeURIComponent(query)}`);
    const productSlug = productSlugFromId(productId);
    const q = query.toLowerCase();
    return allNodes
      .filter((n) => !productSlug || nodeProductSlug.get(n.id) === productSlug)
      .filter((n) => !q || (n.label + n.type).toLowerCase().includes(q));
  },

  /**
   * Garante que existe um `GraphNode` para o conteúdo de origem antes de
   * criar uma aresta a partir dele (Sprint 12, Tarefa H.2) — sem isto,
   * `createEdge` referenciaria um `sourceNodeId` inexistente e o backend
   * real rejeitaria com 404 ("Edge exige sourceNodeId e targetNodeId
   * existentes", etapa 07). Cria o nó automaticamente se ainda não existir.
   * `productSlug` marca o produto-dono do nó (idempotente mesmo se o nó já
   * existir) — é o que permite `createEdge` aplicar a regra de mesmo produto.
   */
  async ensureNodeForContent(productId: string, nodeId: string, label: string, type: KGEntityType = "Página"): Promise<void> {
    if (IS_API_MODE) return apiClient.post(`/products/${productId}/graph/nodes`, { refId: nodeId, refType: "CONTENT", label, nodeType: "PAGE" });
    const productSlug = productSlugFromId(productId) ?? productId;
    nodeProductSlug.set(nodeId, productSlug);
    if (allNodes.some((n) => n.id === nodeId)) return;
    logApiCall("POST", `/api/v1/products/${productId}/graph/nodes`, { id: nodeId, label, type });
    allNodes.push({ id: nodeId, label, type, status: "ativo", x: 0, y: 0, props: [] });
  },

  /**
   * Cria uma aresta do catálogo fechado de `edgeType` ao linkar uma
   * referência inline (`kg-ref`) durante a autoria — Sprint 11, Tarefa C.3 /
   * Sprint 12, Tarefa H.1 (docs/trace, Seção A: `POST .../graph/edges`).
   *
   * Validações deliberadamente mais permissivas que o backend real (etapa
   * 07, Seção C.1, que rejeitaria com 400) porque o mock não tem validação
   * de transação: não bloqueia o salvamento do conteúdo, só não cria a edge
   * e loga um aviso — (1) nó de destino inexistente, (2) nó de destino de
   * outro produto (ADR-0016: edge nunca cruza produto), (3) edge idêntica já
   * existente (evita duplicar ao salvar o mesmo conteúdo sem mudar as refs).
   */
  async createEdge(productId: string, from: string, to: string, edgeType: EdgeType = "RELATED_TO"): Promise<void> {
    if (IS_API_MODE) return apiClient.post(`/products/${productId}/graph/edges`, { sourceNodeId: from, targetNodeId: to, edgeType });
    const productSlug = productSlugFromId(productId) ?? productId;
    if (!allNodes.some((n) => n.id === to)) {
      if (!import.meta.env.PROD) console.warn(`knowledgeService.createEdge: nó de destino "${to}" não existe — edge não criada.`);
      return;
    }
    if (nodeProductSlug.get(to) !== productSlug) {
      if (!import.meta.env.PROD) console.warn(`knowledgeService.createEdge: nó de destino "${to}" pertence a outro produto — Knowledge Graph não conecta conteúdos de produtos diferentes (ADR-0016). Edge não criada.`);
      return;
    }
    if (allEdges.some((e) => e.from === from && e.to === to && e.verb === edgeType)) return;
    logApiCall("POST", `/api/v1/products/${productId}/graph/edges`, { from, to, edgeType });
    allEdges.push({ from, to, verb: edgeType });
  },
};
