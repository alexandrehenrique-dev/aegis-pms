export type AssetSummary = {
  name: string; type: string; size: string; status: string;
  tags: string; usage: string; uploadedAt: string;
};

export type ListAssetsResponse = AssetSummary[];
export type ListAssetTagsResponse = string[];
