import { mockUsers, mockTenantsByUser } from "../../auth/mocks/users";
import { logApiCall } from "../../../shared/services/devLog";
import { seedNotifications, seedStatusByUser } from "../mocks/notifications.mocks";
import { IS_API_MODE } from "../../../infra/apiMode";
import { apiClient } from "../../../shared/services/apiClient";
import type { Notification, NotificationWithStatus, UserNotificationStatus } from "../contracts/notification";
import type { CreateNotificationRequest } from "../contracts/requests";

// Stores em memória só para a sessão do navegador — mesmo padrão de
// domains/tenants/services/tenantsService.ts.
const notificationsStore: Notification[] = [...seedNotifications];
const statusStore: Record<string, UserNotificationStatus[]> = Object.fromEntries(
  Object.entries(seedStatusByUser).map(([userId, statuses]) => [userId, statuses.map((s) => ({ ...s }))]),
);

let currentUserId: string | null = null;

/**
 * Chamado pelo AuthProvider a cada troca de usuário logado — mesmo papel do
 * `setAuthTokenProvider` do apiClient: hoje resolve contra os mocks de
 * login, e quando o backend existir o token JWT já identifica o usuário,
 * então nenhum chamador deste service precisa mudar.
 */
export function setNotificationsCurrentUser(userId: string | null) {
  currentUserId = userId;
}

/** Só as notificações para as quais o usuário é destinatário de fato (tem uma `UserNotificationStatus` própria) — nunca todas com um status padrão inventado. */
function notificationsFor(userId: string): NotificationWithStatus[] {
  return (statusStore[userId] ?? [])
    .map((status) => {
      const notification = notificationsStore.find((n) => n.id === status.notificationId);
      return notification ? { ...notification, ...status } : null;
    })
    .filter((n): n is NotificationWithStatus => n !== null);
}

function byCreatedAtDesc(a: Notification, b: Notification): number {
  return a.createdAt < b.createdAt ? 1 : a.createdAt > b.createdAt ? -1 : 0;
}

// Guarda literal `import.meta.env.PROD` (não `IS_API_MODE`, um booleano
// importado que o esbuild não consegue provar morto entre módulos) — só
// assim `mockUsers`/`mockTenantsByUser` são eliminados do bundle de
// produção pelo tree-shaking. Em modo api, `create()` nunca chama estas
// funções (retorna antes, via `apiClient`); em build de produção "mock"
// (fora do fluxo suportado — mock só roda via `vite dev`), o fallback vazio
// é aceitável.
function allUserIds(): string[] {
  /* v8 ignore next */
  if (import.meta.env.PROD) return [];
  return Object.values(mockUsers).map((m) => m.user.id);
}

function userIdsForTenant(tenantId: string): string[] {
  /* v8 ignore next */
  if (import.meta.env.PROD) return [];
  return Object.entries(mockTenantsByUser)
    .filter(([, tenants]) => tenants.some((t) => t.id === tenantId))
    .map(([userId]) => userId);
}

export const notificationsService = {
  async listMine(): Promise<NotificationWithStatus[]> {
    if (IS_API_MODE) return apiClient.get<NotificationWithStatus[]>("/notifications/mine");
    if (!currentUserId) return [];
    return notificationsFor(currentUserId).sort(byCreatedAtDesc);
  },

  /** A mais antiga ainda não mostrada automaticamente — fila de "primeiro acesso", uma por vez (Sprint 14, Tarefa C). */
  async getPendingModal(): Promise<NotificationWithStatus | null> {
    if (IS_API_MODE) return apiClient.get<NotificationWithStatus | null>("/notifications/mine/pending-modal");
    if (!currentUserId) return null;
    const pending = notificationsFor(currentUserId)
      .filter((n) => n.presentationMode === "MODAL_ONCE" && !n.autoShown)
      .sort((a, b) => (a.createdAt < b.createdAt ? -1 : a.createdAt > b.createdAt ? 1 : 0));
    return pending[0] ?? null;
  },

  async markShown(notificationId: string): Promise<void> {
    if (IS_API_MODE) return apiClient.post(`/notifications/${notificationId}/mark-shown`);
    if (!currentUserId) return;
    logApiCall("POST", `/api/v1/notifications/${notificationId}/mark-shown`);
    const list = statusStore[currentUserId] ?? (statusStore[currentUserId] = []);
    const existing = list.find((s) => s.notificationId === notificationId);
    if (existing) existing.autoShown = true;
    else list.push({ notificationId, autoShown: true, read: false });
  },

  async markRead(notificationId: string): Promise<void> {
    if (IS_API_MODE) return apiClient.post(`/notifications/${notificationId}/mark-read`);
    if (!currentUserId) return;
    logApiCall("POST", `/api/v1/notifications/${notificationId}/mark-read`);
    const list = statusStore[currentUserId] ?? (statusStore[currentUserId] = []);
    const existing = list.find((s) => s.notificationId === notificationId);
    if (existing) { existing.read = true; existing.readAt = new Date().toISOString(); }
    else list.push({ notificationId, autoShown: false, read: true, readAt: new Date().toISOString() });
  },

  /** Super Admin cria e o mock "espalha" — uma `UserNotificationStatus` por destinatário, exatamente como o backend fará via fan-out na criação (Seção F). */
  async create(req: CreateNotificationRequest): Promise<Notification> {
    if (IS_API_MODE) return apiClient.post<Notification>("/notifications", req);
    logApiCall("POST", "/api/v1/notifications", req);
    const created: Notification = {
      id: `n-${Date.now()}`,
      type: req.type,
      title: req.title,
      bodyMarkdown: req.bodyMarkdown,
      presentationMode: req.presentationMode,
      homologationBadge: req.homologationBadge,
      createdBySubject: currentUserId ?? "system",
      createdAt: new Date().toISOString(),
    };
    notificationsStore.push(created);

    const targetUserIds = req.recipients.mode === "all" ? allUserIds()
      : req.recipients.mode === "tenant" ? userIdsForTenant(req.recipients.tenantId)
      : req.recipients.userIds;

    for (const userId of targetUserIds) {
      const list = statusStore[userId] ?? (statusStore[userId] = []);
      if (!list.some((s) => s.notificationId === created.id)) {
        list.push({ notificationId: created.id, autoShown: false, read: false });
      }
    }

    return created;
  },

  /** Gestão/auditoria do Super Admin — todas as notificações já criadas, independente de destinatário. */
  async listAll(): Promise<Notification[]> {
    if (IS_API_MODE) return apiClient.get<Notification[]>("/notifications");
    return [...notificationsStore].sort(byCreatedAtDesc);
  },
};
