import type { WFItem } from "../mocks/content.mocks";

export type ContentRow = {
  title: string; type: string; lang: string; author: string;
  status: string; updatedAt: string; publication: string; version: string;
};

export type ListContentResponse = ContentRow[];
export type ListEditEventsResponse = string[];
export type ListWorkflowItemsResponse = WFItem[];
