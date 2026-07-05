export type AssetSummary = {
  id?: string;
  name: string; type: string; size: string; status: string;
  tags: string; usage: string; uploadedAt: string;
};

export type AssetDetailResponse = {
  id: string;
  name: string;
  friendlyName?: string | null;
  altText?: string | null;
  caption?: string | null;
  credit?: string | null;
  mimeType: string;
  category: string;
  sizeBytes: number;
  status: string;
  tags: string[];
  uploadedBySubject?: string | null;
  createdAt: string;
  updatedAt: string;
};

export type ListAssetsResponse = AssetSummary[];
export type ListAssetTagsResponse = string[];
