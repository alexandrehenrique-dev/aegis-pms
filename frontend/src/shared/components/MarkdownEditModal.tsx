import { useRef, useState } from "react";
import { Bold, Code, Heading1, Heading2, Heading3, Heading4, Italic, Link2, List, Network, Palette, Quote, X } from "lucide-react";
import { Button } from "./Primitives";
import { ModalShell } from "./ModalShell";
import { Markdown } from "./Markdown";
import { Popover, PopoverContent, PopoverTrigger } from "./ui/popover";
import { EntityPicker } from "../../domains/knowledge/components/EntityPicker";
import type { KGNode } from "../../domains/knowledge/mocks/knowledge.mocks";

type ToolbarAction = "bold" | "italic" | "list" | "link" | "h1" | "h2" | "h3" | "h4" | "code" | "quote";

/**
 * Paleta fechada de cor de texto (Sprint 18, Tarefa B.1) — exatamente 5 cores
 * fixas mais "Padrão" (remover cor), nunca um input de cor livre/hex. Cada
 * `colorClass` é 1:1 com uma das 6 classes definidas em `theme.css`,
 * replicadas na allowlist de sanitização do `Markdown.tsx`.
 */
const TEXT_COLORS = [
  { id: "red", label: "Vermelho", colorClass: "text-aegis-red", swatchClass: "bg-[var(--chart-5)]" },
  { id: "blue", label: "Azul", colorClass: "text-aegis-blue", swatchClass: "bg-[var(--chart-2)]" },
  { id: "green", label: "Verde", colorClass: "text-aegis-green", swatchClass: "bg-[var(--chart-3)]" },
  { id: "amber", label: "Âmbar", colorClass: "text-aegis-amber", swatchClass: "bg-[var(--chart-4)]" },
  { id: "violet", label: "Violeta", colorClass: "text-aegis-violet", swatchClass: "bg-[var(--byop-violet)]" },
] as const;

const COLOR_SPAN_PATTERN = /^<span class="(text-aegis-(?:red|blue|green|amber|violet))">([\s\S]*)<\/span>$/;

/** Envolve (ou desenvolve, se `colorClass` for `null`) o texto selecionado num `<span class="text-aegis-*">` — mesma mecânica de inserção das demais `ToolbarAction`. */
function applyColor(value: string, selectionStart: number, selectionEnd: number, colorClass: string | null): { next: string; cursor: number } {
  const before = value.slice(0, selectionStart);
  const selected = value.slice(selectionStart, selectionEnd);
  const after = value.slice(selectionEnd);
  const alreadyWrapped = selected.match(COLOR_SPAN_PATTERN);
  const inner = alreadyWrapped ? alreadyWrapped[2] : (selected || "texto colorido");
  const replacement = colorClass ? `<span class="${colorClass}">${inner}</span>` : inner;
  return { next: `${before}${replacement}${after}`, cursor: before.length + replacement.length };
}

/** Insere `{{kg-ref:nodeId:Label}}` na posição do cursor (Sprint 16, Tarefa C) — mesma mecânica de inserção das demais `ToolbarAction`, parametrizada pelo nó escolhido em vez de um texto fixo. */
function insertEntityRef(value: string, selectionStart: number, selectionEnd: number, node: KGNode): { next: string; cursor: number } {
  const before = value.slice(0, selectionStart);
  const after = value.slice(selectionEnd);
  const ref = `{{kg-ref:${node.id}:${node.label}}}`;
  return { next: `${before}${ref}${after}`, cursor: before.length + ref.length };
}

/** Aplica um prefixo de bloco (`#`, `>`, etc.) em cada linha selecionada — usado por headings e citação. */
function prefixLines(before: string, selected: string, after: string, prefix: string, placeholder: string): { next: string; cursor: number } {
  const lines = (selected || placeholder).split("\n").map((line) => `${prefix}${line}`).join("\n");
  const next = `${before}${lines}${after}`;
  return { next, cursor: before.length + lines.length };
}

