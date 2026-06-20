export type NotificationType = "ONBOARDING" | "FEATURE" | "WARNING" | "MAINTENANCE" | "GENERAL";

/**
 * `MODAL_ONCE` aparece como modal automático na primeira oportunidade de
 * cada destinatário (depois disso, só no sino); `BELL_ONLY` nunca aparece
 * como modal automático, só existe no sino — para avisos de baixa prioridade.
 */
export type PresentationMode = "MODAL_ONCE" | "BELL_ONLY";

export type Notification = {
  id: string;
  type: NotificationType;
  title: string;
  bodyMarkdown: string;
  presentationMode: PresentationMode;
  createdBySubject: string;
  createdAt: string;
  /** Mostra o badge "Em homologação" no modal/sino — campo explícito em vez de inferir por parsing do texto. */
  homologationBadge?: boolean;
};

/** Uma linha por (notificação, destinatário) — estado de leitura/exibição é por usuário, nunca na própria `Notification`. */
export type UserNotificationStatus = {
  notificationId: string;
  autoShown: boolean;
  read: boolean;
  readAt?: string;
};

export type NotificationWithStatus = Notification & UserNotificationStatus;
