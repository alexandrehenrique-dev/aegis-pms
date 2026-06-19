import { Badge, Button, Card, Field, PageHeader, SelectLike } from "../../../shared/components/Primitives";
import { LeadStatusBadge } from "../components/FormBadges";
import { FormsTimeline } from "../components/FormsTimeline";

function UTMCard() {
  return (
    <Card>
      <h2 className="mb-3 text-lg font-semibold">Origem / UTM</h2>
      {[["source", "google"], ["medium", "cpc"], ["campaign", "maestro-junho"], ["content", "hero-form"]].map((x) => (
        <div key={x[0]} className="mb-2 flex justify-between rounded-lg bg-muted p-2 text-sm"><span>{x[0]}</span><b>{x[1]}</b></div>
      ))}
    </Card>
  );
}

export function SubmissionDetails() {
  return (
    <>
      <PageHeader title="Camila Rocha" module="Forms" desc="Detalhe da submissão com contexto de lead, origem, timeline e qualificação." badge="Lead Novo">
        <Button>Atribuir</Button>
        <Button primary>Marcar qualificado</Button>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
        <div className="space-y-4">
          <Card>
            <h2 className="mb-3 text-lg font-semibold">Dados enviados</h2>
            {[["Nome", "Camila Rocha"], ["Email", "camila@studio.com"], ["Telefone", "+55 11 98888-1212"], ["Mensagem", "Gostaria de orçamento para evento corporativo em agosto."]].map((x) => (
              <div key={x[0]} className="mb-2 rounded-lg bg-muted p-3 text-sm"><b>{x[0]}</b><p>{x[1]}</p></div>
            ))}
          </Card>
          <Card><h2 className="mb-3 text-lg font-semibold">Timeline</h2><FormsTimeline /></Card>
        </div>
        <div className="space-y-4">
          <Card>
            <h2 className="mb-3 text-lg font-semibold">Operação do lead</h2>
            <LeadStatusBadge status="Novo" />
            <div className="mt-3 flex flex-wrap gap-1">{["orçamento", "corporativo", "alto-fit"].map((t) => <Badge key={t} tone="blue">{t}</Badge>)}</div>
            <Field label="Observações" value="Responder ainda hoje e solicitar briefing." textarea />
            <SelectLike label="Responsável" value="Marina Costa" />
          </Card>
          <UTMCard />
        </div>
      </div>
    </>
  );
}
