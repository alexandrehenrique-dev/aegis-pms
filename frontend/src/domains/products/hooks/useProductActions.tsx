import { useState, type MouseEvent } from "react";
import { createPortal } from "react-dom";
import { useAuth } from "../../../core/auth/AuthContext";
import { toast } from "../../../core/notifications/toast";
import { ConfirmDialog } from "../../../shared/components/ConfirmDialog";
import { Field } from "../../../shared/components/Primitives";
import { ProductContextMenu, type ContextMenuTarget } from "../components/ProductContextMenu";
import { EditProductModal } from "../components/EditProductModal";
import type { EditableProduct } from "../contracts/responses";
import type { UpdateProductRequest, DeleteProductRequest } from "../contracts/requests";

export type ProductPersistence = {
  update: (req: UpdateProductRequest) => Promise<void>;
  remove: (req: DeleteProductRequest) => Promise<void>;
};

/**
 * Editar/Excluir produto via botão direito — único caminho de gestão para
 * produtos bloqueados/sem módulos, que não podem ser abertos. Acesso
 * restrito a Tenant Admin/Super Admin, espelhando o bloqueio de
 * `/products/new` em `core/permissions/roles.ts` (Product Manager/Editor/
 * Viewer não criam nem editam/excluem produtos, só operam o que já está
 * habilitado). `persistence` é injetada pelo chamador — quem efetivamente
 * grava a mudança varia por tela (`productsService` em ProductsList,
 * mutação via AuthContext em ProductSelectScreen).
 */
export function useProductActions(product: EditableProduct, persistence: ProductPersistence, onChanged: () => void) {
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

  const handleSave = async (req: UpdateProductRequest) => {
    await persistence.update(req);
    toast.success("Produto atualizado com sucesso!");
    onChanged();
  };

  const handleDelete = async () => {
    setDeleting(true);
    try {
      await persistence.remove({ confirmationText });
      toast.success(`${product.name} foi excluído.`, { description: "Exclusão lógica — pode ser restaurado pelo suporte dentro da política de retenção." });
      setPendingDelete(false);
      setConfirmationText("");
      onChanged();
    } finally {
      setDeleting(false);
    }
  };

  // Portal direto em document.body: este hook roda dentro de PCard, que vive
  // dentro do motion.div animado de ProductSelectScreen — um ancestral com
  // transform (framer-motion aplica isso para animar x/y) vira o "containing
  // block" de qualquer elemento fixed descendente, então sem o portal estes
  // overlays ficavam confinados à caixa daquele ancestral em vez do viewport.
  const portal = createPortal(
    <>
      {contextMenu && (
        <ProductContextMenu position={contextMenu} onEdit={openEdit} onDelete={openDelete} onClose={() => setContextMenu(null)} />
      )}
      {editing && (
        <EditProductModal product={product} onClose={() => setEditing(false)} onSave={handleSave} />
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
    </>,
    document.body,
  );

  return { canManage, onContextMenu, openEdit, openDelete, portal };
}
