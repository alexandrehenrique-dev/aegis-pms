import { useEffect, useState } from "react";
import { AnimatePresence } from "motion/react";
import { NotificationModal } from "./components/NotificationModal";
import { notificationsService } from "./services/notificationsService";
import type { NotificationWithStatus } from "./contracts/notification";

/**
 * Gatilho de exibição automática (Sprint 14, Tarefa C) — vive uma vez no
 * layout do produto (`AppShell`), não em cada página, para checar só ao
 * entrar no produto, nunca a cada navegação interna. Mostra a notificação
 * `MODAL_ONCE` mais antiga ainda não exibida para o usuário; ao fechar,
 * marca como mostrada e checa de novo — se houver outra pendente, mostra a
 * próxima, em fila, nunca todas empilhadas.
 */
export function PendingNotificationGate() {
  const [pending, setPending] = useState<NotificationWithStatus | null>(null);

  const loadNext = () => {
    notificationsService.getPendingModal().then(setPending);
  };

  useEffect(() => {
    loadNext();
  }, []);

  const handleClose = async () => {
    if (!pending) return;
    await notificationsService.markShown(pending.id);
    loadNext();
  };

  return (
    <AnimatePresence>
      {pending && <NotificationModal key={pending.id} notification={pending} onClose={handleClose} />}
    </AnimatePresence>
  );
}
