import { createPortal } from "react-dom";
import { motion } from "motion/react";
import { AlertTriangle } from "lucide-react";
import { fade } from "../../../shared/components/Primitives";
import { AegisLogo } from "../../../shared/components/AegisLogo";
import { Markdown } from "../../../shared/components/Markdown";
import type { Notification } from "../contracts/notification";

/**
 * Modal genérica de notificação (Sprint 14, Tarefa B.1) — substitui o
 * `DemoWelcomeModal` fixo: recebe qualquer `Notification` (onboarding ou
 * qualquer outro tipo criado pelo Super Admin) e renderiza título + corpo
 * via `<Markdown>`. O mesmo componente serve tanto o gatilho automático
 * (`PendingNotificationGate`) quanto o clique num item do sino.
 *
 * Renderiza via `createPortal` direto em `document.body` (Sprint 15, Tarefa
 * D.3) — quando aberta a partir do sino dentro de `AppShell`, um ancestral
 * com `transform` (a animação Framer Motion do próprio `AppShell`/header)
 * virava o "containing block" deste `fixed`, descentralizando a modal em
 * relação à viewport real. Mesmo motivo documentado em `Notifications.tsx`
 * e `ContextActionMenu.tsx` para o uso de portal.
 */
export function NotificationModal({ notification, onClose }: { notification: Notification; onClose: () => void }) {
  return createPortal(
    <motion.div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4 backdrop-blur-sm" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={onClose}>
      <motion.div {...fade} className="w-full max-w-md rounded-2xl border border-border bg-card p-6 shadow-[0_24px_80px_rgba(0,0,0,0.2)]" onClick={(e) => e.stopPropagation()}>
        <div className="mb-4 text-center">
          <AegisLogo size="md" className="mx-auto" />
          <h2 className="mt-3 font-semibold tracking-[-.02em]">{notification.title}</h2>
        </div>
        {notification.homologationBadge && (
          <div className="mb-4 flex items-center gap-2 rounded-xl border border-[#D97706]/25 bg-[#FBF1DF] p-3 text-xs text-[#8A5A12]">
            <AlertTriangle size={14} className="shrink-0" />Este produto está em processo de homologação.
          </div>
        )}
        <div className="mb-5 max-h-[50vh] overflow-y-auto text-sm">
          <Markdown>{notification.bodyMarkdown}</Markdown>
        </div>
        <button onClick={onClose} className="flex w-full items-center justify-center gap-2 rounded-xl bg-primary py-2.5 text-sm font-medium text-primary-foreground transition hover:bg-primary/90 active:scale-[0.97] shadow-[0_4px_14px_rgba(124,58,237,.25)]">Entendi</button>
      </motion.div>
    </motion.div>,
    document.body,
  );
}
