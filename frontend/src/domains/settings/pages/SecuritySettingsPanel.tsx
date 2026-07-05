import { useEffect, useMemo, useState } from "react";
import { CheckCircle2, Loader2, Send, Shield, X } from "lucide-react";
import { Badge, Button, Card, EmptyState, Field, PageHeader, SkeletonLines } from "../../../shared/components/Primitives";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "../../../shared/components/ui/dialog";
import { toast } from "../../../core/notifications/toast";
import { settingsService } from "../services/settingsService";
import { useAuth } from "../../../core/auth/useAuth";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import type { ProductSecuritySettings, UpdateProductSecuritySettingsRequest } from "../contracts/responses";

const SESSIONS = [
  { device: "Chrome · macOS", location: "São Paulo, BR", lastActive: "agora" },
  { device: "Safari · iPhone", location: "São Paulo, BR", lastActive: "há 2 h" },
  { device: "Edge · Windows", location: "Curitiba, BR", lastActive: "há 1 dia" },
];

type IntegrationStatus = "connected" | "disconnected" | "attention";
type IntegrationType = "Webhook" | "Analytics" | "Email" | "Telegram";
type IntegrationItem = {
  id: "webhooks" | "analytics" | "email" | "telegram";
  name: string;
  type: IntegrationType;
  status: IntegrationStatus;
  environment: string;
  updatedAt: string;
};
type FilterKey = "type" | "status";

const FILTER_CONFIG: Array<{ key: FilterKey; label: string; icon: React.ReactNode; field: keyof IntegrationItem }> = [
  { key: "type", label: "Tipo", icon: <Shield size={13} />, field: "type" },
  { key: "status", label: "Status", icon: <CheckCircle2 size={13} />, field: "status" },
];

function Chip({ active, onClick, children }: { active: boolean; onClick: () => void; children: React.ReactNode }) {
  return (
    <button
      onClick={onClick}
      className={`flex items-center gap-1 rounded-full px-2.5 py-0.5 text-[11px] font-medium transition-colors ${
        active ? "bg-primary text-primary-foreground shadow-sm" : "bg-muted text-muted-foreground hover:bg-muted/70"
      }`}
    >
      {children}
    </button>
  );
}

function FilterPanel({
  integrations,
  filters,
  onChange,
  onClearAll,
}: {
  integrations: IntegrationItem[];
  filters: Partial<Record<FilterKey, string | null>>;
  onChange: (key: FilterKey, value: string | null) => void;
  onClearAll: () => void;
}) {
  const hasActive = Object.values(filters).some(Boolean);
  return (
    <Card className="sticky top-4 h-fit">
      <div className="mb-3 flex items-center justify-between">
        <h2 className="text-sm font-semibold">Filtros</h2>
        {hasActive && (
          <button onClick={onClearAll} className="flex items-center gap-1 rounded-md px-2 py-0.5 text-[11px] text-muted-foreground transition-colors hover:bg-muted hover:text-foreground">
            <X size={10} /> Limpar
          </button>
        )}
      </div>
      {FILTER_CONFIG.map(({ key, label, icon, field }) => {
        const opts = Array.from(new Set(integrations.map((item) => String(item[field])))).filter(Boolean).sort();
        const active = filters[key] ?? null;
        return (
          <div key={key} className="mb-4">
            <p className="mb-1.5 flex items-center gap-1.5 text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">
              {icon} {label}
            </p>
            <div className="flex flex-wrap gap-1">
              <Chip active={!active} onClick={() => onChange(key, null)}>Todos</Chip>
              {opts.map((option) => (
                <Chip key={option} active={active === option} onClick={() => onChange(key, active === option ? null : option)}>
                  {key === "status" ? statusLabel(option) : option}
                </Chip>
              ))}
            </div>
          </div>
        );
      })}
    </Card>
  );
}

function IntegrationCard({ integration, onConfigure }: { integration: IntegrationItem; onConfigure: () => void }) {
  return (
    <Card>
      <div className="flex justify-between"><h3 className="font-semibold">{integration.name}</h3><Badge tone={integration.status === "connected" ? "green" : integration.status === "attention" ? "amber" : "neutral"}>{statusLabel(integration.status)}</Badge></div>
      <p className="mt-2 text-sm text-muted-foreground">Ambiente: {integration.environment} · Última sync: {integration.updatedAt}</p>
      <Button onClick={onConfigure}>Configurar</Button>
    </Card>
  );
}

