import { useEffect, useState } from "react";
import { AlertTriangle, Calendar, GripVertical, Link2, Loader2, Plus, Send, Trash2 } from "lucide-react";
import { useDragReorder } from "../../../shared/hooks/useDragReorder";
import { Badge, Button, Card, Field, SelectLike } from "../../../shared/components/Primitives";
import { PermissionHint } from "../../../shared/components/Primitives";
import { MarkdownField } from "../../../shared/components/MarkdownField";
import { ImageFieldEditor, MediaField } from "../../../shared/components/MediaField";
import { Popover, PopoverContent, PopoverTrigger } from "../../../shared/components/ui/popover";
import { toast } from "../../../core/notifications/toast";
import { contentService } from "../services/contentService";
import { ContentStatusBadge } from "./ContentStatusBadge";
import { VersionTimeline } from "./VersionTimeline";
import type { BlockType, Page, Section } from "../../pages/contracts/responses";
import { EntityPicker } from "../../knowledge/components/EntityPicker";
import { knowledgeService } from "../../knowledge/services/knowledgeService";
import { TwoColumnEditor } from "../../pages/components/TwoColumnEditor";
import { AudioBlockEditor } from "../../pages/components/AudioBlockEditor";
import { ItemsCrudEditor } from "../../pages/components/ItemsCrudEditor";
import { ITEMS_CRUD_CONFIG } from "../../pages/itemsCrudConfig";
import { EventsManagerDrawer } from "../../pages/components/EventsManagerDrawer";
import { EventSelector } from "../../pages/components/EventSelector";
import { FormIdSelector } from "../../pages/components/FormIdSelector";
import { pagesService } from "../../pages/services/pagesService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import type { KGNode } from "../../knowledge/mocks/knowledge.mocks";

const SECTION_DRAG_TYPE = "page-section";

function SectionRow({ section, index, isSelected, isLast, totalCount, onSelect, onRequestDelete, onHoverReorder, onDrop }: {
  section: Section; index: number; isSelected: boolean; isLast: boolean; totalCount: number;
  onSelect: () => void; onRequestDelete: () => void;
  onHoverReorder: (from: number, to: number) => void; onDrop: () => void;
}) {
  const { ref, isDragging, isOver } = useDragReorder({ dragType: SECTION_DRAG_TYPE, index, onHoverReorder, onDrop });

  return (
    <div
      ref={ref}
      style={{ opacity: isDragging ? 0.4 : 1 }}
      className={`mb-1 flex w-full items-center gap-1 rounded-lg p-1 transition ${isSelected ? "bg-muted" : "hover:bg-muted"} ${isOver ? "ring-2 ring-primary/30" : ""}`}
    >
      <GripVertical size={14} className="shrink-0 cursor-grab text-muted-foreground/50 active:cursor-grabbing" />
      <button onClick={onSelect} className="flex flex-1 items-center justify-between p-1 text-left text-sm">
        <span>{section.label}</span>
        <span className="flex gap-1"><Badge>{section.type}</Badge>{isLast && totalCount > 4 && <AlertTriangle size={14} className="text-[#8A5A12]" />}</span>
      </button>
      <button onClick={onRequestDelete} aria-label={`Remover ${section.label}`} className="rounded-lg p-1.5 text-muted-foreground transition hover:bg-destructive/10 hover:text-destructive"><Trash2 size={14} /></button>
    </div>
  );
}

