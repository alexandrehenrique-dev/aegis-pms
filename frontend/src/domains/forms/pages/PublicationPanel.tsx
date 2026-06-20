import { useState } from "react";
import { Loader2 } from "lucide-react";
import { Button, Card, PageHeader } from "../../../shared/components/Primitives";
import { ConversionCard } from "../components/ConversionCard";
import { toast } from "../../../core/notifications/toast";
import { formsService } from "../services/formsService";

const EMBED_SNIPPET = "<aegis-form id=contato-comercial />";

export function PublicationPanel() {
  const [publishing, setPublishing] = useState(false);

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
    </>
  );
}
