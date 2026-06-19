import { useNavigate } from "react-router";
import { Sparkles } from "lucide-react";
import { Badge, Button, Card, PageHeader } from "../../../shared/components/Primitives";

function KnowledgeInsightCard({ text, severity }: { text: string; severity: string }) {
  const navigate = useNavigate();
  return (
    <Card onClick={() => navigate("/knowledge/entities/pg-home")}>
      <div className="flex justify-between"><Badge tone={severity === "alta" ? "red" : severity === "média" ? "amber" : "blue"}>{severity}</Badge><Sparkles size={17} className="text-primary" /></div>
      <p className="mt-4 font-medium">{text}</p>
      <p className="mt-2 text-sm text-muted-foreground">Ação sugerida vinculada ao recurso afetado.</p>
      <Button onClick={() => navigate("/knowledge/entities/pg-home")}>Abrir entidade</Button>
    </Card>
  );
}

export function KnowledgeInsights() {
  const insights: [string, string][] = [
    ["5 assets não estão sendo utilizados.", "média"],
    ["Página Home possui 12 dependências.", "alta"],
    ["SEO está conectado a apenas 40% das páginas.", "alta"],
    ["Existem 3 formulários sem relacionamento.", "média"],
    ["Categorias legadas podem ser mescladas.", "baixa"],
    ["Hero Image impacta SEO e preview público.", "alta"],
  ];
  return (
    <>
      <PageHeader title="Knowledge Insights" module="Knowledge Graph" desc="Inteligência operacional sobre conexões, dependências e lacunas." badge="Insights">
        <Button>Marcar revisado</Button>
      </PageHeader>
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{insights.map((i) => <KnowledgeInsightCard key={i[0]} text={i[0]} severity={i[1]} />)}</div>
    </>
  );
}
