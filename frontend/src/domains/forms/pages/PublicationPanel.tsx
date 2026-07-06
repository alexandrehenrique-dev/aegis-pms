import { useEffect, useState } from "react";
import { useNavigate } from "react-router";
import { Loader2 } from "lucide-react";
import { Button, Card, PageHeader, SelectLike } from "../../../shared/components/Primitives";
import { ConversionCard } from "../components/ConversionCard";
import { DeliveryChannelsCard } from "../components/DeliveryChannelsCard";
import { toast } from "../../../core/notifications/toast";
import { formsService } from "../services/formsService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";
import type { ApiError } from "../../../shared/services/apiClient";
import type { FormDelivery } from "../contracts/responses";

/**
 * F.5 (BUG-SPRINT consolidado) — "Publicação" é uma aba de nível de módulo
 * (`navConfig.ts`: Dashboard/Submissions/Publicação/Analytics), não uma rota
 * por formulário; por isso o formulário é escolhido aqui via seletor (em vez
 * de `useParams`), com o primeiro formulário do produto como padrão — nunca
 * mais o `form-contato-comercial` fixo que ignorava qualquer outro form.
 */
export function PublicationPanel() {
  const navigate = useNavigate();
  const { product } = useCurrentProduct();
  const productId = product?.id ?? "";
  const [publishing, setPublishing] = useState(false);
  const [selectedFormId, setSelectedFormId] = useState<string | null>(null);
  const { data: forms } = useAsyncData(() => (productId ? formsService.listForms(productId) : Promise.resolve([])), [productId]);
  const { data: form } = useAsyncData(
    () => (productId && selectedFormId ? formsService.getForm(productId, selectedFormId) : Promise.resolve(undefined)),
    [productId, selectedFormId],
  );
  const { data: loadedDelivery } = useAsyncData(
    () => (productId && selectedFormId ? formsService.getDelivery(productId, selectedFormId) : Promise.resolve(null)),
    [productId, selectedFormId],
  );
  const [delivery, setDelivery] = useState<FormDelivery | null>(null);
  const [savingDelivery, setSavingDelivery] = useState(false);

  useEffect(() => setDelivery(loadedDelivery ?? null), [loadedDelivery]);
  useEffect(() => {
    if (!selectedFormId && forms && forms.length > 0) setSelectedFormId(forms[0].id);
  }, [forms, selectedFormId]);

  const handleSaveDelivery = async () => {
    if (!delivery || !selectedFormId) return;
    setSavingDelivery(true);
    try {
      await formsService.saveDelivery(productId, selectedFormId, delivery);
      toast.success("Configuração de entrega salva.");
    } catch (error) {
      const apiError = error as ApiError;
      if (apiError.status === 400 && apiError.code === "telegram_invalid_credentials") {
        toast.error("Não foi possível validar o Telegram.", { description: "Revise o chat ID e o token do bot antes de salvar." });
      } else {
        toast.error("Não foi possível salvar a configuração de entrega.");
      }
    } finally {
      setSavingDelivery(false);
    }
  };

  const embedSnippet = selectedFormId ? `<aegis-form id="${selectedFormId}" />` : "—";

  const handleCopyEmbed = async () => {
    if (!selectedFormId) return;
    await navigator.clipboard.writeText(embedSnippet);
    toast.success("Copiado");
  };

  const handlePublish = async () => {
    if (!selectedFormId) return;
    setPublishing(true);
    try {
      await formsService.publish(productId, selectedFormId);
      toast.success("Alterações publicadas!");
      navigate("/forms/list");
    } catch (err: unknown) {
      toast.error("Falha ao publicar", {
        description: (err as { message?: string }).message ?? "Tente novamente.",
      });
    } finally {
      setPublishing(false);
    }
  };

  const publicationChannels: [string, string][] = [
    ["Embed", embedSnippet],
    ["Script", '<script src="/aegis/forms.js"></script>'],
    ["Iframe", selectedFormId ? `<iframe src="/forms/${selectedFormId}"></iframe>` : "—"],
    ["Status", form?.status ?? "—"],
  ];

  return (
    <>
      <PageHeader title="Publicação do Formulário" module="Forms" desc="Controle embed, script, iframe e status de publicação." badge={form?.status ?? "Publicação"}>
        {forms && forms.length > 0 && (
          <SelectLike label="" value={form?.name ?? "Selecione um formulário"} options={forms.map((f) => f.name)}
            onChange={(name) => setSelectedFormId(forms.find((f) => f.name === name)?.id ?? null)} />
        )}
        <Button onClick={handleCopyEmbed} disabled={!selectedFormId}>Copiar embed</Button>
        <Button primary onClick={handlePublish} disabled={publishing || !selectedFormId}>{publishing && <Loader2 size={15} className="animate-spin" />}{publishing ? "Publicando..." : "Publicar alterações"}</Button>
      </PageHeader>
      {!selectedFormId ? (
        <Card><p className="text-sm text-muted-foreground">Nenhum formulário neste produto ainda — crie um em "Forms" antes de configurar a publicação.</p></Card>
      ) : (
        <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
          <Card>
            <h2 className="mb-3 text-lg font-semibold">Canais de publicação</h2>
            {publicationChannels.map(([label, value]) => (
              <div key={label} className="mb-2 rounded-xl border border-border p-3 text-sm"><b>{label}</b><p className="mt-1 break-all font-mono text-xs text-muted-foreground">{value}</p></div>
            ))}
          </Card>
          <Card>
            <h2 className="mb-3 text-lg font-semibold">Impacto operacional</h2>
            <p className="text-sm text-muted-foreground">Este formulário captura leads e cria eventos vinculados ao produto {product?.name ?? "atual"}.</p>
            <ConversionCard />
          </Card>
        </div>
      )}
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
