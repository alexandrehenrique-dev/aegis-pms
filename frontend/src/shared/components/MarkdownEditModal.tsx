import { useRef, useState } from "react";
import { motion } from "motion/react";
import { Bold, Italic, Link2, List, X } from "lucide-react";
import { Button, fade } from "./Primitives";
import { Markdown } from "./Markdown";

type ToolbarAction = "bold" | "italic" | "list" | "link";

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
export function MarkdownEditModal({ title = "Editar texto", value, onSave, onClose }: { title?: string; value: string; onSave: (next: string) => void; onClose: () => void }) {
  const [draft, setDraft] = useState(value);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

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

  return (
    <motion.div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30 backdrop-blur-[3px] p-4" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={onClose}>
      <motion.div {...fade} className="flex max-h-[85vh] w-full max-w-3xl flex-col rounded-2xl border border-border bg-card p-6 shadow-[0_24px_80px_rgba(0,0,0,0.2)]" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between">
          <h3 className="font-semibold">{title}</h3>
          <button onClick={onClose} aria-label="Fechar" className="rounded-lg p-1.5 text-muted-foreground transition hover:bg-muted"><X size={16} /></button>
        </div>

        <div className="mt-4 flex gap-1 border-b border-border pb-3">
          <button onClick={() => runAction("bold")} title="Negrito" className="rounded-lg p-2 transition hover:bg-muted"><Bold size={15} /></button>
          <button onClick={() => runAction("italic")} title="Itálico" className="rounded-lg p-2 transition hover:bg-muted"><Italic size={15} /></button>
          <button onClick={() => runAction("list")} title="Lista" className="rounded-lg p-2 transition hover:bg-muted"><List size={15} /></button>
          <button onClick={() => runAction("link")} title="Link" className="rounded-lg p-2 transition hover:bg-muted"><Link2 size={15} /></button>
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
      </motion.div>
    </motion.div>
  );
}
