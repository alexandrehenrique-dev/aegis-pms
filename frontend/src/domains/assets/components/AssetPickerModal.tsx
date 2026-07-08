import { useEffect, useMemo, useRef, useState } from "react";
import { motion } from "motion/react";
import { Search, X } from "lucide-react";
import { Badge, Button, SkeletonLines } from "../../../shared/components/Primitives";
import { fade } from "../../../shared/components/motion";
import { assetsService } from "../services/assetsService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { AssetTypeIcon } from "./AssetBits";
import { toast } from "../../../core/notifications/toast";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";
import type { AssetSummary } from "../contracts/responses";

export type AssetTypeFilter = "imagem" | "PDF" | "áudio" | "vídeo" | "qualquer";

const FILTER_OPTIONS: AssetTypeFilter[] = ["qualquer", "imagem", "PDF", "áudio", "vídeo"];

function previewKind(type: string | undefined) {
  const normalized = (type ?? "").trim().toLowerCase();
  if (normalized === "imagem" || normalized === "image") return "image";
  if (normalized === "pdf") return "pdf";
  if (normalized === "vídeo" || normalized === "video") return "video";
  if (normalized === "áudio" || normalized === "audio") return "audio";
  return "document";
}

/**
 * Variante modal do `AssetPicker` (Sprint 13, Tarefa C) — diferente da tela
 * standalone (`AssetPicker.tsx`), devolve a seleção via `onSelect`, o que
 * permite conectar qualquer campo de mídia do editor de blocos a um asset
 * real. `typeFilter` restringe a lista (ex.: só "imagem" para campos de
 * imagem); passar `lockFilter` impede o usuário de trocar o filtro.
 */