function applyToolbarAction(value: string, selectionStart: number, selectionEnd: number, action: ToolbarAction): { next: string; cursor: number } {
  const before = value.slice(0, selectionStart);
  const selected = value.slice(selectionStart, selectionEnd);
  const after = value.slice(selectionEnd);

  if (action === "bold") {
    const next = `${before}**${selected || "texto em negrito"}**${after}`;
    return { next, cursor: before.length + 2 + (selected || "texto em negrito").length + 2 };
  }
  if (action === "italic") {
    const next = `${before}_${selected || "texto em itálico"}_${after}`;
    return { next, cursor: before.length + 1 + (selected || "texto em itálico").length + 1 };
  }
  if (action === "link") {
    const label = selected || "texto do link";
    const next = `${before}[${label}](https://)${after}`;
    return { next, cursor: before.length + label.length + 3 };
  }
  if (action === "h1") return prefixLines(before, selected, after, "# ", "Título 1");
  if (action === "h2") return prefixLines(before, selected, after, "## ", "Título 2");
  if (action === "h3") return prefixLines(before, selected, after, "### ", "Título 3");
  if (action === "h4") return prefixLines(before, selected, after, "#### ", "Título 4");
  if (action === "quote") return prefixLines(before, selected, after, "> ", "Citação");
  if (action === "code") {
    const code = selected || "código";
    const next = `${before}\`\`\`\n${code}\n\`\`\`${after}`;
    return { next, cursor: before.length + 4 + code.length };
  }
  // list
  const lines = (selected || "item da lista").split("\n").map((line) => `- ${line}`).join("\n");
  const next = `${before}${lines}${after}`;
  return { next, cursor: before.length + lines.length };
}

/**
 * Modal dedicada de edição de markdown (Sprint 13, Tarefa B.3) — substitui a
 * edição inline em `<textarea>` simples para qualquer campo de texto longo.
 * Segue o padrão visual de modal já usado no app (título, áreas separadas,
 * ações primária/secundária distintas no rodapé), o mesmo espírito do modal
 * "Novo Pomodoro" do Aion Logbook citado como referência.
 */