export function ContentStructureTree({ page, selectedId, onSelect, onAddBlock, onRequestDelete, onReorder }: {
  page: Page | null; selectedId: string | null; onSelect: (id: string) => void; onAddBlock: (type: BlockType) => void; onRequestDelete: (id: string) => void;
  onReorder: (sectionIds: string[]) => void;
}) {
  const [newType, setNewType] = useState<BlockType>("text");
  const [orderedSections, setOrderedSections] = useState<Section[]>(page?.sections ?? []);
  const { data: blockTypes } = useAsyncData(() => pagesService.listBlockTypes(), []);

  useEffect(() => setOrderedSections(page?.sections ?? []), [page]);

  const handleHoverReorder = (from: number, to: number) => {
    setOrderedSections((prev) => {
      const next = [...prev];
      const [moved] = next.splice(from, 1);
      next.splice(to, 0, moved);
      return next;
    });
  };

  return (
    <Card className="h-full">
      <h2 className="mb-3 text-lg font-semibold">Estrutura</h2>
      <p className="mb-2 text-xs text-muted-foreground">Arraste pelo ícone para reordenar.</p>
      {!page && <p className="text-sm text-muted-foreground">Nenhuma página carregada para este produto.</p>}
      {orderedSections.map((s, i) => (
        <SectionRow
          key={s.id}
          section={s}
          index={i}
          isSelected={selectedId === s.id}
          isLast={i === orderedSections.length - 1}
          totalCount={orderedSections.length}
          onSelect={() => onSelect(s.id)}
          onRequestDelete={() => onRequestDelete(s.id)}
          onHoverReorder={handleHoverReorder}
          onDrop={() => onReorder(orderedSections.map((sec) => sec.id))}
        />
      ))}
      {page && (
        <div className="mt-3 flex items-center gap-2">
          <SelectLike label="" value={newType} options={[...(blockTypes ?? [])]} onChange={(v) => setNewType(v as BlockType)} />
          <Button onClick={() => onAddBlock(newType)}><Plus size={15} />Adicionar bloco</Button>
        </div>
      )}
    </Card>
  );
}

function isPlainObject(v: unknown): v is Record<string, unknown> {
  return !!v && typeof v === "object" && !Array.isArray(v);
}

function isArrayOfObjects(v: unknown): v is Record<string, unknown>[] {
  return Array.isArray(v) && v.length > 0 && v.every((item) => isPlainObject(item));
}

/** Campos de texto longo (Sprint 13, Tarefa B.2) editados via `MarkdownField`, não `<textarea>` simples. */
const MARKDOWN_FIELD_KEYS = new Set(["body", "desc", "description", "a", "answer"]);

/** Aplica um valor num caminho aninhado e devolve a árvore `content` completa atualizada (merge raso no nível 1 continua correto). */
function setNestedField(obj: Record<string, unknown>, path: string[], value: string): Record<string, unknown> {
  const [head, ...rest] = path;
  if (rest.length === 0) return { ...obj, [head]: value };
  const child = isPlainObject(obj[head]) ? (obj[head] as Record<string, unknown>) : {};
  return { ...obj, [head]: setNestedField(child, rest, value) };
}

function ObjectFieldsEditor({ obj, path, onPatch }: { obj: Record<string, unknown>; path: string[]; onPatch: (path: string[], value: string) => void }) {
  return (
    <>
      {Object.entries(obj).map(([k, v]) => {
        const fullPath = [...path, k];
        if (typeof v === "string") {
          if (MARKDOWN_FIELD_KEYS.has(k)) {
            return <MarkdownField key={fullPath.join(".")} label={fullPath.join(" › ")} value={v} onChange={(nv) => onPatch(fullPath, nv)} />;
          }
          return (
            <Field key={fullPath.join(".")} label={fullPath.join(" › ")} value={v} onChange={(nv) => onPatch(fullPath, nv)} />
          );
        }
        if (isPlainObject(v)) {
          if (typeof v.src === "string") {
            return (
              <div key={fullPath.join(".")} className="rounded-lg border border-border p-3 md:col-span-2">
                <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">{k}</p>
                <ImageFieldEditor
                  src={v.src}
                  alt={typeof v.alt === "string" ? v.alt : ""}
                  onChangeSrc={(nv) => onPatch([...fullPath, "src"], nv)}
                  onChangeAlt={(nv) => onPatch([...fullPath, "alt"], nv)}
                />
              </div>
            );
          }
          return (
            <div key={fullPath.join(".")} className="rounded-lg border border-border p-3 md:col-span-2">
              <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">{k}</p>
              <div className="grid gap-3 md:grid-cols-2">
                <ObjectFieldsEditor obj={v} path={fullPath} onPatch={onPatch} />
              </div>
            </div>
          );
        }
        return null;
      })}
    </>
  );
}

