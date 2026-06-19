import { Button, Card, PageHeader, SkeletonLines, PartialErrorWidget } from "../../../shared/components/Primitives";
import { ComparisonBadge, InsightPanel } from "../components/AnalyticsBits";
import { analyticsService } from "../services/analyticsService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import type { HealthSignal } from "../contracts/responses";

function HealthScoreCard({ h }: { h: HealthSignal }) {
  return (
    <Card>
      <div className="flex items-center justify-between"><h3 className="font-semibold">{h.label}</h3><ComparisonBadge value={h.status} /></div>
      <p className="mt-4 text-3xl font-semibold">{h.score}</p>
      <p className="mt-1 text-sm text-muted-foreground">{h.status} · indicador claro, sem velocímetro decorativo.</p>
      <Button>{h.tone === "atenção" ? "Resolver pendência" : "Ver sinais"}</Button>
    </Card>
  );
}

export function ProductHealthPanel() {
  const { data: health, loading, error } = useAsyncData(() => analyticsService.listHealth(), []);

  return (
    <>
      <PageHeader title="Product Health" module="Analytics" desc="Painel de saúde operacional do produto por módulo e sinais críticos." badge="Saúde">
        <Button>Atualizar leitura</Button>
        <Button primary>Gerar plano de ação</Button>
      </PageHeader>
      {loading ? <SkeletonLines /> : error || !health ? <PartialErrorWidget /> : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{health.map((h) => <HealthScoreCard key={h.label} h={h} />)}</div>
      )}
      <div className="mt-4"><InsightPanel /></div>
    </>
  );
}
