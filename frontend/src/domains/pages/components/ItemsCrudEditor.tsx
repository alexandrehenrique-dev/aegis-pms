import { useState } from "react";
import { ArrowDown, ArrowUp, Plus, Trash2 } from "lucide-react";
import { Button, Field } from "../../../shared/components/Primitives";
import { ConfirmDialog } from "../../../shared/components/ConfirmDialog";
import type { ItemCrudRule } from "../itemsCrudConfig";

function hasContent(item: Record<string, unknown>): boolean {
  return Object.values(item).some((v) => typeof v === "string" && v.trim() !== "");
}

function isMissing(item: Record<string, unknown>, field: string): boolean {
  const v = item[field];
  return typeof v !== "string" || v.trim() === "";
}

/**
 * CRUD genérico de itens em blocos com lista (Sprint 12, Tarefa D) — usado
 * por `gallery`, `card-list`, `feature-grid`, `timeline`, `faq` e links de
 * `navbar`/`footer`. Diferente do antigo `ArrayFieldEditor`, que só editava
 * campos de itens já existentes, este adiciona, remove (com confirmação se
 * o item tiver conteúdo) e reordena, além de aplicar `rules` por bloco.
 */
export function ItemsCrudEditor({ items, onChange, newItem, rules }: {
  items: Record<string, unknown>[];
  onChange: (items: Record<string, unknown>[]) => void;
  newItem: Record<string, unknown>;
  rules?: ItemCrudRule;
}) {
  const [pendingRemoveIndex, setPendingRemoveIndex] = useState<number | null>(null);

  const updateItem = (i: number, patch: Record<string, unknown>) => onChange(items.map((it, j) => (j === i ? { ...it, ...patch } : it)));
  const moveItem = (i: number, dir: -1 | 1) => {
    const j = i + dir;
    if (j < 0 || j >= items.length) return;
    const next = [...items];
    [next[i], next[j]] = [next[j], next[i]];
    onChange(next);
  };
  const addItem = () => onChange([...items, { ...newItem }]);
  const requestRemove = (i: number) => {
    if (hasContent(items[i])) setPendingRemoveIndex(i);
    else onChange(items.filter((_, j) => j !== i));
  };
  const confirmRemove = () => {
    if (pendingRemoveIndex === null) return;
    onChange(items.filter((_, j) => j !== pendingRemoveIndex));
    setPendingRemoveIndex(null);
  };

  const atMax = rules?.maxItems !== undefined && items.length >= rules.maxItems;

  return (
    <div className="space-y-2">
      {pendingRemoveIndex !== null && (
        <ConfirmDialog
          title="Remover item?"
          desc="Este item tem conteúdo preenchido — removê-lo descarta esse conteúdo imediatamente."
          danger
          onCancel={() => setPendingRemoveIndex(null)}
          onConfirm={confirmRemove}
        />
      )}
      {items.map((item, i) => (
        <div key={i} className="rounded-lg border border-border p-3">
          <div className="mb-2 flex items-center justify-end gap-1">
            <button onClick={() => moveItem(i, -1)} disabled={i === 0} aria-label="Mover para cima" className="rounded-md p-1 text-muted-foreground hover:bg-muted disabled:opacity-30"><ArrowUp size={13} /></button>
            <button onClick={() => moveItem(i, 1)} disabled={i === items.length - 1} aria-label="Mover para baixo" className="rounded-md p-1 text-muted-foreground hover:bg-muted disabled:opacity-30"><ArrowDown size={13} /></button>
            <button onClick={() => requestRemove(i)} aria-label="Remover item" className="rounded-md p-1 text-muted-foreground hover:bg-destructive/10 hover:text-destructive"><Trash2 size={13} /></button>
          </div>
          <div className="grid gap-2 md:grid-cols-2">
            {Object.entries(item).filter(([, v]) => typeof v === "string").map(([k, v]) => {
              const required = rules?.requiredFields?.includes(k);
              const missing = required && isMissing(item, k);
              return (
                <div key={k}>
                  <Field label={required ? `${k} *` : k} value={v as string} onChange={(nv) => updateItem(i, { [k]: nv })} textarea={k === "desc" || k === "a" || k === "body"} />
                  {missing && <p className="mt-1 text-xs text-destructive">Campo obrigatório.</p>}
                </div>
              );
            })}
          </div>
        </div>
      ))}
      <div>
        <Button onClick={addItem} disabled={atMax}><Plus size={14} />Adicionar item</Button>
        {atMax && <span className="ml-2 text-xs text-muted-foreground">Limite de {rules?.maxItems} itens atingido.</span>}
      </div>
    </div>
  );
}
