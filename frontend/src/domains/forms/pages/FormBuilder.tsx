import { useState } from "react";
import { useNavigate } from "react-router";
import { Loader2, Plus } from "lucide-react";
import { Badge, Button, Card, Field, PageHeader, SelectLike, SkeletonLines, PartialErrorWidget } from "../../../shared/components/Primitives";
import { formsService } from "../services/formsService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { toast } from "../../../core/notifications/toast";

type CanvasField = {
  id: string;
  label: string;
  placeholder: string;
  required: boolean;
  help: string;
  validation: string;
  mask: string;
  defaultValue: string;
  condition: string;
};

function makeField(label: string): CanvasField {
  return { id: `${label}-${Date.now()}`, label, placeholder: "", required: true, help: "", validation: "Nenhuma", mask: "—", defaultValue: "", condition: "Sempre visível" };
}

function FieldCard({ name, onAdd }: { name: string; onAdd: () => void }) {
  return <button onClick={onAdd} className="flex w-full items-center justify-between rounded-xl border border-border bg-card p-3 text-left text-sm transition hover:bg-muted"><span>{name}</span><Plus size={14} /></button>;
}

function FieldPalette({ onAddField }: { onAddField: (name: string) => void }) {
  const { data: fieldTypes, loading, error } = useAsyncData(() => formsService.listFieldTypes(), []);
  return (
    <Card className="h-full">
      <h2 className="mb-3 text-lg font-semibold">Biblioteca de campos</h2>
      {loading ? <SkeletonLines /> : error || !fieldTypes ? <PartialErrorWidget /> : (
        <div className="grid gap-2">{fieldTypes.map((f) => <FieldCard key={f} name={f} onAdd={() => onAddField(f)} />)}</div>
      )}
    </Card>
  );
}

function FormBuilderCanvas({ fields, selectedId, onSelect }: { fields: CanvasField[]; selectedId: string | null; onSelect: (id: string) => void }) {
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
          {fields.map((f, i) => (
            <button key={f.id} onClick={() => onSelect(f.id)} className={`mt-3 w-full rounded-xl border p-3 text-left ${selectedId === f.id ? "border-primary" : "border-border"}`}>
              <div className="flex items-center justify-between"><label className="text-sm font-medium">{i + 1}. {f.label}</label><Badge tone={f.required ? "amber" : "neutral"}>{f.required ? "obrigatório" : "opcional"}</Badge></div>
              <div className="mt-2 h-10 rounded-lg bg-muted" />
            </button>
          ))}
          <Button primary>Botão Enviar</Button>
        </div>
      </div>
    </Card>
  );
}

function FormPropertiesPanel({ field, onChange }: { field: CanvasField | undefined; onChange: (patch: Partial<CanvasField>) => void }) {
  if (!field) {
    return (
      <Card className="h-full">
        <h2 className="mb-3 text-lg font-semibold">Propriedades</h2>
        <p className="text-sm text-muted-foreground">Selecione um campo no canvas para editar suas propriedades.</p>
      </Card>
    );
  }
  return (
    <Card className="h-full">
      <h2 className="mb-3 text-lg font-semibold">Propriedades</h2>
      <div className="space-y-3">
        <Field label="Label" value={field.label} onChange={(v) => onChange({ label: v })} />
        <Field label="Placeholder" value={field.placeholder} onChange={(v) => onChange({ placeholder: v })} />
        <SelectLike label="Obrigatório" value={field.required ? "Sim" : "Não"} options={["Sim", "Não"]} onChange={(v) => onChange({ required: v === "Sim" })} />
        <Field label="Ajuda" value={field.help} onChange={(v) => onChange({ help: v })} />
        <SelectLike label="Validação" value={field.validation} options={["Nenhuma", "Email válido", "Telefone válido", "Somente números"]} onChange={(v) => onChange({ validation: v })} />
        <Field label="Máscara" value={field.mask} onChange={(v) => onChange({ mask: v })} />
        <Field label="Valor padrão" value={field.defaultValue} onChange={(v) => onChange({ defaultValue: v })} />
        <SelectLike label="Condição" value={field.condition} options={["Sempre visível", "Condicional"]} onChange={(v) => onChange({ condition: v })} />
      </div>
    </Card>
  );
}

export function FormBuilder() {
  const navigate = useNavigate();
  const [fields, setFields] = useState<CanvasField[]>([
    { id: "nome", label: "Nome", placeholder: "Seu nome", required: true, help: "", validation: "Nenhuma", mask: "—", defaultValue: "", condition: "Sempre visível" },
    { id: "email", label: "Email", placeholder: "seu@email.com", required: true, help: "Usaremos este email para responder sua solicitação.", validation: "Email válido", mask: "—", defaultValue: "", condition: "Sempre visível" },
    { id: "telefone", label: "Telefone", placeholder: "(11) 99999-9999", required: false, help: "", validation: "Telefone válido", mask: "—", defaultValue: "", condition: "Sempre visível" },
    { id: "mensagem", label: "Mensagem", placeholder: "", required: true, help: "", validation: "Nenhuma", mask: "—", defaultValue: "", condition: "Sempre visível" },
  ]);
  const [selectedId, setSelectedId] = useState<string | null>("email");
  const [saving, setSaving] = useState(false);
  const [publishing, setPublishing] = useState(false);

  const selectedField = fields.find((f) => f.id === selectedId);

  const handleAddField = (name: string) => {
    const created = makeField(name);
    setFields((prev) => [...prev, created]);
    setSelectedId(created.id);
    toast.success(`Campo "${name}" adicionado ao canvas.`);
  };

  const handlePatchField = (patch: Partial<CanvasField>) => {
    setFields((prev) => prev.map((f) => (f.id === selectedId ? { ...f, ...patch } : f)));
  };

  const handleSaveDraft = async () => {
    setSaving(true);
    try {
      await formsService.saveDraft();
      toast.success("Rascunho salvo!");
    } finally {
      setSaving(false);
    }
  };

  const handlePublish = async () => {
    setPublishing(true);
    try {
      await formsService.publish();
      toast.success("Formulário publicado!");
    } finally {
      setPublishing(false);
    }
  };

  return (
    <>
      <PageHeader title="Form Builder" module="Forms" desc="Construa o fluxo de captura como ativo operacional do produto." badge="Builder">
        <Button onClick={handleSaveDraft} disabled={saving}>{saving && <Loader2 size={15} className="animate-spin" />}{saving ? "Salvando..." : "Salvar rascunho"}</Button>
        <Button onClick={() => navigate("/forms/preview")}>Preview</Button>
        <Button primary onClick={handlePublish} disabled={publishing}>{publishing && <Loader2 size={15} className="animate-spin" />}{publishing ? "Publicando..." : "Publicar"}</Button>
      </PageHeader>
      <div className="mb-4 xl:hidden"><div className="flex gap-2 overflow-auto pb-1">{["1 Campos", "2 Canvas", "3 Propriedades", "4 Preview"].map((x) => <Badge key={x} tone="blue">{x}</Badge>)}</div></div>
      <div className="grid gap-4 xl:grid-cols-[280px_1fr_340px]">
        <FieldPalette onAddField={handleAddField} />
        <FormBuilderCanvas fields={fields} selectedId={selectedId} onSelect={setSelectedId} />
        <FormPropertiesPanel field={selectedField} onChange={handlePatchField} />
      </div>
    </>
  );
}
