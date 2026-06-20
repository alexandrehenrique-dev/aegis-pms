import { kgNodes, kgEdges } from "../mocks/knowledge.mocks";
import type { ListEdgesResponse, ListNodesResponse } from "../contracts/responses";

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
};
