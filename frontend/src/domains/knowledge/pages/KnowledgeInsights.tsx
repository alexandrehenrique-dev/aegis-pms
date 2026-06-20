import { useState } from "react";
import { useNavigate } from "react-router";
import { CheckCircle2, Sparkles } from "lucide-react";
import { Badge, Button, Card, PageHeader } from "../../../shared/components/Primitives";
import { toast } from "../../../core/notifications/toast";
import { knowledgeService } from "../services/knowledgeService";

function KnowledgeInsightCard({ text, severity, reviewed, onReview }: { text: string; severity: string; reviewed: boolean; onReview: () => void }) {
  const navigate = useNavigate();
  return (
    <Card onClick={() => navigate("/knowledge/entities/pg-home")}>
      <div className="flex justify-between"><Badge tone={severity === "alta" ? "red" : severity === "média" ? "amber" : "blue"}>{severity}</Badge><Sparkles size={17} className="text-primary" /></div>
      <p className="mt-4 font-medium">{text}</p>
      <p className="mt-2 text-sm text-muted-foreground">Ação sugerida vinculada ao recurso afetado.</p>
      <div className="flex gap-2" onClick={(e) => e.stopPropagation()}>
        <Button onClick={() => navigate("/knowledge/entities/pg-home")}>Abrir entidade</Button>
        <Button onClick={onReview} disabled={reviewed}>{reviewed ? <><CheckCircle2 size={14} />Revisado</> : "Marcar revisado"}</Button>
      </div>
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
  const [reviewed, setReviewed] = useState<Set<string>>(new Set());

  const handleReview = async (text: string) => {
    await knowledgeService.markInsightReviewed(text);
    setReviewed((prev) => new Set(prev).add(text));
    toast.success("Insight marcado como revisado.");
  };

  return (
    <>
      <PageHeader title="Knowledge Insights" module="Knowledge Graph" desc="Inteligência operacional sobre conexões, dependências e lacunas." badge="Insights">
        <Button onClick={() => insights.forEach((i) => handleReview(i[0]))}>Marcar revisado</Button>
      </PageHeader>
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {insights.map((i) => <KnowledgeInsightCard key={i[0]} text={i[0]} severity={i[1]} reviewed={reviewed.has(i[0])} onReview={() => handleReview(i[0])} />)}
      </div>
    </>
  );
}
