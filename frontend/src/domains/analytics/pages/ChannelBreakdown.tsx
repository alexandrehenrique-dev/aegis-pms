import { Button, Card, PageHeader, SkeletonLines, PartialErrorWidget } from "../../../shared/components/Primitives";
import { ChartContainer, ComparisonBadge } from "../components/AnalyticsBits";
import { analyticsService } from "../services/analyticsService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";

export function ChannelBreakdown() {
  const { data: channels, loading, error } = useAsyncData(() => analyticsService.listChannels(), []);

  return (
    <>
      <PageHeader title="Traffic & Channels" module="Analytics" desc="Compare tráfego e conversão por origem para decidir onde agir." badge="Canais">
        <Button>Últimos 30 dias</Button>
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
