import { IS_API_MODE } from "../../../infra/apiMode";
import { apiClient, type ApiError } from "../../../shared/services/apiClient";

/**
 * Conecta InviteScreen/ForgotPasswordScreen/ResetPasswordScreen aos endpoints
 * reais da etapa 29 do backend (`AuthController`, `/api/v1/auth/**`) — ver
 * Sprint 21 frontend. Antes da Sprint de Integração 01 estes fluxos sempre
 * chamavam a API real mesmo em modo mock (dependiam do backend local do
 * docker-compose estar de pé); agora respeitam `IS_API_MODE` como o resto do
 * app, para funcionar também num frontend mock isolado, sem backend.
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
    if (!IS_API_MODE) {
      return Promise.resolve({
        userName: "Usuário Mock",
        userEmail: "dev@mock.local",
        tenantName: "Dev Tenant",
        productNames: ["Maestro Beton"],
        role: "editor",
        inviterName: "Ana Martins",
        expiresAt: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
      });
    }
    return apiClient.post<InviteTokenData>("/auth/invite/validate", { token });
  },

  activateAccount(token: string, password: string): Promise<void> {
    if (!IS_API_MODE) return Promise.resolve();
    return apiClient.post("/auth/activate", { token, password });
  },

  requestPasswordReset(email: string): Promise<void> {
    if (!IS_API_MODE) return Promise.resolve();
    return apiClient.post("/auth/reset-password/request", { email });
  },

  confirmPasswordReset(token: string, password: string): Promise<void> {
    if (!IS_API_MODE) return Promise.resolve();
    return apiClient.post("/auth/reset-password/confirm", { token, password });
  },
};
