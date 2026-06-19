import { Button, Card, PageHeader } from "../../../shared/components/Primitives";
import { ComparisonBadge, InsightPanel } from "../components/AnalyticsBits";
import { health } from "../mocks/analytics.mocks";

function HealthScoreCard({ h }: { h: string[] }) {
  return (
    <Card>
      <div className="flex items-center justify-between"><h3 className="font-semibold">{h[0]}</h3><ComparisonBadge value={h[1]} /></div>
      <p className="mt-4 text-3xl font-semibold">{h[2]}</p>
      <p className="mt-1 text-sm text-muted-foreground">{h[1]} · indicador claro, sem velocímetro decorativo.</p>
      <Button>{h[3] === "atenção" ? "Resolver pendência" : "Ver sinais"}</Button>
    </Card>
  );
}

export function ProductHealthPanel() {
  return (
    <>
      <PageHeader title="Product Health" module="Analytics" desc="Painel de saúde operacional do produto por módulo e sinais críticos." badge="Saúde">
        <Button>Atualizar leitura</Button>
        <Button primary>Gerar plano de ação</Button>
      </PageHeader>
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{health.map((h) => <HealthScoreCard key={h[0]} h={h} />)}</div>
      <div className="mt-4"><InsightPanel /></div>
    </>
  );
}
