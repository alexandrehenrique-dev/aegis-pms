import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router";
import { AlertTriangle } from "lucide-react";
import { Badge, Button, Card, EmptyState, PageHeader, SkeletonLines, PartialErrorWidget } from "../../../shared/components/Primitives";
import { kgColor, type KGEntityType, type KGNode } from "../mocks/knowledge.mocks";
import { knowledgeService } from "../services/knowledgeService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { KGBadge } from "../components/KGBadge";

const RESOURCE_ROUTES: Record<KGEntityType, string> = {
  Tenant: "/settings/tenant", Produto: "/products", Página: "/content/list", Asset: "/assets",
  Formulário: "/forms/list", Submission: "/forms/submissions", Lead: "/forms/submissions",
  Categoria: "/content/list", Tag: "/assets/tags", Autor: "/users", SEO: "/settings/product",
  Tópico: "/content/list", Poema: "/content/list", Manifesto: "/content/list",
  Música: "/knowledge/search", Livro: "/knowledge/search", Playlist: "/knowledge/search",
};

export function EntityDetails() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { data: kgNodes, loading: loadingNodes, error: errorNodes } = useAsyncData(() => knowledgeService.listNodes(), []);
  const { data: kgEdges, loading: loadingEdges, error: errorEdges } = useAsyncData(() => knowledgeService.listEdges(), []);
  const [sel, setSel] = useState<KGNode | null>(null);

  useEffect(() => {
    if (kgNodes && !sel) setSel((id && kgNodes.find((n) => n.id === id)) || kgNodes[2]);
  }, [kgNodes, id, sel]);

  if (loadingNodes || loadingEdges) return <SkeletonLines />;
  if (errorNodes || errorEdges || !kgNodes || !kgEdges || !sel) return <PartialErrorWidget />;

  const c = kgColor[sel.type];
  const incoming = kgEdges.filter((e) => e.to === sel.id).map((e) => kgNodes.find((n) => n.id === e.from)!).filter(Boolean);
  const outgoing = kgEdges.filter((e) => e.from === sel.id).map((e) => kgNodes.find((n) => n.id === e.to)!).filter(Boolean);

  return (
    <>
      <PageHeader title={sel.label} desc="Entidade de negócio: dados, relações e análise de impacto operacional." badge={sel.type}>
        <Button onClick={() => navigate(RESOURCE_ROUTES[sel.type] ?? "/content/list")}>Abrir recurso</Button>
        <Button primary onClick={() => navigate("/knowledge/graph")}>Ver no Graph</Button>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[1fr_280px]">
        <div className="space-y-4">
          <Card>
            <div className="mb-4 flex items-center gap-3">
              <div style={{ background: `${c}18`, borderColor: c }} className="grid h-12 w-12 shrink-0 place-items-center rounded-xl border-2"><span style={{ color: c }} className="text-[10px] font-bold">{sel.type.slice(0, 2).toUpperCase()}</span></div>
              <div><h2 className="text-xl font-semibold">{sel.label}</h2><Badge tone={sel.status === "ativo" || sel.status === "publicado" ? "green" : "amber"}>{sel.status}</Badge></div>
            </div>
            <div className="grid gap-3 md:grid-cols-2">{sel.props.map((p) => <div key={p.k} className="rounded-xl bg-muted p-3 text-sm"><p className="text-xs text-muted-foreground">{p.k}</p><b>{p.v}</b></div>)}</div>
          </Card>
          <Card>
            <h2 className="mb-3 text-lg font-semibold">Impact Analysis</h2>
            <p className="mb-4 text-sm text-muted-foreground">Se <b>{sel.label}</b> for alterado:</p>
            {incoming.length > 0 && (
              <>
                <p className="mb-2 text-sm font-medium flex items-center gap-1.5"><AlertTriangle size={14} className="text-[#dc2626]" />Quem usa esta entidade</p>
                {incoming.map((n) => (
                  <div key={n.id} onClick={() => setSel(n)} className="mb-2 flex cursor-pointer items-center gap-3 rounded-xl border border-[#fee2e2] bg-[#fee2e2]/40 p-3 text-sm hover:bg-[#fee2e2]/70">
                    <div style={{ background: `${kgColor[n.type]}20` }} className="grid h-8 w-8 shrink-0 place-items-center rounded-lg"><span style={{ color: kgColor[n.type] }} className="text-[9px] font-bold">{n.type.slice(0, 2).toUpperCase()}</span></div>
                    <div className="min-w-0"><p className="truncate font-medium">{n.label}</p><p className="text-xs text-muted-foreground">{n.type}</p></div>
                    <Badge tone="red">impactado</Badge>
                  </div>
                ))}
              </>
            )}
            {outgoing.length > 0 && (
              <>
                <p className="mb-2 mt-3 text-sm font-medium flex items-center gap-1.5"><AlertTriangle size={14} className="text-[#d97706]" />O que esta entidade controla</p>
                {outgoing.map((n) => (
                  <div key={n.id} onClick={() => setSel(n)} className="mb-2 flex cursor-pointer items-center gap-3 rounded-xl border border-[#fef3c7] bg-[#fef3c7]/40 p-3 text-sm hover:bg-[#fef3c7]/70">
                    <div style={{ background: `${kgColor[n.type]}20` }} className="grid h-8 w-8 shrink-0 place-items-center rounded-lg"><span style={{ color: kgColor[n.type] }} className="text-[9px] font-bold">{n.type.slice(0, 2).toUpperCase()}</span></div>
                    <div className="min-w-0"><p className="truncate font-medium">{n.label}</p><p className="text-xs text-muted-foreground">{n.type}</p></div>
                    <Badge tone="amber">dependente</Badge>
                  </div>
                ))}
              </>
            )}
            {incoming.length === 0 && outgoing.length === 0 && <EmptyState compact title="Sem dependências" description="Nenhuma entidade conectada." />}
          </Card>
        </div>
        <Card>
          <h2 className="mb-3 font-semibold">Navegar entidades</h2>
          {kgNodes.map((n) => (
            <button key={n.id} onClick={() => setSel(n)} className={`flex w-full items-center gap-2 rounded-xl px-2.5 py-2 text-left text-sm transition hover:bg-muted ${n.id === sel.id ? "bg-muted font-medium" : ""}`}>
              <KGBadge type={n.type} /><span className="truncate">{n.label}</span>
            </button>
          ))}
        </Card>
      </div>
    </>
  );
}
