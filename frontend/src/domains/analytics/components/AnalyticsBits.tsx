import { useState } from "react";
import { useNavigate } from "react-router";
import { Filter } from "lucide-react";
import { Badge, Button, Card, SkeletonLines, PartialErrorWidget } from "../../../shared/components/Primitives";
import { Popover, PopoverContent, PopoverTrigger } from "../../../shared/components/ui/popover";
import { analyticsService } from "../services/analyticsService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";
import type { AnalyticsKpi } from "../contracts/responses";

const KPI_ROUTES: Record<string, string> = {
  "Visitas": "/analytics/channels",
  "Conversões": "/analytics/forms",
  "Taxa de conversão": "/analytics/forms",
  "Formulários recebidos": "/forms/submissions",
  "Conteúdos publicados": "/content/list",
  "Conteúdos pendentes": "/content/workflow",
  "Assets recentes": "/assets",
  "Páginas mais acessadas": "/analytics/content",
  "Leads qualificados": "/forms/submissions",
  "Tempo médio na página": "/analytics/content",
};

export function ComparisonBadge({ value }: { value: string }) {
  return <Badge tone={value.includes("-") || value.includes("atenção") ? "amber" : value.includes("estável") ? "neutral" : "green"}>{value}</Badge>;
}

export function TrendIndicator({ tone }: { tone: string }) {
  return <span className={`h-2.5 w-2.5 rounded-full ${tone === "positivo" ? "bg-primary" : tone === "atenção" ? "bg-[#D97706]" : "bg-muted-foreground"}`} />;
}

export function KPIBlock({ k }: { k: AnalyticsKpi }) {
  const navigate = useNavigate();
  return (
    <Card>
      <div className="flex items-start justify-between"><p className="text-sm text-muted-foreground">{k.label}</p><TrendIndicator tone={k.tone} /></div>
      <div className="mt-3 flex items-end justify-between gap-3"><p className="text-2xl font-semibold tracking-[-.02em]">{k.value}</p><ComparisonBadge value={k.comparison} /></div>
      <p className="mt-2 text-xs leading-5 text-muted-foreground">{k.note}</p>
      <Button onClick={() => navigate(KPI_ROUTES[k.label] ?? "/analytics")}>{k.tone === "atenção" ? "Investigar" : "Ver detalhe"}</Button>
    </Card>
  );
}

export function KPIGrid() {
  const { product } = useCurrentProduct();
  const productId = product?.id ?? "p1";
  const { data: kpis, loading, error } = useAsyncData(() => analyticsService.listKpis(productId), [productId]);
  if (loading) return <SkeletonLines />;
  if (error || !kpis) return <PartialErrorWidget />;
  return <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">{kpis.map((k) => <KPIBlock key={k.label} k={k} />)}</div>;
}

const PERIODS = ["Últimos 7 dias", "Últimos 30 dias", "Últimos 90 dias"];
const CHANNELS = ["Direto", "Google", "Instagram", "WhatsApp", "Referral", "Orgânico", "Campanha"];

export function PeriodSelector() {
  const navigate = useNavigate();
  const [period, setPeriod] = useState(PERIODS[1]);
  const [comparing, setComparing] = useState(false);
  const [channel, setChannel] = useState<string | null>(null);

  return (
    <div className="flex flex-wrap gap-2">
      <Popover>
        <PopoverTrigger asChild><Button>{period}</Button></PopoverTrigger>
        <PopoverContent>
          <div className="flex flex-col gap-1">{PERIODS.map((p) => <Button key={p} onClick={() => setPeriod(p)} primary={period === p}>{p}</Button>)}</div>
        </PopoverContent>
      </Popover>
      <Button onClick={() => setComparing((c) => !c)} primary={comparing}>Comparar período anterior</Button>
      <Popover>
        <PopoverTrigger asChild><Button><Filter size={15} />{channel ?? "Canal"}</Button></PopoverTrigger>
        <PopoverContent>
          <div className="flex flex-wrap gap-1">
            <Button onClick={() => setChannel(null)} primary={!channel}>Todos</Button>
            {CHANNELS.map((c) => <Button key={c} onClick={() => setChannel(c)} primary={channel === c}>{c}</Button>)}
          </div>
        </PopoverContent>
      </Popover>
      <Button onClick={() => navigate("/analytics/content")}>Conteúdo</Button>
    </div>
  );
}

export function ChartContainer({ title, type = "line" }: { title: string; type?: string }) {
  return (
    <Card>
      <div className="mb-4 flex items-center justify-between">
        <div><h3 className="font-semibold">{title}</h3><p className="text-xs text-muted-foreground">Instrumentação pendente · {type}</p></div>
        <Badge>sem dados</Badge>
      </div>
      <div className="grid h-40 place-items-center rounded-xl bg-muted/40 p-3 text-sm text-muted-foreground">
        Dados não disponíveis ainda.
      </div>
    </Card>
  );
}

export function InsightPanel() {
  return (
    <Card>
      <h2 className="mb-3 text-lg font-semibold">Insights e próximas ações</h2>
      <div className="rounded-xl border border-border p-3 text-sm text-muted-foreground">
        Insights automáticos aparecem quando os endpoints de tendência retornarem sinais suficientes para o produto.
      </div>
    </Card>
  );
}
