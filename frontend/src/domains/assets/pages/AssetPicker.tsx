import { useEffect, useRef, useState } from "react";
import { Search } from "lucide-react";
import { Badge, Button, Card, PageHeader, SkeletonLines, PartialErrorWidget } from "../../../shared/components/Primitives";
import { assetsService } from "../services/assetsService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { AssetPreview, AssetTypeIcon } from "../components/AssetBits";
import { toast } from "../../../core/notifications/toast";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";
import { useAssetObjectUrl } from "../hooks/useAssetObjectUrl";
import { useViewAsRole } from "../../../core/permissions/useViewAsRole";
import { PermGate } from "../../../app/guards/PermGate";

function previewKind(type: string | undefined) {
  const normalized = (type ?? "").trim().toLowerCase();
  if (normalized === "imagem" || normalized === "image") return "image";
  if (normalized === "pdf") return "pdf";
  if (normalized === "vídeo" || normalized === "video") return "video";
  if (normalized === "áudio" || normalized === "audio") return "audio";
  return "document";
}

export function AssetPicker() {
  const { product } = useCurrentProduct();
  const { viewAsRole } = useViewAsRole();
  const productId = product?.id ?? "";
  const canEdit = viewAsRole !== "viewer";
  const [selected, setSelected] = useState<string | null>(null);
  const { data: assets, loading, error } = useAsyncData(() => (productId ? assetsService.listAssets(productId) : Promise.resolve([])), [productId]);
  const [visibleAssets, setVisibleAssets] = useState(assets ?? []);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const selectedAsset = visibleAssets.find((a) => (a.id ?? a.name) === selected);
  const selectedAssetId = selectedAsset?.id ?? selectedAsset?.name;
  const selectedKind = previewKind(selectedAsset?.type);
  const canPreviewSelected = selectedKind !== "document";
  const { url: selectedPreviewUrl, loading: selectedPreviewLoading } = useAssetObjectUrl(selectedAssetId, Boolean(selectedAssetId && canPreviewSelected));

  useEffect(() => {
    const nextAssets = assets ?? [];
    setVisibleAssets(nextAssets);
    setSelected((current) => current ?? (nextAssets[0]?.id ?? nextAssets[0]?.name ?? null));
  }, [assets]);

  const handleQuickUpload = () => fileInputRef.current?.click();

  const handleFilesSelected = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;
    await assetsService.uploadFiles(productId, Array.from(files));
    const refreshed = await assetsService.listAssets(productId);
    setVisibleAssets(refreshed);
    toast.success(`${files.length} arquivo(s) enviado(s)!`);
    const uploaded = refreshed.find((asset) => asset.name === files[0].name);
    setSelected(uploaded?.id ?? uploaded?.name ?? null);
    e.target.value = "";
  };

  const handleConfirmSelection = () => {
    toast.success("Asset selecionado!", { description: selectedAsset?.name });
  };

  return (
    <>
      <input ref={fileInputRef} type="file" multiple hidden onChange={handleFilesSelected} />
      <PageHeader title="Asset Picker" module="Assets" desc="Componente reutilizável para Editor, SEO, Forms e configurações futuras." badge="Picker">
        <PermGate allowed={canEdit}><Button onClick={handleQuickUpload}>Upload rápido</Button></PermGate>
        <Button primary disabled={!selectedAsset} onClick={handleConfirmSelection}>Confirmar seleção</Button>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
        <Card>
          <div className="mb-3 flex items-center gap-2 rounded-xl border border-border px-3 py-2"><Search size={16} /><input placeholder="Buscar asset existente..." className="w-full bg-transparent text-sm outline-none" /></div>
          <div className="mb-3 flex flex-wrap gap-2"><Badge>Busca</Badge><Badge>Filtros</Badge><Badge>Seleção única</Badge><Badge>Seleção múltipla</Badge><Badge>Sem resultados</Badge></div>
          {loading ? <SkeletonLines /> : error || !assets ? <PartialErrorWidget /> : (
            <div className="grid gap-3 md:grid-cols-2">
              {visibleAssets.slice(0, 6).map((a) => (
                <button key={a.id ?? a.name} onClick={() => setSelected(a.id ?? a.name)} className={`rounded-xl border p-3 text-left ${(a.id ?? a.name) === selected ? "border-primary bg-muted" : "border-border"}`}>
                  <div className="mb-3 aspect-video overflow-hidden rounded-xl bg-muted">
                    <AssetPreview a={a} assetId={a.id ?? a.name} />
                  </div>
                  <p className="mt-2 font-medium">{a.name}</p>
                  <p className="text-sm text-muted-foreground">{a.type} · {a.size}</p>
                </button>
              ))}
            </div>
          )}
        </Card>
        <Card>
          <h2 className="mb-3 text-lg font-semibold">Preview lateral</h2>
          <div className="aspect-video overflow-hidden rounded-xl bg-muted p-4">
            {selectedKind === "image" && selectedPreviewUrl ? (
              <img src={selectedPreviewUrl} alt={selectedAsset?.name ?? "Preview do asset"} className="h-full w-full rounded-lg object-contain" />
            ) : selectedKind === "pdf" && selectedPreviewUrl ? (
              <iframe src={`${selectedPreviewUrl}#page=1&toolbar=0&navpanes=0&scrollbar=0`} title={selectedAsset?.name ?? "Preview do PDF"} className="h-full w-full rounded-lg border border-border bg-white" />
            ) : selectedKind === "video" && selectedPreviewUrl ? (
              <video src={selectedPreviewUrl} preload="metadata" muted playsInline className="h-full w-full rounded-lg bg-black object-cover" />
            ) : selectedKind === "audio" && selectedPreviewUrl ? (
              <div className="flex h-full flex-col justify-between rounded-lg bg-card p-3"><AssetTypeIcon type="áudio" /><audio src={selectedPreviewUrl} controls className="w-full" /></div>
            ) : selectedPreviewLoading ? (
              <div className="grid h-full place-items-center text-sm text-muted-foreground">Carregando preview...</div>
            ) : (
              <AssetTypeIcon type={selectedAsset?.type ?? ""} />
            )}
          </div>
          <p className="mt-3 font-medium">{selectedAsset?.name ?? "Nenhum asset selecionado"}</p>
          <p className="text-sm text-muted-foreground">Asset selecionado vinculado ao produto ativo.</p>
          <Button primary disabled={!selectedAsset} onClick={handleConfirmSelection}>Usar asset</Button>
        </Card>
      </div>
    </>
  );
}
