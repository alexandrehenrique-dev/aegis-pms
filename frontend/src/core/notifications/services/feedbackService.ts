import { logApiCall } from "../../../shared/services/devLog";
import { IS_API_MODE } from "../../../infra/apiMode";
import { apiClient } from "../../../shared/services/apiClient";
import type { CreateFeedbackRequest, FeedbackStatus, FeedbackSummary } from "../contracts/feedback";

// Store em memória só para a sessão do navegador — mesmo padrão de
// notificationsService.ts. Pronto para a etapa 25 do backend quando existir.
const feedbackStore: FeedbackSummary[] = [];

function generateReadableId(): string {
  return `AGS-${Math.floor(1000 + Math.random() * 9000)}`;
}

export const feedbackService = {
  /**
   * `FeedbackModal.handleSubmit` (Sprint 18, Tarefa D.3) — substitui o
   * `setTimeout` fake anterior. `authorCtx` só é usado em modo mock: o
   * backend real deriva `tenantId`/`createdBySubject` do JWT autenticado,
   * então o payload de `POST /feedback` nunca carrega esses dois campos.
   */
  async create(req: CreateFeedbackRequest, authorCtx: { tenantId: string; createdBySubject: string }): Promise<{ id: string }> {
    if (IS_API_MODE) return apiClient.post<{ id: string }>("/feedback", req);
    logApiCall("POST", "/api/v1/feedback", req);
    const id = generateReadableId();
    feedbackStore.push({ ...req, id, status: "aberto", createdAt: new Date().toISOString(), ...authorCtx });
    return { id };
  },

  /** Consulta do mock (auditoria) — espelha `GET /api/v1/feedback` do backend (Super Admin). */
  async listAll(): Promise<FeedbackSummary[]> {
    if (IS_API_MODE) return apiClient.get<FeedbackSummary[]>("/feedback");
    return [...feedbackStore];
  },

  /** Espelha `GET /api/v1/tenants/{tenantId}/feedback` — usado por Tenant Admin. */
  async listByTenant(tenantId: string): Promise<FeedbackSummary[]> {
    if (IS_API_MODE) return apiClient.get<FeedbackSummary[]>(`/tenants/${tenantId}/feedback`);
    return feedbackStore.filter((f) => f.tenantId === tenantId);
  },

  /** Espelha `PUT /api/v1/feedback/{feedbackId}/status` — `feedbackId` é o ID legível (`AGS-####`). */
  async updateStatus(feedbackId: string, status: FeedbackStatus): Promise<void> {
    if (IS_API_MODE) {
      await apiClient.put<void>(`/feedback/${feedbackId}/status`, { status });
      return;
    }
    logApiCall("PUT", `/api/v1/feedback/${feedbackId}/status`, { status });
    const item = feedbackStore.find((f) => f.id === feedbackId);
    if (item) item.status = status;
  },
};
