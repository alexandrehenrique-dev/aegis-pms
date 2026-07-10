import { useState } from "react";
import { useNavigate } from "react-router";
import { CheckCircle2, Sparkles } from "lucide-react";
import { Badge, Button, Card, EmptyState, PageHeader, PartialErrorWidget, SkeletonLines } from "../../../shared/components/Primitives";
import { toast } from "../../../core/notifications/toast";
import { knowledgeService, type KnowledgeInsight } from "../services/knowledgeService";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";

function KnowledgeInsightCard({ insight, reviewed, onReview }: { insight: KnowledgeInsight; reviewed: boolean; onReview: () => void }) {
  const navigate = useNavigate();
  return (
    <Card onClick={() => navigate(insight.targetPath)}>
      <div className="flex justify-between"><Badge tone={insight.severity === "alta" ? "red" : insight.severity === "média" ? "amber" : "blue"}>{insight.severity}</Badge><Sparkles size={17} className="text-primary" /></div>
      <p className="mt-4 font-medium">{insight.text}</p>
      <p className="mt-2 text-sm text-muted-foreground">Ação sugerida vinculada ao recurso afetado.</p>
      <div className="flex gap-2" onClick={(e) => e.stopPropagation()}>
        <Button onClick={() => navigate(insight.targetPath)}>Abrir destino</Button>
        <Button onClick={onReview} disabled={reviewed}>{reviewed ? <><CheckCircle2 size={14} />Revisado</> : "Marcar revisado"}</Button>
      </div>
    </Card>
  );
}

export function KnowledgeInsights() {
  const { product } = useCurrentProduct();
  const productId = product?.id ?? "p1";
  const [reviewed, setReviewed] = useState<Set<string>>(new Set());
  const [revision, setRevision] = useState(0);
  const { data, loading, error } = useAsyncData(() => knowledgeService.listInsights(productId), [productId, revision]);
  const insights = (data ?? []).filter((insight) => !reviewed.has(insight.id));

  const handleReview = async (insight: KnowledgeInsight) => {
    await knowledgeService.markInsightReviewed(productId, insight.text);
    setReviewed((prev) => new Set(prev).add(insight.id));
    toast.success("Insight marcado como revisado.");
  };

  const handleReviewAll = async () => {
    await Promise.all(insights.map((insight) => knowledgeService.markInsightReviewed(productId, insight.text)));
    setReviewed((prev) => new Set([...prev, ...insights.map((insight) => insight.id)]));
    setRevision((current) => current + 1);
    toast.success("Insights marcados como revisados.");
  };

  return (
    <>
      <PageHeader title="Knowledge Insights" module="Knowledge Graph" desc="Inteligência operacional sobre conexões, dependências e lacunas." badge="Insights">
        <Button onClick={handleReviewAll} disabled={loading || insights.length === 0}>Marcar revisado</Button>
      </PageHeader>
      {loading && <SkeletonLines />}
      {error && <PartialErrorWidget />}
      {!loading && !error && insights.length === 0 && <EmptyState title="Sem insights pendentes" description="O produto ainda não tem dados suficientes ou todas as sugestões já foram revisadas." />}
      {!loading && !error && insights.length > 0 && (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {insights.map((insight) => <KnowledgeInsightCard key={insight.id} insight={insight} reviewed={reviewed.has(insight.id)} onReview={() => handleReview(insight)} />)}
        </div>
      )}
    </>
  );
}
