import { assets, assetTags } from "../mocks/assets.mocks";
import type { AssetSummary, ListAssetsResponse, ListAssetTagsResponse } from "../contracts/responses";

const assetsStore: AssetSummary[] = assets.map(([name, type, size, status, tags, usage, uploadedAt]) => ({
  name, type, size, status, tags, usage, uploadedAt,
}));

export const assetsService = {
  async listAssets(): Promise<ListAssetsResponse> {
    return assetsStore;
  },
  async listTags(): Promise<ListAssetTagsResponse> {
    return assetTags;
  },
};
