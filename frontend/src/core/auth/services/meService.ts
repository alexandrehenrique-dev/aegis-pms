import { apiClient } from "../../../shared/services/apiClient";

/** Espelha `br.com.byop.aegis.api.me.MeResponse` — sem tenants/produtos, ver `docs/sprints/integration/01_autenticacao_real.md` Seção D e F: essa lista vem de `tenantsService`/`productsService`, não de `/me`. */
export type MeResponse = {
  subject: string;
  email: string;
  username: string;
  name: string;
  role: string;
  tutorialCompleted: boolean;
};

/** Só chamado em modo api — em modo mock o `AuthProvider` já recebe usuário/tenants/produtos direto de `authService.login()`. */
export const meService = {
  getMe(): Promise<MeResponse> {
    return apiClient.get<MeResponse>("/me");
  },
};
