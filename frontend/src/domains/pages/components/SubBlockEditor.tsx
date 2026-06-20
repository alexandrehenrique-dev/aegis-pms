import { useState } from "react";
import { ArrowDown, ArrowUp, Plus, Trash2 } from "lucide-react";
import { Button, Field, SelectLike } from "../../../shared/components/Primitives";
import { MarkdownField } from "../../../shared/components/MarkdownField";
import { MediaField } from "../../../shared/components/MediaField";
import { MINI_BLOCK_DEFAULT_CONTENT } from "../blockDefaults";
import type { MiniBlock, MiniBlockType } from "../contracts/responses";

const MARKDOWN_KEYS = new Set(["body", "desc", "description"]);

function MiniBlockFieldsEditor({ block, onChange }: { block: MiniBlock; onChange: (content: Record<string, unknown>) => void }) {
  return (
    <div className="space-y-2">
      {Object.entries(block.content).filter(([, v]) => typeof v === "string").map(([k, v]) => {
        const onFieldChange = (nv: string) => onChange({ ...block.content, [k]: nv });
        if (k === "src") return <MediaField key={k} label={k} value={v as string} typeFilter="imagem" onChange={onFieldChange} />;
        if (MARKDOWN_KEYS.has(k)) return <MarkdownField key={k} label={k} value={v as string} onChange={onFieldChange} />;
        return <Field key={k} label={k} value={v as string} onChange={onFieldChange} />;
      })}
    </div>
  );
}

/**
 * Motor genérico de sub-blocos (Sprint 13, Tarefa E.2) — dado um bloco com
 * `BLOCK_ACCEPTS_CHILDREN` preenchido, renderiza a lista de filhos com as
 * mesmas 4 operações que um bloco de topo tem: criar, editar, remover e
 * reordenar. Reaproveitável por qualquer bloco-pai, não só `two-column`
 * (que é apenas o primeiro consumidor deste motor, ver `TwoColumnEditor`).
 */
export function SubBlockEditor({ label, blocks, allowedTypes, onChange }: {
  label: string;
  blocks: MiniBlock[];
  allowedTypes: MiniBlockType[];
  onChange: (blocks: MiniBlock[]) => void;
}) {
  const [newType, setNewType] = useState<MiniBlockType>(allowedTypes[0]);

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
                <button onClick={() => removeBlock(i)} aria-label="Remover sub-bloco" className="rounded-md p-1 text-muted-foreground hover:bg-destructive/10 hover:text-destructive"><Trash2 size={13} /></button>
              </div>
            </div>
            <MiniBlockFieldsEditor block={block} onChange={(content) => updateBlock(i, content)} />
          </div>
        ))}
      </div>
      <div className="mt-2 space-y-2">
        <SelectLike label="" value={newType} options={allowedTypes} onChange={(v) => setNewType(v as MiniBlockType)} />
        <Button onClick={addBlock} className="w-full"><Plus size={14} />Adicionar bloco</Button>
      </div>
    </div>
  );
}
