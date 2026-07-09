import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router";
import { AnimatePresence } from "motion/react";
import { Loader2, Plus, Trash2 } from "lucide-react";
import { Badge, Button, Card, Field, PageHeader, SelectLike, SkeletonLines, PartialErrorWidget } from "../../../shared/components/Primitives";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "../../../shared/components/ui/dialog";
import { ConfirmDialog } from "../../../shared/components/ConfirmDialog";
import { formsService } from "../services/formsService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { toast } from "../../../core/notifications/toast";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";
import type { ApiError } from "../../../shared/services/apiClient";
import type { FormField } from "../contracts/responses";

const UPLOAD_FORMAT_OPTIONS = ["PDF", "Imagem", "DOCX", "ZIP"];
const UPLOAD_FORMAT_MIME_TYPES: Record<string, string[]> = {
  PDF: ["application/pdf"],
  Imagem: ["image/jpeg", "image/png", "image/webp", "image/gif"],
  DOCX: ["application/vnd.openxmlformats-officedocument.wordprocessingml.document"],
  ZIP: ["application/zip", "application/x-zip-compressed"],
};
const FORM_TYPE_OPTIONS = ["Contato", "Orçamento", "RSVP", "Pesquisa", "Newsletter", "Cadastro"];
const DEFAULT_FORM_NAME = "Novo formulário";

const PUBLISH_ERROR_MESSAGES: Record<string, string> = {
  FORM_NAME_REQUIRED: "Defina um nome para o formulário antes de publicar.",
  FORM_HAS_NO_FIELDS: "Adicione ao menos um campo antes de publicar.",
  FORM_REQUIRES_REQUIRED_FIELD: "Marque ao menos um campo como obrigatório antes de publicar.",
  FORM_DUPLICATE_FIELD_LABEL: "Há campos com o mesmo rótulo — renomeie-os antes de publicar.",
  FORM_UPLOAD_ACCEPTED_FILE_TYPES_REQUIRED: "Todo campo de upload precisa de ao menos um formato aceito.",
};

