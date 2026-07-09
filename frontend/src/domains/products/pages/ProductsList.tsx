import { useState } from "react";
import { useNavigate } from "react-router";
import { Plus, Search, X } from "lucide-react";
import { Button, EmptyState, PageHeader } from "../../../shared/components/Primitives";
import { ProductStatusBadge } from "../../../shared/components/ProductStatusBadge";
import { ProductCard } from "../components/ProductCard";
import { useAuth } from "../../../core/auth/useAuth";
import { getProductSlug } from "../../../shared/utils/productSlugs";
import type { ProductStatus } from "../../../shared/types";
import type { ProductSummary } from "../contracts/responses";

export function ProductsList() {
  const navigate = useNavigate();
  const { tenantProducts, switchProduct } = useAuth();
  const [view, setView] = useState<"grid" | "list">("grid");
  const [q, setQ] = useState("");
  const [sf, setSf] = useState<ProductStatus | "todos">("todos");
  const statuses: Array<ProductStatus | "todos"> = ["todos", "Ativo", "Pendente", "Arquivado", "Sem módulos"];
  const products: ProductSummary[] = tenantProducts.map((product) => ({
    ...product,
    last: product.status === "Arquivado" ? "Produto arquivado" : "Acesso concedido neste tenant",
    score: product.status === "Ativo" ? "Operacional" : "Requer atenção",
  }));

  const filtered = products.filter((p) => p.name.toLowerCase().includes(q.toLowerCase()) && (sf === "todos" || p.status === sf));

  return (
    <>
      <PageHeader title="Produtos" module="Produtos" desc="Administre os produtos digitais deste tenant.">
        <Button onClick={() => setView(view === "grid" ? "list" : "grid")}>{view === "grid" ? "Lista" : "Grid"}</Button>
        <Button data-tour="products-novo" primary onClick={() => navigate("/products/new")}><Plus size={15} />Novo produto</Button>
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
        <div data-tour="products-list" className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{filtered.map((p) => <ProductCard key={p.name} p={p} />)}</div>
      ) : (
        <div className="space-y-2">
          {filtered.map((p) => (
            <div key={p.name} className="flex flex-col gap-3 rounded-xl border border-border bg-card p-4 md:flex-row md:items-center md:justify-between">
              <div><p className="font-semibold">{p.name}</p><p className="text-sm text-muted-foreground">{p.type} · {p.last}</p></div>
              <div className="flex items-center gap-2">
                <ProductStatusBadge status={p.status} />
                <Button onClick={() => { if (p.id) switchProduct(p.id); navigate(`/products/${getProductSlug(p.name)}${p.modules ? "" : "?empty=1"}`); }}>Abrir</Button>
              </div>
            </div>
          ))}
        </div>
      )}
    </>
  );
}