function statusLabel(status: string): string {
  if (status === "connected") return "conectado";
  if (status === "attention") return "requer atenção";
  if (status === "disconnected") return "desconectado";
  return "desconectado";
}

function lastSync(settings: ProductSecuritySettings): string {
  return settings.updatedAt ? new Date(settings.updatedAt).toLocaleString("pt-BR") : "—";
}

function integrationsFrom(settings: ProductSecuritySettings): IntegrationItem[] {
  const updatedAt = lastSync(settings);
  return [
    { id: "webhooks", name: "Webhooks", type: "Webhook", status: settings.webhookStatus, environment: "produção", updatedAt },
    { id: "analytics", name: "Analytics Provider", type: "Analytics", status: settings.analyticsStatus, environment: "produção", updatedAt },
    { id: "email", name: "Email Provider", type: "Email", status: settings.emailStatus, environment: "produção", updatedAt },
    { id: "telegram", name: "Telegram", type: "Telegram", status: settings.telegramAlert ? "connected" : "disconnected", environment: "produção", updatedAt },
  ];
}

function buildPayload(current: ProductSecuritySettings, patch: Partial<UpdateProductSecuritySettingsRequest>): UpdateProductSecuritySettingsRequest {
  return {
    webhookUrl: current.webhookUrl ?? undefined,
    analyticsEnabled: current.analyticsEnabled,
    analyticsProviderKey: current.analyticsProviderKey ?? undefined,
    emailDeliveryEnabled: current.emailDeliveryEnabled,
    telegramAlert: null,
    ...patch,
  };
}

