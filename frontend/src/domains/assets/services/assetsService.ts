import { assets, assetTags } from "../mocks/assets.mocks";
import { logApiCall } from "../../../shared/services/devLog";
import { IS_API_MODE } from "../../../infra/apiMode";
import { apiClient, resolveBaseUrl } from "../../../shared/services/apiClient";
import type { AssetDetailResponse, AssetSummary, ListAssetsResponse, ListAssetTagsResponse } from "../contracts/responses";

const assetsStore: AssetSummary[] = assets.map(([name, type, size, status, tags, usage, uploadedAt]) => ({
  name, type, size, status, tags, usage, uploadedAt,
}));

const assetTagsStore: string[] = [...assetTags];

function inferAssetType(file: File): string {
  if (file.type.startsWith("image/")) return "imagem";
  if (file.type === "application/pdf") return "PDF";
  if (file.type.startsWith("audio/")) return "áudio";
  if (file.type.startsWith("video/")) return "vídeo";
  return "qualquer";
}

function formatSize(bytes: number): string {
  return bytes >= 1024 * 1024 ? `${(bytes / (1024 * 1024)).toFixed(1)} MB` : `${Math.max(1, Math.round(bytes / 1024))} KB`;
}

export const assetsService = {
  async listAssets(productId: string): Promise<ListAssetsResponse> {
    if (IS_API_MODE) return apiClient.get<ListAssetsResponse>(`/products/${productId}/assets`);
    return assetsStore;
  },
  async listTags(productId: string): Promise<ListAssetTagsResponse> {
    if (IS_API_MODE) return apiClient.get<ListAssetTagsResponse>(`/products/${productId}/asset-tags`);
    return assetTagsStore;
  },
  async getAsset(productId: string, assetId: string): Promise<AssetDetailResponse | undefined> {
    if (IS_API_MODE) return apiClient.get<AssetDetailResponse>(`/products/${productId}/assets/${assetId}`);
    const asset = assetsStore.find((item) => item.id === assetId || item.name === assetId);
    if (!asset) return undefined;
    return {
      id: asset.id ?? asset.name,
      name: asset.name,
      friendlyName: asset.name,
      mimeType: asset.type === "imagem" ? "image/*" : asset.type === "vídeo" ? "video/*" : asset.type === "áudio" ? "audio/*" : asset.type === "PDF" ? "application/pdf" : "application/octet-stream",
      category: asset.type,
      sizeBytes: 0,
      status: asset.status,
      tags: asset.tags ? asset.tags.split(", ").filter(Boolean) : [],
      createdAt: asset.uploadedAt,
      updatedAt: asset.uploadedAt,
    };
  },
  async createTag(productId: string, name: string): Promise<void> {
    if (IS_API_MODE) return apiClient.post(`/products/${productId}/asset-tags`, name);
    logApiCall("POST", `/api/v1/products/${productId}/asset-tags`, { name });
    if (!assetTagsStore.includes(name)) assetTagsStore.push(name);
  },
  async renameTag(productId: string, oldName: string, newName: string): Promise<void> {
    if (IS_API_MODE) {
      await apiClient.delete(`/products/${productId}/asset-tags/${encodeURIComponent(oldName)}`);
      await apiClient.post(`/products/${productId}/asset-tags`, newName);
      return;
    }
    logApiCall("PUT", `/api/v1/products/${productId}/asset-tags/${oldName}`, { newName });
    const i = assetTagsStore.indexOf(oldName);
    if (i >= 0) assetTagsStore[i] = newName;
  },
  async removeTag(productId: string, name: string): Promise<void> {
    if (IS_API_MODE) return apiClient.delete(`/products/${productId}/asset-tags/${encodeURIComponent(name)}`);
    logApiCall("DELETE", `/api/v1/products/${productId}/asset-tags/${name}`);
    const i = assetTagsStore.indexOf(name);
    if (i >= 0) assetTagsStore.splice(i, 1);
  },
  async mergeTags(productId: string, tagsToMerge: string[], into: string): Promise<void> {
    if (IS_API_MODE) {
      await Promise.all(tagsToMerge.filter((tag) => tag !== into).map((tag) => apiClient.delete(`/products/${productId}/asset-tags/${encodeURIComponent(tag)}`)));
      await apiClient.post(`/products/${productId}/asset-tags`, into);
      return;
    }
    logApiCall("POST", `/api/v1/products/${productId}/asset-tags/merge`, { tagsToMerge, into });
    tagsToMerge.forEach((t) => {
      if (t !== into) {
        const i = assetTagsStore.indexOf(t);
        if (i >= 0) assetTagsStore.splice(i, 1);
      }
    });
    if (!assetTagsStore.includes(into)) assetTagsStore.push(into);
  },
  async saveMetadata(productId: string, assetId: string, metadata: Record<string, unknown>): Promise<void> {
    if (IS_API_MODE) return apiClient.put(`/products/${productId}/assets/${assetId}/metadata`, metadata);
    logApiCall("PUT", `/api/v1/products/${productId}/assets/${assetId}/metadata`, metadata);
  },
  async archiveAsset(productId: string, assetId: string): Promise<void> {
    if (IS_API_MODE) return apiClient.delete(`/products/${productId}/assets/${assetId}`);
    const a = assetsStore.find((x) => x.name === assetId || x.id === assetId);
    if (!a) return;
    logApiCall("DELETE", `/api/v1/products/${productId}/assets/${assetId}`);
    a.status = "arquivado";
  },
  async uploadFiles(productId: string, files: File[]): Promise<void> {
    await Promise.all(files.map(async (file) => {
      if (IS_API_MODE) {
        const fd = new FormData();
        fd.append("file", file);
        await apiClient.upload(`/products/${productId}/assets`, fd);
        return;
      }
      const assetId = `mock-asset-${Date.now()}`;
      logApiCall("POST", `/api/v1/products/${productId}/assets`, { name: file.name, size: file.size });
      assetsStore.push({ id: assetId, name: file.name, type: inferAssetType(file), size: formatSize(file.size), status: "ativo", tags: "", usage: "", uploadedAt: new Date().toLocaleDateString("pt-BR") });
    }));
  },
  /** Upload de um arquivo real do sistema do usuário (Sprint 18, Tarefa D.2) — reaproveitado por qualquer picker fora do contexto de assets (ex.: anexo do `FeedbackModal`), nunca um endpoint de upload próprio por domínio. Devolve o `assetId` (mock: o próprio `name`) para referenciar o asset criado. */
  async upload(file: File, productId: string): Promise<{ assetId: string }> {
    if (IS_API_MODE) {
      const fd = new FormData();
      fd.append("file", file);
      const created = await apiClient.upload<{ id: string }>(`/products/${productId}/assets`, fd);
      return { assetId: created.id };
    }
    const assetId = `mock-asset-${Date.now()}`;
    logApiCall("POST", `/api/v1/products/${productId}/assets`, { name: file.name, size: file.size });
    assetsStore.push({ id: assetId, name: file.name, type: inferAssetType(file), size: formatSize(file.size), status: "ativo", tags: "", usage: "", uploadedAt: new Date().toLocaleDateString("pt-BR") });
    return { assetId };
  },
  getDownloadUrl(assetId: string): string {
    return `${resolveBaseUrl()}/assets/${assetId}/download`;
  },
  getFileUrl(assetId: string): string {
    return `${resolveBaseUrl()}/assets/${assetId}/file`;
  },
};
