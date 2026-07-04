import { contentByProduct, contents, editEvents, wfInitialItems } from "../mocks/content.mocks";
import { logApiCall } from "../../../shared/services/devLog";
import { slugify } from "../../../shared/utils/slugify";
import { resolveEnabledModules } from "../../../core/products/moduleDefaults";
import { knowledgeService } from "../../knowledge/services/knowledgeService";
import { IS_API_MODE } from "../../../infra/apiMode";
import { apiClient } from "../../../shared/services/apiClient";
import { requireCurrentProductId } from "../../../core/products/currentProductContext";
import type { ContentRow, ListContentResponse, ListEditEventsResponse, ListWorkflowItemsResponse } from "../contracts/responses";

/** Produto efetivo da sessão, no formato mínimo exigido por `resolveEnabledModules` — `null` quando chamado fora de um contexto de produto (ex.: seed inicial). `id` (UUID real, nunca slug — Sprint de Integração 02, Seção A) é o que identifica o produto nas chamadas ao Knowledge Graph. */
type ContentProductContext = { id: string; name: string; type: string; modulesList?: string[] } | null;

const KG_REF_PATTERN = /\{\{kg-ref:([\w-]+):([^}]+)\}\}/g;

/**
 * Sprint 16, Tarefa B — fecha o ciclo que deixava `knowledgeService.createEdge`/
 * `ensureNodeForContent` como código morto: ao salvar um `Content` com pelo
 * menos uma referência `{{kg-ref:nodeId:Label}}` no corpo, garante o nó de
 * origem (o próprio conteúdo) e cria a edge para cada referência encontrada.
 *
 * Module-gating (ADR-0015): mock do equivalente a `@RequireModule(KNOWLEDGE_GRAPH)`
 * que o backend real (etapa 07) aplicará no endpoint — sem o módulo
 * habilitado no produto, ignora silenciosamente (o `{{kg-ref}}` fica como
 * texto bruto no corpo, sem node/edge criado).
 */
async function syncKnowledgeGraphRefs(content: ContentRow, product: ContentProductContext): Promise<void> {
  if (typeof content.body !== "string") return;
  if (!resolveEnabledModules(product).includes("Knowledge Graph")) return;
  const matches = [...content.body.matchAll(KG_REF_PATTERN)];
  if (matches.length === 0) return;
  const productId = product ? product.id : "maestro-beton";
  await knowledgeService.ensureNodeForContent(content.id, content.title, productId, "Página");
  for (const [, nodeId] of matches) {
    await knowledgeService.createEdge(content.id, nodeId, productId, "RELATED_TO");
  }
}

const contentStore: ContentRow[] = contents.map(([title, type, lang, author, status, updatedAt, publication, version]) => ({
  id: slugify(title), title, type, lang, author, status, updatedAt, publication, version,
}));

// Todo o conteúdo por produto entra também no store genérico — `getContent`/
// `updateContent` resolvem por `id` independente de produto, então precisam
// de uma única lista combinada (Sprint 15, Tarefa A).
for (const rows of Object.values(contentByProduct)) contentStore.push(...rows);