function IntegrationConfigDialog({
  productId,
  integration,
  settings,
  onSaved,
  onClose,
}: {
  productId: string;
  integration: IntegrationItem | null;
  settings: ProductSecuritySettings | null;
  onSaved: (settings: ProductSecuritySettings) => void;
  onClose: () => void;
}) {
  const [webhookUrl, setWebhookUrl] = useState("");
  const [webhookSecret, setWebhookSecret] = useState("");
  const [analyticsEnabled, setAnalyticsEnabled] = useState(false);
  const [analyticsProviderKey, setAnalyticsProviderKey] = useState("");
  const [emailDeliveryEnabled, setEmailDeliveryEnabled] = useState(false);
  const [telegramChatId, setTelegramChatId] = useState("");
  const [telegramBotToken, setTelegramBotToken] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!settings) return;
    setWebhookUrl(settings.webhookUrl ?? "");
    setWebhookSecret("");
    setAnalyticsEnabled(settings.analyticsEnabled);
    setAnalyticsProviderKey(settings.analyticsProviderKey ?? "");
    setEmailDeliveryEnabled(settings.emailDeliveryEnabled);
    setTelegramChatId(settings.telegramAlert?.chatId ?? "");
    setTelegramBotToken("");
  }, [settings, integration?.id]);

  if (!integration || !settings) return null;

  const handleSave = async () => {
    setSaving(true);
    try {
      let payload: UpdateProductSecuritySettingsRequest;
      if (integration.id === "webhooks") {
        payload = buildPayload(settings, { webhookUrl: webhookUrl.trim(), webhookSecret: webhookSecret.trim() || undefined });
      } else if (integration.id === "analytics") {
        payload = buildPayload(settings, { analyticsEnabled, analyticsProviderKey: analyticsProviderKey.trim() });
      } else if (integration.id === "email") {
        payload = buildPayload(settings, { emailDeliveryEnabled });
      } else {
        payload = buildPayload(settings, { telegramAlert: { chatId: telegramChatId.trim(), botToken: telegramBotToken.trim() } });
      }
      const updated = await settingsService.updateProductSettings(productId, payload);
      onSaved(updated);
      toast.success(`${integration.name} atualizado.`);
      onClose();
    } finally {
      setSaving(false);
    }
  };

  return (
    <DialogContent>
      <DialogHeader><DialogTitle>Configurar {integration.name}</DialogTitle></DialogHeader>
      <div className="space-y-3">
        {integration.id === "webhooks" && (
          <>
            <Field label="URL do webhook" value={webhookUrl} onChange={setWebhookUrl} locked={saving} />
            <Field label="Secret" type="password" value={webhookSecret} onChange={setWebhookSecret} locked={saving} />
          </>
        )}
        {integration.id === "analytics" && (
          <>
            <label className="flex items-center gap-2 rounded-lg border border-border p-3 text-sm">
              <input type="checkbox" checked={analyticsEnabled} onChange={(event) => setAnalyticsEnabled(event.target.checked)} disabled={saving} />
              Analytics habilitado
            </label>
            <Field label="Provider key" type="password" value={analyticsProviderKey} onChange={setAnalyticsProviderKey} locked={saving} />
          </>
        )}
        {integration.id === "email" && (
          <label className="flex items-center gap-2 rounded-lg border border-border p-3 text-sm">
            <input type="checkbox" checked={emailDeliveryEnabled} onChange={(event) => setEmailDeliveryEnabled(event.target.checked)} disabled={saving} />
            Entrega de e-mail habilitada
          </label>
        )}
        {integration.id === "telegram" && (
          <>
            <Field label="Chat ID" value={telegramChatId} onChange={setTelegramChatId} locked={saving} />
            <Field label="Bot Token" type="password" value={telegramBotToken} onChange={setTelegramBotToken} locked={saving} />
          </>
        )}
        <Button primary onClick={handleSave} disabled={saving}>
          {saving && <Loader2 size={15} className="animate-spin" />}Salvar integração
        </Button>
      </div>
    </DialogContent>
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
  const { data: productSettings, loading: loadingSettings } = useAsyncData(
    () => (effectiveProduct ? settingsService.getProductSettings(effectiveProduct.id) : Promise.resolve(null)),
    [effectiveProduct?.id],
  );
  const [settings, setSettings] = useState<ProductSecuritySettings | null>(null);
  const integrations = useMemo(() => settings ? integrationsFrom(settings) : [], [settings]);
  const [filters, setFilters] = useState<Partial<Record<FilterKey, string | null>>>({});
  const [sessionsOpen, setSessionsOpen] = useState(false);
  const [configuring, setConfiguring] = useState<IntegrationItem | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => { if (productSettings) setSettings(productSettings); }, [productSettings]);

  const filteredIntegrations = useMemo(
    () =>
      integrations.filter((integration) =>
        FILTER_CONFIG.every(({ key, field }) => {
          const active = filters[key];
          return !active || String(integration[field]) === active;
        }),
      ),
    [integrations, filters],
  );

  const handleFilter = (key: FilterKey, value: string | null) => setFilters((current) => ({ ...current, [key]: value }));

  const handleSaveSecurity = async () => {
    if (!effectiveProduct) return;
    setSaving(true);
    try {
      if (settings) {
        const updated = await settingsService.updateProductSettings(effectiveProduct.id, buildPayload(settings, {}));
        setSettings(updated);
      } else {
        await settingsService.saveSecurity(effectiveProduct.id);
      }
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
        {effectiveProduct && (
          <IntegrationConfigDialog
            productId={effectiveProduct.id}
            integration={configuring}
            settings={settings}
            onSaved={setSettings}
            onClose={() => setConfiguring(null)}
          />
        )}
      </Dialog>
      <PageHeader title="Security & Integrations" module="Configurações" desc="Segurança e integrações essenciais, com estados e risco operacional." badge="Admin">
        <Button onClick={() => setSessionsOpen(true)}>Revisar sessões</Button>
        <Button primary onClick={handleSaveSecurity} disabled={saving}>{saving && <Loader2 size={15} className="animate-spin" />}{saving ? "Salvando..." : "Salvar segurança"}</Button>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[240px_1fr]">
        <FilterPanel integrations={integrations} filters={filters} onChange={handleFilter} onClearAll={() => setFilters({})} />
        <div className="space-y-4">
          {loadingSettings ? <SkeletonLines /> : filteredIntegrations.length === 0 ? (
            <EmptyState compact title="Nenhuma integração encontrada" description="Remova filtros para ver mais integrações." primaryAction={{ label: "Limpar filtros", onClick: () => setFilters({}) }} />
          ) : (
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{filteredIntegrations.map((integration) => <IntegrationCard key={integration.id} integration={integration} onConfigure={() => setConfiguring(integration)} />)}</div>
          )}
          {effectiveProduct && <TelegramAlertSection productId={effectiveProduct.id} />}
        </div>
      </div>
    </>
  );
}
