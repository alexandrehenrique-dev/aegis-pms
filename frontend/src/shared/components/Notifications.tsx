import { useState } from "react";
import { createPortal } from "react-dom";
import { AnimatePresence, motion } from "motion/react";
import { AlertTriangle, Bell, GraduationCap, Info, Megaphone, X, type LucideIcon } from "lucide-react";
import { fade } from "./Primitives";
import { useAsyncData } from "../hooks/useAsyncData";
import { formatRelativeTime } from "../utils/relativeTime";
import { notificationsService } from "../../core/notifications/services/notificationsService";
import { NotificationModal } from "../../core/notifications/components/NotificationModal";
import type { NotificationType, NotificationWithStatus } from "../../core/notifications/contracts/notification";

const TYPE_ICON: Record<NotificationType, LucideIcon> = {
  ONBOARDING: GraduationCap,
  FEATURE: Megaphone,
  WARNING: AlertTriangle,
  MAINTENANCE: AlertTriangle,
  GENERAL: Info,
};

/**
 * Lista de notificações compartilhada pelas duas apresentações abaixo —
 * extraída para não duplicar o `.map` entre a variante desktop e mobile.
 * Clicar num item sempre abre a modal com o conteúdo completo (Sprint 14,
 * Tarefa E.3) — nunca navega para uma rota arbitrária.
 */
function NotificationItems({ items, onSelect }: { items: NotificationWithStatus[]; onSelect: (n: NotificationWithStatus) => void }) {
  return (
    <>
      {items.map((n) => {
        const Icon = TYPE_ICON[n.type];
        return (
          <button key={n.id} onClick={() => onSelect(n)} className="flex w-full items-start gap-3 rounded-xl p-3 text-left transition hover:bg-muted">
            <span className={`mt-1.5 h-2 w-2 shrink-0 rounded-full ${n.read ? "bg-muted" : "bg-primary"}`} />
            <Icon size={15} className="mt-0.5 shrink-0 text-muted-foreground" />
            <div>
              <p className="text-sm font-medium leading-snug">{n.title}</p>
              <p className="mt-0.5 text-xs text-muted-foreground">{formatRelativeTime(n.createdAt)}</p>
            </div>
          </button>
        );
      })}
    </>
  );
}

/**
 * Sprint 13, Tarefa P — em telas estreitas, o dropdown ancorado (`absolute
 * right-0`) media sua largura a partir do botão do sino, não da borda da
 * viewport; como o sino não fica colado na borda direita real da tela em
 * mobile (há outros ícones depois dele), o painel abria estourando para a
 * ESQUERDA da viewport (texto cortado, sem scroll). Mesmo padrão de duas
 * apresentações por CSS já usado em `ContextActionMenu`: painel ancorado em
 * `lg:` e acima, bottom sheet de tela cheia abaixo de `lg`. Portal para
 * `document.body` pela mesma razão documentada lá — qualquer ancestral com
 * `transform` (motion) vira containing block de um `fixed` interno.
 *
 * Sprint 14, Tarefa E — deixa de ler `mocks/timeline.ts` e passa a consumir
 * `notificationsService.listMine()` (dados reais, por usuário).
 */
export function Notifications() {
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState<NotificationWithStatus | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const refresh = () => setReloadKey((k) => k + 1);
  const { data: notifications } = useAsyncData(() => notificationsService.listMine(), [reloadKey]);
  const items = notifications ?? [];
  const hasUnread = items.some((n) => !n.read);

  const handleSelect = (n: NotificationWithStatus) => {
    setActive(n);
    setOpen(false);
  };

  const handleCloseActive = async () => {
    if (active && !active.read) await notificationsService.markRead(active.id);
    setActive(null);
    refresh();
  };

  const handleMarkAllRead = async () => {
    await Promise.all(items.filter((n) => !n.read).map((n) => notificationsService.markRead(n.id)));
    refresh();
  };

  return (
    <div className="relative">
      <button onClick={() => setOpen(!open)} aria-label="Notificações" className="relative rounded-xl border border-border bg-card p-2 transition hover:bg-muted">
        <Bell size={16} />
        {hasUnread && <span className="absolute right-1.5 top-1.5 h-2 w-2 rounded-full bg-primary" />}
      </button>

      <AnimatePresence>
        {open && (
          <motion.div {...fade} className="absolute right-0 top-11 z-40 hidden w-[360px] rounded-2xl border border-border bg-card shadow-[0_8px_32px_rgba(0,0,0,0.12)] dark:shadow-[0_8px_32px_rgba(0,0,0,0.4)] lg:block" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center justify-between border-b border-border px-4 py-3">
              <h3 className="text-sm font-semibold">Notificações</h3>
              <button onClick={handleMarkAllRead} className="text-xs text-muted-foreground transition hover:text-foreground">Marcar lidas</button>
            </div>
            <div className="p-2">
              <NotificationItems items={items} onSelect={handleSelect} />
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {open &&
        createPortal(
          <motion.div
            className="fixed inset-0 z-40 bg-black/20 lg:hidden"
            initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            onClick={() => setOpen(false)}
          >
            <motion.div
              className="absolute inset-x-0 bottom-0 max-h-[80vh] overflow-auto rounded-t-2xl border-t border-border bg-card pb-[max(0.5rem,env(safe-area-inset-bottom))]"
              initial={{ y: 320 }} animate={{ y: 0 }} exit={{ y: 320 }}
              onClick={(e) => e.stopPropagation()}
            >
              <div className="flex items-center justify-between border-b border-border px-4 py-3">
                <h3 className="text-sm font-semibold">Notificações</h3>
                <div className="flex items-center gap-2">
                  <button onClick={handleMarkAllRead} className="text-xs text-muted-foreground transition hover:text-foreground">Marcar lidas</button>
                  <button onClick={() => setOpen(false)} aria-label="Fechar" className="rounded-full p-1.5 hover:bg-muted"><X size={16} /></button>
                </div>
              </div>
              <div className="p-2">
                <NotificationItems items={items} onSelect={handleSelect} />
              </div>
            </motion.div>
          </motion.div>,
          document.body,
        )}

      <AnimatePresence>
        {active && <NotificationModal key={active.id} notification={active} onClose={handleCloseActive} />}
      </AnimatePresence>
    </div>
  );
}
