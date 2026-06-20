import { Pencil, Trash2 } from "lucide-react";
import { ContextActionMenu, type ContextMenuTarget } from "../../../shared/components/ContextActionMenu";

export type { ContextMenuTarget };

/** Editar/Excluir produto — botão direito no desktop, botão "⋮" + painel de tela cheia no mobile (ver ContextActionMenu). Útil sobretudo em produtos bloqueados/sem módulos que não podem ser abertos. */
export function ProductContextMenu({ position, onEdit, onDelete, onClose }: { position: ContextMenuTarget; onEdit: () => void; onDelete: () => void; onClose: () => void }) {
  return (
    <ContextActionMenu
      position={position}
      onClose={onClose}
      items={[
        { label: "Editar", icon: <Pencil size={14} />, onClick: onEdit },
        { label: "Excluir", icon: <Trash2 size={14} />, destructive: true, onClick: onDelete },
      ]}
    />
  );
}
