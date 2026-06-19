import { kgNodes, kgEdges } from "../mocks/knowledge.mocks";
import type { ListEdgesResponse, ListNodesResponse } from "../contracts/responses";

export const knowledgeService = {
  async listNodes(): Promise<ListNodesResponse> {
    return kgNodes;
  },
  async listEdges(): Promise<ListEdgesResponse> {
    return kgEdges;
  },
};
