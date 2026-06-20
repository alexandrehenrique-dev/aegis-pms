import { useState } from "react";
import { useNavigate } from "react-router";
import { Loader2 } from "lucide-react";
import { Button, Card, PageHeader, SkeletonLines, PartialErrorWidget } from "../../../shared/components/Primitives";
import { ComparisonBadge, InsightPanel } from "../components/AnalyticsBits";
import { analyticsService } from "../services/analyticsService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { toast } from "../../../core/notifications/toast";
import type { HealthSignal } from "../contracts/responses";

const HEALTH_ROUTES: Record<string, string> = {
  "Saúde geral": "/analytics", "Conteúdo": "/content/workflow", "Forms": "/forms/submissions",
  "Assets": "/assets", "SEO": "/settings/product", "Performance": "/analytics",
};

function HealthScoreCard({ h }: { h: HealthSignal }) {
  const navigate = useNavigate();
  return (
    <Card>
      <div className="flex items-center justify-between"><h3 className="font-semibold">{h.label}</h3><ComparisonBadge value={h.status} /></div>
      <p className="mt-4 text-3xl font-semibold">{h.score}</p>
      <p className="mt-1 text-sm text-muted-foreground">{h.status} · indicador claro, sem velocímetro decorativo.</p>
      <Button onClick={() => navigate(HEALTH_ROUTES[h.label] ?? "/analytics")}>{h.tone === "atenção" ? "Resolver pendência" : "Ver sinais"}</Button>
    </Card>
  );
}

export function ProductHealthPanel() {
  const [reloadKey, setReloadKey] = useState(0);
  const { data: health, loading, error } = useAsyncData(() => analyticsService.listHealth(), [reloadKey]);
  const [refreshing, setRefreshing] = useState(false);
  const [generating, setGenerating] = useState(false);

  const handleRefresh = async () => {
    setRefreshing(true);
    setReloadKey((k) => k + 1);
    toast.success("Leitura atualizada!");
    setRefreshing(false);
  };

  const handleGeneratePlan = async () => {
    setGenerating(true);
    try {
      await analyticsService.generateActionPlan();
      toast.success("Plano de ação gerado!");
    } finally {
      setGenerating(false);
    }
  };

  return (
    <>
      <PageHeader title="Product Health" module="Analytics" desc="Painel de saúde operacional do produto por módulo e sinais críticos." badge="Saúde">
        <Button onClick={handleRefresh} disabled={refreshing}>{refreshing && <Loader2 size={15} className="animate-spin" />}{refreshing ? "Atualizando..." : "Atualizar leitura"}</Button>
        <Button primary onClick={handleGeneratePlan} disabled={generating}>{generating && <Loader2 size={15} className="animate-spin" />}{generating ? "Gerando..." : "Gerar plano de ação"}</Button>
      </PageHeader>
      {loading ? <SkeletonLines /> : error || !health ? <PartialErrorWidget /> : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{health.map((h) => <HealthScoreCard key={h.label} h={h} />)}</div>
      )}
      <div className="mt-4"><InsightPanel /></div>
    </>
  );
}
