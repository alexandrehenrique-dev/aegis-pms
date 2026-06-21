/** Espelha `CreateFeedbackRequest`/`FeedbackSummary` do backend (etapa 25 — `25_dominio_feedback.md`). */
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

export type FeedbackSummary = CreateFeedbackRequest & {
  id: string;
  status: FeedbackStatus;
  createdAt: string;
};
