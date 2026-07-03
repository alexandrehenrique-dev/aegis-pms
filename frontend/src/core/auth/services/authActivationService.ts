import { apiClient, type ApiError } from "../../../shared/services/apiClient";

/**
 * Conecta InviteScreen/ForgotPasswordScreen/ResetPasswordScreen aos endpoints
 * reais da etapa 29 do backend (`AuthController`, `/api/v1/auth/**`) — ver
 * Sprint 21 frontend. Diferente dos demais services do app, estes fluxos não
 * têm variante mock: são sempre chamadas reais, já que dependem de um token
 * emitido pelo backend (não há como simular com fidelidade no cliente).
 */

export type InviteTokenData = {
  userName: string;
  userEmail: string;
  tenantName: string;
  productNames: string[];
  role: string;
  inviterName: string;
  expiresAt: string;
};

/** Código de erro devolvido pelo backend no corpo `{ error, message }` (ver `AuthActionErrorResponse`). */
export type AuthActionErrorCode = "TOKEN_NOT_FOUND" | "TOKEN_EXPIRED" | "TOKEN_ALREADY_USED" | "WEAK_PASSWORD" | "TOO_MANY_REQUESTS";

export function authActionErrorCode(err: unknown): AuthActionErrorCode | undefined {
  return (err as ApiError | undefined)?.code as AuthActionErrorCode | undefined;
}

export const authActivationService = {
  validateInviteToken(token: string): Promise<InviteTokenData> {
    return apiClient.post<InviteTokenData>("/auth/invite/validate", { token });
  },

  activateAccount(token: string, password: string): Promise<void> {
    return apiClient.post("/auth/activate", { token, password });
  },

  requestPasswordReset(email: string): Promise<void> {
    return apiClient.post("/auth/reset-password/request", { email });
  },

  confirmPasswordReset(token: string, password: string): Promise<void> {
    return apiClient.post("/auth/reset-password/confirm", { token, password });
  },
};
