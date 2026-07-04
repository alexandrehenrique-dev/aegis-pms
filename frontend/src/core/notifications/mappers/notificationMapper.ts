import type { Notification, NotificationWithStatus } from "../contracts/notification";

export type NotificationDto = {
  id: string;
  type: Notification["type"];
  title: string;
  bodyMarkdown: string;
  presentationMode: Notification["presentationMode"];
  createdBySubject?: string | null;
  createdAt: string;
  updatedAt?: string;
  homologationBadge?: boolean;
};

export type NotificationWithStatusDto = NotificationDto & {
  autoShown: boolean;
  read: boolean;
  readAt?: string | null;
};

export function mapNotification(dto: NotificationDto): Notification {
  return {
    id: dto.id,
    type: dto.type,
    title: dto.title,
    bodyMarkdown: dto.bodyMarkdown,
    presentationMode: dto.presentationMode,
    createdBySubject: dto.createdBySubject ?? "system",
    createdAt: dto.createdAt,
    homologationBadge: dto.homologationBadge,
  };
}

export function mapNotificationWithStatus(dto: NotificationWithStatusDto): NotificationWithStatus {
  return {
    ...mapNotification(dto),
    notificationId: dto.id,
    autoShown: dto.autoShown,
    read: dto.read,
    readAt: dto.readAt ?? undefined,
  };
}
