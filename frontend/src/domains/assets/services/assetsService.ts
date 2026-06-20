import { assets, assetTags } from "../mocks/assets.mocks";
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
    if (!assetTagsStore.includes(name)) assetTagsStore.push(name);
  },
  async renameTag(oldName: string, newName: string): Promise<void> {
    const i = assetTagsStore.indexOf(oldName);
    if (i >= 0) assetTagsStore[i] = newName;
  },
  async removeTag(name: string): Promise<void> {
    const i = assetTagsStore.indexOf(name);
    if (i >= 0) assetTagsStore.splice(i, 1);
  },
  async mergeTags(tagsToMerge: string[], into: string): Promise<void> {
    tagsToMerge.forEach((t) => {
      if (t !== into) {
        const i = assetTagsStore.indexOf(t);
        if (i >= 0) assetTagsStore.splice(i, 1);
      }
    });
    if (!assetTagsStore.includes(into)) assetTagsStore.push(into);
  },
  async saveMetadata(): Promise<void> {},
  async archiveAsset(name: string): Promise<void> {
    const a = assetsStore.find((x) => x.name === name);
    if (a) a.status = "arquivado";
  },
  async uploadFiles(): Promise<void> {},
  async downloadAsset(_name: string): Promise<void> {
    void _name;
  },
};