function ArrayFieldEditor({ items, onChange }: { items: Record<string, unknown>[]; onChange: (items: Record<string, unknown>[]) => void }) {
  return (
    <div className="space-y-2">
      {items.map((item, i) => (
        <div key={i} className="grid gap-2 rounded-lg border border-border p-3 md:grid-cols-2">
          {Object.entries(item).filter(([, v]) => typeof v === "string").map(([k, v]) => {
            const onFieldChange = (nv: string) => onChange(items.map((it, j) => (j === i ? { ...it, [k]: nv } : it)));
            if (k === "src") {
              return <MediaField key={k} label={k} value={v as string} typeFilter="imagem" onChange={onFieldChange} />;
            }
            if (k === "fileAssetId") {
              return <MediaField key={k} label={k} value={v as string} typeFilter="qualquer" onChange={onFieldChange} />;
            }
            if (MARKDOWN_FIELD_KEYS.has(k)) {
              return <MarkdownField key={k} label={k} value={v as string} onChange={onFieldChange} />;
            }
            return <Field key={k} label={k} value={v as string} onChange={onFieldChange} />;
          })}
        </div>
      ))}
    </div>
  );
}

export function BlockEditorCanvas({ section, productSlug, onChangeContent, onRequestDelete }: {
  section: Section | null; productSlug: string; onChangeContent: (patch: Record<string, unknown>) => void; onRequestDelete: (id: string) => void;
}) {
  const [showEventsManager, setShowEventsManager] = useState(false);
  const [eventsRefreshKey, setEventsRefreshKey] = useState(0);
  const handleEventsManagerOpenChange = (open: boolean) => {
    setShowEventsManager(open);
    if (!open) setEventsRefreshKey((k) => k + 1);
  };

  if (!section) {
    return (
      <Card>
        <h2 className="text-lg font-semibold">Editor / Canvas</h2>
        <p className="mt-2 text-sm text-muted-foreground">Selecione um bloco na estrutura para editar.</p>
      </Card>
    );
  }

  const isTwoColumn = section.type === "two-column";
  const isAudio = section.type === "audio";
  const isEventList = section.type === "event-list";
  const itemsCrudConfig = ITEMS_CRUD_CONFIG[section.type];
  const hasFormIdSelector = section.type === "contact" || section.type === "form";
  const { content } = section;
  const excludedKeys = new Set<string>([
    ...(isTwoColumn ? ["left", "right"] : []),
    ...(isAudio ? ["source", "fileAssetId", "spotifyUrl", "autoplay"] : []),
    ...(isEventList ? ["selectedEventIds"] : []),
    ...(itemsCrudConfig ? [itemsCrudConfig.key] : []),
    ...(hasFormIdSelector ? ["formId"] : []),
  ]);
  const fieldableContent = Object.fromEntries(Object.entries(content).filter(([k]) => !excludedKeys.has(k)));
  const stringFields = Object.entries(fieldableContent).filter(([, v]) => typeof v === "string") as [string, string][];
  const nestedObjectFields = Object.entries(fieldableContent).filter(([, v]) => isPlainObject(v)) as [string, Record<string, unknown>][];
  const arrayFields = Object.entries(fieldableContent).filter(([, v]) => isArrayOfObjects(v)) as [string, Record<string, unknown>[]][];
  const handledKeys = new Set([...stringFields.map(([k]) => k), ...nestedObjectFields.map(([k]) => k), ...arrayFields.map(([k]) => k), ...excludedKeys]);
  const advancedKeys = Object.keys(content).filter((k) => !handledKeys.has(k));
  const canLinkEntity = section.type === "text" || section.type === "rich-text";

  const handleNestedPatch = (path: string[], value: string) => onChangeContent(setNestedField(content, path, value));

  const handleLinkEntity = async (node: KGNode) => {
    const body = typeof content.body === "string" ? content.body : "";
    onChangeContent({ body: `${body}${body ? " " : ""}{{kg-ref:${node.id}:${node.label}}}` });
    await knowledgeService.ensureNodeForContent(section.id, section.label);
    await knowledgeService.createEdge(section.id, node.id, "RELATED_TO");
    toast.success("Referência linkada", { description: `${node.label} inserido no corpo e relação criada no grafo.` });
  };

  return (
    <Card>
      <div className="flex items-start justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold">Editor / Canvas — {section.label}</h2>
          <p className="mb-4 text-sm text-muted-foreground">Bloco do tipo <b>{section.type}</b>, vindo de `pagesService` — não é mais uma string solta.</p>
        </div>
        <Button onClick={() => onRequestDelete(section.id)}><Trash2 size={14} />Remover bloco</Button>
      </div>
      <div className="grid gap-3 md:grid-cols-2">
        {stringFields.map(([k, v]) => {
          const onFieldChange = (nv: string) => onChangeContent({ [k]: nv });
          if (MARKDOWN_FIELD_KEYS.has(k)) {
            return <MarkdownField key={k} label={k} value={v} onChange={onFieldChange} />;
          }
          return <Field key={k} label={k} value={v} onChange={onFieldChange} />;
        })}
        {nestedObjectFields.map(([k, v]) => (
          <div key={k} className="rounded-lg border border-border p-3 md:col-span-2">
            <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">{k}</p>
            {typeof v.src === "string" ? (
              <ImageFieldEditor
                src={v.src}
                alt={typeof v.alt === "string" ? v.alt : ""}
                onChangeSrc={(nv) => handleNestedPatch([k, "src"], nv)}
                onChangeAlt={(nv) => handleNestedPatch([k, "alt"], nv)}
              />
            ) : (
              <div className="grid gap-3 md:grid-cols-2">
                <ObjectFieldsEditor obj={v} path={[k]} onPatch={handleNestedPatch} />
              </div>
            )}
          </div>
        ))}
        {hasFormIdSelector && (
          <FormIdSelector productSlug={productSlug} value={typeof content.formId === "string" ? content.formId : ""} onChange={(formId) => onChangeContent({ formId })} />
        )}
      </div>
      {isTwoColumn && <TwoColumnEditor content={content} onChange={onChangeContent} />}
      {isAudio && <AudioBlockEditor content={content} onChange={onChangeContent} />}
      {isEventList && (
        <div className="mt-3 space-y-3">
          <EventSelector
            productSlug={productSlug}
            selectedIds={Array.isArray(content.selectedEventIds) ? (content.selectedEventIds as string[]) : []}
            onChange={(selectedEventIds) => onChangeContent({ selectedEventIds })}
            refreshKey={eventsRefreshKey}
          />
          <Button onClick={() => setShowEventsManager(true)}><Calendar size={14} />Gerenciar eventos</Button>
          <EventsManagerDrawer productSlug={productSlug} open={showEventsManager} onOpenChange={handleEventsManagerOpenChange} />
        </div>
      )}
      {canLinkEntity && (
        <div className="mt-3">
          <Popover>
            <PopoverTrigger asChild><Button><Link2 size={15} />Linkar a outra entidade</Button></PopoverTrigger>
            <PopoverContent><EntityPicker onSelect={handleLinkEntity} /></PopoverContent>
          </Popover>
        </div>
      )}
      {itemsCrudConfig && (
        <div className="mt-4">
          <p className="mb-2 text-sm font-medium">{itemsCrudConfig.key} ({(Array.isArray(content[itemsCrudConfig.key]) ? content[itemsCrudConfig.key] as unknown[] : []).length})</p>
          <ItemsCrudEditor
            items={Array.isArray(content[itemsCrudConfig.key]) ? (content[itemsCrudConfig.key] as Record<string, unknown>[]) : []}
            onChange={(next) => onChangeContent({ [itemsCrudConfig.key]: next })}
            newItem={itemsCrudConfig.newItem}
            rules={itemsCrudConfig.rules}
          />
        </div>
      )}
      {arrayFields.map(([k, items]) => (
        <div key={k} className="mt-4">
          <p className="mb-2 text-sm font-medium">{k} ({items.length})</p>
          <ArrayFieldEditor items={items} onChange={(next) => onChangeContent({ [k]: next })} />
        </div>
      ))}
      {advancedKeys.length > 0 && (
        <details className="mt-4 rounded-xl border border-border bg-muted p-3 text-xs">
          <summary className="cursor-pointer font-medium">Dados avançados ({advancedKeys.join(", ")})</summary>
          <pre className="mt-2 overflow-auto">{JSON.stringify(Object.fromEntries(advancedKeys.map((k) => [k, content[k]])), null, 2)}</pre>
        </details>
      )}
    </Card>
  );
}

