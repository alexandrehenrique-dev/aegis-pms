import { Badge, Button, Card, PageHeader } from "../../../shared/components/Primitives";

function IntegrationCard({ i }: { i: string[] }) {
  return (
    <Card>
      <div className="flex justify-between"><h3 className="font-semibold">{i[0]}</h3><Badge tone={i[1] === "conectado" ? "green" : i[1] === "erro" || i[1] === "requer atenção" ? "amber" : "neutral"}>{i[1]}</Badge></div>
      <p className="mt-2 text-sm text-muted-foreground">Ambiente: {i[2]} · Última sync: {i[3]}</p>
      <Button>Configurar</Button>
    </Card>
  );
}

export function SecuritySettingsPanel() {
  const ints = [
    ["Webhooks", "conectado", "produção", "há 20 min"],
    ["Analytics Provider", "requer atenção", "produção", "ontem"],
    ["Storage Provider", "conectado", "produção", "há 1 h"],
    ["Email Provider", "desconectado", "staging", "—"],
    ["WhatsApp futuro", "futuro", "—", "—"],
    ["Telegram futuro", "futuro", "—", "—"],
  ];
  return (
    <>
      <PageHeader title="Security & Integrations" module="Configurações" desc="Segurança e integrações essenciais, com estados e risco operacional." badge="Admin">
        <Button>Revisar sessões</Button>
        <Button primary>Salvar segurança</Button>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[360px_1fr]">
        <Card>
          <h2 className="mb-3 text-lg font-semibold">Segurança</h2>
          {["Sessões ativas", "Tokens futuros", "Política de senha", "Rate limit futuro", "2FA futuro"].map((x) => <div key={x} className="mb-2 rounded-lg bg-muted p-3 text-sm">{x}</div>)}
        </Card>
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{ints.map((i) => <IntegrationCard key={i[0]} i={i} />)}</div>
      </div>
    </>
  );
}
