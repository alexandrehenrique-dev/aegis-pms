import { useNavigate } from "react-router";
import { Eye, FileText, Gauge, Image, MoreHorizontal } from "lucide-react";
import { Badge, Button, Card } from "../../../shared/components/Primitives";

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

export function AssetCard({ a }: { a: string[] }) {
  const navigate = useNavigate();
  const slug = a[0].replace(/\.[a-z0-9]+$/i, "");
  return (
    <Card>
      <div className="mb-4 aspect-[4/3] rounded-xl border border-border bg-[linear-gradient(135deg,#EEF3F0,#FFFFFF)] p-3">
        <div className="flex justify-between"><AssetTypeIcon type={a[1]} /><button className="rounded-lg bg-card p-1"><MoreHorizontal size={17} /></button></div>
        <div className="mt-10 h-12 rounded-lg bg-white/70" />
      </div>
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0"><h3 className="truncate font-semibold">{a[0]}</h3><p className="text-sm text-muted-foreground">{a[1]} · {a[2]}</p></div>
        <AssetStatusBadge status={a[3]} />
      </div>
      <div className="mt-3 flex flex-wrap gap-1">{a[4].split(", ").map((t) => <Badge key={t}>{t}</Badge>)}</div>
      <p className="mt-3 text-sm text-muted-foreground">Uso: {a[5]}</p>
      <div className="mt-4 flex gap-2">
        <Button onClick={() => navigate(`/assets/${slug}`)}>Abrir</Button>
        <Button onClick={() => navigate("/assets/picker")}>Selecionar</Button>
      </div>
    </Card>
  );
}
