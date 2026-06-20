import { useNavigate } from "react-router";
import { Archive, Copy, Eye, FileText, Gauge, Image, MoreHorizontal } from "lucide-react";
import { Badge, Button, Card } from "../../../shared/components/Primitives";
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "../../../shared/components/ui/dropdown-menu";
import { toast } from "../../../core/notifications/toast";
import { assetsService } from "../services/assetsService";
import type { AssetSummary } from "../contracts/responses";

export function AssetStatusBadge({ status }: { status: string }) {
  return <Badge tone={status === "ativo" ? "green" : status === "processando" ? "amber" : status === "erro" ? "red" : "neutral"}>{status}</Badge>;
}

export function AssetTypeIcon({ type }: { type: string }) {
  return (
    <div className="grid h-10 w-10 place-items-center rounded-xl bg-muted text-primary">
      {type === "vídeo" ? <Eye size={18} /> : type === "áudio" ? <Gauge size={18} /> : type === "PDF" ? <FileText size={18} /> : <Image size={18} />}
    </div>
  );
}

export function AssetCard({ a }: { a: AssetSummary }) {
  const navigate = useNavigate();
  const slug = a.name.replace(/\.[a-z0-9]+$/i, "");

  const handleCopyReference = async () => {
    await navigator.clipboard.writeText(`/assets/${a.name}`);
    toast.success("Copiado");
  };

  const handleArchive = async () => {
    await assetsService.archiveAsset(a.name);
    toast.success("Asset arquivado.", { description: a.name });
  };

  return (
    <Card>
      <div className="mb-4 aspect-[4/3] rounded-xl border border-border bg-[linear-gradient(135deg,#EEF3F0,#FFFFFF)] p-3">
        <div className="flex justify-between">
          <AssetTypeIcon type={a.type} />
          <DropdownMenu>
            <DropdownMenuTrigger asChild><button className="rounded-lg bg-card p-1"><MoreHorizontal size={17} /></button></DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem onSelect={() => navigate(`/assets/${slug}`)}><Eye size={14} />Abrir</DropdownMenuItem>
              <DropdownMenuItem onSelect={handleCopyReference}><Copy size={14} />Copiar referência</DropdownMenuItem>
              <DropdownMenuItem variant="destructive" onSelect={handleArchive}><Archive size={14} />Arquivar</DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
        <div className="mt-10 h-12 rounded-lg bg-white/70" />
      </div>
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0"><h3 className="truncate font-semibold">{a.name}</h3><p className="text-sm text-muted-foreground">{a.type} · {a.size}</p></div>
        <AssetStatusBadge status={a.status} />
      </div>
      <div className="mt-3 flex flex-wrap gap-1">{a.tags.split(", ").map((t) => <Badge key={t}>{t}</Badge>)}</div>
      <p className="mt-3 text-sm text-muted-foreground">Uso: {a.usage}</p>
      <div className="mt-4 flex gap-2">
        <Button onClick={() => navigate(`/assets/${slug}`)}>Abrir</Button>
        <Button onClick={() => navigate("/assets/picker")}>Selecionar</Button>
      </div>
    </Card>
  );
}
