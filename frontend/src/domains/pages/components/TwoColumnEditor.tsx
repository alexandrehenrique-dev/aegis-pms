import { SubBlockEditor } from "./SubBlockEditor";
import { BLOCK_ACCEPTS_CHILDREN, type MiniBlock } from "../contracts/responses";

/**
 * Sub-editor de `two-column` (Sprint 12, Tarefa C; generalizado na Sprint 13,
 * Tarefa E.3) — cada coluna é um consumidor do motor genérico `SubBlockEditor`,
 * não uma implementação própria de CRUD de filhos.
 */
export function TwoColumnEditor({ content, onChange }: { content: Record<string, unknown>; onChange: (patch: Record<string, unknown>) => void }) {
  const left = Array.isArray(content.left) ? (content.left as MiniBlock[]) : [];
  const right = Array.isArray(content.right) ? (content.right as MiniBlock[]) : [];
  const allowedTypes = BLOCK_ACCEPTS_CHILDREN["two-column"] ?? [];

  return (
    <div className="mt-4 grid gap-3 md:grid-cols-2">
      <SubBlockEditor label="Coluna 1 (left)" blocks={left} allowedTypes={allowedTypes} onChange={(blocks) => onChange({ left: blocks })} />
      <SubBlockEditor label="Coluna 2 (right)" blocks={right} allowedTypes={allowedTypes} onChange={(blocks) => onChange({ right: blocks })} />
    </div>
  );
}
