/** Espelha `CreateFeedbackRequest`/`FeedbackSummary` do backend (etapa 27 — `27_dominio_feedback.md`). */
export type CreateFeedbackRequest = {
  productId?: string;
  category: string;
  priority: string;
  description: string;
  screenName?: string;
  /** Referência a um `Asset` (etapa 11) — nunca um upload próprio do domínio `feedback`. */
  attachmentAssetId?: string;
};

export type FeedbackStatus = "aberto" | "em_analise" | "resolvido";

/**
 * `tenantId`/`createdBySubject` espelham `FeedbackSummary` real do backend
 * (`br.com.byop.aegis.feedback.dto.FeedbackSummary`) — o backend não devolve
 * `productName` (só `productId`), então a UI resolve o nome do produto (se
 * precisar) a partir do contexto já carregado, nunca do payload de feedback.
 */
export type FeedbackSummary = CreateFeedbackRequest & {
  id: string;
  status: FeedbackStatus;
  createdAt: string;
  tenantId: string;
  createdBySubject: string;
};
