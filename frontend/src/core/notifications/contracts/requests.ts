import type { NotificationType, PresentationMode } from "./notification";

export type NotificationRecipients =
  | { mode: "all" }
  | { mode: "tenant"; tenantId: string }
  | { mode: "users"; userIds: string[] };

export type CreateNotificationRequest = {
  title: string;
  bodyMarkdown: string;
  type: NotificationType;
  presentationMode: PresentationMode;
  homologationBadge?: boolean;
  recipients: NotificationRecipients;
};
