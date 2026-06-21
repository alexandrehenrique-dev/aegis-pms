import { contentByProduct, contents, editEvents, wfInitialItems } from "../mocks/content.mocks";
import { logApiCall } from "../../../shared/services/devLog";
import { slugify } from "../../../shared/utils/slugify";
import type { ContentRow, ListContentResponse, ListEditEventsResponse, ListWorkflowItemsResponse } from "../contracts/responses";

const contentStore: ContentRow[] = contents.map(([title, type, lang, author, status, updatedAt, publication, version]) => ({
  id: slugify(title), title, type, lang, author, status, updatedAt, publication, version,
}));

// Todo o conteúdo por produto entra também no store genérico — `getContent`/
// `updateContent` resolvem por `id` independente de produto, então precisam
// de uma única lista combinada (Sprint 15, Tarefa A).
for (const rows of Object.values(contentByProduct)) contentStore.push(...rows);

export const contentService = {
  async listContent(): Promise<ListContentResponse> {
    return contentStore;
  },

  /** Conteúdo por produto (Sprint 11, Tarefa E.2) — produtos sem entrada aqui ainda usam o mock genérico de `contentStore`. */
  async listContentByProduct(productSlug: string): Promise<ListContentResponse> {
    return contentByProduct[productSlug] ?? [];
  },

  /** Busca um `Content` por id real (Sprint 15, Tarefa A) — usado pelo editor dedicado, nunca por slug de título improvisado. */
  async getContent(id: string): Promise<ContentRow | undefined> {
    return contentStore.find((c) => c.id === id);
  },

  /** Atualiza campos do `Content` (título, corpo, metadata por tipo) — mutação em memória, mesma estratégia de `productsService`/`pagesService`. */
  async updateContent(id: string, patch: Partial<ContentRow>): Promise<ContentRow | undefined> {
    logApiCall("PATCH", `/api/v1/products/{productId}/content/${id}`, patch);
    const row = contentStore.find((c) => c.id === id);
    if (!row) return undefined;
    Object.assign(row, patch);
    return row;
  },

  async listEditEvents(): Promise<ListEditEventsResponse> {
    return editEvents;
  },
  async listWorkflowItems(): Promise<ListWorkflowItemsResponse> {
    return wfInitialItems;
  },

  /** Cria um artigo do domínio `content` (Sprint 12, Tarefa B) — distinto de `pagesService.createPage`, que cria uma `Page` institucional. */
  async createContent(payload: { title: string; type: string; lang: string; author: string; metadata?: Record<string, unknown> }): Promise<ContentRow> {
    logApiCall("POST", "/api/v1/products/{productId}/content", payload);
    const created: ContentRow = { id: `${slugify(payload.title)}-${Date.now().toString(36)}`, ...payload, status: "Draft", updatedAt: "agora", publication: "—", version: "v1" };
    contentStore.unshift(created);
    return created;
  },
  // Pontos de integração real (Sprint 07) — docs/trace/00_endpoints_esperados.md, Seção B.1.
  async restoreVersion(version: string): Promise<void> {
    logApiCall("POST", "/api/v1/products/{productId}/content/{contentId}/versions/{version}/restore", { version });
  },
  async submitForReview(id?: string): Promise<void> {
    logApiCall("POST", `/api/v1/products/{productId}/content/${id ?? "{contentId}"}/transition`, { from: "Draft", to: "In Review" });
    const row = id ? contentStore.find((c) => c.id === id) : undefined;
    if (row) row.status = "In Review";
  },
  async publish(): Promise<void> {
    logApiCall("POST", "/api/v1/products/{productId}/content/{contentId}/transition", { from: "In Review", to: "Published" });
  },
  async archive(): Promise<void> {
    logApiCall("POST", "/api/v1/products/{productId}/content/{contentId}/transition", { from: "Published", to: "Archived" });
  },
  async saveDraft(): Promise<void> {
    logApiCall("PUT", "/api/v1/products/{productId}/content/{contentId}");
  },
  async schedulePublish(): Promise<void> {
    logApiCall("POST", "/api/v1/products/{productId}/content/{contentId}/transition", { from: "In Review", to: "Published", scheduled: true });
  },
};
