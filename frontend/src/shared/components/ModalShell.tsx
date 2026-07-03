import type { ReactNode } from "react";
import { createPortal } from "react-dom";
import { motion } from "motion/react";
import { fade } from "./motion";

/**
 * Base reutilizável de modal "tipo diálogo" — overlay + conteúdo centralizado
 * (Sprint 15, Tarefa D.1). Extraído de `ConfirmDialog.tsx`, que junto com
 * `CreateProductModal.tsx`/`EditProductModal.tsx`/`MarkdownEditModal.tsx`
 * repetia o mesmo par overlay/conteúdo com pequenas variações de largura —
 * o risco real não era nenhum desses fechar sozinho hoje (o
 * `stopPropagation()` já está correto em todos), mas qualquer modal *futuro*
 * divergir do padrão. Centralizar aqui também resolve de uma vez o problema
 * de "fixed descentralizado por transform de ancestral" (ver
 * `NotificationModal.tsx`/`Notifications.tsx`/`ContextActionMenu.tsx`): todo
 * consumidor de `ModalShell` já renderiza via `createPortal` em
 * `document.body`, sem precisar lembrar disso a cada novo modal.
 */
export function ModalShell({ onClose, maxWidthClassName = "max-w-md", children }: { onClose: () => void; maxWidthClassName?: string; children: ReactNode }) {
  return createPortal(
    <motion.div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30 backdrop-blur-[3px] p-4" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={onClose}>
      <motion.div {...fade} className={`w-full ${maxWidthClassName} rounded-2xl border border-border bg-card p-6 shadow-[0_24px_80px_rgba(0,0,0,0.2)]`} onClick={(e) => e.stopPropagation()}>
        {children}
      </motion.div>
    </motion.div>,
    document.body,
  );
}
