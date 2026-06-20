import { useState } from "react";
import { ArrowDown, ArrowUp, Plus, Trash2 } from "lucide-react";
import { Button, Field, SelectLike } from "../../../shared/components/Primitives";
import { MINI_BLOCK_DEFAULT_CONTENT } from "../blockDefaults";
import { MINI_BLOCK_TYPES, type MiniBlock, type MiniBlockType } from "../contracts/responses";

function MiniBlockFieldsEditor({ block, onChange }: { block: MiniBlock; onChange: (content: Record<string, unknown>) => void }) {
  return (
    <div className="space-y-2">
      {Object.entries(block.content).filter(([, v]) => typeof v === "string").map(([k, v]) => (
        <Field key={k} label={k} value={v as string} onChange={(nv) => onChange({ ...block.content, [k]: nv })} textarea={k === "body"} />
      ))}
    </div>
  );
}

function ColumnEditor({ label, blocks, onChange }: { label: string; blocks: MiniBlock[]; onChange: (blocks: MiniBlock[]) => void }) {
  const [newType, setNewType] = useState<MiniBlockType>("text");

  const updateBlock = (i: number, content: Record<string, unknown>) => onChange(blocks.map((b, j) => (j === i ? { ...b, content } : b)));
  const removeBlock = (i: number) => onChange(blocks.filter((_, j) => j !== i));
  const moveBlock = (i: number, dir: -1 | 1) => {
    const j = i + dir;
    if (j < 0 || j >= blocks.length) return;
    const next = [...blocks];
    [next[i], next[j]] = [next[j], next[i]];
    onChange(next);
  };
  const addBlock = () => onChange([...blocks, { type: newType, content: MINI_BLOCK_DEFAULT_CONTENT[newType] }]);

  return (
    <div className="rounded-xl border border-border p-3">
      <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">{label}</p>
      <div className="space-y-2">
        {blocks.map((block, i) => (
          <div key={i} className="rounded-lg border border-border bg-muted/30 p-2.5">
            <div className="mb-2 flex items-center justify-between gap-1">
              <span className="rounded-md bg-card px-2 py-0.5 text-xs font-medium">{block.type}</span>
              <div className="flex gap-1">
                <button onClick={() => moveBlock(i, -1)} disabled={i === 0} aria-label="Mover para cima" className="rounded-md p-1 text-muted-foreground hover:bg-muted disabled:opacity-30"><ArrowUp size={13} /></button>
                <button onClick={() => moveBlock(i, 1)} disabled={i === blocks.length - 1} aria-label="Mover para baixo" className="rounded-md p-1 text-muted-foreground hover:bg-muted disabled:opacity-30"><ArrowDown size={13} /></button>
                <button onClick={() => removeBlock(i)} aria-label="Remover mini-bloco" className="rounded-md p-1 text-muted-foreground hover:bg-destructive/10 hover:text-destructive"><Trash2 size={13} /></button>
              </div>
            </div>
            <MiniBlockFieldsEditor block={block} onChange={(content) => updateBlock(i, content)} />
          </div>
        ))}
      </div>
      <div className="mt-2 space-y-2">
        <SelectLike label="" value={newType} options={[...MINI_BLOCK_TYPES]} onChange={(v) => setNewType(v as MiniBlockType)} />
        <Button onClick={addBlock} className="w-full"><Plus size={14} />Adicionar bloco</Button>
      </div>
    </div>
  );
}

/**
 * Sub-editor de `two-column` (Sprint 12, Tarefa C) — cada coluna é uma lista
 * de mini-blocos tipados (`text`/`rich-text`/`image`/`cta`), alinhado com a
 * etapa 21 do backend. Substitui o editor genérico de objeto fixo
 * `{title, body}` por coluna.
 */
export function TwoColumnEditor({ content, onChange }: { content: Record<string, unknown>; onChange: (patch: Record<string, unknown>) => void }) {
  const left = Array.isArray(content.left) ? (content.left as MiniBlock[]) : [];
  const right = Array.isArray(content.right) ? (content.right as MiniBlock[]) : [];

  return (
    <div className="mt-4 grid gap-3 md:grid-cols-2">
      <ColumnEditor label="Coluna 1 (left)" blocks={left} onChange={(blocks) => onChange({ left: blocks })} />
      <ColumnEditor label="Coluna 2 (right)" blocks={right} onChange={(blocks) => onChange({ right: blocks })} />
    </div>
  );
}
