import { assets, assetTags } from "../mocks/assets.mocks";
import { logApiCall } from "../../../shared/services/devLog";
import { IS_API_MODE } from "../../../infra/apiMode";
import { apiClient } from "../../../shared/services/apiClient";
import type { AssetSummary, ListAssetsResponse, ListAssetTagsResponse } from "../contracts/responses";

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
  async listAssets(productId?: string): Promise<ListAssetsResponse> {
    if (IS_API_MODE) return apiClient.get<ListAssetsResponse>(`/products/${productId ?? "{productId}"}/assets`);
    return assetsStore;
  },
  async listTags(): Promise<ListAssetTagsResponse> {
    if (IS_API_MODE) return apiClient.get<ListAssetTagsResponse>("/products/{productId}/assets/tags");
    return assetTagsStore;
  },
  async createTag(name: string): Promise<void> {
    if (IS_API_MODE) return apiClient.post("/products/{productId}/assets/tags", { name });
    logApiCall("POST", "/api/v1/products/{productId}/assets/tags", { name });
    if (!assetTagsStore.includes(name)) assetTagsStore.push(name);
  },
  async renameTag(oldName: string, newName: string): Promise<void> {
    if (IS_API_MODE) return apiClient.put(`/products/{productId}/assets/tags/${oldName}`, { newName });
    logApiCall("PUT", `/api/v1/products/{productId}/assets/tags/${oldName}`, { newName });
    const i = assetTagsStore.indexOf(oldName);
    if (i >= 0) assetTagsStore[i] = newName;
  },
  async removeTag(name: string): Promise<void> {
    if (IS_API_MODE) return apiClient.delete(`/products/{productId}/assets/tags/${name}`);
    logApiCall("DELETE", `/api/v1/products/{productId}/assets/tags/${name}`);
    const i = assetTagsStore.indexOf(name);
    if (i >= 0) assetTagsStore.splice(i, 1);
  },
  async mergeTags(tagsToMerge: string[], into: string): Promise<void> {
    if (IS_API_MODE) return apiClient.post("/products/{productId}/assets/tags/merge", { tagsToMerge, into });
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
    if (IS_API_MODE) return apiClient.put("/products/{productId}/assets/{assetId}/metadata");
    logApiCall("PUT", "/api/v1/products/{productId}/assets/{assetId}/metadata");
  },
  async archiveAsset(name: string): Promise<void> {
    if (IS_API_MODE) return apiClient.post(`/products/{productId}/assets/${name}/archive`);
    const a = assetsStore.find((x) => x.name === name);
    if (!a) return;
    logApiCall("POST", `/api/v1/products/{productId}/assets/${name}/archive`);
    a.status = "arquivado";
  },
  /** Noop intencional: usar `upload(file: File)` para upload unitário com persistência no store. `uploadFiles()` é o endpoint batch que o backend implementará na etapa 11. */
  async uploadFiles(): Promise<void> {
    if (IS_API_MODE) return apiClient.post("/products/{productId}/assets/upload");
    logApiCall("POST", "/api/v1/products/{productId}/assets/upload (batch — sem persistência no mock)");
  },
  /** Upload de um arquivo real do sistema do usuário (Sprint 18, Tarefa D.2) — reaproveitado por qualquer picker fora do contexto de assets (ex.: anexo do `FeedbackModal`), nunca um endpoint de upload próprio por domínio. Devolve o `assetId` (mock: o próprio `name`) para referenciar o asset criado. */
  async upload(file: File): Promise<{ assetId: string }> {
    if (IS_API_MODE) {
      logApiCall("POST", "/api/v1/products/{productId}/assets (upload multipart)");
      return { assetId: file.name };
    }
    logApiCall("POST", "/api/v1/products/{productId}/assets", { name: file.name, size: file.size });
    assetsStore.push({ name: file.name, type: inferAssetType(file), size: formatSize(file.size), status: "ativo", tags: "", usage: "", uploadedAt: new Date().toLocaleDateString("pt-BR") });
    return { assetId: file.name };
  },
  async downloadAsset(name: string): Promise<void> {
    if (IS_API_MODE) return apiClient.get(`/products/{productId}/assets/${name}/download`);
    logApiCall("GET", `/api/v1/products/{productId}/assets/${name}/download`);
  },
};
