import { useMemo, useState } from "react";
import { useNavigate } from "react-router";
import { Search, X } from "lucide-react";
import { Badge, Button, Card, EmptyState, PageHeader, SkeletonLines, PartialErrorWidget } from "../../../shared/components/Primitives";
import { Popover, PopoverContent, PopoverTrigger } from "../../../shared/components/ui/popover";
import { kgColor } from "../mocks/knowledge.mocks";
import { knowledgeService } from "../services/knowledgeService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";

export function EntitySearch() {
  const navigate = useNavigate();
  const { product } = useCurrentProduct();
  const productId = product?.id ?? "p1";
  const [q, setQ] = useState("");
  const [typeFilter, setTypeFilter] = useState<string | null>(null);
  const { data: kgNodes, loading: loadingNodes, error: errorNodes } = useAsyncData(() => knowledgeService.listNodes(productId), [productId]);
  const { data: kgEdges, loading: loadingEdges, error: errorEdges } = useAsyncData(() => knowledgeService.listEdges(productId), [productId]);

  const types = useMemo(() => Array.from(new Set((kgNodes ?? []).map((n) => n.type))), [kgNodes]);

  if (loadingNodes || loadingEdges) return <SkeletonLines />;
  if (errorNodes || errorEdges || !kgNodes || !kgEdges) return <PartialErrorWidget />;

  const res = kgNodes
    .filter((n) => !q || (n.label + n.type + n.status).toLowerCase().includes(q.toLowerCase()))
    .filter((n) => !typeFilter || n.type === typeFilter);
  return (
    <>
      <PageHeader title="Entity Search" desc="Busca por entidades de negócio: conteúdo, assets, formulários, leads, tags e SEO." badge="Busca">
        <Popover>
          <PopoverTrigger asChild><Button>Filtros</Button></PopoverTrigger>
          <PopoverContent>
            <p className="mb-2 text-sm font-medium">Tipo de entidade</p>
            <div className="flex flex-wrap gap-1">
              <Button onClick={() => setTypeFilter(null)} primary={!typeFilter}>Todas</Button>
              {types.map((t) => <Button key={t} onClick={() => setTypeFilter(t)} primary={typeFilter === t}>{t}</Button>)}
            </div>
          </PopoverContent>
        </Popover>
      </PageHeader>
      <div className="mb-4 flex items-center gap-2 rounded-2xl border border-border bg-card px-4 py-3">
        <Search size={17} />
        <input autoFocus value={q} onChange={(e) => setQ(e.target.value)} placeholder="Buscar Página Home, Asset Hero, Camila Rocha..." className="w-full bg-transparent text-sm outline-none" />
        {q && <button onClick={() => setQ("")} className="text-muted-foreground hover:text-foreground"><X size={15} /></button>}
      </div>
      {res.length === 0 ? <EmptyState title="Sem resultados" description="Nenhuma entidade de negócio corresponde à busca." /> : (
        <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
          {res.map((n) => {
            const c = kgColor[n.type] || "#374151";
            return (
              <Card key={n.id} onClick={() => navigate(`/knowledge/entities/${n.id}`)}>
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <span style={{ color: c }} className="text-[10px] font-bold uppercase tracking-wider">{n.type}</span>
                    <h3 className="mt-0.5 font-semibold">{n.label}</h3>
                    <Badge tone={n.status === "ativo" || n.status === "publicado" ? "green" : "amber"}>{n.status}</Badge>
                  </div>
                  <div style={{ background: `${c}14` }} className="grid h-10 w-10 shrink-0 place-items-center rounded-xl"><span style={{ color: c }} className="text-[10px] font-bold">{n.type.slice(0, 2).toUpperCase()}</span></div>
                </div>
                <div className="mt-3 space-y-1">{n.props.slice(0, 2).map((p) => <div key={p.k} className="flex justify-between text-xs"><span className="text-muted-foreground">{p.k}</span><b>{p.v}</b></div>)}</div>
                <p className="mt-2 text-xs text-muted-foreground">{kgEdges.filter((e) => e.from === n.id || e.to === n.id).length} relações</p>
              </Card>
            );
          })}
        </div>
      )}
    </>
  );
}
