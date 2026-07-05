import { useState } from "react";
import { useNavigate } from "react-router";
import { CheckCircle2 } from "lucide-react";
import { Badge, Button, Card, EmptyState, PageHeader, PartialErrorWidget, SkeletonLines } from "../../../shared/components/Primitives";
import { toast } from "../../../core/notifications/toast";
import { analyticsService } from "../services/analyticsService";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import type { TrendCardResponse } from "../contracts/responses";

function TrendCard({ trend, reviewed, onReview }: { trend: TrendCardResponse; reviewed: boolean; onReview: () => void }) {
  const navigate = useNavigate();
  const dest = trend.severity === "positiva" ? "/analytics/content" : "/analytics";
  return (
    <Card onClick={() => navigate(dest)}>
      <div className="flex justify-between"><Badge tone={trend.severity === "alta" ? "amber" : trend.severity === "positiva" ? "green" : "blue"}>{trend.type}</Badge><span className="font-mono text-xs text-muted-foreground">{trend.metric}</span></div>
      <p className="mt-4 font-medium">{trend.text}</p>
      <p className="mt-2 text-sm text-muted-foreground">Ação recomendada: {trend.severity === "positiva" ? "ampliar aprendizado" : "investigar e corrigir"}.</p>
      <div className="flex gap-2" onClick={(e) => e.stopPropagation()}>
        <Button onClick={() => navigate(dest)}>Executar ação</Button>
        <Button onClick={onReview} disabled={reviewed}>{reviewed ? <><CheckCircle2 size={14} />Revisado</> : "Marcar revisado"}</Button>
      </div>
    </Card>
  );
}

export function TrendCards() {
  const { product } = useCurrentProduct();
  const productId = product?.id ?? "p1";
  const { data: trends, loading, error } = useAsyncData(() => analyticsService.listTrends(productId), [productId]);
  const [reviewed, setReviewed] = useState<Set<string>>(new Set());

  const handleReview = async (label: string) => {
    await analyticsService.markTrendReviewed(productId, label);
    setReviewed((prev) => new Set(prev).add(label));
    toast.success("Tendência marcada como revisada.");
  };

  return (
    <>
      <PageHeader title="Trend Cards" module="Analytics" desc="Tendências e insights operacionais que geram ação." badge="Insights">
        <Button onClick={() => (trends ?? []).forEach((trend) => handleReview(trend.type))}>Marcar revisado</Button>
      </PageHeader>
      {loading ? <SkeletonLines /> : error ? <PartialErrorWidget /> : !trends || trends.length === 0 ? (
        <EmptyState title="Sem tendências disponíveis" description="Quando houver sinais suficientes, os insights aparecem aqui." />
      ) : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {trends.map((trend) => <TrendCard key={trend.type} trend={trend} reviewed={reviewed.has(trend.type) || trend.reviewed} onReview={() => handleReview(trend.type)} />)}
        </div>
      )}
    </>
  );
}
