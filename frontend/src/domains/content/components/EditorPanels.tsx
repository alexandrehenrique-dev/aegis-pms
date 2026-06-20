import { useState } from "react";
import { AlertTriangle, Link2, Loader2, Plus, Send } from "lucide-react";
import { Badge, Button, Card, Field, SelectLike } from "../../../shared/components/Primitives";
import { PermissionHint } from "../../../shared/components/Primitives";
import { Popover, PopoverContent, PopoverTrigger } from "../../../shared/components/ui/popover";
import { toast } from "../../../core/notifications/toast";
import { contentService } from "../services/contentService";
import { ContentStatusBadge } from "./ContentStatusBadge";
import { VersionTimeline } from "./VersionTimeline";
import { BLOCK_TYPES, type BlockType, type Page, type Section } from "../../pages/contracts/responses";
import { EntityPicker } from "../../knowledge/components/EntityPicker";
import { knowledgeService } from "../../knowledge/services/knowledgeService";
import type { KGNode } from "../../knowledge/mocks/knowledge.mocks";

export function ContentStructureTree({ page, selectedId, onSelect, onAddBlock }: {
  page: Page | null; selectedId: string | null; onSelect: (id: string) => void; onAddBlock: (type: BlockType) => void;
}) {
  const [newType, setNewType] = useState<BlockType>("text");
  const sections = page?.sections ?? [];

  return (
    <Card className="h-full">
      <h2 className="mb-3 text-lg font-semibold">Estrutura</h2>
      {!page && <p className="text-sm text-muted-foreground">Nenhuma página carregada para este produto.</p>}
      {sections.map((s, i) => (
        <button key={s.id} onClick={() => onSelect(s.id)} className={`mb-1 flex w-full items-center justify-between rounded-lg p-2 text-left text-sm ${selectedId === s.id ? "bg-muted" : "hover:bg-muted"}`}>
          <span>{s.label}</span>
          <span className="flex gap-1"><Badge>{s.type}</Badge>{i === sections.length - 1 && sections.length > 4 && <AlertTriangle size={14} className="text-[#8A5A12]" />}</span>
        </button>
      ))}
      {page && (
        <div className="mt-3 flex items-center gap-2">
          <SelectLike label="" value={newType} options={[...BLOCK_TYPES]} onChange={(v) => setNewType(v as BlockType)} />
          <Button onClick={() => onAddBlock(newType)}><Plus size={15} />Adicionar bloco</Button>
        </div>
      )}
    </Card>
  );
}

function ItemsEditor({ items, onChange }: { items: Record<string, unknown>[]; onChange: (items: Record<string, unknown>[]) => void }) {
  return (
    <div className="space-y-2">
      {items.map((item, i) => (
        <div key={i} className="rounded-lg border border-border p-3">
          {typeof item.title === "string" && (
            <Field label="Título" value={item.title} onChange={(v) => onChange(items.map((it, j) => (j === i ? { ...it, title: v } : it)))} />
          )}
          {typeof item.desc === "string" && (
            <Field label="Descrição" value={item.desc} onChange={(v) => onChange(items.map((it, j) => (j === i ? { ...it, desc: v } : it)))} textarea />
          )}
        </div>
      ))}
    </div>
  );
}

export function BlockEditorCanvas({ section, onChangeContent }: {
  section: Section | null; onChangeContent: (patch: Record<string, unknown>) => void;
}) {
  if (!section) {
    return (
      <Card>
        <h2 className="text-lg font-semibold">Editor / Canvas</h2>
        <p className="mt-2 text-sm text-muted-foreground">Selecione um bloco na estrutura para editar.</p>
      </Card>
    );
  }

  const { content } = section;
  const stringFields = Object.entries(content).filter(([, v]) => typeof v === "string") as [string, string][];
  const items = Array.isArray(content.items) ? (content.items as Record<string, unknown>[]) : null;
  const advancedKeys = Object.keys(content).filter((k) => k !== "items" && typeof content[k] !== "string");
  const canLinkEntity = section.type === "text" || section.type === "rich-text";

  const handleLinkEntity = async (node: KGNode) => {
    const body = typeof content.body === "string" ? content.body : "";
    onChangeContent({ body: `${body}${body ? " " : ""}{{kg-ref:${node.id}:${node.label}}}` });
    await knowledgeService.createEdge(section.id, node.id, "relacionado a");
    toast.success("Referência linkada", { description: `${node.label} inserido no corpo e relação criada no grafo.` });
  };

  return (
    <Card>
      <h2 className="text-lg font-semibold">Editor / Canvas — {section.label}</h2>
      <p className="mb-4 text-sm text-muted-foreground">Bloco do tipo <b>{section.type}</b>, vindo de `pagesService` — não é mais uma string solta.</p>
      <div className="grid gap-3 md:grid-cols-2">
        {stringFields.map(([k, v]) => (
          <Field key={k} label={k} value={v} onChange={(nv) => onChangeContent({ [k]: nv })} textarea={k === "body"} />
        ))}
      </div>
      {canLinkEntity && (
        <div className="mt-3">
          <Popover>
            <PopoverTrigger asChild><Button><Link2 size={15} />Linkar a outra entidade</Button></PopoverTrigger>
            <PopoverContent><EntityPicker onSelect={handleLinkEntity} /></PopoverContent>
          </Popover>
        </div>
      )}
      {items && (
        <div className="mt-4">
          <p className="mb-2 text-sm font-medium">Itens ({items.length})</p>
          <ItemsEditor items={items} onChange={(next) => onChangeContent({ items: next })} />
        </div>
      )}
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

export function PropertiesPanel({ page, section }: { page: Page | null; section: Section | null }) {
  const [tab, setTab] = useState("Propriedades");
  return (
    <Card className="h-full">
      <div className="mb-3 flex gap-1 overflow-auto">
        {["Propriedades", "SEO", "Workflow", "Histórico"].map((t) => (
          <button key={t} onClick={() => setTab(t)} className={`rounded-lg px-2 py-1 text-xs ${tab === t ? "bg-primary text-white" : "bg-muted"}`}>{t}</button>
        ))}
      </div>
      {tab === "SEO" ? <SEOPanel page={page} /> : tab === "Workflow" ? <WorkflowPanel /> : tab === "Histórico" ? <VersionTimeline compact /> : (
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
