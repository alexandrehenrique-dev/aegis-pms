import { useMemo, useState } from "react";
import { Filter } from "lucide-react";
import { Badge, Button, Card, PageHeader, SkeletonLines, PartialErrorWidget } from "../../../shared/components/Primitives";
import { Popover, PopoverContent, PopoverTrigger } from "../../../shared/components/ui/popover";
import { knowledgeService } from "../services/knowledgeService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { KGBadge } from "../components/KGBadge";
import { toast } from "../../../core/notifications/toast";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";
import type { KGEdge, KGNode } from "../mocks/knowledge.mocks";

function exportEdgesCsv(edges: KGEdge[], nodes: KGNode[]) {
  const header = ["Origem", "Relação", "Destino"];
  const rows = edges.map((e) => [nodes.find((n) => n.id === e.from)?.label ?? e.from, e.verb, nodes.find((n) => n.id === e.to)?.label ?? e.to]);
  const csv = [header, ...rows].map((r) => r.map((c) => `"${c}"`).join(",")).join("\n");
  const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = "relacionamentos.csv";
  a.click();
  URL.revokeObjectURL(url);
}

export function RelationshipExplorer() {
  const { product } = useCurrentProduct();
  const productId = product?.id ?? "p1";
  const { data: kgNodes, loading: loadingNodes, error: errorNodes } = useAsyncData(() => knowledgeService.listNodes(productId), [productId]);
  const { data: kgEdges, loading: loadingEdges, error: errorEdges } = useAsyncData(() => knowledgeService.listEdges(productId), [productId]);
  const [verbFilter, setVerbFilter] = useState<string | null>(null);

  const verbs = useMemo(() => Array.from(new Set((kgEdges ?? []).map((e) => e.verb))), [kgEdges]);

  if (loadingNodes || loadingEdges) return <SkeletonLines />;
  if (errorNodes || errorEdges || !kgNodes || !kgEdges) return <PartialErrorWidget />;

  const filteredEdges = kgEdges.filter((e) => !verbFilter || e.verb === verbFilter);

  return (
    <>
      <PageHeader title="Relationship Explorer" desc="Relações reais entre entidades de negócio do produto Maestro Beton." badge="Relações">
        <Popover>
          <PopoverTrigger asChild><Button><Filter size={15} />Filtrar</Button></PopoverTrigger>
          <PopoverContent>
            <p className="mb-2 text-sm font-medium">Tipo de relação</p>
            <div className="flex flex-wrap gap-1">
              <Button onClick={() => setVerbFilter(null)} primary={!verbFilter}>Todas</Button>
              {verbs.map((v) => <Button key={v} onClick={() => setVerbFilter(v)} primary={verbFilter === v}>{v}</Button>)}
            </div>
          </PopoverContent>
        </Popover>
        <Button primary onClick={() => { exportEdgesCsv(filteredEdges, kgNodes); toast.success("Relações exportadas!"); }}>Exportar</Button>
      </PageHeader>
      <Card>
        <div className="hidden overflow-auto rounded-xl border border-border lg:block">
          <table className="w-full text-left text-sm">
            <thead className="bg-muted text-xs text-muted-foreground"><tr>{["Origem", "Relação", "Destino", "Tipo Origem", "Tipo Destino"].map((h) => <th key={h} className="p-3">{h}</th>)}</tr></thead>
            <tbody>
              {filteredEdges.map((e) => {
                const fn = kgNodes.find((n) => n.id === e.from), tn = kgNodes.find((n) => n.id === e.to);
                if (!fn || !tn) return null;
                return (
                  <tr key={`${e.from}${e.to}`} className="border-t border-border hover:bg-muted/40">
                    <td className="p-3 font-medium">{fn.label}</td>
                    <td className="p-3"><Badge tone="neutral">{e.verb}</Badge></td>
                    <td className="p-3">{tn.label}</td>
                    <td className="p-3"><KGBadge type={fn.type} /></td>
                    <td className="p-3"><KGBadge type={tn.type} /></td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
        <div className="grid gap-2 lg:hidden">
          {filteredEdges.map((e) => {
            const fn = kgNodes.find((n) => n.id === e.from), tn = kgNodes.find((n) => n.id === e.to);
            if (!fn || !tn) return null;
            return <div key={`${e.from}${e.to}m`} className="rounded-xl border border-border p-3 text-sm"><b>{fn.label}</b><span className="mx-2 italic text-muted-foreground">{e.verb}</span><b>{tn.label}</b></div>;
          })}
        </div>
      </Card>
    </>
  );
}
