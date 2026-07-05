import { useState } from "react";
import { Button, Card, EmptyState, KPIWidget, PageHeader, PartialErrorWidget, SkeletonLines } from "../../../shared/components/Primitives";
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "../../../shared/components/ui/dropdown-menu";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { formsService } from "../services/formsService";

const PERIODS = ["Últimos 7 dias", "Últimos 30 dias", "Últimos 90 dias"];

function parseMetricNumber(value: string): number {
  const parsed = Number.parseInt(value.replace(/\D/g, ""), 10);
  return Number.isNaN(parsed) ? 0 : parsed;
}

function parseConversion(value: string): number | null {
  const parsed = Number.parseFloat(value.replace("%", "").replace(",", ".").trim());
  return Number.isNaN(parsed) ? null : parsed;
}

export function BasicFormAnalytics() {
  const { product } = useCurrentProduct();
  const productId = product?.id ?? "";
  const { data: forms, loading, error } = useAsyncData(() => (productId ? formsService.listForms(productId) : Promise.resolve([])), [productId]);
  const [period, setPeriod] = useState(PERIODS[1]);
  const currentForms = forms ?? [];
  const totalResponses = currentForms.reduce((total, form) => total + parseMetricNumber(form.responses), 0);
  const conversionValues = currentForms.map((form) => parseConversion(form.conversion)).filter((value): value is number => value !== null);
  const averageConversion = conversionValues.length === 0 ? "—" : `${(conversionValues.reduce((total, value) => total + value, 0) / conversionValues.length).toFixed(1)}%`;
  return (
    <>
      <PageHeader title="Analytics Básico do Formulário" module="Forms" desc="Métricas do formulário, sem entrar ainda no módulo Analytics avançado." badge="Métricas">
        <DropdownMenu>
          <DropdownMenuTrigger asChild><Button>{period}</Button></DropdownMenuTrigger>
          <DropdownMenuContent>
            {PERIODS.map((p) => <DropdownMenuItem key={p} onSelect={() => setPeriod(p)}>{p}</DropdownMenuItem>)}
          </DropdownMenuContent>
        </DropdownMenu>
      </PageHeader>
      {loading ? <SkeletonLines /> : error ? <PartialErrorWidget /> : currentForms.length === 0 ? (
        <EmptyState title="Sem métricas de formulário" description="Quando houver formulários no produto, os indicadores aparecem aqui." />
      ) : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
          <KPIWidget label="Visualizações" value="—" detail="instrumentação pendente" />
          <KPIWidget label="Envios" value={String(totalResponses)} detail={`${currentForms.length} formulário(s)`} />
          <KPIWidget label="Taxa de conversão" value={averageConversion} detail={`${conversionValues.length} com métrica`} />
          <KPIWidget label="Abandono" value="—" detail="instrumentação pendente" />
          <KPIWidget label="Campo mais preenchido" value="—" detail="instrumentação pendente" />
        </div>
      )}
      <Card className="mt-4">
        <h2 className="mb-3 text-lg font-semibold">Campos mais preenchidos</h2>
        <p className="text-sm text-muted-foreground">Dados por campo aparecem quando o backend expuser telemetria de preenchimento.</p>
      </Card>
    </>
  );
}