export function AssetPickerModal({ open, typeFilter = "qualquer", lockFilter = false, onSelect, onClose }: {
  open: boolean;
  typeFilter?: AssetTypeFilter;
  lockFilter?: boolean;
  onSelect: (asset: AssetSummary) => void;
  onClose: () => void;
}) {
  const { product } = useCurrentProduct();
  const productId = product?.id ?? "";
  const [retryKey, setRetryKey] = useState(0);
  const { data: assets, loading, error, rawError } = useAsyncData(() => (productId ? assetsService.listAssets(productId) : Promise.resolve([])), [productId, retryKey]);
  const [visibleAssets, setVisibleAssets] = useState<AssetSummary[]>([]);
  const [query, setQuery] = useState("");
  const [activeFilter, setActiveFilter] = useState<AssetTypeFilter>(typeFilter);
  const [selectedName, setSelectedName] = useState<string | null>(null);
  const [sessionUploadIds, setSessionUploadIds] = useState<string[]>([]);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const filtered = useMemo(() => {
    return visibleAssets.filter((a) => {
      const matchesType = activeFilter === "qualquer" || previewKind(a.type) === previewKind(activeFilter);
      const normalizedQuery = query.trim().toLowerCase();
      const matchesQuery = !normalizedQuery
        || a.name.toLowerCase().includes(normalizedQuery)
        || a.type.toLowerCase().includes(normalizedQuery)
        || a.tags.toLowerCase().includes(normalizedQuery);
      return matchesType && matchesQuery;
    });
  }, [visibleAssets, activeFilter, query]);

  useEffect(() => setVisibleAssets(assets ?? []), [assets]);

  if (!open) return null;

  const handleFilesSelected = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;
    const uploadedIds: string[] = [];
    for (const file of Array.from(files)) {
      const { assetId } = await assetsService.upload(file, productId);
      uploadedIds.push(assetId);
    }
    setSessionUploadIds((prev) => [...prev, ...uploadedIds]);
    const refreshed = await assetsService.listAssets(productId);
    setVisibleAssets(refreshed);
    toast.success(`${files.length} arquivo(s) enviado(s)!`);
    setSelectedName(files[0].name);
    e.target.value = "";
  };

  const handleConfirm = () => {
    const asset = visibleAssets.find((a) => a.name === selectedName);
    if (!asset) return;
    setSessionUploadIds([]);
    onSelect(asset);
  };

  /** J.5.3 — se o usuário fez upload rápido e depois cancelou sem confirmar a seleção, o(s) asset(s) enviados nesta sessão do modal são descartados para não ficarem órfãos vinculados a nada. */
  const handleCancel = async () => {
    if (sessionUploadIds.length > 0) {
      await Promise.all(sessionUploadIds.map((id) => assetsService.deleteAsset(productId, id).catch(() => undefined)));
      setSessionUploadIds([]);
    }
    onClose();
  };

  const errorInfo = (() => {
    const code = (rawError as { code?: string } | null)?.code;
    if (code === "module_disabled") {
      return {
        title: "Módulo Assets não habilitado",
        description: "Este produto não tem o módulo Assets habilitado. Habilite-o em Configurações > Módulos para selecionar arquivos.",
      };
    }
    return {
      title: "Não foi possível carregar os assets",
      description: "Ocorreu um erro ao buscar os arquivos deste produto.",
    };
  })();

  return (
    <motion.div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30 backdrop-blur-[3px] p-4" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={handleCancel}>
      <motion.div {...fade} className="flex max-h-[85vh] w-full max-w-2xl flex-col rounded-2xl border border-border bg-card p-6 shadow-[0_24px_80px_rgba(0,0,0,0.2)]" onClick={(e) => e.stopPropagation()}>
        <input ref={fileInputRef} type="file" multiple hidden onChange={handleFilesSelected} />
        <div className="flex items-center justify-between">
          <h3 className="font-semibold">Selecionar asset</h3>
          <button onClick={handleCancel} aria-label="Fechar" className="rounded-lg p-1.5 text-muted-foreground transition hover:bg-muted"><X size={16} /></button>
        </div>

        <div className="mt-4 flex items-center gap-2 rounded-xl border border-border px-3 py-2">
          <Search size={16} />
          <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Buscar asset existente..." className="w-full bg-transparent text-sm outline-none" />
        </div>

        <div className="mt-3 flex flex-wrap gap-2">
          {FILTER_OPTIONS.map((opt) => (
            <button key={opt} disabled={lockFilter} onClick={() => setActiveFilter(opt)} className="disabled:cursor-not-allowed">
              <Badge tone={activeFilter === opt ? "violet" : "neutral"}>{opt}</Badge>
            </button>
          ))}
        </div>

        <div className="mt-3 flex-1 overflow-auto">
          {loading ? <SkeletonLines /> : error || !assets ? (
            <div className="flex flex-col items-center gap-2 py-8 text-center text-sm">
              <p className="font-medium text-foreground">{errorInfo.title}</p>
              <p className="text-muted-foreground">{errorInfo.description}</p>
              <Button onClick={() => setRetryKey((k) => k + 1)} className="mt-1">Tentar novamente</Button>
            </div>
          ) : filtered.length === 0 ? (
            <p className="py-6 text-center text-sm text-muted-foreground">Nenhum asset encontrado para este filtro.</p>
          ) : (
            <div className="grid gap-3 md:grid-cols-2">
              {filtered.map((a) => (
                <button key={a.name} onClick={() => setSelectedName(a.name)} className={`rounded-xl border p-3 text-left ${selectedName === a.name ? "border-primary bg-muted" : "border-border"}`}>
                  <AssetTypeIcon type={a.type} />
                  <p className="mt-2 truncate font-medium">{a.name}</p>
                  <p className="text-sm text-muted-foreground">{a.type} · {a.size}</p>
                </button>
              ))}
            </div>
          )}
        </div>

        <div className="mt-5 flex items-center justify-between border-t border-border pt-4">
          <Button onClick={() => fileInputRef.current?.click()}>Upload rápido</Button>
          <div className="flex gap-2">
            <Button onClick={handleCancel}>Cancelar</Button>
            <Button primary disabled={!selectedName} onClick={handleConfirm}>Usar asset</Button>
          </div>
        </div>
      </motion.div>
    </motion.div>
  );
}
