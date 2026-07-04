# Sprint de Integração 07 — Knowledge Graph e Analytics

> **Pré-requisito:** Sprint 04 concluída (produto ativo com UUID real, content service integrado).
>
> **Foco:** substituir todos os placeholders literais `{productId}` no `knowledgeService` e `analyticsService`; integrar a busca de nós e as métricas ao backend real.
>
> **Branch:** `integration/07-knowledge-graph-analytics`

---

## A. `knowledgeService.ts` — eliminar todos os placeholders literais

A auditoria encontrou `{productId}` literal em 8 métodos. Cada um deve receber `productId: string` como primeiro argumento:

### A.1 — `listNodes(productId)` e `listEdges(productId)`

```ts
// ANTES (literal):
async listNodes() { return apiClient.get('/products/{productId}/graph/nodes'); }
async listEdges() { return apiClient.get('/products/{productId}/graph/edges'); }

// DEPOIS:
async listNodes(productId: string): Promise<ListNodesResponse> {
  if (IS_API_MODE) return apiClient.get<ListNodesResponse>(`/products/${productId}/graph/nodes`);
  return allNodes;
}
async listEdges(productId: string): Promise<ListEdgesResponse> {
  if (IS_API_MODE) return apiClient.get<ListEdgesResponse>(`/products/${productId}/graph/edges`);
  return allEdges;
}
```

Atualizar todos os chamadores (`GraphCanvasView`, `GraphInspector`, `EntitySearch` etc.) para passar `effectiveProduct.id`.

### A.2 — `markInsightReviewed(productId, text)`, `resolveOrphan(productId, id, action)`, `resolveOrphans(productId, ids)`

```ts
async markInsightReviewed(productId: string, text: string): Promise<void> {
  if (IS_API_MODE) return apiClient.post(`/products/${productId}/graph/insights/review`, { text });
  logApiCall('POST', `/api/v1/products/${productId}/graph/insights/review`, { text });
}
async resolveOrphan(productId: string, id: string, action: string): Promise<void> {
  if (IS_API_MODE) return apiClient.post(`/products/${productId}/graph/orphans/${id}/resolve`, { action });
  logApiCall('POST', `/api/v1/products/${productId}/graph/orphans/${id}/resolve`, { action });
}
async resolveOrphans(productId: string, ids: string[]): Promise<void> {
  if (IS_API_MODE) return apiClient.post(`/products/${productId}/graph/orphans/resolve`, { ids });
  logApiCall('POST', `/api/v1/products/${productId}/graph/orphans/resolve`, { ids });
}
```

### A.3 — `getNodePreview(productId, nodeId)` — path errado

```ts
// ANTES (path sem prefixo de produto):
async getNodePreview(nodeId: string) { return apiClient.get(`/graph/nodes/${nodeId}/preview`); }

// DEPOIS:
async getNodePreview(productId: string, nodeId: string): Promise<GraphNodePreview | undefined> {
  if (IS_API_MODE) return apiClient.get<GraphNodePreview>(`/products/${productId}/graph/nodes/${nodeId}/preview`);
  const node = allNodes.find(n => n.id === nodeId);
  if (!node) return undefined;
  return { id: node.id, label: node.label, type: node.type, summary: node.summary ?? '', difficulty: node.difficulty, thumbnail: node.thumbnail };
}
```

### A.4 — `searchNodes(productId, query)` — slug → UUID

`searchNodes` já usa `productSlug` em vez de `productId` no path API. Corrigir para usar UUID e manter slug apenas no mock:

```ts
async searchNodes(query: string, productId: string): Promise<KGNode[]> {
  if (IS_API_MODE) return apiClient.get<KGNode[]>(`/products/${productId}/graph/nodes/search?q=${encodeURIComponent(query)}`);
  // mock: lookup de slug a partir do UUID
  const productSlug = productsStore.find(p => p.id === productId)?.key;
  const q = query.toLowerCase();
  return allNodes
    .filter(n => !productSlug || nodeProductSlug.get(n.id) === productSlug)
    .filter(n => !q || (n.label + n.type).toLowerCase().includes(q));
}
```

### A.5 — `ensureNodeForContent(productId, ...)` e `createEdge(productId, ...)` — já corretos

Estes dois métodos já constroem o path com `productSlug` corretamente em modo API. Verificar apenas que os chamadores passam `productId` (UUID) no lugar de `productSlug`. Se `syncKnowledgeGraphRefs` em `contentService.ts` passa o slug, corrigir para passar o UUID do produto.

---

## B. `analyticsService.ts` — eliminar todos os placeholders literais

Todos os 5 métodos têm `{productId}` literal. Adicionar `productId: string` como primeiro argumento:

