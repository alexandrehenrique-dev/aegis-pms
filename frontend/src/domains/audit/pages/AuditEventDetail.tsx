import { Button, Card, PageHeader } from "../../../shared/components/Primitives";

export function AuditEventDetail() {
  return (
    <>
      <PageHeader title="Evento aud_8f42" module="Auditoria" desc="Detalhe do evento com antes/depois, payload e identificadores técnicos futuros." badge="Evento crítico">
        <Button>Copiar ID</Button>
        <Button>Exportar evento</Button>
        <Button primary>Abrir recurso</Button>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
        <Card>
          <h2 className="mb-3 text-lg font-semibold">Antes / Depois</h2>
          <div className="grid gap-3 md:grid-cols-2">
            <pre className="overflow-auto rounded-xl bg-[#FDEBE8] p-4 text-xs">{`{ role: "Editor", publish: false }`}</pre>
            <pre className="overflow-auto rounded-xl bg-[#ede9fe] p-4 text-xs">{`{ role: "Editor", publish: true }`}</pre>
          </div>
          <h2 className="mb-3 mt-4 text-lg font-semibold">Payload resumido</h2>
          <pre className="overflow-auto rounded-xl bg-muted p-4 text-xs">{`trace_id: future_tr_91\nip: futuro\nuser_agent: futuro`}</pre>
        </Card>
        <Card>
          <h2 className="mb-3 text-lg font-semibold">Metadados</h2>
          {[["Ator", "Ana Martins"], ["Papel", "Tenant Admin"], ["Tenant", "BYOP"], ["Produto", "Maestro Beton"], ["Módulo", "Permissions"], ["Recurso", "Editor role"], ["Ação", "permissão alterada"], ["Data/hora", "ontem 18:10"]].map((x) => (
            <div key={x[0]} className="mb-2 flex justify-between rounded-lg bg-muted p-2 text-sm"><span>{x[0]}</span><b>{x[1]}</b></div>
          ))}
        </Card>
      </div>
    </>
  );
}
