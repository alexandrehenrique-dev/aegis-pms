import { useEffect, useRef } from "react";
import { Pencil, Trash2 } from "lucide-react";

export type ContextMenuTarget = { x: number; y: number };

/** Menu de contexto (botão direito) sobre um card de tenant — atalho para Editar/Excluir, além dos botões visíveis no card. */
export function TenantContextMenu({ position, onEdit, onDelete, onClose }: { position: ContextMenuTarget; onEdit: () => void; onDelete: () => void; onClose: () => void }) {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClick = (e: MouseEvent) => { if (ref.current && !ref.current.contains(e.target as Node)) onClose(); };
    const handleEsc = (e: KeyboardEvent) => { if (e.key === "Escape") onClose(); };
    window.addEventListener("click", handleClick);
    window.addEventListener("keydown", handleEsc);
    return () => { window.removeEventListener("click", handleClick); window.removeEventListener("keydown", handleEsc); };
  }, [onClose]);

  return (
    <div ref={ref} style={{ position: "fixed", left: position.x, top: position.y, zIndex: 60 }} className="w-44 rounded-xl border border-border bg-card p-1.5 shadow-[0_8px_32px_rgba(0,0,0,0.16)]">
      <button onClick={onEdit} className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-sm transition hover:bg-muted"><Pencil size={14} />Editar</button>
      <button onClick={onDelete} className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-sm text-destructive transition hover:bg-destructive/10"><Trash2 size={14} />Excluir</button>
    </div>
  );
}