/** H.2.3 (BUG-SPRINT-05) — coleta nome (obrigatório) e tipo antes de criar o formulário; nunca mais nasce silenciosamente como "Novo formulário". */
function CreateFormDialog({ open, creating, onConfirm, onCancel }: {
  open: boolean; creating: boolean; onConfirm: (name: string, type: string) => void; onCancel: () => void;
}) {
  const [name, setName] = useState("");
  const [type, setType] = useState(FORM_TYPE_OPTIONS[0]);
  const nameErr = !name.trim() ? "Nome é obrigatório" : undefined;
  const [touched, setTouched] = useState(false);

  return (
    <Dialog open={open} onOpenChange={(next) => { if (!next) onCancel(); }}>
      <DialogContent>
        <DialogHeader><DialogTitle>Novo formulário</DialogTitle></DialogHeader>
        <div className="space-y-3">
          <Field label="Nome do formulário" value={name} onChange={setName} onBlur={() => setTouched(true)} error={touched ? nameErr : undefined} />
          <SelectLike label="Tipo" value={type} options={FORM_TYPE_OPTIONS} onChange={setType} />
        </div>
        <DialogFooter>
          <Button onClick={onCancel}>Cancelar</Button>
          <Button primary disabled={!!nameErr || creating} onClick={() => onConfirm(name.trim(), type)}>
            {creating && <Loader2 size={15} className="animate-spin" />}{creating ? "Criando..." : "Criar formulário"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
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

function FormBuilderCanvas({ formName, onFormNameChange, fields, selectedId, onSelect, onRequestRemove }: {
  formName: string; onFormNameChange: (v: string) => void; fields: FormField[]; selectedId: string | null; onSelect: (id: string) => void; onRequestRemove: (id: string) => void;
}) {
  return (
    <Card>
      <div className="mb-4 flex items-center justify-between">
        <div><h2 className="text-lg font-semibold">Canvas visual</h2><p className="text-sm text-muted-foreground">Grid, ordem, arraste visual e obrigatoriedade.</p></div>
        <Badge tone={formName.trim() && formName !== DEFAULT_FORM_NAME ? "blue" : "amber"}>{formName || "sem nome"}</Badge>
      </div>
      <div className="rounded-2xl border border-dashed border-border bg-muted/30 p-4">
        <div className="mx-auto max-w-xl rounded-2xl border border-border bg-card p-5">
          <Field label="Nome do formulário" value={formName} onChange={onFormNameChange} />
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
  const acceptedFormats = field.acceptedFormats ?? formatsFromMimeTypes(field.acceptedFileTypes ?? []);
  const toggleFormat = (fmt: string) => {
    const next = acceptedFormats.includes(fmt) ? acceptedFormats.filter((f) => f !== fmt) : [...acceptedFormats, fmt];
    onChange({ acceptedFormats: next, acceptedFileTypes: mimeTypesFromFormats(next) });
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

function mimeTypesFromFormats(formats: string[]): string[] {
  return Array.from(new Set(formats.flatMap((format) => UPLOAD_FORMAT_MIME_TYPES[format] ?? [])));
}

function formatsFromMimeTypes(mimeTypes: string[]): string[] {
  return UPLOAD_FORMAT_OPTIONS.filter((format) => (UPLOAD_FORMAT_MIME_TYPES[format] ?? []).some((mimeType) => mimeTypes.includes(mimeType)));
}

export function FormBuilder() {
  const navigate = useNavigate();
  const { slug: routeFormId } = useParams<{ slug: string }>();
  const { product } = useCurrentProduct();
  const productId = product?.id ?? "";
  const [creating, setCreating] = useState(false);

  /**
   * H.2.3 (BUG-SPRINT-05) — "/forms/new" (sem `:slug`) antes criava o
   * formulário silenciosamente com o nome fixo "Novo formulário" e nenhuma UI
   * para renomeá-lo depois. Agora coleta nome (obrigatório) e tipo num modal
   * antes de chamar `createForm`; só então navega para `/forms/{id}`
   * (mesma rota de edição), substituindo a entrada de histórico para o botão
   * "Voltar" não reabrir "/forms/new".
   */
  const handleCreateConfirm = (name: string, type: string) => {
    if (!productId) return;
    setCreating(true);
    formsService.createForm(productId, { name, type })
      .then((created) => navigate(`/forms/${created.id}`, { replace: true }))
      .catch((err: unknown) => {
        toast.error("Não foi possível criar o formulário", {
          description: (err as { message?: string }).message ?? "Tente novamente.",
        });
        navigate("/forms/list");
      })
      .finally(() => setCreating(false));
  };

  const formId = routeFormId;
  const { data: form } = useAsyncData(() => (formId && productId ? formsService.getForm(productId, formId) : Promise.resolve(undefined)), [productId, formId]);
  const { data: loadedFields } = useAsyncData(() => (formId && productId ? formsService.getFormFields(productId, formId) : Promise.resolve([])), [productId, formId]);

  const [fields, setFields] = useState<FormField[]>([]);
  const [formName, setFormName] = useState(DEFAULT_FORM_NAME);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [pendingRemoveId, setPendingRemoveId] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [publishing, setPublishing] = useState(false);

  useEffect(() => {
    setFields(loadedFields ?? []);
    setSelectedId(loadedFields?.[0]?.id ?? null);
  }, [loadedFields]);

  useEffect(() => {
    setFormName(form?.name ?? DEFAULT_FORM_NAME);
  }, [form?.name]);

  if (!formId) {
    return (
      <CreateFormDialog
        open
        creating={creating}
        onConfirm={handleCreateConfirm}
        onCancel={() => navigate("/forms/list")}
      />
    );
  }

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
      if (formId && productId) await formsService.saveFormFields(productId, formId, fields, formName);
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

  /** H.3.1 — valida nome e campos no frontend antes de sequer chamar o backend; H.3.2 — mapeia os códigos de erro do backend (defesa em profundidade) para mensagens humanizadas. */
  const handlePublish = async () => {
    if (!formName.trim() || formName === DEFAULT_FORM_NAME) {
      toast.error("Defina um nome para o formulário antes de publicar.");
      return;
    }
    if (fields.length === 0) {
      toast.error("Adicione ao menos um campo antes de publicar.");
      return;
    }
    setPublishing(true);
    try {
      await formsService.saveFormFields(productId, formId, fields, formName);
      await formsService.publish(productId, formId);
      toast.success("Formulário publicado!");
      navigate("/forms/list");
    } catch (err: unknown) {
      const code = (err as ApiError).code;
      toast.error("Falha ao publicar formulário", {
        description: (code && PUBLISH_ERROR_MESSAGES[code]) ?? (err as { message?: string }).message ?? "Tente novamente.",
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
        <Button onClick={() => navigate(`/forms/${formId}/preview`)}>Preview</Button>
        <Button primary onClick={handlePublish} disabled={publishing}>{publishing && <Loader2 size={15} className="animate-spin" />}{publishing ? "Publicando..." : "Publicar"}</Button>
      </PageHeader>
      <div className="mb-4 xl:hidden"><div className="flex gap-2 overflow-auto pb-1">{["1 Campos", "2 Canvas", "3 Propriedades", "4 Preview"].map((x) => <Badge key={x} tone="blue">{x}</Badge>)}</div></div>
      <div className="grid gap-4 xl:grid-cols-[280px_1fr_340px]">
        <FieldPalette onAddField={handleAddField} />
        <FormBuilderCanvas formName={formName} onFormNameChange={setFormName} fields={fields} selectedId={selectedId} onSelect={setSelectedId} onRequestRemove={setPendingRemoveId} />
        <FormPropertiesPanel field={selectedField} onChange={handlePatchField} />
      </div>
    </>
  );
}
