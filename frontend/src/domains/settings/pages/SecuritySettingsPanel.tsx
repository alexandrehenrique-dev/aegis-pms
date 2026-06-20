import { useState } from "react";
import { Loader2 } from "lucide-react";
import { Badge, Button, Card, PageHeader } from "../../../shared/components/Primitives";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "../../../shared/components/ui/dialog";
import { toast } from "../../../core/notifications/toast";
import { settingsService } from "../services/settingsService";

const SESSIONS = [
  { device: "Chrome · macOS", location: "São Paulo, BR", lastActive: "agora" },
  { device: "Safari · iPhone", location: "São Paulo, BR", lastActive: "há 2 h" },
  { device: "Edge · Windows", location: "Curitiba, BR", lastActive: "há 1 dia" },
];

function IntegrationCard({ i, onConfigure }: { i: string[]; onConfigure: () => void }) {
  return (
    <Card>
      <div className="flex justify-between"><h3 className="font-semibold">{i[0]}</h3><Badge tone={i[1] === "conectado" ? "green" : i[1] === "erro" || i[1] === "requer atenção" ? "amber" : "neutral"}>{i[1]}</Badge></div>
      <p className="mt-2 text-sm text-muted-foreground">Ambiente: {i[2]} · Última sync: {i[3]}</p>
      <Button onClick={onConfigure}>Configurar</Button>
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
  const [sessionsOpen, setSessionsOpen] = useState(false);
  const [configuring, setConfiguring] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const handleSaveSecurity = async () => {
    setSaving(true);
    try {
      await settingsService.saveSecurity();
      toast.success("Configurações de segurança salvas!");
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <Dialog open={sessionsOpen} onOpenChange={setSessionsOpen}>
        <DialogContent>
          <DialogHeader><DialogTitle>Sessões ativas</DialogTitle></DialogHeader>
          <div className="space-y-2">
            {SESSIONS.map((s) => (
              <div key={s.device} className="rounded-lg bg-muted p-3 text-sm">
                <div className="flex justify-between"><b>{s.device}</b><span className="text-muted-foreground">{s.lastActive}</span></div>
                <p className="text-muted-foreground">{s.location}</p>
              </div>
            ))}
          </div>
        </DialogContent>
      </Dialog>
      <Dialog open={configuring !== null} onOpenChange={(open) => !open && setConfiguring(null)}>
        <DialogContent>
          <DialogHeader><DialogTitle>Configurar {configuring}</DialogTitle></DialogHeader>
          <p className="text-sm text-muted-foreground">Integração mock — a configuração real desta integração chega quando o backend (Sprint 07) estiver disponível.</p>
        </DialogContent>
      </Dialog>
      <PageHeader title="Security & Integrations" module="Configurações" desc="Segurança e integrações essenciais, com estados e risco operacional." badge="Admin">
        <Button onClick={() => setSessionsOpen(true)}>Revisar sessões</Button>
        <Button primary onClick={handleSaveSecurity} disabled={saving}>{saving && <Loader2 size={15} className="animate-spin" />}{saving ? "Salvando..." : "Salvar segurança"}</Button>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[360px_1fr]">
        <Card>
          <h2 className="mb-3 text-lg font-semibold">Segurança</h2>
          {["Sessões ativas", "Tokens futuros", "Política de senha", "Rate limit futuro", "2FA futuro"].map((x) => <div key={x} className="mb-2 rounded-lg bg-muted p-3 text-sm">{x}</div>)}
        </Card>
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{ints.map((i) => <IntegrationCard key={i[0]} i={i} onConfigure={() => setConfiguring(i[0])} />)}</div>
      </div>
    </>
  );
}
