import { useEffect, useState } from "react";
import { useNavigate } from "react-router";
import { Plus, Search } from "lucide-react";
import { Badge, Button, EmptyState, PageHeader, PartialErrorWidget, SkeletonLines } from "../../../shared/components/Primitives";
import { PermGate } from "../../../app/guards/PermGate";
import { useViewAsRole } from "../../../core/permissions/useViewAsRole";
import { assetsService } from "../services/assetsService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { AssetCard, AssetStatusBadge } from "../components/AssetBits";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";

export function AssetLibrary() {
  const navigate = useNavigate();
  const { viewAsRole } = useViewAsRole();
  const { product } = useCurrentProduct();
  const productId = product?.id ?? "";
  const canEdit = viewAsRole !== "viewer";
  const [view, setView] = useState<"grid" | "list">("grid");
  const [q, setQ] = useState("");
  const { data: assets, loading, error } = useAsyncData(() => (productId ? assetsService.listAssets(productId) : Promise.resolve([])), [productId]);
  const [visibleAssets, setVisibleAssets] = useState(assets ?? []);

  useEffect(() => setVisibleAssets(assets ?? []), [assets]);

  if (loading) return <SkeletonLines />;
  if (error || !assets) return <PartialErrorWidget />;

  const handleDeleted = (assetId: string) => {
    setVisibleAssets((current) => current.filter((asset) => (asset.id ?? asset.name) !== assetId));
  };

  const rows = visibleAssets.filter((a) => (a.name + a.tags + a.type).toLowerCase().includes(q.toLowerCase()));

  return (
    <>
      <PageHeader title="Assets" module="Assets" desc="Gerencie imagens, vídeos, documentos e arquivos vinculados a este produto." badge={product?.name}>
        <PermGate allowed={canEdit}><Button onClick={() => navigate("/assets/tags")}>Organizar tags</Button></PermGate>
        <Button onClick={() => setView(view === "grid" ? "list" : "grid")}>{view === "grid" ? "Lista" : "Grid"}</Button>
        <PermGate allowed={canEdit}><Button primary onClick={() => navigate("/assets/upload")}><Plus size={15} />Upload de asset</Button></PermGate>
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
      {rows.length === 0 ? <EmptyState title="Busca sem resultado" description="Nenhum asset corresponde aos filtros atuais." /> : view === "grid" ? (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">{rows.map((a) => <AssetCard key={a.name} a={a} productId={productId} canEdit={canEdit} onDeleted={handleDeleted} />)}</div>
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
                  <td className="p-3"><Button onClick={() => navigate(`/assets/${a.id ?? a.name}`)}>Abrir</Button></td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="grid gap-2 p-3 lg:hidden">{rows.map((a) => <AssetCard key={a.name} a={a} productId={productId} canEdit={canEdit} onDeleted={handleDeleted} />)}</div>
        </div>
      )}
    </>
  );
}
