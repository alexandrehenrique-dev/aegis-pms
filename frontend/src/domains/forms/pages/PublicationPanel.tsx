import { Button, Card, PageHeader } from "../../../shared/components/Primitives";
import { ConversionCard } from "../components/ConversionCard";

export function PublicationPanel() {
  return (
    <>
      <PageHeader title="Publicação do Formulário" module="Forms" desc="Controle URL, embed, script, iframe, domínio e status de publicação." badge="Publicado">
        <Button>Copiar embed</Button>
        <Button primary>Publicar alterações</Button>
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
