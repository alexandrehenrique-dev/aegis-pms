import { useState } from "react";
import { Button, Card, EmptyState, KPIWidget, PageHeader, SkeletonLines, PartialErrorWidget } from "../../../shared/components/Primitives";
import { Popover, PopoverContent, PopoverTrigger } from "../../../shared/components/ui/popover";
import { ChartContainer } from "../components/AnalyticsBits";
import { formsService } from "../../forms/services/formsService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";
import type { FormSummary } from "../../forms/contracts/responses";

function parseMetricNumber(value: string): number {
  const parsed = Number.parseInt(value.replace(/\D/g, ""), 10);
  return Number.isNaN(parsed) ? 0 : parsed;
}

function parseConversion(value: string): number | null {
  const parsed = Number.parseFloat(value.replace("%", "").replace(",", ".").trim());
  return Number.isNaN(parsed) ? null : parsed;
}

function FormAnalyticsTable({ rows }: { rows: FormSummary[] }) {
  return (
    <Card>
      <h2 className="mb-3 text-lg font-semibold">Tabela de formulários</h2>
      {rows.map((form) => (
        <div key={form.id} className="mb-2 grid gap-2 rounded-xl border border-border p-3 text-sm lg:grid-cols-7">
          <b>{form.name}</b>
          <span>{form.type}</span>
          <span>{form.responses} respostas</span>
          <span>{form.conversion}</span>
          <span>{form.status}</span>
          <span>{form.lastActivity}</span>
          <span>{form.publication}</span>
        </div>
      ))}
    </Card>
  );
}

export function FormAnalyticsModule() {
  const { product } = useCurrentProduct();
  const productId = product?.id ?? "";
  const { data: formsData, loading, error } = useAsyncData(() => (productId ? formsService.listForms(productId) : Promise.resolve([])), [productId]);
  const [form, setForm] = useState<string | null>(null);
  const forms = formsData ?? [];
  const visibleForms = form ? forms.filter((item) => item.name === form) : forms;
  const totalResponses = visibleForms.reduce((total, item) => total + parseMetricNumber(item.responses), 0);
  const conversionValues = visibleForms.map((item) => parseConversion(item.conversion)).filter((value): value is number => value !== null);
  const averageConversion = conversionValues.length === 0 ? "—" : `${(conversionValues.reduce((total, value) => total + value, 0) / conversionValues.length).toFixed(1)}%`;
  const inactiveForms = visibleForms.filter((item) => parseMetricNumber(item.responses) === 0).length;
  return (
    <>
      <PageHeader title="Form Analytics" module="Analytics" desc="Mede respostas, conversão, abandono, origem e qualificação dos leads." badge="Forms">
        <Popover>
          <PopoverTrigger asChild><Button>Formulário: {form ?? "todos"}</Button></PopoverTrigger>
          <PopoverContent>
            <div className="flex flex-col gap-1">
              <Button onClick={() => setForm(null)} primary={!form}>Todos</Button>
              {forms.map((item) => <Button key={item.id} onClick={() => setForm(item.name)} primary={form === item.name}>{item.name}</Button>)}
            </div>
          </PopoverContent>
        </Popover>
      </PageHeader>
      {loading && <SkeletonLines />}
      {error && <PartialErrorWidget />}
      {!loading && !error && visibleForms.length === 0 && <EmptyState title="Sem dados de formulários" description="Quando o produto receber formulários, as métricas aparecem aqui." />}
      {!loading && !error && visibleForms.length > 0 && (
        <>
      <div className="grid gap-4 xl:grid-cols-4">
        <KPIWidget label="Respostas" value={String(totalResponses)} detail={`${visibleForms.length} formulário(s)`} />
        <KPIWidget label="Conversão" value={averageConversion} detail={`${conversionValues.length} com métrica`} />
        <KPIWidget label="Sem respostas" value={String(inactiveForms)} detail="revisar publicação" />
        <KPIWidget label="Publicados" value={String(visibleForms.filter((item) => item.publication === "Publicado").length)} detail="ativos para captura" />
      </div>
      <div className="mt-4 grid gap-4 xl:grid-cols-2">
        <ChartContainer title="Submissions por período" />
        <ChartContainer title="Origem UTM" type="stacked" />
      </div>
      <div className="mt-4"><FormAnalyticsTable rows={visibleForms} /></div>
        </>
      )}
    </>
  );
}