export const contentService = {
  async listContent(): Promise<ListContentResponse> {
    if (IS_API_MODE) return apiClient.get<ListContentResponse>(`/products/${requireCurrentProductId()}/content`);
    return contentStore;
  },

  /** Conteúdo por produto (Sprint 11, Tarefa E.2) — produtos sem entrada aqui ainda usam o mock genérico de `contentStore`. `productId` é o UUID real (Sprint de Integração 02, Seção A), não mais slug. */
  async listContentByProduct(productId: string): Promise<ListContentResponse> {
    if (IS_API_MODE) return apiClient.get<ListContentResponse>(`/products/${productId}/content`);
    return contentByProduct[productId] ?? [];
  },

  /** Busca um `Content` por id real (Sprint 15, Tarefa A) — usado pelo editor dedicado, nunca por slug de título improvisado. */
  async getContent(id: string): Promise<ContentRow | undefined> {
    if (IS_API_MODE) return apiClient.get<ContentRow>(`/products/${requireCurrentProductId()}/content/${id}`);
    return contentStore.find((c) => c.id === id);
  },

  /** Atualiza campos do `Content` (título, corpo, metadata por tipo) — mutação em memória, mesma estratégia de `productsService`/`pagesService`. `product` (produto efetivo da sessão) habilita o parsing de `kg-ref` no corpo (Sprint 16, Tarefa B) quando o patch alterar `body`. */
  async updateContent(id: string, patch: Partial<ContentRow>, product: ContentProductContext = null): Promise<ContentRow | undefined> {
    if (IS_API_MODE) return apiClient.put<ContentRow>(`/products/${requireCurrentProductId()}/content/${id}`, patch);
    logApiCall("PATCH", `/api/v1/products/{productId}/content/${id}`, patch);
    const row = contentStore.find((c) => c.id === id);
    if (!row) return undefined;
    Object.assign(row, patch);
    if (typeof patch.body === "string") await syncKnowledgeGraphRefs(row, product);
    return row;
  },

  async listEditEvents(): Promise<ListEditEventsResponse> {
    if (IS_API_MODE) return apiClient.get<ListEditEventsResponse>(`/products/${requireCurrentProductId()}/content/edit-events`);
    return editEvents;
  },
  async listWorkflowItems(): Promise<ListWorkflowItemsResponse> {
    if (IS_API_MODE) return apiClient.get<ListWorkflowItemsResponse>(`/products/${requireCurrentProductId()}/content/workflow-items`);
    return wfInitialItems;
  },

  /** Cria um artigo do domínio `content` (Sprint 12, Tarefa B) — distinto de `pagesService.createPage`, que cria uma `Page` institucional. `product` segue o mesmo papel de `updateContent` (Sprint 16, Tarefa B). */
  async createContent(payload: { title: string; type: string; lang: string; author: string; body?: string; metadata?: Record<string, unknown> }, product: ContentProductContext = null): Promise<ContentRow> {
    if (IS_API_MODE) return apiClient.post<ContentRow>(`/products/${requireCurrentProductId()}/content`, payload);
    logApiCall("POST", "/api/v1/products/{productId}/content", payload);
    const created: ContentRow = { id: `${slugify(payload.title)}-${Date.now().toString(36)}`, ...payload, status: "Draft", updatedAt: "agora", publication: "—", version: "v1" };
    contentStore.unshift(created);
    if (typeof created.body === "string") await syncKnowledgeGraphRefs(created, product);
    return created;
  },
  /**
   * Pontos de integração real (Sprint 07) — docs/trace/00_endpoints_esperados.md,
   * Seção B.1. `id` (contentId) é obrigatório: as telas que chamam este método
   * (`VersionTimeline`/`VersionCompareView`) ainda são mockups estáticos sem
   * roteamento por conteúdo real (Sprint de Integração 04 conecta isso);
   * "home" replica o `contentId` que essas mesmas telas já usam nas rotas
   * (`navigate("/content/home/...")`).
   */
  async restoreVersion(version: string, id = "home"): Promise<void> {
    if (IS_API_MODE) return apiClient.post(`/products/${requireCurrentProductId()}/content/${id}/versions/${version}/restore`);
    logApiCall("POST", `/api/v1/products/{productId}/content/${id}/versions/${version}/restore`, { version });
  },
  async submitForReview(id?: string): Promise<void> {
    if (IS_API_MODE && id) return apiClient.post(`/products/${requireCurrentProductId()}/content/${id}/transition`, { from: "Draft", to: "In Review" });
    logApiCall("POST", `/api/v1/products/{productId}/content/${id ?? "{contentId}"}/transition`, { from: "Draft", to: "In Review" });
    const row = id ? contentStore.find((c) => c.id === id) : undefined;
    if (row) row.status = "In Review";
  },
  /** Publicar deve mudar o `status` no store (bug fix Sprint 20, Tarefa D.2) — antes só logava a chamada, sem refletir na lista/badge de conteúdo. */
  async publish(id?: string): Promise<void> {
    if (IS_API_MODE && id) return apiClient.post(`/products/${requireCurrentProductId()}/content/${id}/transition`, { from: "In Review", to: "Published" });
    logApiCall("POST", `/api/v1/products/{productId}/content/${id ?? "{contentId}"}/transition`, { from: "In Review", to: "Published" });
    if (id) {
      const row = contentStore.find((c) => c.id === id);
      if (row) row.status = "Published";
    }
  },
  async archive(id?: string): Promise<void> {
    if (IS_API_MODE && id) return apiClient.post(`/products/${requireCurrentProductId()}/content/${id}/transition`, { from: "Published", to: "Archived" });
    logApiCall("POST", `/api/v1/products/{productId}/content/${id ?? "{contentId}"}/transition`, { from: "Published", to: "Archived" });
    if (id) {
      const row = contentStore.find((c) => c.id === id);
      if (row) row.status = "Archived";
    }
  },
  /** Exclusão (bug fix Sprint 20, Tarefa D.2) — antes não existia método de exclusão para `Content`. */
  async deleteContent(id: string): Promise<void> {
    if (IS_API_MODE) return apiClient.delete(`/products/${requireCurrentProductId()}/content/${id}`);
    logApiCall("DELETE", `/api/v1/products/{productId}/content/${id}`);
    const index = contentStore.findIndex((c) => c.id === id);
    if (index >= 0) contentStore.splice(index, 1);
  },
  /** Ver nota de `restoreVersion` sobre `id = "home"` — mesma limitação (tela estática, sem contentId real ainda). */
  async saveDraft(id = "home"): Promise<void> {
    if (IS_API_MODE) return apiClient.put(`/products/${requireCurrentProductId()}/content/${id}`);
    logApiCall("PUT", `/api/v1/products/{productId}/content/${id}`);
  },
  async schedulePublish(id = "home"): Promise<void> {
    if (IS_API_MODE) return apiClient.post(`/products/${requireCurrentProductId()}/content/${id}/transition`, { from: "In Review", to: "Published", scheduled: true });
    logApiCall("POST", `/api/v1/products/{productId}/content/${id}/transition`, { from: "In Review", to: "Published", scheduled: true });
  },
};
