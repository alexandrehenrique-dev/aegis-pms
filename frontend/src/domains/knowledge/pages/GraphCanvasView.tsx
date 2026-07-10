import { useMemo, useRef, useState, type PointerEvent as ReactPointerEvent } from "react";
import { AlertTriangle, Info, Search, X, ZoomIn, ZoomOut } from "lucide-react";
import { Button, EmptyState, PageHeader, Card, SkeletonLines, PartialErrorWidget } from "../../../shared/components/Primitives";
import { kgColor, KG_H, KG_W, type KGEdge, type KGNode } from "../mocks/knowledge.mocks";
import { knowledgeService } from "../services/knowledgeService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";

const CANVAS_WIDTH = 864;
const CANVAS_HEIGHT = 570;
const ZOOM_MIN = 0.1;
const ZOOM_MAX = 1.8;
const ZOOM_STEP = 0.1;

function clamp(value: number, min: number, max: number) {
  return Math.min(max, Math.max(min, value));
}

export function GraphCanvasView() {
  const { product } = useCurrentProduct();
  const productId = product?.id ?? "p1";
  const [sel, setSel] = useState<KGNode | null>(null);
  const [tf, setTf] = useState("todos");
  const [q, setQ] = useState("");
  const [zoom, setZoom] = useState(1);
  const [nodePositions, setNodePositions] = useState<Record<string, { x: number; y: number }>>({});
  const canvasViewportRef = useRef<HTMLDivElement>(null);
  const { data: kgNodes, loading: loadingNodes, error: errorNodes } = useAsyncData(() => knowledgeService.listNodes(productId), [productId]);
  const { data: kgEdges, loading: loadingEdges, error: errorEdges } = useAsyncData(() => knowledgeService.listEdges(productId), [productId]);
  const edges = kgEdges ?? [];
  const positionedNodes = useMemo(() => (kgNodes ?? []).map((node, index) => {
    const pos = nodePositions[node.id];
    return {
      ...node,
      x: pos?.x ?? (node.x ?? 40 + (index % 4) * 190),
      y: pos?.y ?? (node.y ?? 40 + Math.floor(index / 4) * 150),
    };
  }), [kgNodes, nodePositions]);

  if (loadingNodes || loadingEdges) return <SkeletonLines />;
  if (errorNodes || errorEdges || !kgNodes || !kgEdges) return <PartialErrorWidget />;

  const nodeIds = new Set(positionedNodes.map((n) => n.id));
  const visibleScopeEdges = edges.filter((e) => nodeIds.has(e.from) && nodeIds.has(e.to));
  const types = [...new Set(positionedNodes.map((n) => n.type))];
  const vis = positionedNodes.filter((n) => (tf === "todos" || n.type === tf) && (q === "" || n.label.toLowerCase().includes(q.toLowerCase())));
  const visIds = new Set(vis.map((n) => n.id));
  const visEdges = visibleScopeEdges.filter((e) => visIds.has(e.from) && visIds.has(e.to));
  const ep = (e: KGEdge) => {
    const fn = positionedNodes.find((n) => n.id === e.from), tn = positionedNodes.find((n) => n.id === e.to);
    if (!fn || !tn) return null;
    const sx = fn.x + KG_W / 2, sy = fn.y + KG_H, tx = tn.x + KG_W / 2, ty = tn.y, my = (sy + ty) / 2;
    return { path: `M ${sx} ${sy} C ${sx} ${my}, ${tx} ${my}, ${tx} ${ty}`, mx: (sx + tx) / 2, my };
  };
  const incoming = sel ? visibleScopeEdges.filter((e) => e.to === sel.id).map((e) => positionedNodes.find((n) => n.id === e.from)!).filter(Boolean) : [];
  const outgoing = sel ? visibleScopeEdges.filter((e) => e.from === sel.id).map((e) => positionedNodes.find((n) => n.id === e.to)!).filter(Boolean) : [];

  const startDrag = (node: KGNode, event: ReactPointerEvent<HTMLButtonElement>) => {
    event.stopPropagation();
    const originX = event.clientX;
    const originY = event.clientY;
    const startX = node.x;
    const startY = node.y;
    const pointerId = event.pointerId;
    event.currentTarget.setPointerCapture(pointerId);
    const handleMove = (moveEvent: PointerEvent) => {
      setNodePositions((current) => ({
        ...current,
        [node.id]: {
          x: clamp(startX + (moveEvent.clientX - originX) / zoom, 8, CANVAS_WIDTH - KG_W - 8),
          y: clamp(startY + (moveEvent.clientY - originY) / zoom, 8, CANVAS_HEIGHT - KG_H - 8),
        },
      }));
    };
    const handleUp = () => {
      window.removeEventListener("pointermove", handleMove);
      window.removeEventListener("pointerup", handleUp);
    };
    window.addEventListener("pointermove", handleMove);
    window.addEventListener("pointerup", handleUp);
  };

  const startCanvasPan = (event: ReactPointerEvent<HTMLDivElement>) => {
    if (event.button !== 0) return;
    if (event.target instanceof HTMLElement && event.target.closest("[data-kg-node]")) return;
    const viewport = canvasViewportRef.current;
    if (!viewport) return;
    event.preventDefault();
    const originX = event.clientX;
    const originY = event.clientY;
    const startScrollLeft = viewport.scrollLeft;
    const startScrollTop = viewport.scrollTop;
    const handleMove = (moveEvent: PointerEvent) => {
      viewport.scrollLeft = startScrollLeft - (moveEvent.clientX - originX);
      viewport.scrollTop = startScrollTop - (moveEvent.clientY - originY);
    };
    const handleUp = () => {
      window.removeEventListener("pointermove", handleMove);
      window.removeEventListener("pointerup", handleUp);
    };
    window.addEventListener("pointermove", handleMove);
    window.addEventListener("pointerup", handleUp);
  };

  return (
    <>
      <PageHeader title="Graph Canvas" desc="Mapa de entidades de negócio e dependências. Clique num nó para ver o impacto." badge="Knowledge Graph">
        <div className="flex items-center gap-2 rounded-xl border border-border bg-card px-2 py-1 text-sm text-muted-foreground">
          <button
            type="button"
            onClick={() => setZoom((current) => clamp(Number((current - ZOOM_STEP).toFixed(2)), ZOOM_MIN, ZOOM_MAX))}
            className="rounded-lg p-1 hover:bg-muted"
            aria-label="Diminuir zoom"
          >
            <ZoomOut size={15} />
          </button>
          <span className="w-12 text-center text-xs font-medium">{Math.round(zoom * 100)}%</span>
          <button
            type="button"
            onClick={() => setZoom((current) => clamp(Number((current + ZOOM_STEP).toFixed(2)), ZOOM_MIN, ZOOM_MAX))}
            className="rounded-lg p-1 hover:bg-muted"
            aria-label="Aumentar zoom"
          >
            <ZoomIn size={15} />
          </button>
        </div>
        <Button onClick={() => setSel(null)}>Limpar seleção</Button>
      </PageHeader>
      <div className="mb-3 flex flex-wrap gap-2">
        <div className="flex items-center gap-2 rounded-xl border border-border bg-card px-3 py-1.5">
          <Search size={13} className="text-muted-foreground" />
          <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Buscar entidade..." className="w-36 bg-transparent text-sm outline-none" />
          {q && <button onClick={() => setQ("")} className="text-muted-foreground"><X size={12} /></button>}
        </div>
        {["todos", ...types].map((t) => (
          <button key={t} onClick={() => setTf(t)} className={`rounded-xl border px-2.5 py-1.5 text-xs transition ${tf === t ? "border-primary bg-primary/5 text-primary font-medium" : "border-border bg-card text-muted-foreground hover:bg-muted"}`}>{t}</button>
        ))}
      </div>
      <div className="grid gap-4 xl:grid-cols-[1fr_296px]">
        <div ref={canvasViewportRef} onPointerDown={startCanvasPan} className="cursor-grab overflow-auto rounded-2xl border border-border active:cursor-grabbing">
          <div style={{ width: CANVAS_WIDTH * zoom, height: CANVAS_HEIGHT * zoom }}>
          <div className="relative" style={{ width: CANVAS_WIDTH, height: CANVAS_HEIGHT, transform: `scale(${zoom})`, transformOrigin: "top left", backgroundImage: "radial-gradient(var(--border) 1px,transparent 1px)", backgroundSize: "24px 24px" }}>
            <svg width={CANVAS_WIDTH} height={CANVAS_HEIGHT} className="pointer-events-none absolute inset-0">
              <defs>
                <marker id="kga" markerWidth="7" markerHeight="7" refX="6" refY="3.5" orient="auto"><polygon points="0 0,7 3.5,0 7" fill="#a1a1aa" /></marker>
                <marker id="kgah" markerWidth="7" markerHeight="7" refX="6" refY="3.5" orient="auto"><polygon points="0 0,7 3.5,0 7" fill="var(--primary)" /></marker>
              </defs>
              {visEdges.map((e) => {
                const r = ep(e);
                if (!r) return null;
                const hi = !!(sel && (e.from === sel.id || e.to === sel.id));
                return (
                  <g key={`${e.from}${e.to}`}>
                    <path d={r.path} fill="none" stroke={hi ? "var(--primary)" : "#d4d4d8"} strokeWidth={hi ? 2 : 1.5} markerEnd={hi ? "url(#kgah)" : "url(#kga)"} />
                    <rect x={r.mx - 24} y={r.my - 8} width={48} height={16} rx={5} fill="var(--card)" opacity={0.9} />
                    <text x={r.mx} y={r.my + 5} textAnchor="middle" fontSize={9} fill={hi ? "var(--primary)" : "#a1a1aa"}>{e.verb}</text>
                  </g>
                );
              })}
            </svg>
            {vis.map((n) => {
              const c = kgColor[n.type] || "#374151";
              const isSel = sel?.id === n.id;
              const isRel = !!(sel && visibleScopeEdges.some((e) => (e.from === sel.id && e.to === n.id) || (e.to === sel.id && e.from === n.id)));
              return (
                <button key={n.id} data-kg-node onPointerDown={(event) => startDrag(n, event)} onClick={() => setSel(isSel ? null : n)} style={{ left: n.x, top: n.y, width: KG_W, height: KG_H, borderColor: isSel ? c : isRel ? c + "88" : "transparent", background: `${c}14` }} className={`absolute flex cursor-grab flex-col justify-center rounded-xl border-2 px-2.5 transition hover:border-current hover:shadow-md active:cursor-grabbing ${isSel ? "shadow-[0_0_0_3px_rgba(124,58,237,0.18)]" : ""}`}>
                  <span style={{ color: c }} className="text-[8px] font-bold uppercase tracking-widest">{n.type}</span>
                  <span className="mt-0.5 w-full truncate text-sm font-semibold leading-tight text-foreground">{n.label}</span>
                  <span className="text-[9px] capitalize text-muted-foreground">{n.status}</span>
                </button>
              );
            })}
          </div>
          </div>
        </div>
        {sel ? (
          <div className="space-y-3">
            <Card>
              <div className="mb-3 flex items-start justify-between">
                <div>
                  <span style={{ color: kgColor[sel.type] }} className="text-[10px] font-bold uppercase tracking-wider">{sel.type}</span>
                  <h3 className="mt-0.5 font-semibold leading-snug">{sel.label}</h3>
                  <span className={`rounded-full px-2 py-1 text-[11px] font-medium ${sel.status === "ativo" || sel.status === "publicado" ? "bg-[#dcfce7] text-[#15803d]" : sel.status === "qualificado" ? "bg-[#ede9fe] text-[#7c3aed]" : "bg-[#fef3c7] text-[#b45309]"}`}>{sel.status}</span>
                </div>
                <button onClick={() => setSel(null)} className="rounded-lg p-1 hover:bg-muted"><X size={14} /></button>
              </div>
              {sel.props.map((p) => <div key={p.k} className="flex justify-between border-b border-border py-1.5 text-xs last:border-0"><span className="text-muted-foreground">{p.k}</span><b>{p.v}</b></div>)}
            </Card>
            <Card>
              <h3 className="mb-1 font-semibold text-sm">Impact Analysis</h3>
              <p className="mb-3 text-xs text-muted-foreground">Se <b>{sel.label}</b> for alterado:</p>
              {incoming.length > 0 && (
                <>
                  <p className="mb-1.5 flex items-center gap-1 text-xs font-medium"><AlertTriangle size={11} className="text-[#dc2626]" />Quem usa ({incoming.length})</p>
                  {incoming.map((n) => <div key={n.id} onClick={() => setSel(n)} className="mb-1 flex cursor-pointer items-center gap-2 rounded-lg bg-[#fee2e2]/50 px-2 py-1.5 text-xs hover:bg-[#fee2e2]/80"><span style={{ color: kgColor[n.type] }} className="font-semibold">{n.type}</span><span className="truncate">{n.label}</span></div>)}
                </>
              )}
              {outgoing.length > 0 && (
                <>
                  <p className="mb-1.5 mt-2 flex items-center gap-1 text-xs font-medium"><AlertTriangle size={11} className="text-[#d97706]" />O que controla ({outgoing.length})</p>
                  {outgoing.map((n) => <div key={n.id} onClick={() => setSel(n)} className="mb-1 flex cursor-pointer items-center gap-2 rounded-lg bg-[#fef3c7]/50 px-2 py-1.5 text-xs hover:bg-[#fef3c7]/80"><span style={{ color: kgColor[n.type] }} className="font-semibold">{n.type}</span><span className="truncate">{n.label}</span></div>)}
                </>
              )}
              {incoming.length === 0 && outgoing.length === 0 && <EmptyState compact title="Sem dependências" description="Nenhuma entidade conectada." />}
            </Card>
            <Card>
              <h3 className="mb-2 font-semibold text-sm">Relações ({visibleScopeEdges.filter((e) => e.from === sel.id || e.to === sel.id).length})</h3>
              {visibleScopeEdges.filter((e) => e.from === sel.id || e.to === sel.id).map((e) => {
                const other = e.from === sel.id ? positionedNodes.find((n) => n.id === e.to) : positionedNodes.find((n) => n.id === e.from);
                if (!other) return null;
                return (
                  <div key={`${e.from}${e.to}`} onClick={() => setSel(other)} className="flex cursor-pointer items-center gap-2 rounded-xl px-2 py-1.5 text-xs transition hover:bg-muted">
                    <span className={`font-mono ${e.from === sel.id ? "text-primary" : "text-muted-foreground"}`}>{e.from === sel.id ? "→" : "←"}</span>
                    <span className="italic text-muted-foreground">{e.verb}</span>
                    <span className="font-medium">{other.label}</span>
                  </div>
                );
              })}
            </Card>
          </div>
        ) : (
          <Card>
            <EmptyState compact title="Selecione um nó" description="Clique em qualquer entidade para ver detalhes, relações e análise de impacto." />
            {/* Tarefa G.1 — não existe (nem está prevista) uma UI manual de "desenhar uma conexão"; sem isto, a sensação era de grafo "só mock", sem entender o mecanismo real de criação de aresta. */}
            <div className="mt-3 flex items-start gap-2 rounded-xl border border-border bg-muted/40 p-3 text-xs text-muted-foreground">
              <Info size={14} className="mt-0.5 shrink-0 text-primary" />
              <p>Conexões são criadas automaticamente ao referenciar <code className="rounded bg-muted px-1 py-0.5">{"{{kg-ref:nodeId:Label}}"}</code> no corpo de um conteúdo (domínio Conteúdo) — não há, ainda, uma forma de desenhar uma aresta manualmente aqui no canvas.</p>
            </div>
            <div className="mt-4 space-y-1">
              <p className="mb-2 text-xs font-medium">Tipos de entidade</p>
              {Object.entries(kgColor).map(([type, color]) => (
                <div key={type} onClick={() => setTf(type === tf ? "todos" : type)} className="flex cursor-pointer items-center gap-2 py-0.5 text-xs text-muted-foreground hover:text-foreground">
                  <span className="h-2.5 w-2.5 shrink-0 rounded-full" style={{ backgroundColor: color }} />
                  <span>{type}</span>
                  <span className="ml-auto">{positionedNodes.filter((n) => n.type === type).length}</span>
                </div>
              ))}
            </div>
          </Card>
        )}
      </div>
    </>
  );
}
