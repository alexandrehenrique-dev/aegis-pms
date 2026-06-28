import { logApiCall } from "../../../shared/services/devLog";
import { IS_API_MODE } from "../../../infra/apiMode";
import { apiClient } from "../../../shared/services/apiClient";
import type { CreateFeedbackRequest, FeedbackSummary } from "../contracts/feedback";

// Store em memória só para a sessão do navegador — mesmo padrão de
// notificationsService.ts. Pronto para a etapa 25 do backend quando existir.
const feedbackStore: FeedbackSummary[] = [];

function generateReadableId(): string {
  return `AGS-${Math.floor(1000 + Math.random() * 9000)}`;
}

export const feedbackService = {
  /** `FeedbackModal.handleSubmit` (Sprint 18, Tarefa D.3) — substitui o `setTimeout` fake anterior. */
  async create(req: CreateFeedbackRequest): Promise<{ id: string }> {
    if (IS_API_MODE) return apiClient.post<{ id: string }>("/feedback", req);
    logApiCall("POST", "/api/v1/feedback", req);
    const id = generateReadableId();
    feedbackStore.push({ ...req, id, status: "aberto", createdAt: new Date().toISOString() });
    return { id };
  },

  /** Consulta do mock (auditoria) — espelha `GET /api/v1/feedback` do backend. */
  async listAll(): Promise<FeedbackSummary[]> {
    if (IS_API_MODE) return apiClient.get<FeedbackSummary[]>("/feedback");
    return [...feedbackStore];
  },
};