function SEOPanel({ page }: { page: Page | null }) {
  const [title, setTitle] = useState(page?.seo.title ?? "Maestro Beton | Experiências");
  const [description, setDescription] = useState(page?.seo.description ?? "Conheça experiências e apresentações do Maestro Beton.");
  const [keywords, setKeywords] = useState(page?.seo.keywords ?? "maestro, eventos, apresentações");
  return (
    <div className="space-y-3">
      <Field label="Title" value={title} onChange={setTitle} />
      <Field label="Description" value={description} onChange={setDescription} textarea />
      <Field label="Keywords" value={keywords} onChange={setKeywords} />
      <div className="rounded-lg border border-border p-3 text-sm">
        <b>Preview Google</b>
        <p className="text-[#1F5FA8]">{title}</p>
        <p className="text-muted-foreground">{description}</p>
      </div>
    </div>
  );
}

function WorkflowPanel() {
  const [status, setStatus] = useState<"Draft" | "In Review" | "Published" | "Archived">("Draft");
  const [busy, setBusy] = useState<string | null>(null);

  const run = async (label: string, action: () => Promise<void>, nextStatus: typeof status) => {
    setBusy(label);
    try {
      await action();
      setStatus(nextStatus);
      toast.success(`${label}!`);
    } finally {
      setBusy(null);
    }
  };

  return (
    <div className="space-y-2 text-sm">
      <ContentStatusBadge status={status} />
      <Button onClick={() => run("Enviado para revisão", contentService.submitForReview, "In Review")} disabled={busy !== null}>
        {busy === "Enviado para revisão" ? <Loader2 size={15} className="animate-spin" /> : <Send size={15} />}Enviar para revisão
      </Button>
      <Button primary onClick={() => run("Publicado", contentService.publish, "Published")} disabled={busy !== null}>
        {busy === "Publicado" && <Loader2 size={15} className="animate-spin" />}Publicar
      </Button>
      <Button onClick={() => run("Arquivado", contentService.archive, "Archived")} disabled={busy !== null}>
        {busy === "Arquivado" && <Loader2 size={15} className="animate-spin" />}Arquivar
      </Button>
      <PermissionHint />
    </div>
  );
}

