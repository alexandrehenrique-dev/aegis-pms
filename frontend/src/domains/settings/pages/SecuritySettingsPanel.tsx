import { useEffect, useState } from "react";
import { CheckCircle2, Loader2, Send } from "lucide-react";
import { Badge, Button, Card, Field, PageHeader, SkeletonLines } from "../../../shared/components/Primitives";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "../../../shared/components/ui/dialog";
import { toast } from "../../../core/notifications/toast";
import { settingsService } from "../services/settingsService";
import { useAuth } from "../../../core/auth/useAuth";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import type { ProductSecuritySettings } from "../contracts/responses";

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

/**
 * Alertas por Telegram (Sprint 23, Seção D) — canal do produto/formulário
 * externo, distinto do Telegram global do Aegis (`POST /feedback`, Sprint 30
 * §D.4). GET ao montar, "Remover" envia `{chatId: "", botToken: ""}` porque
 * o backend trata `telegramAlert: null` como "não alterar" (nunca limpa).
 */
function TelegramAlertSection({ productId }: { productId: string }) {
  const { data, loading, error } = useAsyncData(() => settingsService.getProductSettings(productId), [productId]);
  // Estado local espelhando `data` (padrão de FeedbackInboxPage.tsx): `useAsyncData`
  // não expõe refetch, então a mutação de `updateProductSettings` precisa
  // atualizar isto diretamente, senão badge/botão "Remover" ficam presos no
  // valor stale do GET inicial mesmo depois de um PUT bem-sucedido.
  const [settings, setSettings] = useState<ProductSecuritySettings | null>(null);
  const [editingToken, setEditingToken] = useState(false);
  const [chatId, setChatId] = useState("");
  const [botToken, setBotToken] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => { if (data) setSettings(data); }, [data]);

  useEffect(() => {
    if (!settings) return;
    setChatId(settings.telegramAlert?.chatId ?? "");
    setEditingToken(!settings.telegramAlert);
    setBotToken("");
  }, [settings]);

  const persist = async (current: ProductSecuritySettings, telegramAlert: { chatId: string; botToken: string } | null) => {
    setSaving(true);
    try {
      const updated = await settingsService.updateProductSettings(productId, {
        webhookUrl: current.webhookUrl ?? undefined,
        analyticsEnabled: current.analyticsEnabled,
        analyticsProviderKey: current.analyticsProviderKey ?? undefined,
        emailDeliveryEnabled: current.emailDeliveryEnabled,
        telegramAlert,
      });
      setSettings(updated);
    } finally {
      setSaving(false);
    }
  };

  const handleSave = async () => {
    if (!settings || !chatId.trim() || !botToken.trim()) return;
    await persist(settings, { chatId: chatId.trim(), botToken: botToken.trim() });
    toast.success("Telegram configurado ✓");
  };

  const handleRemove = async () => {
    if (!settings) return;
    await persist(settings, { chatId: "", botToken: "" });
    setChatId("");
    setBotToken("");
    setEditingToken(true);
    toast.success("Configuração do Telegram removida.");
  };

  if (loading) return <SkeletonLines />;
  if (error || !settings) return null;

  return (
    <Card>
      <div className="flex items-center justify-between">
        <h3 className="font-semibold">Alertas por Telegram</h3>
        {settings.telegramAlert && <Badge tone="green"><CheckCircle2 size={12} className="mr-1 inline" />configurado</Badge>}
      </div>
      <div className="mt-3 space-y-3">
        <Field label="Chat ID" value={chatId} onChange={setChatId} locked={saving} />
        {editingToken ? (
          <Field label="Bot Token" type="password" value={botToken} onChange={setBotToken} locked={saving} />
        ) : (
          <div>
            <span className="mb-1 block text-sm font-medium">Bot Token</span>
            <div className="flex items-center gap-2">
              <span className="flex-1 rounded-lg border border-border bg-card p-3 text-sm text-muted-foreground">{settings.telegramAlert?.botTokenMasked}</span>
              <Button onClick={() => setEditingToken(true)}>Alterar</Button>
            </div>
          </div>
        )}
        <div className="flex gap-2">
          <Button primary onClick={handleSave} disabled={saving || !chatId.trim() || !botToken.trim()}>
            {saving ? <Loader2 size={15} className="animate-spin" /> : <Send size={15} />}Salvar
          </Button>
          {settings.telegramAlert && <Button onClick={handleRemove} disabled={saving}>Remover configuração</Button>}
        </div>
      </div>
    </Card>
  );
}

export function SecuritySettingsPanel() {
  const { effectiveProduct } = useAuth();
  const ints = [
    ["Webhooks", "conectado", "produção", "há 20 min"],
    ["Analytics Provider", "requer atenção", "produção", "ontem"],
    ["Storage Provider", "conectado", "produção", "há 1 h"],
    ["Email Provider", "desconectado", "staging", "—"],
    ["WhatsApp futuro", "futuro", "—", "—"],
  ];
  const [sessionsOpen, setSessionsOpen] = useState(false);
  const [configuring, setConfiguring] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const handleSaveSecurity = async () => {
    if (!effectiveProduct) return;
    setSaving(true);
    try {
      await settingsService.saveSecurity(effectiveProduct.id);
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
        <div className="space-y-4">
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{ints.map((i) => <IntegrationCard key={i[0]} i={i} onConfigure={() => setConfiguring(i[0])} />)}</div>
          {effectiveProduct && <TelegramAlertSection productId={effectiveProduct.id} />}
        </div>
      </div>
    </>
  );
}
