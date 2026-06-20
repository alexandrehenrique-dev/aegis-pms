import { useState, type MouseEvent } from "react";
import { useAuth } from "../../../core/auth/AuthContext";
import { productsService } from "../services/productsService";
import { toast } from "../../../core/notifications/toast";
import { ConfirmDialog } from "../../../shared/components/ConfirmDialog";
import { Field } from "../../../shared/components/Primitives";
import { ProductContextMenu, type ContextMenuTarget } from "../components/ProductContextMenu";
import { EditProductModal } from "../components/EditProductModal";
import type { ProductSummary } from "../contracts/responses";

/**
 * Editar/Excluir produto via botão direito (e, opcionalmente, via menu de 3
 * pontos) — único caminho de gestão para produtos bloqueados/sem módulos,
 * que não podem ser abertos para edição interna. Acesso restrito a
 * Tenant Admin/Super Admin, espelhando o bloqueio de `/products/new` em
 * `core/permissions/roles.ts` (Product Manager/Editor/Viewer não criam nem
 * editam/excluem produtos, só operam o que já está habilitado).
 */
export function useProductActions(product: ProductSummary, onChanged: () => void) {
  const { authUser } = useAuth();
  const canManage = authUser?.role === "super_admin" || authUser?.role === "tenant_admin";

  const [contextMenu, setContextMenu] = useState<ContextMenuTarget | null>(null);
  const [editing, setEditing] = useState(false);
  const [pendingDelete, setPendingDelete] = useState(false);
  const [confirmationText, setConfirmationText] = useState("");
  const [deleting, setDeleting] = useState(false);

  const onContextMenu = (e: MouseEvent) => {
    if (!canManage) return;
    e.preventDefault();
    setContextMenu({ x: e.clientX, y: e.clientY });
  };

  const openEdit = () => { setContextMenu(null); setEditing(true); };
  const openDelete = () => { setContextMenu(null); setPendingDelete(true); };

  const handleDelete = async () => {
    setDeleting(true);
    try {
      await productsService.remove(product.id ?? product.name, { confirmationText });
      toast.success(`${product.name} foi excluído.`, { description: "Exclusão lógica — pode ser restaurado pelo suporte dentro da política de retenção." });
      setPendingDelete(false);
      setConfirmationText("");
      onChanged();
    } finally {
      setDeleting(false);
    }
  };

  const portal = (
    <>
      {contextMenu && (
        <ProductContextMenu position={contextMenu} onEdit={openEdit} onDelete={openDelete} onClose={() => setContextMenu(null)} />
      )}
      {editing && (
        <EditProductModal product={product} onClose={() => setEditing(false)} onSaved={() => { setEditing(false); onChanged(); }} />
      )}
      {pendingDelete && (
        <ConfirmDialog
          title={`Excluir ${product.name}?`}
          desc="Esta ação é irreversível para o usuário: o produto sai imediatamente da listagem e perde acesso operacional. Digite o nome do produto para confirmar."
          danger
          loading={deleting}
          confirmDisabled={confirmationText.trim() !== product.name}
          onCancel={() => { setPendingDelete(false); setConfirmationText(""); }}
          onConfirm={handleDelete}
        >
          <Field label={`Digite "${product.name}" para confirmar`} value={confirmationText} onChange={setConfirmationText} />
        </ConfirmDialog>
      )}
    </>
  );

  return { canManage, onContextMenu, openEdit, openDelete, portal };
}
