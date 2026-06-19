import { useState } from "react";
import { Search, X } from "lucide-react";
import { Badge, Button, Card, EmptyState, PageHeader, SkeletonLines, PartialErrorWidget } from "../../../shared/components/Primitives";
import { kgColor } from "../mocks/knowledge.mocks";
import { knowledgeService } from "../services/knowledgeService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";

export function EntitySearch() {
  const [q, setQ] = useState("");
  const { data: kgNodes, loading: loadingNodes, error: errorNodes } = useAsyncData(() => knowledgeService.listNodes(), []);
  const { data: kgEdges, loading: loadingEdges, error: errorEdges } = useAsyncData(() => knowledgeService.listEdges(), []);

  if (loadingNodes || loadingEdges) return <SkeletonLines />;
  if (errorNodes || errorEdges || !kgNodes || !kgEdges) return <PartialErrorWidget />;

  const res = q ? kgNodes.filter((n) => (n.label + n.type + n.status).toLowerCase().includes(q.toLowerCase())) : kgNodes;
  return (
    <>
      <PageHeader title="Entity Search" desc="Busca por entidades de negócio: conteúdo, assets, formulários, leads, tags e SEO." badge="Busca">
        <Button>Filtros</Button>
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
              <Card key={n.id}>
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
