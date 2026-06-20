import { assets, assetTags } from "../mocks/assets.mocks";
import { logApiCall } from "../../../shared/services/devLog";
import type { AssetSummary, ListAssetsResponse, ListAssetTagsResponse } from "../contracts/responses";

const assetsStore: AssetSummary[] = assets.map(([name, type, size, status, tags, usage, uploadedAt]) => ({
  name, type, size, status, tags, usage, uploadedAt,
}));

const assetTagsStore: string[] = [...assetTags];

export const assetsService = {
  async listAssets(): Promise<ListAssetsResponse> {
    return assetsStore;
  },
  async listTags(): Promise<ListAssetTagsResponse> {
    return assetTagsStore;
  },
  async createTag(name: string): Promise<void> {
    logApiCall("POST", "/api/v1/products/{productId}/assets/tags", { name });
    if (!assetTagsStore.includes(name)) assetTagsStore.push(name);
  },
  async renameTag(oldName: string, newName: string): Promise<void> {
    logApiCall("PUT", `/api/v1/products/{productId}/assets/tags/${oldName}`, { newName });
    const i = assetTagsStore.indexOf(oldName);
    if (i >= 0) assetTagsStore[i] = newName;
  },
  async removeTag(name: string): Promise<void> {
    logApiCall("DELETE", `/api/v1/products/{productId}/assets/tags/${name}`);
    const i = assetTagsStore.indexOf(name);
    if (i >= 0) assetTagsStore.splice(i, 1);
  },
  async mergeTags(tagsToMerge: string[], into: string): Promise<void> {
    logApiCall("POST", "/api/v1/products/{productId}/assets/tags/merge", { tagsToMerge, into });
    tagsToMerge.forEach((t) => {
      if (t !== into) {
        const i = assetTagsStore.indexOf(t);
        if (i >= 0) assetTagsStore.splice(i, 1);
      }
    });
    if (!assetTagsStore.includes(into)) assetTagsStore.push(into);
  },
  async saveMetadata(): Promise<void> {
    logApiCall("PUT", "/api/v1/products/{productId}/assets/{assetId}/metadata");
  },
  async archiveAsset(name: string): Promise<void> {
    const a = assetsStore.find((x) => x.name === name);
    if (!a) return;
    logApiCall("POST", `/api/v1/products/{productId}/assets/${name}/archive`);
    a.status = "arquivado";
  },
  async uploadFiles(): Promise<void> {
    logApiCall("POST", "/api/v1/products/{productId}/assets/upload");
  },
  async downloadAsset(name: string): Promise<void> {
    logApiCall("GET", `/api/v1/products/{productId}/assets/${name}/download`);
  },
};
