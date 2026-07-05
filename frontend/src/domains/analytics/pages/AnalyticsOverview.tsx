import { useNavigate } from "react-router";
import { PageHeader } from "../../../shared/components/Primitives";
import { ChartContainer, InsightPanel, KPIGrid, PeriodSelector } from "../components/AnalyticsBits";
import { Button } from "../../../shared/components/Primitives";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";

export function AnalyticsOverview() {
  const navigate = useNavigate();
  const { product } = useCurrentProduct();
  return (
    <>
      <PageHeader title="Analytics" desc="Acompanhe performance, conversões, conteúdo e sinais operacionais do produto." badge={product?.name}>
        <PeriodSelector />
        <Button onClick={() => navigate("/analytics/reports")}>Ver relatórios</Button>
        <Button primary onClick={() => navigate("/analytics/trends")}>Tendências</Button>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
        <div className="space-y-4">
          <KPIGrid />
          <div className="grid gap-4 xl:grid-cols-2">
            <ChartContainer title="Evolução de visitas" type="line" />
            <ChartContainer title="Conversões por período" type="area" />
          </div>
        </div>
        <InsightPanel />
      </div>
    </>
  );
}
