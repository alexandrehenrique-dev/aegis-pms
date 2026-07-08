import { IS_API_MODE } from "../../../infra/apiMode";
import { apiClient, type ApiError } from "../../../shared/services/apiClient";
import { mockUsers, mockTenantsByUser, mockProductsByUser } from "../mocks/users";
import type { AuthUser, LoginError, ProductOption, TenantOption } from "../../../shared/types";

export type LoginRequest = { username: string; password: string };
export type LoginTokens = { accessToken: string; refreshToken: string; expiresIn: number };

/**
 * Em modo mock, `login()` já resolve o usuário/tenants/produtos localmente
 * (não há `GET /me` para consultar) — o `AuthProvider` usa estes campos
 * diretamente em vez de ir buscar dados reais. Em modo api, ficam `undefined`
 * e o `AuthProvider` busca tudo via `meService`/`tenantsService`.
 */
export type LoginResult = LoginTokens & {
  user?: AuthUser;
  tenants?: TenantOption[];
  products?: Record<string, ProductOption[]>;
};

export const authService = {
  async login(req: LoginRequest): Promise<LoginResult> {
    if (IS_API_MODE) return apiClient.post<LoginTokens>("/auth/login", req);

    // Guarda literal (não `IS_API_MODE`, que é um booleano importado — o
    // esbuild não consegue provar a branch morta através de um binding
    // importado) — só isto garante que `mockUsers` (senhas em texto puro)
    // seja eliminado do bundle de build de produção pelo tree-shaking.
    /* v8 ignore next 3 */
    if (import.meta.env.PROD) {
      throw { status: 500, message: "Modo mock indisponível em build de produção." } satisfies ApiError;
    }

    const entry = mockUsers[req.username.toLowerCase().trim()];
    if (!entry || entry.password !== req.password) {
      const err: ApiError = { status: 401, message: "E-mail ou senha incorretos.", code: "INVALID_CREDENTIALS" };
      throw err;
    }
    if (entry.error === "blocked") {
      const err: ApiError = { status: 423, message: "Esta conta está bloqueada.", code: "ACCOUNT_DISABLED" };
      throw err;
    }

    return {
      accessToken: `mock-token-${entry.user.id}`,
      refreshToken: `mock-refresh-${entry.user.id}`,
      expiresIn: 3600,
      user: entry.user,
      tenants: mockTenantsByUser[entry.user.id] ?? [],
      products: mockProductsByUser[entry.user.id] ?? {},
    };
  },

  async refresh(refreshToken: string): Promise<LoginTokens> {
    if (IS_API_MODE) return apiClient.post<LoginTokens>("/auth/refresh", { refreshToken });
    return { accessToken: `mock-token-refreshed-${Date.now()}`, refreshToken, expiresIn: 3600 };
  },

  async logout(refreshToken: string | null): Promise<void> {
    if (IS_API_MODE) await apiClient.post<void>("/auth/logout", { refreshToken });
  },
};

/** Converte o erro de `authService.login` (real ou mock, ambos moldados como `ApiError`) no `LoginError` que a UI já sabia exibir. */
export function loginErrorFromException(err: unknown): LoginError {
  const apiErr = err as ApiError | undefined;
  if (!apiErr) return "invalid";
  if (apiErr.status === 0) return "server";
  switch (apiErr.code) {
    case "ACCOUNT_DISABLED":
      return "blocked";
    // O.1 (BUG-SPRINT-05) — perfil incompleto no Keycloak (sem firstName/lastName)
    // é distinto de conta desabilitada: o usuário só precisa reabrir o link de
    // convite para completar a ativação, não falar com o administrador.
    case "ACCOUNT_NOT_FULLY_SET_UP":
      return "profileIncomplete";
    case "REFRESH_TOKEN_EXPIRED":
      return "expired";
    case "KEYCLOAK_AUTHENTICATION_ERROR":
      return "server";
    case "INVALID_CREDENTIALS":
      return "invalid";
    default:
      return apiErr.status >= 500 ? "server" : "invalid";
  }
}
