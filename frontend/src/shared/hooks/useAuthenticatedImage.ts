import { useEffect, useState } from "react";
import { IS_API_MODE } from "../../infra/apiMode";
import { assetsService } from "../../domains/assets/services/assetsService";
import { resolveAssetSrc } from "../utils/resolveAssetSrc";

/**
 * K.2 (BUG-SPRINT-05) — resolve um `assetId` para uma URL renderizável em
 * `<img>`/`<audio>`/`<video> src`. O endpoint de download exige
 * `Authorization: Bearer`, que uma tag de mídia nunca envia sozinha — em API
 * mode, este hook busca o arquivo via `apiClient.getBlob` (que já anexa o
 * header) e expõe uma blob URL local. Em mock mode, delega direto para
 * `resolveAssetSrc` (placeholder síncrono, sem fetch).
 */
export function useAuthenticatedImage(assetId: string | undefined): string | undefined {
  const [blobUrl, setBlobUrl] = useState<string | undefined>(undefined);

  useEffect(() => {
    if (!assetId || !IS_API_MODE) {
      setBlobUrl(undefined);
      return undefined;
    }
    let revoked = false;
    let objectUrl: string | undefined;
    assetsService
      .loadAssetFile(assetId)
      .then((blob) => {
        if (revoked) return;
        objectUrl = URL.createObjectURL(blob);
        setBlobUrl(objectUrl);
      })
      .catch(() => {
        if (!revoked) setBlobUrl(undefined);
      });
    return () => {
      revoked = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [assetId]);

  if (!IS_API_MODE) return resolveAssetSrc(assetId);
  return blobUrl;
}
