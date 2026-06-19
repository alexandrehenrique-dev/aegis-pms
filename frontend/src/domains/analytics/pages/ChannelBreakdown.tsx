import { Button, Card, PageHeader } from "../../../shared/components/Primitives";
import { ChartContainer, ComparisonBadge } from "../components/AnalyticsBits";
import { channels } from "../mocks/analytics.mocks";

export function ChannelBreakdown() {
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
            {channels.map((c) => (
              <div key={c[0]} className="mb-2 grid grid-cols-4 gap-2 rounded-xl border border-border p-3 text-sm"><b>{c[0]}</b><span>{c[1]} visitas</span><span>{c[2]} conv.</span><ComparisonBadge value={c[3]} /></div>
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
