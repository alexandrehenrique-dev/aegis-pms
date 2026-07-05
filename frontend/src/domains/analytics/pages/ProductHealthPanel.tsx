import { useState } from "react";
import { useNavigate } from "react-router";
import { Loader2 } from "lucide-react";
import { Button, Card, PageHeader, SkeletonLines, PartialErrorWidget, EmptyState } from "../../../shared/components/Primitives";
import { ComparisonBadge } from "../components/AnalyticsBits";
import { analyticsService } from "../services/analyticsService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { toast } from "../../../core/notifications/toast";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";
import type { HealthSignal } from "../contracts/responses";

const HEALTH_ROUTES: Record<string, string> = {
  overview: "/analytics", content: "/content/workflow", forms: "/forms/submissions", assets: "/assets",
  "Saúde geral": "/analytics", "Conteúdo": "/content/workflow", "Forms": "/forms/submissions", "Assets": "/assets",
};

function HealthScoreCard({ h }: { h: HealthSignal }) {
  const navigate = useNavigate();
  const route = HEALTH_ROUTES[h.actionTarget ?? h.label] ?? "/analytics";
  const detail = h.detail ?? `${h.status} para o produto ativo.`;
  return (
    <Card>
      <div className="flex items-center justify-between"><h3 className="font-semibold">{h.label}</h3><ComparisonBadge value={h.status} /></div>
      <p className="mt-4 text-3xl font-semibold">{h.score}</p>
      <p className="mt-1 text-sm text-muted-foreground">{detail}</p>
      <Button onClick={() => navigate(route)}>{h.actionLabel ?? (h.tone === "atenção" ? "Resolver pendência" : "Ver sinais")}</Button>
    </Card>
  );
}

function HealthInsightsPanel({ health }: { health: HealthSignal[] }) {
  const actionable = health.filter((signal) => signal.tone === "atenção" || signal.tone === "neutro");
  const signals = actionable.length > 0 ? actionable : health;

  return (
    <Card>
      <h2 className="mb-3 text-lg font-semibold">Insights e próximas ações</h2>
      <div className="grid gap-2">
        {signals.map((signal) => (
          <div key={signal.label} className="rounded-xl border border-border p-3 text-sm">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <strong>{signal.label}</strong>
              <ComparisonBadge value={signal.status} />
            </div>
            <p className="mt-2 text-muted-foreground">{signal.detail ?? signal.status}</p>
          </div>
        ))}
      </div>
    </Card>
  );
}

export function ProductHealthPanel() {
  const { product } = useCurrentProduct();
  const productId = product?.id;
  const [reloadKey, setReloadKey] = useState(0);
  const { data: health, loading, error } = useAsyncData(
    () => productId ? analyticsService.listHealth(productId) : Promise.resolve([]),
    [productId, reloadKey],
  );
  const [refreshing, setRefreshing] = useState(false);
  const [generating, setGenerating] = useState(false);

  const handleRefresh = async () => {
    setRefreshing(true);
    setReloadKey((k) => k + 1);
    toast.success("Leitura atualizada!");
    setRefreshing(false);
  };

  const handleGeneratePlan = async () => {
    if (!productId) return;
    setGenerating(true);
    try {
      await analyticsService.generateActionPlan(productId);
      toast.success("Plano de ação gerado!");
    } finally {
      setGenerating(false);
    }
  };

  return (
    <>
      <PageHeader title="Product Health" module="Analytics" desc="Painel de saúde operacional do produto por módulo e sinais críticos." badge="Saúde">
        <Button onClick={handleRefresh} disabled={refreshing}>{refreshing && <Loader2 size={15} className="animate-spin" />}{refreshing ? "Atualizando..." : "Atualizar leitura"}</Button>
        <Button primary onClick={handleGeneratePlan} disabled={generating || !productId}>{generating && <Loader2 size={15} className="animate-spin" />}{generating ? "Gerando..." : "Gerar plano de ação"}</Button>
      </PageHeader>
      {!productId ? <EmptyState title="Nenhum produto ativo." description="Selecione um produto para carregar a saúde operacional real." /> : loading ? <SkeletonLines /> : error || !health ? <PartialErrorWidget /> : health.length === 0 ? <EmptyState title="Sem sinais de saúde." description="O endpoint de saúde não retornou sinais para o produto ativo." /> : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{health.map((h) => <HealthScoreCard key={h.label} h={h} />)}</div>
      )}
      {health && health.length > 0 && <div className="mt-4"><HealthInsightsPanel health={health} /></div>}
    </>
  );
}
