import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router";
import { AnimatePresence } from "motion/react";
import { Loader2, Plus, Trash2 } from "lucide-react";
import { Badge, Button, Card, Field, PageHeader, SelectLike, SkeletonLines, PartialErrorWidget } from "../../../shared/components/Primitives";
import { ConfirmDialog } from "../../../shared/components/ConfirmDialog";
import { formsService } from "../services/formsService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { toast } from "../../../core/notifications/toast";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";
import type { FormField } from "../contracts/responses";

const UPLOAD_FORMAT_OPTIONS = ["PDF", "Imagem", "DOCX", "ZIP"];

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

function FormBuilderCanvas({ formName, fields, selectedId, onSelect, onRequestRemove }: {
  formName: string; fields: FormField[]; selectedId: string | null; onSelect: (id: string) => void; onRequestRemove: (id: string) => void;
}) {
  return (
    <Card>
      <div className="mb-4 flex items-center justify-between">
        <div><h2 className="text-lg font-semibold">Canvas visual</h2><p className="text-sm text-muted-foreground">Grid, ordem, arraste visual e obrigatoriedade.</p></div>
        <Badge tone="blue">{formName}</Badge>
      </div>
      <div className="rounded-2xl border border-dashed border-border bg-muted/30 p-4">
        <div className="mx-auto max-w-xl rounded-2xl border border-border bg-card p-5">
          <h3 className="text-xl font-semibold">{formName}</h3>
          <p className="mt-1 text-sm text-muted-foreground">Formulário vinculado ao produto.</p>
          {fields.map((f, i) => (
            <div key={f.id} className="mt-3 flex items-start gap-1">
              <button onClick={() => onSelect(f.id)} className={`flex-1 rounded-xl border p-3 text-left ${selectedId === f.id ? "border-primary" : "border-border"}`}>
                <div className="flex items-center justify-between"><label className="text-sm font-medium">{i + 1}. {f.label}</label><Badge tone={f.required ? "amber" : "neutral"}>{f.required ? "obrigatório" : "opcional"}</Badge></div>
                <div className="mt-2 h-10 rounded-lg bg-muted" />
              </button>
              <button onClick={() => onRequestRemove(f.id)} aria-label={`Remover campo ${f.label}`} className="mt-1 rounded-lg p-2 text-muted-foreground transition hover:bg-destructive/10 hover:text-destructive"><Trash2 size={14} /></button>
            </div>
          ))}
          {fields.length === 0 && <p className="mt-3 text-sm text-muted-foreground">Nenhum campo ainda — adicione a partir da biblioteca de campos.</p>}
          <Button primary className="mt-3">Botão Enviar</Button>
        </div>
      </div>
    </Card>
  );
}

function FormPropertiesPanel({ field, onChange }: { field: FormField | undefined; onChange: (patch: Partial<FormField>) => void }) {
  if (!field) {
    return (
      <Card className="h-full">
        <h2 className="mb-3 text-lg font-semibold">Propriedades</h2>
        <p className="text-sm text-muted-foreground">Selecione um campo no canvas para editar suas propriedades.</p>
      </Card>
    );
  }
  const acceptedFormats = field.acceptedFormats ?? [];
  const toggleFormat = (fmt: string) => {
    const next = acceptedFormats.includes(fmt) ? acceptedFormats.filter((f) => f !== fmt) : [...acceptedFormats, fmt];
    onChange({ acceptedFormats: next });
  };
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
        {field.type === "Upload" && (
          <div>
            <p className="mb-1 text-sm font-medium">Formatos aceitos</p>
            <div className="flex flex-wrap gap-1.5">
              {UPLOAD_FORMAT_OPTIONS.map((fmt) => (
                <button key={fmt} onClick={() => toggleFormat(fmt)} className={`rounded-full border px-3 py-1 text-xs transition ${acceptedFormats.includes(fmt) ? "border-primary bg-primary text-primary-foreground" : "border-border bg-card text-muted-foreground"}`}>{fmt}</button>
              ))}
            </div>
            <p className="mt-1 text-xs text-muted-foreground">{acceptedFormats.length === 0 ? "Nenhum selecionado = aceita qualquer formato." : `Aceita: ${acceptedFormats.join(", ")}.`}</p>
          </div>
        )}
      </div>
    </Card>
  );
}

