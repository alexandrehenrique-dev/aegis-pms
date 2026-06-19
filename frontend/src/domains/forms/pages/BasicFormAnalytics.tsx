import { Button, Card, KPIWidget, PageHeader } from "../../../shared/components/Primitives";

export function BasicFormAnalytics() {
  return (
    <>
      <PageHeader title="Analytics Básico do Formulário" module="Forms" desc="Métricas do formulário, sem entrar ainda no módulo Analytics avançado." badge="Métricas">
        <Button>Últimos 30 dias</Button>
      </PageHeader>
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
        <KPIWidget label="Visualizações" value="3.482" detail="Home + embed" />
        <KPIWidget label="Envios" value="248" detail="+18 hoje" />
        <KPIWidget label="Taxa de conversão" value="7.1%" detail="+0.6 p.p." />
        <KPIWidget label="Abandono" value="22%" detail="campo Mensagem" />
        <KPIWidget label="Campo mais preenchido" value="Email" detail="99.2% completo" />
      </div>
      <Card className="mt-4">
        <h2 className="mb-3 text-lg font-semibold">Campos mais preenchidos</h2>
        {["Nome", "Email", "Telefone", "Mensagem"].map((f, i) => (
          <div key={f} className="mb-2">
            <div className="flex justify-between text-sm"><span>{f}</span><b>{99 - i * 8}%</b></div>
            <div className="h-2 rounded-full bg-muted"><div className="h-2 rounded-full bg-primary" style={{ width: `${99 - i * 8}%` }} /></div>
          </div>
        ))}
      </Card>
    </>
  );
}
