import { useState } from "react";
import { Button, Card, PageHeader, SkeletonLines, PartialErrorWidget } from "../../../shared/components/Primitives";
import { Popover, PopoverContent, PopoverTrigger } from "../../../shared/components/ui/popover";
import { ChartContainer, ComparisonBadge } from "../components/AnalyticsBits";
import { analyticsService } from "../services/analyticsService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";

const PERIODS = ["Últimos 7 dias", "Últimos 30 dias", "Últimos 90 dias"];

export function ChannelBreakdown() {
  const { product } = useCurrentProduct();
  const productId = product?.id ?? "p1";
  const { data: channels, loading, error } = useAsyncData(() => analyticsService.listChannels(productId), [productId]);
  const [period, setPeriod] = useState(PERIODS[1]);

  return (
    <>
      <PageHeader title="Traffic & Channels" module="Analytics" desc="Compare tráfego e conversão por origem para decidir onde agir." badge="Canais">
        <Popover>
          <PopoverTrigger asChild><Button>{period}</Button></PopoverTrigger>
          <PopoverContent>
            <div className="flex flex-col gap-1">{PERIODS.map((p) => <Button key={p} onClick={() => setPeriod(p)} primary={period === p}>{p}</Button>)}</div>
          </PopoverContent>
        </Popover>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
        <div className="space-y-4">
          <ChartContainer title="Gráfico de canais" type="bar" />
          <Card>
            <h2 className="mb-3 text-lg font-semibold">Tabela de origem</h2>
            {loading ? <SkeletonLines /> : error || !channels ? <PartialErrorWidget /> : channels.map((c) => (
              <div key={c.name} className="mb-2 grid grid-cols-4 gap-2 rounded-xl border border-border p-3 text-sm"><b>{c.name}</b><span>{c.visits} visitas</span><span>{c.conversion} conv.</span><ComparisonBadge value={c.trend} /></div>
            ))}
          </Card>
        </div>
        <Card>
          <h2 className="mb-3 text-lg font-semibold">Destaques</h2>
          <p className="mb-3 text-sm text-muted-foreground">Instagram trouxe tráfego, mas WhatsApp converteu melhor.</p>
          <p className="text-sm text-muted-foreground">Google orgânico cresceu após atualização da página Home.</p>
        </Card>
      </div>
    </>
  );
}
