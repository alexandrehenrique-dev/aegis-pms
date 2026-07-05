import { useEffect, useMemo, useRef, useState } from "react";
import { Search, X } from "lucide-react";
import { Button, Card, EmptyState, PageHeader, SkeletonLines, PartialErrorWidget } from "../../../shared/components/Primitives";
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

type AssetPickerFilter = "all" | "image" | "pdf" | "video" | "audio" | "document";

const TYPE_FILTERS: Array<{ value: AssetPickerFilter; label: string }> = [
  { value: "all", label: "Todos" },
  { value: "image", label: "Imagens" },
  { value: "pdf", label: "PDFs" },
  { value: "video", label: "Vídeos" },
  { value: "audio", label: "Áudios" },
  { value: "document", label: "Documentos" },
];

function FilterChip({ active, onClick, children }: { active: boolean; onClick: () => void; children: React.ReactNode }) {
  return (
    <button
      onClick={onClick}
      className={`flex items-center gap-1 rounded-full px-2.5 py-0.5 text-[11px] font-medium transition-colors ${
        active ? "bg-primary text-primary-foreground shadow-sm" : "bg-muted text-muted-foreground hover:bg-muted/70"
      }`}
    >
      {children}
    </button>
  );
}

export function AssetPicker() {
  const { product } = useCurrentProduct();
  const { viewAsRole } = useViewAsRole();
  const productId = product?.id ?? "";
  const canEdit = viewAsRole !== "viewer";
  const [selected, setSelected] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [typeFilter, setTypeFilter] = useState<AssetPickerFilter>("all");
  const [selectedOnly, setSelectedOnly] = useState(false);
  const { data: assets, loading, error } = useAsyncData(() => (productId ? assetsService.listAssets(productId) : Promise.resolve([])), [productId]);
  const [visibleAssets, setVisibleAssets] = useState(assets ?? []);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const selectedAsset = visibleAssets.find((a) => (a.id ?? a.name) === selected);
  const selectedAssetId = selectedAsset?.id ?? selectedAsset?.name;
  const selectedKind = previewKind(selectedAsset?.type);
  const canPreviewSelected = selectedKind !== "document";
  const { url: selectedPreviewUrl, loading: selectedPreviewLoading } = useAssetObjectUrl(selectedAssetId, Boolean(selectedAssetId && canPreviewSelected));
  const filteredAssets = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();
    return visibleAssets.filter((asset) => {
      const assetId = asset.id ?? asset.name;
      const kind = previewKind(asset.type);
      const matchesQuery = !normalizedQuery
        || asset.name.toLowerCase().includes(normalizedQuery)
        || asset.type.toLowerCase().includes(normalizedQuery)
        || asset.tags.toLowerCase().includes(normalizedQuery);
      const matchesType = typeFilter === "all" || kind === typeFilter;
      const matchesSelection = !selectedOnly || assetId === selected;
      return matchesQuery && matchesType && matchesSelection;
    });
  }, [query, selected, selectedOnly, typeFilter, visibleAssets]);
  const hasActiveFilters = query.trim() || typeFilter !== "all" || selectedOnly;

  const clearFilters = () => {
    setQuery("");
    setTypeFilter("all");
    setSelectedOnly(false);
  };

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
          <div className="mb-3 flex items-center gap-2 rounded-xl border border-border px-3 py-2">
            <Search size={16} />
            <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Buscar asset existente..." className="w-full bg-transparent text-sm outline-none" />
          </div>
          <div className="mb-3 flex flex-wrap gap-1">
            {TYPE_FILTERS.map((filter) => (
              <FilterChip key={filter.value} active={typeFilter === filter.value} onClick={() => setTypeFilter(filter.value)}>
                {filter.label}
              </FilterChip>
            ))}
            <FilterChip active={selectedOnly} onClick={() => setSelectedOnly((current) => !current)}>Selecionado</FilterChip>
            {hasActiveFilters && (
              <FilterChip active={false} onClick={clearFilters}><X size={10} /> Limpar</FilterChip>
            )}
          </div>
          {loading ? <SkeletonLines /> : error || !assets ? <PartialErrorWidget /> : (
            filteredAssets.length === 0 ? (
              <EmptyState compact title="Nenhum asset encontrado" description="Remova filtros ou busque por outro termo." primaryAction={{ label: "Limpar filtros", onClick: clearFilters }} />
            ) : (
              <div className="grid gap-3 md:grid-cols-2">
                {filteredAssets.map((a) => (
                  <button key={a.id ?? a.name} onClick={() => setSelected(a.id ?? a.name)} className={`rounded-xl border p-3 text-left ${(a.id ?? a.name) === selected ? "border-primary bg-muted" : "border-border"}`}>
                    <div className="mb-3 aspect-video overflow-hidden rounded-xl bg-muted">
                      <AssetPreview a={a} assetId={a.id ?? a.name} />
                    </div>
                    <p className="mt-2 font-medium">{a.name}</p>
                    <p className="text-sm text-muted-foreground">{a.type} · {a.size}</p>
                  </button>
                ))}
              </div>
            )
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
