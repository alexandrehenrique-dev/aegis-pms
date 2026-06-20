import { useState } from "react";
import { useNavigate } from "react-router";
import { Pencil, Plus, Search, Trash2, X } from "lucide-react";
import { Button, EmptyState, PageHeader, SkeletonLines, PartialErrorWidget } from "../../../shared/components/Primitives";
import { ProductStatusBadge } from "../../../shared/components/ProductStatusBadge";
import { ProductCard } from "../components/ProductCard";
import { productsService } from "../services/productsService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { useProductActions } from "../hooks/useProductActions";
import type { ProductSummary } from "../contracts/responses";
import type { ProductStatus } from "../../../shared/types";

function ProductListRow({ p, onChanged }: { p: ProductSummary; onChanged: () => void }) {
  const navigate = useNavigate();
  const { canManage, onContextMenu, openEdit, openDelete, portal } = useProductActions(p, onChanged);

  return (
    <div onContextMenu={onContextMenu} className="flex flex-col gap-3 rounded-xl border border-border bg-card p-4 md:flex-row md:items-center md:justify-between">
      {portal}
      <div><p className="font-semibold">{p.name}</p><p className="text-sm text-muted-foreground">{p.type} · {p.last}</p></div>
      <div className="flex items-center gap-2">
        <ProductStatusBadge status={p.status} />
        <Button onClick={() => navigate(p.modules ? "/products/maestro-beton" : "/products/maestro-beton?empty=1")}>Abrir</Button>
        {canManage && <Button onClick={openEdit}><Pencil size={14} />Editar</Button>}
        {canManage && <Button onClick={openDelete}><Trash2 size={14} />Excluir</Button>}
      </div>
    </div>
  );
}

export function ProductsList() {
  const navigate = useNavigate();
  const [view, setView] = useState<"grid" | "list">("grid");
  const [q, setQ] = useState("");
  const [sf, setSf] = useState<ProductStatus | "todos">("todos");
  const [reloadKey, setReloadKey] = useState(0);
  const refresh = () => setReloadKey((k) => k + 1);
  const { data: products, loading, error } = useAsyncData(() => productsService.listProducts(), [reloadKey]);
  const statuses: Array<ProductStatus | "todos"> = ["todos", "Ativo", "Pendente", "Arquivado", "Sem módulos"];

  if (loading) return <SkeletonLines />;
  if (error || !products) return <PartialErrorWidget />;

  const filtered = products.filter((p) => p.name.toLowerCase().includes(q.toLowerCase()) && (sf === "todos" || p.status === sf));

  return (
    <>
      <PageHeader title="Produtos" module="Produtos" desc="Administre os produtos digitais deste tenant. Botão direito sobre um produto para editar ou excluir.">
        <Button onClick={() => setView(view === "grid" ? "list" : "grid")}>{view === "grid" ? "Lista" : "Grid"}</Button>
        <Button primary onClick={() => navigate("/products/new")}><Plus size={15} />Novo produto</Button>
      </PageHeader>
      <div className="mb-4 space-y-2">
        <div className="flex items-center gap-2 rounded-xl border border-border bg-card px-3 py-2">
          <Search size={16} />
          <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Buscar Maestro, Genesis, WikiDev..." className="w-full bg-transparent text-sm outline-none" />
          {q && <button onClick={() => setQ("")} className="text-muted-foreground hover:text-foreground"><X size={14} /></button>}
        </div>
        <div className="flex gap-1.5 overflow-x-auto pb-px">
          {statuses.map((s) => (
            <button key={s} onClick={() => setSf(s)} className={`whitespace-nowrap rounded-xl border px-3 py-1 text-sm transition ${sf === s ? "border-primary bg-primary/5 text-primary font-medium" : "border-border bg-card text-muted-foreground hover:bg-muted"}`}>{s === "todos" ? "Todos" : s}</button>
          ))}
        </div>
      </div>
      {filtered.length === 0 ? (
        <EmptyState title="Busca sem resultado" description="Ajuste filtros por tipo, status ou limpe a busca." />
      ) : view === "grid" ? (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{filtered.map((p) => <ProductCard key={p.name} p={p} onChanged={refresh} />)}</div>
      ) : (
        <div className="space-y-2">
          {filtered.map((p) => <ProductListRow key={p.name} p={p} onChanged={refresh} />)}
        </div>
      )}
    </>
  );
}
