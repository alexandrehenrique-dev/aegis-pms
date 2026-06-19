import { useState } from "react";
import { useNavigate } from "react-router";
import { Plus, Search } from "lucide-react";
import { Badge, Button, EmptyState, PageHeader, PartialErrorWidget, PermissionHint, SkeletonLines } from "../../../shared/components/Primitives";
import { assetsService } from "../services/assetsService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { AssetCard, AssetStatusBadge } from "../components/AssetBits";

export function AssetLibrary() {
  const navigate = useNavigate();
  const [view, setView] = useState<"grid" | "list">("grid");
  const [q, setQ] = useState("");
  const { data: assets, loading, error } = useAsyncData(() => assetsService.listAssets(), []);

  if (loading) return <SkeletonLines />;
  if (error || !assets) return <PartialErrorWidget />;

  const rows = assets.filter((a) => (a.name + a.tags + a.type).toLowerCase().includes(q.toLowerCase()));

  return (
    <>
      <PageHeader title="Assets" module="Assets" desc="Gerencie imagens, vídeos, documentos e arquivos vinculados a este produto." badge="Maestro Beton">
        <Button onClick={() => navigate("/assets/tags")}>Organizar tags</Button>
        <Button onClick={() => setView(view === "grid" ? "list" : "grid")}>{view === "grid" ? "Lista" : "Grid"}</Button>
        <Button primary onClick={() => navigate("/assets/upload")}><Plus size={15} />Upload de asset</Button>
      </PageHeader>
      <div className="mb-4 rounded-2xl border border-border bg-card p-3">
        <div className="flex items-center gap-2 rounded-xl border border-border px-3 py-2">
          <Search size={16} />
          <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Buscar por nome, tag ou tipo..." className="w-full bg-transparent text-sm outline-none" />
        </div>
        <div className="mt-3 flex flex-wrap gap-2 text-xs">
          <Badge>Tipo: imagem, vídeo, áudio, PDF</Badge>
          <Badge>Status: ativo, processando, arquivado, erro</Badge>
          <Badge>Uso: SEO, página, não utilizado</Badge>
          <Badge>Tag</Badge>
          <Badge>Data de upload</Badge>
        </div>
      </div>
      <div className="mb-4 grid gap-3 md:grid-cols-3">
        <div><p className="mb-2 text-sm font-medium">Loading skeleton</p><SkeletonLines /></div>
        <PartialErrorWidget />
        <PermissionHint />
      </div>
      {rows.length === 0 ? <EmptyState title="Busca sem resultado" description="Nenhum asset corresponde aos filtros atuais." /> : view === "grid" ? (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">{rows.map((a) => <AssetCard key={a.name} a={a} />)}</div>
      ) : (
        <div className="overflow-hidden rounded-2xl border border-border bg-card">
          <table className="hidden w-full text-left text-sm lg:table">
            <thead className="bg-muted text-xs text-muted-foreground"><tr>{["Nome", "Tipo", "Tamanho", "Status", "Tags", "Uso", "Data", "Ações"].map((h) => <th key={h} className="p-3 font-medium">{h}</th>)}</tr></thead>
            <tbody>
              {rows.map((a) => (
                <tr key={a.name} className="border-t border-border hover:bg-muted/40">
                  <td className="p-3">{a.name}</td>
                  <td className="p-3">{a.type}</td>
                  <td className="p-3">{a.size}</td>
                  <td className="p-3"><AssetStatusBadge status={a.status} /></td>
                  <td className="p-3">{a.tags}</td>
                  <td className="p-3">{a.usage}</td>
                  <td className="p-3">{a.uploadedAt}</td>
                  <td className="p-3"><Button onClick={() => navigate(`/assets/${a.name.replace(/\.[a-z0-9]+$/i, "")}`)}>Abrir</Button></td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="grid gap-2 p-3 lg:hidden">{rows.map((a) => <AssetCard key={a.name} a={a} />)}</div>
        </div>
      )}
    </>
  );
}
