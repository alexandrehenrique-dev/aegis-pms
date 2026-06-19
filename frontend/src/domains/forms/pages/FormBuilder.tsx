import { Plus } from "lucide-react";
import { Badge, Button, Card, Field, PageHeader, SelectLike } from "../../../shared/components/Primitives";
import { fieldTypes } from "../mocks/forms.mocks";

function FieldCard({ name }: { name: string }) {
  return <button className="flex w-full items-center justify-between rounded-xl border border-border bg-card p-3 text-left text-sm transition hover:bg-muted"><span>{name}</span><Plus size={14} /></button>;
}

function FieldPalette() {
  return (
    <Card className="h-full">
      <h2 className="mb-3 text-lg font-semibold">Biblioteca de campos</h2>
      <div className="grid gap-2">{fieldTypes.map((f) => <FieldCard key={f} name={f} />)}</div>
    </Card>
  );
}

function FormBuilderCanvas() {
  return (
    <Card>
      <div className="mb-4 flex items-center justify-between">
        <div><h2 className="text-lg font-semibold">Canvas visual</h2><p className="text-sm text-muted-foreground">Grid, ordem, arraste visual e obrigatoriedade.</p></div>
        <Badge tone="blue">Contato Comercial</Badge>
      </div>
      <div className="rounded-2xl border border-dashed border-border bg-muted/30 p-4">
        <div className="mx-auto max-w-xl rounded-2xl border border-border bg-card p-5">
          <h3 className="text-xl font-semibold">Contato Comercial</h3>
          <p className="mt-1 text-sm text-muted-foreground">Fluxo de aquisição vinculado ao produto Maestro Beton.</p>
          {[["1", "Nome", "obrigatório"], ["2", "Email", "obrigatório"], ["3", "Telefone", "opcional"], ["4", "Mensagem", "obrigatório"]].map((x) => (
            <div key={x[0]} className="mt-3 rounded-xl border border-border p-3">
              <div className="flex items-center justify-between"><label className="text-sm font-medium">{x[0]}. {x[1]}</label><Badge tone={x[2] === "obrigatório" ? "amber" : "neutral"}>{x[2]}</Badge></div>
              <div className="mt-2 h-10 rounded-lg bg-muted" />
            </div>
          ))}
          <Button primary>Botão Enviar</Button>
        </div>
      </div>
    </Card>
  );
}

function FormPropertiesPanel() {
  return (
    <Card className="h-full">
      <h2 className="mb-3 text-lg font-semibold">Propriedades</h2>
      <div className="space-y-3">
        <Field label="Label" value="Email" />
        <Field label="Placeholder" value="seu@email.com" />
        <SelectLike label="Obrigatório" value="Sim" />
        <Field label="Ajuda" value="Usaremos este email para responder sua solicitação." />
        <SelectLike label="Validação" value="Email válido" />
        <Field label="Máscara" value="—" />
        <Field label="Valor padrão" value="" />
        <SelectLike label="Condição" value="Sempre visível" />
      </div>
    </Card>
  );
}

export function FormBuilder() {
  return (
    <>
      <PageHeader title="Form Builder" module="Forms" desc="Construa o fluxo de captura como ativo operacional do produto." badge="Builder">
        <Button>Salvar rascunho</Button>
        <Button>Preview</Button>
        <Button primary>Publicar</Button>
      </PageHeader>
      <div className="mb-4 xl:hidden"><div className="flex gap-2 overflow-auto pb-1">{["1 Campos", "2 Canvas", "3 Propriedades", "4 Preview"].map((x) => <Badge key={x} tone="blue">{x}</Badge>)}</div></div>
      <div className="grid gap-4 xl:grid-cols-[280px_1fr_340px]"><FieldPalette /><FormBuilderCanvas /><FormPropertiesPanel /></div>
    </>
  );
}
