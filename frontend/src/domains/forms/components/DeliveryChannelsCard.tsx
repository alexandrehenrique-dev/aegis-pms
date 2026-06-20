import { Card, Field, SelectLike } from "../../../shared/components/Primitives";
import type { DeliveryChannel, DeliveryChannelType, FormDelivery } from "../contracts/responses";

const CHANNEL_LABELS: Record<DeliveryChannelType, string> = { email: "E-mail", whatsapp: "WhatsApp", telegram: "Telegram", webhook: "Webhook" };

function ChannelFields({ channel, onChangeConfig }: { channel: DeliveryChannel; onChangeConfig: (config: Record<string, string>) => void }) {
  const set = (key: string, value: string) => onChangeConfig({ ...channel.config, [key]: value });

  if (channel.type === "email") return <Field label="Endereço de e-mail" value={channel.config.address ?? ""} onChange={(v) => set("address", v)} />;
  if (channel.type === "whatsapp") return <Field label="Número (com DDI)" value={channel.config.number ?? ""} onChange={(v) => set("number", v)} />;
  if (channel.type === "telegram") {
    return (
      <div className="grid gap-3 md:grid-cols-2">
        <Field label="Chat ID" value={channel.config.chatId ?? ""} onChange={(v) => set("chatId", v)} />
        <Field label="Token do bot" value={channel.config.botToken ?? ""} onChange={(v) => set("botToken", v)} />
      </div>
    );
  }
  return (
    <div className="grid gap-3 md:grid-cols-2">
      <Field label="URL" value={channel.config.url ?? ""} onChange={(v) => set("url", v)} />
      <SelectLike label="Método" value={channel.config.method ?? "POST"} options={["POST", "PUT"]} onChange={(v) => set("method", v)} />
    </div>
  );
}

/**
 * Configuração de entrega de respostas por múltiplos canais (Sprint 13,
 * Tarefa J) — antes, uma submissão só gerava um toast interno (`toast.ts`),
 * sem nenhum lugar para configurar "para onde mandar". Cada checkbox marcado
 * expande os campos daquele canal; mais de um pode estar ativo ao mesmo
 * tempo. O disparo real (enviar o webhook, mandar o WhatsApp) é trabalho de
 * backend, fora de escopo do frontend — aqui só a configuração é salva.
 */
export function DeliveryChannelsCard({ delivery, onChange }: { delivery: FormDelivery; onChange: (next: FormDelivery) => void }) {
  const updateChannel = (type: DeliveryChannelType, patch: Partial<DeliveryChannel>) => {
    onChange({ channels: delivery.channels.map((c) => (c.type === type ? { ...c, ...patch } : c)) });
  };

  return (
    <Card>
      <h2 className="mb-1 text-lg font-semibold">Receber respostas por</h2>
      <p className="mb-3 text-sm text-muted-foreground">Marque um ou mais canais — o disparo real é feito pelo backend a cada nova submissão.</p>
      <div className="space-y-3">
        {delivery.channels.map((channel) => (
          <div key={channel.type} className="rounded-lg border border-border p-3">
            <label className="flex items-center gap-2 text-sm font-medium">
              <input type="checkbox" checked={channel.enabled} onChange={(e) => updateChannel(channel.type, { enabled: e.target.checked })} className="accent-primary" />
              {CHANNEL_LABELS[channel.type]}
            </label>
            {channel.enabled && (
              <div className="mt-2">
                <ChannelFields channel={channel} onChangeConfig={(config) => updateChannel(channel.type, { config })} />
              </div>
            )}
          </div>
        ))}
      </div>
    </Card>
  );
}
