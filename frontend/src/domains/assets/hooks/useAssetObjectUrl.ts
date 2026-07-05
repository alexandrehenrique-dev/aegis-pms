import { useEffect, useState } from "react";
import { assetsService } from "../services/assetsService";

export function useAssetObjectUrl(assetId: string | undefined, enabled = true) {
  const [url, setUrl] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!assetId || !enabled) {
      setUrl(null);
      return undefined;
    }

    let revoked = false;
    let objectUrl: string | null = null;
    setLoading(true);
    assetsService.loadAssetFile(assetId)
      .then((blob) => {
        if (revoked) return;
        objectUrl = URL.createObjectURL(blob);
        setUrl(objectUrl);
      })
      .catch(() => {
        if (!revoked) setUrl(null);
      })
      .finally(() => {
        if (!revoked) setLoading(false);
      });

    return () => {
      revoked = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [assetId, enabled]);

  return { url, loading };
}
