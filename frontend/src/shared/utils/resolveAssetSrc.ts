import { IS_API_MODE } from "../../infra/apiMode";
import { resolveBaseUrl } from "../services/apiClient";
import { findAssetByIdSync } from "../../domains/assets/services/assetsService";

/**
 * Resolve um `assetId` (UUID em API mode; UUID ou `name` de seed em mock
 * mode — ver `findAssetByIdSync`) para uma URL renderizável em `<img>` (E.11,
 * BUG-SPRINT consolidado). Content antigo com `src`/`imageAssetId` guardando
 * um filename (pré-fix) não resolve — cai no placeholder de texto do
 * chamador, que é o comportamento aceito na nota de migração do bug.
 */
export function resolveAssetSrc(assetId: string | undefined): string | undefined {
  if (!assetId) return undefined;
  if (IS_API_MODE) return `${resolveBaseUrl()}/assets/${assetId}/download`;
  const asset = findAssetByIdSync(assetId);
  return asset ? `https://placehold.co/800x400?text=${encodeURIComponent(asset.name)}` : undefined;
}
