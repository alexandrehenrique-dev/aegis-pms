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
  async restoreVersion(version: string): Promise<void> {
    void version;
  },
  async submitForReview(): Promise<void> {},
  async publish(): Promise<void> {},
  async archive(): Promise<void> {},
  async saveDraft(): Promise<void> {},
  async schedulePublish(): Promise<void> {},
};
