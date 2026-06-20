import { contentByProduct, contents, editEvents, wfInitialItems } from "../mocks/content.mocks";
import type { ContentRow, ListContentResponse, ListEditEventsResponse, ListWorkflowItemsResponse } from "../contracts/responses";

const contentStore: ContentRow[] = contents.map(([title, type, lang, author, status, updatedAt, publication, version]) => ({
  title, type, lang, author, status, updatedAt, publication, version,
}));

export const contentService = {
  async listContent(): Promise<ListContentResponse> {
    return contentStore;
  },

  /** Conteúdo por produto (Sprint 11, Tarefa E.2) — produtos sem entrada aqui ainda usam o mock genérico de `contentStore`. */
  async listContentByProduct(productSlug: string): Promise<ListContentResponse> {
    return contentByProduct[productSlug] ?? [];
  },
  async listEditEvents(): Promise<ListEditEventsResponse> {
    return editEvents;
  },
  async listWorkflowItems(): Promise<ListWorkflowItemsResponse> {
    return wfInitialItems;
  },
  // Pontos de integração real (Sprint 07) — docs/trace/00_endpoints_esperados.md, Seção B.1.
  // {contentId} é placeholder: este service ainda não recebe o id do conteúdo selecionado
  // (ContentEditor/WorkflowPanel chamam estes métodos sem parâmetro hoje).
  async restoreVersion(version: string): Promise<void> {
    console.log("[mock→backend] POST /api/v1/products/{productId}/content/{contentId}/versions/{version}/restore", { version });
  },
  async submitForReview(): Promise<void> {
    console.log("[mock→backend] POST /api/v1/products/{productId}/content/{contentId}/transition", { from: "Draft", to: "In Review" });
  },
  async publish(): Promise<void> {
    console.log("[mock→backend] POST /api/v1/products/{productId}/content/{contentId}/transition", { from: "In Review", to: "Published" });
  },
  async archive(): Promise<void> {
    console.log("[mock→backend] POST /api/v1/products/{productId}/content/{contentId}/transition", { from: "Published", to: "Archived" });
  },
  async saveDraft(): Promise<void> {
    console.log("[mock→backend] PUT /api/v1/products/{productId}/content/{contentId}");
  },
  async schedulePublish(): Promise<void> {
    console.log("[mock→backend] POST /api/v1/products/{productId}/content/{contentId}/transition", { from: "In Review", to: "Published", scheduled: true });
  },
};
