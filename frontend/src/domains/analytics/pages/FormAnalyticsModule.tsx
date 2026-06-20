import { useState } from "react";
import { Button, Card, KPIWidget, PageHeader } from "../../../shared/components/Primitives";
import { Popover, PopoverContent, PopoverTrigger } from "../../../shared/components/ui/popover";
import { ChartContainer } from "../components/AnalyticsBits";

const FORMS = ["Contato Comercial", "Orçamento Maestro", "Newsletter"];

function FormAnalyticsTable() {
  const rows = [
    ["Contato Comercial", "3.482", "248", "7.1%", "74", "12 min", "Responder novos"],
    ["Orçamento Maestro", "1.920", "93", "9.4%", "41", "1 h", "Qualificar leads"],
    ["Newsletter", "740", "0", "0%", "0", "21 dias", "Revisar publicação"],
  ];
  return (
    <Card>
      <h2 className="mb-3 text-lg font-semibold">Tabela de formulários</h2>
      {rows.map((r) => (
        <div key={r[0]} className="mb-2 grid gap-2 rounded-xl border border-border p-3 text-sm lg:grid-cols-7">
          <b>{r[0]}</b>
          {r.slice(1).map((c, i) => <span key={`${r[0]}-metric-${i}`}>{c}</span>)}
        </div>
      ))}
    </Card>
  );
}

export function FormAnalyticsModule() {
  const [form, setForm] = useState<string | null>(null);
  return (
    <>
      <PageHeader title="Form Analytics" module="Analytics" desc="Mede respostas, conversão, abandono, origem e qualificação dos leads." badge="Forms">
        <Popover>
          <PopoverTrigger asChild><Button>Formulário: {form ?? "todos"}</Button></PopoverTrigger>
          <PopoverContent>
            <div className="flex flex-col gap-1">
              <Button onClick={() => setForm(null)} primary={!form}>Todos</Button>
              {FORMS.map((f) => <Button key={f} onClick={() => setForm(f)} primary={form === f}>{f}</Button>)}
            </div>
          </PopoverContent>
        </Popover>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-4">
        <KPIWidget label="Respostas" value="439" detail="+18 hoje" />
        <KPIWidget label="Conversão" value="7.4%" detail="Contato + Orçamento" />
        <KPIWidget label="Abandono" value="22%" detail="campo Mensagem" />
        <KPIWidget label="Tempo resposta" value="38 min" detail="melhorou 14%" />
      </div>
      <div className="mt-4 grid gap-4 xl:grid-cols-2">
        <ChartContainer title="Submissions por período" />
        <ChartContainer title="Origem UTM" type="stacked" />
      </div>
      <div className="mt-4"><FormAnalyticsTable /></div>
    </>
  );
}
