import { useRef, useState } from "react";
import { useNavigate, useParams } from "react-router";
import { AnimatePresence } from "motion/react";
import { Badge, Button, Card, EmptyState, PageHeader, PartialErrorWidget, SkeletonLines } from "../../../shared/components/Primitives";
import { ConfirmDialog } from "../../../shared/components/ConfirmDialog";
import { PermGate } from "../../../app/guards/PermGate";
import { useViewAsRole } from "../../../core/permissions/useViewAsRole";
import { AssetUsagePanel } from "../components/AssetUsagePanel";
import { toast } from "../../../core/notifications/toast";
import { assetsService } from "../services/assetsService";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { AssetTypeIcon } from "../components/AssetBits";
import type { AssetDetailResponse } from "../contracts/responses";

function formatBytes(bytes: number): string {
  if (!bytes) return "—";
  return bytes >= 1024 * 1024 ? `${(bytes / (1024 * 1024)).toFixed(1)} MB` : `${Math.max(1, Math.round(bytes / 1024))} KB`;
}

function assetType(asset: AssetDetailResponse): string {
  if (asset.mimeType.startsWith("image/")) return "imagem";
  if (asset.mimeType.startsWith("video/")) return "vídeo";
  if (asset.mimeType.startsWith("audio/")) return "áudio";
  if (asset.mimeType === "application/pdf") return "PDF";
  return asset.category || "arquivo";
}

function AssetPreviewPanel({ asset }: { asset: AssetDetailResponse }) {
  const previewUrl = assetsService.getDownloadUrl(asset.id);
  const type = assetType(asset);
  return (
    <Card>
      <div className="flex min-h-[360px] items-center justify-center rounded-2xl border border-border bg-muted/30 p-4">
        {asset.mimeType.startsWith("image/") ? (
          <img src={previewUrl} alt={asset.altText ?? asset.name} className="max-h-[520px] max-w-full rounded-xl object-contain" />
        ) : asset.mimeType.startsWith("video/") ? (
          <video src={previewUrl} controls className="max-h-[520px] max-w-full rounded-xl" />
        ) : asset.mimeType.startsWith("audio/") ? (
          <div className="w-full max-w-xl rounded-xl border border-border bg-card p-6">
            <AssetTypeIcon type={type} />
            <p className="mt-4 font-medium">{asset.name}</p>
            <audio src={previewUrl} controls className="mt-4 w-full" />
          </div>
        ) : asset.mimeType === "application/pdf" ? (
          <iframe src={previewUrl} title={asset.name} className="h-[520px] w-full rounded-xl border border-border bg-white" />
        ) : (
          <div className="grid place-items-center text-center">
            <AssetTypeIcon type={type} />
            <h2 className="mt-4 text-xl font-semibold">{asset.name}</h2>
            <p className="mt-1 text-sm text-muted-foreground">Preview indisponível para este tipo de arquivo.</p>
          </div>
        )}
      </div>
      <div className="mt-4 grid gap-2 md:grid-cols-3">
        <Badge>{type}</Badge>
        <Badge>{formatBytes(asset.sizeBytes)}</Badge>
        <Badge>{asset.mimeType}</Badge>
      </div>
    </Card>
  );
}

export function AssetDetail() {
  const navigate = useNavigate();
  const { slug: assetId } = useParams<{ slug: string }>();
  const { viewAsRole } = useViewAsRole();
  const { product } = useCurrentProduct();
  const productId = product?.id ?? "";
  const canEdit = viewAsRole !== "viewer";
  const replaceInputRef = useRef<HTMLInputElement>(null);
  const [confirmArchive, setConfirmArchive] = useState(false);
  const [archiving, setArchiving] = useState(false);
  const { data: asset, loading, error } = useAsyncData(
    () => (productId && assetId ? assetsService.getAsset(productId, assetId) : Promise.resolve(undefined)),
    [productId, assetId],
  );

  if (loading) return <SkeletonLines />;
  if (error) return <PartialErrorWidget />;
  if (!asset || !assetId) return <EmptyState title="Asset não encontrado" description="Volte para a biblioteca e escolha um arquivo existente." />;

  const handleCopyReference = async () => {
    await navigator.clipboard.writeText(`/assets/${asset.id}`);
    toast.success("Copiado");
  };

  const handleReplaceFile = () => replaceInputRef.current?.click();

  const handleFileReplaced = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!e.target.files || e.target.files.length === 0) return;
    await assetsService.upload(e.target.files[0], productId);
    toast.success("Arquivo substituído!", { description: e.target.files[0].name });
    e.target.value = "";
  };

  const handleArchive = async () => {
    setArchiving(true);
    try {
      await assetsService.archiveAsset(productId, asset.id);
      toast.success("Asset arquivado.", { description: asset.name });
      setConfirmArchive(false);
    } finally {
      setArchiving(false);
    }
  };

  return (
    <>
      <input ref={replaceInputRef} type="file" hidden onChange={handleFileReplaced} />
      <AnimatePresence>
        {confirmArchive && <ConfirmDialog title="Arquivar este asset?" desc="O asset deixará de aparecer nas listagens ativas. Referências em uso podem precisar revisão." danger loading={archiving} onConfirm={handleArchive} onCancel={() => setConfirmArchive(false)} />}
      </AnimatePresence>
      <PageHeader title={asset.name} module="Assets" desc="Preview, metadados, tags, uso no sistema e ações do asset." badge={asset.status}>
        <PermGate allowed={canEdit}><Button onClick={() => navigate(`/assets/${asset.id}/metadata`)}>Editar metadados</Button></PermGate>
        <Button onClick={handleCopyReference}>Copiar referência</Button>
        <a href={assetsService.getDownloadUrl(asset.id)} download={asset.name} target="_blank" rel="noreferrer">
          <Button primary>Baixar</Button>
        </a>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[1fr_380px]">
        <div className="space-y-4"><AssetPreviewPanel asset={asset} /><AssetUsagePanel /></div>
        <Card>
          <h2 className="mb-3 text-lg font-semibold">Metadados</h2>
          {[
            ["Tipo", assetType(asset)],
            ["Tamanho", formatBytes(asset.sizeBytes)],
            ["Dimensões", "—"],
            ["Status", asset.status],
            ["Upload", asset.createdAt ? new Date(asset.createdAt).toLocaleString("pt-BR") : "—"],
            ["URL futura", `/assets/${asset.id}`],
            ["Alt text", asset.altText ?? "—"],
            ["Crédito", asset.credit ?? "—"],
          ].map((x) => (
            <div key={x[0]} className="mb-2 flex justify-between rounded-lg bg-muted p-2 text-sm"><span>{x[0]}</span><b className="text-right">{x[1]}</b></div>
          ))}
          <div className="mt-3 flex flex-wrap gap-1">{asset.tags.length === 0 ? <Badge>sem tags</Badge> : asset.tags.map((t) => <Badge key={t} tone="blue">{t}</Badge>)}</div>
          <PermGate allowed={canEdit}>
            <div className="mt-4 space-y-2">
              <Button onClick={handleReplaceFile}>Substituir arquivo</Button>
              <Button onClick={() => setConfirmArchive(true)}>Arquivar</Button>
            </div>
          </PermGate>
        </Card>
      </div>
    </>
  );
}