export function MarkdownEditModal({ title = "Editar texto", value, onSave, onClose, enableEntityLink = false, productSlug }: {
  title?: string; value: string; onSave: (next: string) => void; onClose: () => void;
  /** Sprint 16, Tarefa C.4 — só `true` para usos de `MarkdownField` no domínio `content`, com o módulo "Knowledge Graph" habilitado no produto efetivo; demais usos (descrição de evento, FAQ, etc.) ficam no padrão `false`, sem o botão. */
  enableEntityLink?: boolean;
  /** Obrigatório quando `enableEntityLink` é `true` — escopa a busca do `EntityPicker` ao produto do conteúdo em edição (ADR-0016). */
  productSlug?: string;
}) {
  const [draft, setDraft] = useState(value);
  const [showEntityPicker, setShowEntityPicker] = useState(false);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const lastSelection = useRef({ start: 0, end: 0 });

  const runAction = (action: ToolbarAction) => {
    const el = textareaRef.current;
    const start = el?.selectionStart ?? draft.length;
    const end = el?.selectionEnd ?? draft.length;
    const { next, cursor } = applyToolbarAction(draft, start, end, action);
    setDraft(next);
    requestAnimationFrame(() => {
      el?.focus();
      el?.setSelectionRange(cursor, cursor);
    });
  };

  const openEntityPicker = () => {
    const el = textareaRef.current;
    lastSelection.current = { start: el?.selectionStart ?? draft.length, end: el?.selectionEnd ?? draft.length };
    setShowEntityPicker(true);
  };

  const captureSelectionForColor = () => {
    const el = textareaRef.current;
    lastSelection.current = { start: el?.selectionStart ?? draft.length, end: el?.selectionEnd ?? draft.length };
  };

  const handlePickColor = (colorClass: string | null) => {
    const { start, end } = lastSelection.current;
    const { next, cursor } = applyColor(draft, start, end, colorClass);
    setDraft(next);
    requestAnimationFrame(() => {
      textareaRef.current?.focus();
      textareaRef.current?.setSelectionRange(cursor, cursor);
    });
  };

  const handleSelectEntity = (node: KGNode) => {
    const { start, end } = lastSelection.current;
    const { next, cursor } = insertEntityRef(draft, start, end, node);
    setDraft(next);
    setShowEntityPicker(false);
    requestAnimationFrame(() => {
      textareaRef.current?.focus();
      textareaRef.current?.setSelectionRange(cursor, cursor);
    });
  };

  return (
    <ModalShell onClose={onClose} maxWidthClassName="max-w-3xl">
      <div className="flex max-h-[85vh] flex-col">
        <div className="flex items-center justify-between">
          <h3 className="font-semibold">{title}</h3>
          <button onClick={onClose} aria-label="Fechar" className="rounded-lg p-1.5 text-muted-foreground transition hover:bg-muted"><X size={16} /></button>
        </div>

        <div className="mt-4 flex flex-wrap gap-1 border-b border-border pb-3">
          <button onClick={() => runAction("h1")} title="Título 1" className="rounded-lg p-2 transition hover:bg-muted"><Heading1 size={15} /></button>
          <button onClick={() => runAction("h2")} title="Título 2" className="rounded-lg p-2 transition hover:bg-muted"><Heading2 size={15} /></button>
          <button onClick={() => runAction("h3")} title="Título 3" className="rounded-lg p-2 transition hover:bg-muted"><Heading3 size={15} /></button>
          <button onClick={() => runAction("h4")} title="Título 4" className="rounded-lg p-2 transition hover:bg-muted"><Heading4 size={15} /></button>
          <span className="mx-1 w-px self-stretch bg-border" />
          <button onClick={() => runAction("bold")} title="Negrito" className="rounded-lg p-2 transition hover:bg-muted"><Bold size={15} /></button>
          <button onClick={() => runAction("italic")} title="Itálico" className="rounded-lg p-2 transition hover:bg-muted"><Italic size={15} /></button>
          <button onClick={() => runAction("list")} title="Lista" className="rounded-lg p-2 transition hover:bg-muted"><List size={15} /></button>
          <button onClick={() => runAction("link")} title="Link" className="rounded-lg p-2 transition hover:bg-muted"><Link2 size={15} /></button>
          <button onClick={() => runAction("quote")} title="Citação" className="rounded-lg p-2 transition hover:bg-muted"><Quote size={15} /></button>
          <button onClick={() => runAction("code")} title="Bloco de código" className="rounded-lg p-2 transition hover:bg-muted"><Code size={15} /></button>
          <span className="mx-1 w-px self-stretch bg-border" />
          <Popover>
            <PopoverTrigger asChild>
              <button onClick={captureSelectionForColor} title="Cor do texto" className="rounded-lg p-2 transition hover:bg-muted"><Palette size={15} /></button>
            </PopoverTrigger>
            <PopoverContent className="w-auto p-2">
              <div className="flex items-center gap-1.5">
                {TEXT_COLORS.map((c) => (
                  <button key={c.id} title={c.label} onClick={() => handlePickColor(c.colorClass)} className={`h-7 w-7 rounded-full border border-border ${c.swatchClass} transition hover:scale-110`} />
                ))}
                <button title="Padrão (remover cor)" onClick={() => handlePickColor(null)} className="flex h-7 w-7 items-center justify-center rounded-full border border-border bg-card text-xs text-muted-foreground transition hover:bg-muted">✕</button>
              </div>
            </PopoverContent>
          </Popover>
          {enableEntityLink && (
            <>
              <span className="mx-1 w-px self-stretch bg-border" />
              <button onClick={openEntityPicker} title="Vincular a outro conteúdo" className="rounded-lg p-2 transition hover:bg-muted"><Network size={15} /></button>
            </>
          )}
        </div>

        <div className="mt-3 grid flex-1 gap-3 overflow-auto md:grid-cols-2">
          <label className="flex flex-col">
            <span className="mb-1 text-xs font-medium uppercase tracking-wide text-muted-foreground">Markdown</span>
            <textarea ref={textareaRef} value={draft} onChange={(e) => setDraft(e.target.value)} className="min-h-56 flex-1 rounded-lg border border-border bg-card p-3 text-sm outline-primary" />
          </label>
          <div className="flex flex-col">
            <span className="mb-1 text-xs font-medium uppercase tracking-wide text-muted-foreground">Pré-visualização</span>
            <div className="min-h-56 flex-1 rounded-lg border border-border bg-muted p-3">
              {draft ? <Markdown>{draft}</Markdown> : <p className="text-sm text-muted-foreground">Nada para mostrar ainda.</p>}
            </div>
          </div>
        </div>

        <div className="mt-5 flex justify-end gap-2 border-t border-border pt-4">
          <Button onClick={onClose}>Cancelar</Button>
          <Button primary onClick={() => onSave(draft)}>Salvar texto</Button>
        </div>
      </div>

      {showEntityPicker && productSlug && (
        <ModalShell onClose={() => setShowEntityPicker(false)} maxWidthClassName="max-w-sm">
          <div className="flex items-center justify-between">
            <h3 className="font-semibold">Vincular a outro conteúdo</h3>
            <button onClick={() => setShowEntityPicker(false)} aria-label="Fechar" className="rounded-lg p-1.5 text-muted-foreground transition hover:bg-muted"><X size={16} /></button>
          </div>
          <div className="mt-3">
            <EntityPicker productSlug={productSlug} onSelect={handleSelectEntity} />
          </div>
        </ModalShell>
      )}
    </ModalShell>
  );
}
