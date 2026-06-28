import { usersRows } from "../mocks/users.mocks";
import { logApiCall } from "../../../shared/services/devLog";
import { IS_API_MODE } from "../../../infra/apiMode";
import { apiClient } from "../../../shared/services/apiClient";
import type { InviteUserRequest } from "../contracts/requests";
import type { ListUsersResponse, UserStatus, UserSummary } from "../contracts/responses";

const usersStore: UserSummary[] = usersRows.map(([name, email, role, products, status, lastAccess, inviteStatus]) => ({
  name, email, role, products, status: status as UserStatus, lastAccess, inviteStatus,
}));

// Papéis administrativos para a regra de "não se trancar para fora" (ADR-0020):
// nunca remover/bloquear o último admin ativo do tenant.
const ADMIN_ROLES = ["Tenant Admin", "Super Admin"];

export const usersService = {
  /**
   * Lista usuários do tenant. No modo mock atual não há conceito de "caller",
   * então a listagem retorna todos os usuários sem filtro client-side. ADR-0019
   * define que um PRODUCT_MANAGER só deve ver usuários com quem compartilha ao
   * menos um ProductAssignment — quando este service trocar de mock para
   * chamada real (Sprint de toggle mock↔real), o backend já devolve a lista
   * pré-filtrada por papel do caller; nenhum filtro adicional deve ser
   * introduzido aqui no frontend.
   */
  async listUsers(tenantId?: string): Promise<ListUsersResponse> {
    if (IS_API_MODE && tenantId) return apiClient.get<ListUsersResponse>(`/tenants/${tenantId}/users`);
    return usersStore;
  },

  /** ADR-0020: true quando `email` é o único administrador (Tenant/Super Admin) ativo do tenant. */
  async isLastActiveAdmin(email: string): Promise<boolean> {
    const activeAdmins = usersStore.filter((u) => ADMIN_ROLES.includes(u.role) && u.status === "ativo");
    return activeAdmins.length === 1 && activeAdmins[0].email === email;
  },

  async invite(req: InviteUserRequest, tenantId?: string): Promise<UserSummary> {
    if (IS_API_MODE && tenantId) return apiClient.post<UserSummary>(`/tenants/${tenantId}/users/invite`, req);
    logApiCall("POST", `/api/v1/tenants/${tenantId ?? "{tenantId}"}/users/invite`, req);
    const created: UserSummary = {
      name: req.name, email: req.email, role: req.role, products: req.allowedProducts,
      status: "convidado", lastAccess: "nunca", inviteStatus: "pendente",
    };
    usersStore.push(created);
    return created;
  },

  async resendInvite(email: string, tenantId?: string): Promise<void> {
    if (IS_API_MODE && tenantId) return apiClient.post(`/tenants/${tenantId}/users/${email}/resend-invite`);
    const u = usersStore.find((x) => x.email === email);
    if (!u) return;
    logApiCall("POST", `/api/v1/tenants/${tenantId ?? "{tenantId}"}/users/${email}/resend-invite`);
    u.inviteStatus = "pendente";
  },

  async blockUser(email: string, tenantId?: string): Promise<void> {
    if (IS_API_MODE && tenantId) return apiClient.post(`/tenants/${tenantId}/users/${email}/block`);
    const u = usersStore.find((x) => x.email === email);
    if (!u) return;
    logApiCall("POST", `/api/v1/tenants/${tenantId ?? "{tenantId}"}/users/${email}/block`);
    u.status = "bloqueado";
  },

  /** Soft delete — remove usuário do tenant (ADR-0020). Reversível via `restoreUser`. */
  async removeUser(email: string, tenantId?: string): Promise<void> {
    if (IS_API_MODE && tenantId) return apiClient.delete(`/tenants/${tenantId}/users/${email}`);
    const u = usersStore.find((x) => x.email === email);
    if (!u) return;
    logApiCall("DELETE", `/api/v1/tenants/${tenantId ?? "{tenantId}"}/users/${email}`);
    u.status = "removido";
  },

  /** Restaura acesso de um usuário removido ou bloqueado (ADR-0020). */
  async restoreUser(email: string, tenantId?: string): Promise<UserSummary> {
    if (IS_API_MODE && tenantId) return apiClient.post<UserSummary>(`/tenants/${tenantId}/users/${email}/restore`);
    const u = usersStore.find((x) => x.email === email);
    if (!u) throw { status: 404, message: `Usuário ${email} não encontrado.` };
    logApiCall("POST", `/api/v1/tenants/${tenantId ?? "{tenantId}"}/users/${email}/restore`);
    u.status = "ativo";
    return u;
  },
};
