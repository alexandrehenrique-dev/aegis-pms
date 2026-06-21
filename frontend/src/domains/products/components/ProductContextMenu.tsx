import { Pencil, Star, Trash2 } from "lucide-react";
import { ContextActionMenu, type ActionMenuItem, type ContextMenuTarget } from "../../../shared/components/ContextActionMenu";

export type { ContextMenuTarget };

/**
 * Editar/Excluir produto — botão direito no desktop, botão "⋮" + painel de
 * tela cheia no mobile (ver ContextActionMenu). Útil sobretudo em produtos
 * bloqueados/sem módulos que não podem ser abertos. "Favoritar"/"Desfavoritar"
 * (Sprint 15, Tarefa E.1) só aparece quando `onToggleFavorite` é passado —
 * hoje só `ProductSelectScreen` injeta isso, porque é o único lugar onde
 * `isFavorite` existe no modelo (`ProductOption`, não `EditableProduct`).
 */
export function ProductContextMenu({ position, isFavorite, onToggleFavorite, onEdit, onDelete, onClose }: {
  position: ContextMenuTarget; isFavorite?: boolean; onToggleFavorite?: () => void; onEdit: () => void; onDelete: () => void; onClose: () => void;
}) {
  const items: ActionMenuItem[] = [];
  if (onToggleFavorite) {
    items.push({ label: isFavorite ? "Desfavoritar" : "Favoritar", icon: <Star size={14} className={isFavorite ? "fill-[#D97706] text-[#D97706]" : ""} />, onClick: onToggleFavorite });
  }
  items.push(
    { label: "Editar", icon: <Pencil size={14} />, onClick: onEdit },
    { label: "Excluir", icon: <Trash2 size={14} />, destructive: true, onClick: onDelete },
  );
  return <ContextActionMenu position={position} onClose={onClose} items={items} />;
}
