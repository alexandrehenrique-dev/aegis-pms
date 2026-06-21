import { assets, assetTags } from "../mocks/assets.mocks";
import { logApiCall } from "../../../shared/services/devLog";
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
  /** Upload de um arquivo real do sistema do usuário (Sprint 18, Tarefa D.2) — reaproveitado por qualquer picker fora do contexto de assets (ex.: anexo do `FeedbackModal`), nunca um endpoint de upload próprio por domínio. Devolve o `assetId` (mock: o próprio `name`) para referenciar o asset criado. */
  async upload(file: File): Promise<{ assetId: string }> {
    logApiCall("POST", "/api/v1/products/{productId}/assets", { name: file.name, size: file.size });
    assetsStore.push({ name: file.name, type: inferAssetType(file), size: formatSize(file.size), status: "ativo", tags: "", usage: "", uploadedAt: new Date().toLocaleDateString("pt-BR") });
    return { assetId: file.name };
  },
  async downloadAsset(name: string): Promise<void> {
    logApiCall("GET", `/api/v1/products/{productId}/assets/${name}/download`);
  },
};
