import { useState } from "react";
import { GripVertical, Plus, Trash2 } from "lucide-react";
import { Button, Field, SelectLike } from "../../../shared/components/Primitives";
import { MarkdownField } from "../../../shared/components/MarkdownField";
import { MediaField } from "../../../shared/components/MediaField";
import { useDragReorder } from "../../../shared/hooks/useDragReorder";
import { MINI_BLOCK_DEFAULT_CONTENT } from "../blockDefaults";
import type { MiniBlock, MiniBlockType } from "../contracts/responses";

const SUB_BLOCK_DRAG_TYPE = "sub-block";

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

function SubBlockRow({ block, index, onChange, onRemove, onHoverReorder }: {
  block: MiniBlock; index: number; onChange: (content: Record<string, unknown>) => void; onRemove: () => void; onHoverReorder: (from: number, to: number) => void;
}) {
  const { ref, isDragging, isOver } = useDragReorder({ dragType: SUB_BLOCK_DRAG_TYPE, index, onHoverReorder });

  return (
    <div ref={ref} style={{ opacity: isDragging ? 0.4 : 1 }} className={`rounded-lg border border-border bg-muted/30 p-2.5 ${isOver ? "ring-2 ring-primary/30" : ""}`}>
      <div className="mb-2 flex items-center justify-between gap-1">
        <span className="flex items-center gap-1.5">
          <GripVertical size={13} className="cursor-grab text-muted-foreground/50 active:cursor-grabbing" />
          <span className="rounded-md bg-card px-2 py-0.5 text-xs font-medium">{block.type}</span>
        </span>
        <button onClick={onRemove} aria-label="Remover sub-bloco" className="rounded-md p-1 text-muted-foreground hover:bg-destructive/10 hover:text-destructive"><Trash2 size={13} /></button>
      </div>
      <MiniBlockFieldsEditor block={block} onChange={onChange} />
    </div>
  );
}

/**
 * Motor genérico de sub-blocos (Sprint 13, Tarefa E.2) — dado um bloco com
 * `BLOCK_ACCEPTS_CHILDREN` preenchido, renderiza a lista de filhos com as
 * mesmas 4 operações que um bloco de topo tem: criar, editar, remover e
 * reordenar (por drag-and-drop, Tarefa F.2). Reaproveitável por qualquer
 * bloco-pai, não só `two-column` (que é apenas o primeiro consumidor deste
 * motor, ver `TwoColumnEditor`).
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
  const reorderBlocks = (from: number, to: number) => {
    const next = [...blocks];
    const [moved] = next.splice(from, 1);
    next.splice(to, 0, moved);
    onChange(next);
  };
  const addBlock = () => onChange([...blocks, { type: newType, content: MINI_BLOCK_DEFAULT_CONTENT[newType] }]);

  return (
    <div className="rounded-xl border border-border p-3">
      <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">{label}</p>
      <div className="space-y-2">
        {blocks.map((block, i) => (
          <SubBlockRow key={i} block={block} index={i} onChange={(content) => updateBlock(i, content)} onRemove={() => removeBlock(i)} onHoverReorder={reorderBlocks} />
        ))}
      </div>
      <div className="mt-2 space-y-2">
        <SelectLike label="" value={newType} options={allowedTypes} onChange={(v) => setNewType(v as MiniBlockType)} />
        <Button onClick={addBlock} className="w-full"><Plus size={14} />Adicionar bloco</Button>
      </div>
    </div>
  );
}
