import { contents, editEvents, wfInitialItems } from "../mocks/content.mocks";
import type { ContentRow, ListContentResponse, ListEditEventsResponse, ListWorkflowItemsResponse } from "../contracts/responses";

const contentStore: ContentRow[] = contents.map(([title, type, lang, author, status, updatedAt, publication, version]) => ({
  title, type, lang, author, status, updatedAt, publication, version,
}));

export const contentService = {
  async listContent(): Promise<ListContentResponse> {
    return contentStore;
  },
  async listEditEvents(): Promise<ListEditEventsResponse> {
    return editEvents;
  },
  async listWorkflowItems(): Promise<ListWorkflowItemsResponse> {
    return wfInitialItems;
  },
};