```ts
export const analyticsService = {
  async listKpis(productId: string): Promise<ListKpisResponse> {
    if (IS_API_MODE) return apiClient.get<ListKpisResponse>(`/products/${productId}/analytics/kpis`);
    return kpisStore;
  },
  async listHealth(productId: string): Promise<ListHealthResponse> {
    if (IS_API_MODE) return apiClient.get<ListHealthResponse>(`/products/${productId}/analytics/health`);
    return healthStore;
  },
  async listChannels(productId: string): Promise<ListChannelsResponse> {
    if (IS_API_MODE) return apiClient.get<ListChannelsResponse>(`/products/${productId}/analytics/channels`);
    return channelsStore;
  },
  async generateReport(productId: string, name: string): Promise<void> {
    if (IS_API_MODE) return apiClient.post(`/products/${productId}/analytics/reports`, { name });
    logApiCall('POST', `/api/v1/products/${productId}/analytics/reports`, { name });
  },
  async markTrendReviewed(productId: string, label: string): Promise<void> {
    if (IS_API_MODE) return apiClient.post(`/products/${productId}/analytics/trends/review`, { label });
    logApiCall('POST', `/api/v1/products/${productId}/analytics/trends/review`, { label });
  },
  async generateActionPlan(productId: string): Promise<void> {
    if (IS_API_MODE) return apiClient.post(`/products/${productId}/analytics/action-plan`);
    logApiCall('POST', `/api/v1/products/${productId}/analytics/action-plan`);
  },
};
```

Atualizar todos os chamadores (telas de analytics) para passar `effectiveProduct.id`.

---

## C. Gaps de backend para KG — verificar antes de integrar

Antes de integrar, confirmar que estes endpoints existem no backend (etapa 07):

| Endpoint | Existe? | Ação |
|---|---|---|
| `GET /products/{id}/graph/nodes` | Verificar | Integrar se existe |
| `GET /products/{id}/graph/nodes/search?q=` | Verificar | Retrofit se não existe |
| `GET /products/{id}/graph/edges` | Verificar | Integrar se existe |
| `POST /products/{id}/graph/nodes` | Verificar | Retrofit se não existe |
| `POST /products/{id}/graph/edges` | Verificar | Integrar se existe |
| `GET /products/{id}/graph/nodes/{id}/preview` | Verificar | Retrofit se não existe |
| `GET /graph/nodes/{id}/related` | Verificar | Endpoint correto é com prefixo produto? |
| `GET /products/{id}/graph/orphans` | Verificar | Integrar se existe |
| `POST /products/{id}/graph/orphans/{id}/resolve` | Verificar | Integrar se existe |
| `GET /products/{id}/analytics/kpis` | Verificar | Provavelmente não existe — analytics é stub |

Para endpoints que **não existem**: manter mock em modo API (guard com `console.warn`) e registrar como retrofit pendente.

---

## D. `listRelated(nodeId)` — adicionar productId

```ts
async listRelated(productId: string, nodeId: string): Promise<RelatedNode[]> {
  if (IS_API_MODE) return apiClient.get<RelatedNode[]>(`/products/${productId}/graph/nodes/${nodeId}/related`);
  return allEdges
    .filter(e => e.from === nodeId || e.to === nodeId)
    .map(e => {
      const otherId = e.from === nodeId ? e.to : e.from;
      const node = allNodes.find(n => n.id === otherId);
      return node ? { node, verb: e.verb, weight: e.weight ?? 0 } : null;
    })
    .filter((x): x is RelatedNode => x !== null)
    .sort((a, b) => b.weight - a.weight);
}
```

---

## E. Analytics — telas estáticas vs dinâmicas

A maioria das telas de analytics usa dados estáticos/mock. Após integrar o `analyticsService`, as telas que chamam `analyticsService.listKpis()` etc. passarão a exibir dados reais se o backend os tiver. Se o backend de analytics não foi implementado (só stub), manter mock e documentar o desvio.

Não bloquear o deploy por analytics — dados de analytics são informativos, não críticos para operação.

---

## F. Critérios de aceite

- [ ] Nenhum placeholder `{productId}` restante em `knowledgeService.ts`.
- [ ] Nenhum placeholder `{productId}` restante em `analyticsService.ts`.
- [ ] `knowledgeService.getNodePreview()` chama `/products/{UUID}/graph/nodes/{nodeId}/preview`.
- [ ] `knowledgeService.searchNodes()` chama `/products/{UUID}/graph/nodes/search?q=`.
- [ ] Tooltip de `kg-ref` funciona em modo API (chama o endpoint real e exibe o preview).
- [ ] `analyticsService.listKpis(productId)` chamado com UUID real — se o backend tem dados, exibe; se não tem, mock continua.
- [ ] Todos os chamadores de KG e analytics passam `effectiveProduct.id` (UUID).
- [ ] Modo mock não regrediu — grafo de conhecimento continua visualizável.
- [ ] `npm run typecheck` — zero erros.

---

## G. Commit sugerido

```bash
git add frontend/src/domains/knowledge/ frontend/src/domains/analytics/
git commit -m "feat(integration): knowledge graph e analytics com productId real; zero placeholders literais"
```
