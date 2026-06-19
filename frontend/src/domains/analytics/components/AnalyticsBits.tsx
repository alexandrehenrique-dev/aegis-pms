import { Filter } from "lucide-react";
import { Badge, Button, Card, SkeletonLines, PartialErrorWidget } from "../../../shared/components/Primitives";
import { analyticsService } from "../services/analyticsService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import type { AnalyticsKpi } from "../contracts/responses";

export function ComparisonBadge({ value }: { value: string }) {
  return <Badge tone={value.includes("-") || value.includes("atenção") ? "amber" : value.includes("estável") ? "neutral" : "green"}>{value}</Badge>;
}

export function TrendIndicator({ tone }: { tone: string }) {
  return <span className={`h-2.5 w-2.5 rounded-full ${tone === "positivo" ? "bg-primary" : tone === "atenção" ? "bg-[#D97706]" : "bg-muted-foreground"}`} />;
}

export function KPIBlock({ k }: { k: AnalyticsKpi }) {
  return (
    <Card>
      <div className="flex items-start justify-between"><p className="text-sm text-muted-foreground">{k.label}</p><TrendIndicator tone={k.tone} /></div>
      <div className="mt-3 flex items-end justify-between gap-3"><p className="text-2xl font-semibold tracking-[-.02em]">{k.value}</p><ComparisonBadge value={k.comparison} /></div>
      <p className="mt-2 text-xs leading-5 text-muted-foreground">{k.note}</p>
      <Button>{k.tone === "atenção" ? "Investigar" : "Ver detalhe"}</Button>
    </Card>
  );
}

export function KPIGrid() {
  const { data: kpis, loading, error } = useAsyncData(() => analyticsService.listKpis(), []);
  if (loading) return <SkeletonLines />;
  if (error || !kpis) return <PartialErrorWidget />;
  return <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">{kpis.map((k) => <KPIBlock key={k.label} k={k} />)}</div>;
}

export function PeriodSelector() {
  return (
    <div className="flex flex-wrap gap-2">
      <Button>Últimos 30 dias</Button>
      <Button>Comparar período anterior</Button>
      <Button><Filter size={15} />Canal</Button>
      <Button>Conteúdo</Button>
    </div>
  );
}

export function ChartContainer({ title, type = "line" }: { title: string; type?: string }) {
  const vals = [42, 68, 55, 80, 62, 91, 73];
  return (
    <Card>
      <div className="mb-4 flex items-center justify-between">
        <div><h3 className="font-semibold">{title}</h3><p className="text-xs text-muted-foreground">Visual implementável · {type}</p></div>
        <Badge tone="blue">operacional</Badge>
      </div>
      <div className="flex h-40 items-end gap-2 rounded-xl bg-muted/40 p-3">
        {vals.map((v, i) => (
          <div key={i} className="flex flex-1 flex-col items-center gap-2">
            <div className={`w-full rounded-t ${type === "area" ? "bg-primary/60" : type === "stacked" ? "bg-[#1F5FA8]" : "bg-primary"}`} style={{ height: `${v}%` }} />
            <span className="font-mono text-[10px] text-muted-foreground">{i + 1}</span>
          </div>
        ))}
      </div>
    </Card>
  );
}

export function InsightPanel() {
  return (
    <Card>
      <h2 className="mb-3 text-lg font-semibold">Insights e próximas ações</h2>
      {["Revisar SEO da Home", "Atualizar página Galeria", "Ver leads não qualificados", "Adicionar alt text em imagens", "Revisar formulário de orçamento"].map((a, i) => (
        <div key={a} className="mb-2 rounded-xl border border-border p-3 text-sm">
          <div className="flex items-center justify-between"><b>{a}</b><Badge tone={i < 2 ? "amber" : "blue"}>{i < 2 ? "atenção" : "ação"}</Badge></div>
          <p className="text-muted-foreground">Recomendação gerada a partir de variação e impacto operacional.</p>
        </div>
      ))}
    </Card>
  );
}
