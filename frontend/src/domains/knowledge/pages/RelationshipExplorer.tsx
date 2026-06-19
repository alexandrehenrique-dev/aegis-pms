import { Filter } from "lucide-react";
import { Badge, Button, Card, PageHeader, SkeletonLines, PartialErrorWidget } from "../../../shared/components/Primitives";
import { knowledgeService } from "../services/knowledgeService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { KGBadge } from "../components/KGBadge";

export function RelationshipExplorer() {
  const { data: kgNodes, loading: loadingNodes, error: errorNodes } = useAsyncData(() => knowledgeService.listNodes(), []);
  const { data: kgEdges, loading: loadingEdges, error: errorEdges } = useAsyncData(() => knowledgeService.listEdges(), []);

  if (loadingNodes || loadingEdges) return <SkeletonLines />;
  if (errorNodes || errorEdges || !kgNodes || !kgEdges) return <PartialErrorWidget />;

  return (
    <>
      <PageHeader title="Relationship Explorer" desc="Relações reais entre entidades de negócio do produto Maestro Beton." badge="Relações">
        <Button><Filter size={15} />Filtrar</Button>
        <Button primary>Exportar</Button>
      </PageHeader>
      <Card>
        <div className="hidden overflow-auto rounded-xl border border-border lg:block">
          <table className="w-full text-left text-sm">
            <thead className="bg-muted text-xs text-muted-foreground"><tr>{["Origem", "Relação", "Destino", "Tipo Origem", "Tipo Destino"].map((h) => <th key={h} className="p-3">{h}</th>)}</tr></thead>
            <tbody>
              {kgEdges.map((e) => {
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
          {kgEdges.map((e) => {
            const fn = kgNodes.find((n) => n.id === e.from), tn = kgNodes.find((n) => n.id === e.to);
            if (!fn || !tn) return null;
            return <div key={`${e.from}${e.to}m`} className="rounded-xl border border-border p-3 text-sm"><b>{fn.label}</b><span className="mx-2 italic text-muted-foreground">{e.verb}</span><b>{tn.label}</b></div>;
          })}
        </div>
      </Card>
    </>
  );
}