function PageJsonViewer({ page }: { page: Page | null }) {
  const [copied, setCopied] = useState(false);
  if (!page) return <p className="text-sm text-muted-foreground">Nenhuma página carregada.</p>;
  const json = JSON.stringify(page, null, 2);
  const handleCopy = async () => {
    await navigator.clipboard.writeText(json);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };
  return (
    <div>
      <div className="mb-2 flex justify-end"><Button onClick={handleCopy}>{copied ? "Copiado!" : "Copiar JSON"}</Button></div>
      <pre className="max-h-[480px] overflow-auto rounded-lg bg-muted p-3 text-xs">{json}</pre>
    </div>
  );
}

export function PropertiesPanel({ page, section }: { page: Page | null; section: Section | null }) {
  const [tab, setTab] = useState("Propriedades");
  return (
    <Card className="h-full">
      <div className="mb-3 flex gap-1 overflow-auto">
        {["Propriedades", "SEO", "Workflow", "Histórico", "JSON"].map((t) => (
          <button key={t} onClick={() => setTab(t)} className={`rounded-lg px-2 py-1 text-xs ${tab === t ? "bg-primary text-white" : "bg-muted"}`}>{t}</button>
        ))}
      </div>
      {tab === "SEO" ? <SEOPanel page={page} /> : tab === "Workflow" ? <WorkflowPanel /> : tab === "Histórico" ? <VersionTimeline compact /> : tab === "JSON" ? <PageJsonViewer page={page} /> : (
        <div className="space-y-2 text-sm">
          {[
            ["página", page?.slug ?? "—"],
            ["bloco", section?.label ?? "—"],
            ["tipo de bloco", section?.type ?? "—"],
            ["idioma", page?.locale ?? "—"],
            ["status", page?.status ?? "—"],
            ["versão", page ? `v${page.version}` : "—"],
          ].map((x) => (
            <div key={x[0]} className="flex justify-between rounded-lg bg-muted p-2"><span>{x[0]}</span><b>{x[1]}</b></div>
          ))}
        </div>
      )}
    </Card>
  );
}