export function FormBuilder() {
  const navigate = useNavigate();
  const { slug: routeFormId } = useParams<{ slug: string }>();
  const { product } = useCurrentProduct();
  const productId = product?.id ?? "";
  const [creating, setCreating] = useState(false);

  /**
   * E.5.1 (BUG-SPRINT consolidado) — "/forms/new" (sem `:slug`) nunca chamava
   * `formsService.createForm()`; o builder só editava campos localmente e o
   * formulário nunca existia de fato. Ao montar sem `routeFormId`, cria o
   * formulário no service e navega para `/forms/{id}` (mesma rota de edição),
   * substituindo a entrada de histórico para o botão "Voltar" não reabrir
   * "/forms/new".
   */
  useEffect(() => {
    if (routeFormId || !productId || creating) return;
    setCreating(true);
    formsService.createForm(productId, { name: "Novo formulário", type: "Contato" })
      .then((created) => navigate(`/forms/${created.id}`, { replace: true }))
      .catch((err: unknown) => {
        toast.error("Não foi possível criar o formulário", {
          description: (err as { message?: string }).message ?? "Tente novamente.",
        });
        navigate("/forms/list");
      });
  }, [routeFormId, productId, creating, navigate]);

  const formId = routeFormId;
  const { data: form } = useAsyncData(() => (formId && productId ? formsService.getForm(productId, formId) : Promise.resolve(undefined)), [productId, formId]);
  const { data: loadedFields } = useAsyncData(() => (formId && productId ? formsService.getFormFields(productId, formId) : Promise.resolve([])), [productId, formId]);

  const [fields, setFields] = useState<FormField[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [pendingRemoveId, setPendingRemoveId] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [publishing, setPublishing] = useState(false);

  useEffect(() => {
    setFields(loadedFields ?? []);
    setSelectedId(loadedFields?.[0]?.id ?? null);
  }, [loadedFields]);

  if (!formId) return <SkeletonLines />;

  const selectedField = fields.find((f) => f.id === selectedId);
  const pendingRemoveField = fields.find((f) => f.id === pendingRemoveId);

  const handleAddField = (type: string) => {
    const created = formsService.makeField(type);
    setFields((prev) => [...prev, created]);
    setSelectedId(created.id);
    toast.success(`Campo "${type}" adicionado ao canvas.`);
  };

  const handlePatchField = (patch: Partial<FormField>) => {
    setFields((prev) => prev.map((f) => (f.id === selectedId ? { ...f, ...patch } : f)));
  };

  const handleConfirmRemove = () => {
    if (!pendingRemoveId) return;
    setFields((prev) => prev.filter((f) => f.id !== pendingRemoveId));
    if (selectedId === pendingRemoveId) setSelectedId(null);
    setPendingRemoveId(null);
  };

  const handleSaveDraft = async () => {
    setSaving(true);
    try {
      if (formId && productId) await formsService.saveFormFields(productId, formId, fields);
      await formsService.saveDraft(productId, formId);
      toast.success("Rascunho salvo!");
    } catch (err: unknown) {
      toast.error("Falha ao salvar rascunho", {
        description: (err as { message?: string }).message ?? "Tente novamente.",
      });
    } finally {
      setSaving(false);
    }
  };

  const handlePublish = async () => {
    setPublishing(true);
    try {
      await formsService.publish(productId, formId);
      toast.success("Formulário publicado!");
      navigate("/forms/list");
    } catch (err: unknown) {
      toast.error("Falha ao publicar formulário", {
        description: (err as { message?: string }).message ?? "Tente novamente.",
      });
    } finally {
      setPublishing(false);
    }
  };

  return (
    <>
      <AnimatePresence>
        {pendingRemoveField && (
          <ConfirmDialog
            title={`Remover campo "${pendingRemoveField.label}"?`}
            desc="O campo e suas propriedades saem imediatamente do formulário."
            danger
            onCancel={() => setPendingRemoveId(null)}
            onConfirm={handleConfirmRemove}
          />
        )}
      </AnimatePresence>
      <PageHeader title="Form Builder" module="Forms" desc="Construa o fluxo de captura como ativo operacional do produto." badge="Builder">
        <Button onClick={() => navigate("/forms/list")}>Voltar à lista</Button>
        <Button onClick={handleSaveDraft} disabled={saving}>{saving && <Loader2 size={15} className="animate-spin" />}{saving ? "Salvando..." : "Salvar rascunho"}</Button>
        <Button onClick={() => navigate("/forms/preview")}>Preview</Button>
        <Button primary onClick={handlePublish} disabled={publishing}>{publishing && <Loader2 size={15} className="animate-spin" />}{publishing ? "Publicando..." : "Publicar"}</Button>
      </PageHeader>
      <div className="mb-4 xl:hidden"><div className="flex gap-2 overflow-auto pb-1">{["1 Campos", "2 Canvas", "3 Propriedades", "4 Preview"].map((x) => <Badge key={x} tone="blue">{x}</Badge>)}</div></div>
      <div className="grid gap-4 xl:grid-cols-[280px_1fr_340px]">
        <FieldPalette onAddField={handleAddField} />
        <FormBuilderCanvas formName={form?.name ?? "Novo formulário"} fields={fields} selectedId={selectedId} onSelect={setSelectedId} onRequestRemove={setPendingRemoveId} />
        <FormPropertiesPanel field={selectedField} onChange={handlePatchField} />
      </div>
    </>
  );
}
