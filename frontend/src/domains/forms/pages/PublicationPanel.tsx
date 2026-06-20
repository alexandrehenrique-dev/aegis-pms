import { useEffect, useState } from "react";
import { Loader2 } from "lucide-react";
import { Button, Card, PageHeader } from "../../../shared/components/Primitives";
import { ConversionCard } from "../components/ConversionCard";
import { DeliveryChannelsCard } from "../components/DeliveryChannelsCard";
import { toast } from "../../../core/notifications/toast";
import { formsService } from "../services/formsService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import type { FormDelivery } from "../contracts/responses";

const EMBED_SNIPPET = "<aegis-form id=contato-comercial />";
const FORM_ID = "form-contato-comercial";

export function PublicationPanel() {
  const [publishing, setPublishing] = useState(false);
  const { data: loadedDelivery } = useAsyncData(() => formsService.getDelivery(FORM_ID), []);
  const [delivery, setDelivery] = useState<FormDelivery | null>(null);
  const [savingDelivery, setSavingDelivery] = useState(false);

  useEffect(() => setDelivery(loadedDelivery ?? null), [loadedDelivery]);

  const handleSaveDelivery = async () => {
    if (!delivery) return;
    setSavingDelivery(true);
    try {
      await formsService.saveDelivery(FORM_ID, delivery);
      toast.success("Configuração de entrega salva.");
    } finally {
      setSavingDelivery(false);
    }
  };

  const handleCopyEmbed = async () => {
    await navigator.clipboard.writeText(EMBED_SNIPPET);
    toast.success("Copiado");
  };

  const handlePublish = async () => {
    setPublishing(true);
    try {
      await formsService.publish();
      toast.success("Alterações publicadas!");
    } finally {
      setPublishing(false);
    }
  };

  return (
    <>
      <PageHeader title="Publicação do Formulário" module="Forms" desc="Controle URL, embed, script, iframe, domínio e status de publicação." badge="Publicado">
        <Button onClick={handleCopyEmbed}>Copiar embed</Button>
        <Button primary onClick={handlePublish} disabled={publishing}>{publishing && <Loader2 size={15} className="animate-spin" />}{publishing ? "Publicando..." : "Publicar alterações"}</Button>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
        <Card>
          <h2 className="mb-3 text-lg font-semibold">Canais de publicação</h2>
          {[["URL", "https://maestrobeton.com/forms/contato"], ["Embed", "<aegis-form id=contato-comercial />"], ["Script", '<script src="/aegis/forms.js"></script>'], ["Iframe", '<iframe src="/forms/contato"></iframe>'], ["Domínio", "maestrobeton.com"], ["Status", "Publicado e rastreável"]].map((x) => (
            <div key={x[0]} className="mb-2 rounded-xl border border-border p-3 text-sm"><b>{x[0]}</b><p className="mt-1 break-all font-mono text-xs text-muted-foreground">{x[1]}</p></div>
          ))}
        </Card>
        <Card>
          <h2 className="mb-3 text-lg font-semibold">Impacto operacional</h2>
          <p className="text-sm text-muted-foreground">Este formulário captura leads e cria eventos vinculados ao produto Maestro Beton.</p>
          <ConversionCard />
        </Card>
      </div>
      {delivery && (
        <div className="mt-4 grid gap-3">
          <DeliveryChannelsCard delivery={delivery} onChange={setDelivery} />
          <div>
            <Button primary onClick={handleSaveDelivery} disabled={savingDelivery}>{savingDelivery && <Loader2 size={15} className="animate-spin" />}{savingDelivery ? "Salvando..." : "Salvar configuração de entrega"}</Button>
          </div>
        </div>
      )}
    </>
  );
}
