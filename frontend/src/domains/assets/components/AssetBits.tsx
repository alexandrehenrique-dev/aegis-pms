import { useNavigate } from "react-router";
import { Copy, Download, Eye, File, FileText, Gauge, Image, MoreHorizontal, Trash2, Video } from "lucide-react";
import { Badge, Button, Card } from "../../../shared/components/Primitives";
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "../../../shared/components/ui/dropdown-menu";
import { toast } from "../../../core/notifications/toast";
import { assetsService } from "../services/assetsService";
import { useAssetObjectUrl } from "../hooks/useAssetObjectUrl";
import type { AssetSummary } from "../contracts/responses";

type PreviewKind = "image" | "pdf" | "video" | "audio" | "document";

function previewKind(type: string): PreviewKind {
  const normalized = type.trim().toLowerCase();
  if (normalized === "imagem" || normalized === "image") return "image";
  if (normalized === "pdf") return "pdf";
  if (normalized === "vídeo" || normalized === "video") return "video";
  if (normalized === "áudio" || normalized === "audio") return "audio";
  return "document";
}

export function AssetStatusBadge({ status }: { status: string }) {
  return <Badge tone={status === "ativo" ? "green" : status === "processando" ? "amber" : status === "erro" ? "red" : "neutral"}>{status}</Badge>;
}

/** Ícone genérico de arquivo para qualquer tipo que não seja imagem (Sprint 12, Tarefa L.1) — antes, qualquer tipo não listado (DOCX, ZIP etc.) caía no ícone de imagem por padrão, o que é enganoso. */
export function AssetTypeIcon({ type }: { type: string }) {
  const kind = previewKind(type);
  const icon = kind === "image" ? <Image size={18} /> : kind === "video" ? <Video size={18} /> : kind === "audio" ? <Gauge size={18} /> : kind === "pdf" ? <FileText size={18} /> : <File size={18} />;
  return <div className="grid h-10 w-10 place-items-center rounded-xl bg-muted text-primary">{icon}</div>;
}

function canRenderInlinePreview(type: string) {
  return previewKind(type) !== "document";
}

export function AssetPreview({ a, assetId }: { a: AssetSummary; assetId: string }) {
  const kind = previewKind(a.type);
  const { url, loading } = useAssetObjectUrl(assetId, canRenderInlinePreview(a.type));
  if (url) {
    if (kind === "image") return <img src={url} alt={a.name} className="h-full w-full rounded-xl object-cover" loading="lazy" />;
    if (kind === "pdf") {
      return (
        <iframe
          src={`${url}#page=1&toolbar=0&navpanes=0&scrollbar=0`}
          title={a.name}
          className="h-full w-full rounded-xl border-0 bg-white"
        />
      );
    }
    if (kind === "video") {
      return <video src={url} preload="metadata" muted playsInline className="h-full w-full rounded-xl bg-black object-cover" />;
    }
    if (kind === "audio") {
      return (
        <div className="flex h-full w-full flex-col justify-between rounded-xl bg-muted p-4">
          <AssetTypeIcon type={a.type} />
          <audio src={url} controls className="w-full" />
        </div>
      );
    }
  }
  if (canRenderInlinePreview(a.type)) {
    return (
      <div className="flex h-full w-full items-center justify-center rounded-xl bg-muted text-xs text-muted-foreground">
        {loading ? "Carregando preview..." : "Preview indisponível"}
      </div>
    );
  }
  const tone = kind === "pdf" ? "bg-[#fee2e2] text-[#dc2626]" : kind === "video" ? "bg-[#dbeafe] text-[#1d4ed8]" : kind === "audio" ? "bg-[#ede9fe] text-[#7c3aed]" : "bg-muted text-muted-foreground";
  return (
    <div className={`flex h-full w-full flex-col items-center justify-center gap-2 rounded-xl ${tone}`}>
      <AssetTypeIcon type={a.type} />
      <span className="text-xs font-semibold uppercase">{a.type}</span>
    </div>
  );
}

export function AssetCard({ a, productId, canEdit, onDeleted }: { a: AssetSummary; productId: string; canEdit: boolean; onDeleted?: (assetId: string) => void }) {
  const navigate = useNavigate();
  const assetId = a.id ?? a.name;

  const handleCopyReference = async () => {
    await navigator.clipboard.writeText(`/assets/${assetId}`);
    toast.success("Copiado");
  };

  const handleDownload = async () => {
    await assetsService.downloadAsset(assetId, a.name);
  };

  const handleDelete = async () => {
    if (!window.confirm(`Excluir "${a.name}"? Esta ação remove o arquivo da biblioteca.`)) return;
    try {
      await assetsService.deleteAsset(productId, assetId);
      toast.success("Asset excluído.", { description: a.name });
      onDeleted?.(assetId);
    } catch {
      toast.error("Não foi possível excluir o asset.", { description: "Verifique se ele ainda está em uso no produto." });
    }
  };

  return (
    <Card>
      <div className="mb-4 aspect-[4/3] rounded-xl border border-border bg-card p-3">
        <div className="relative h-full">
          <AssetPreview a={a} assetId={assetId} />
          <DropdownMenu>
            <DropdownMenuTrigger asChild><button className="absolute right-2 top-2 rounded-lg bg-card/90 p-1 shadow-sm"><MoreHorizontal size={17} /></button></DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem onSelect={() => navigate(`/assets/${assetId}`)}><Eye size={14} />Abrir</DropdownMenuItem>
              <DropdownMenuItem onSelect={handleCopyReference}><Copy size={14} />Copiar referência</DropdownMenuItem>
              <DropdownMenuItem onSelect={handleDownload}><Download size={14} />Baixar</DropdownMenuItem>
              {canEdit && <DropdownMenuItem variant="destructive" onSelect={handleDelete}><Trash2 size={14} />Excluir</DropdownMenuItem>}
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0"><h3 className="truncate font-semibold">{a.name}</h3><p className="text-sm text-muted-foreground">{a.type} · {a.size}</p></div>
        <AssetStatusBadge status={a.status} />
      </div>
      {a.tags && <div className="mt-3 flex flex-wrap gap-1">{a.tags.split(", ").filter(Boolean).map((t) => <Badge key={t}>{t}</Badge>)}</div>}
      <p className="mt-3 text-sm text-muted-foreground">Uso: {a.usage}</p>
      <div className="mt-4 flex gap-2">
        <Button onClick={() => navigate(`/assets/${assetId}`)}>Abrir</Button>
        <Button onClick={handleDownload}>Baixar</Button>
        <Button onClick={() => navigate("/assets/picker")}>Selecionar</Button>
      </div>
    </Card>
  );
}
