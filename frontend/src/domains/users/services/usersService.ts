import { usersRows } from "../mocks/users.mocks";
import { logApiCall } from "../../../shared/services/devLog";
import type { InviteUserRequest } from "../contracts/requests";
import type { ListUsersResponse, UserSummary } from "../contracts/responses";

const usersStore: UserSummary[] = usersRows.map(([name, email, role, products, status, lastAccess, inviteStatus]) => ({
  name, email, role, products, status, lastAccess, inviteStatus,
}));

export const usersService = {
  async listUsers(): Promise<ListUsersResponse> {
    return usersStore;
  },

  async invite(req: InviteUserRequest): Promise<UserSummary> {
    logApiCall("POST", "/api/v1/admin/users/invite", req);
    const created: UserSummary = {
      name: req.name, email: req.email, role: req.role, products: req.allowedProducts,
      status: "convidado", lastAccess: "nunca", inviteStatus: "pendente",
    };
    usersStore.push(created);
    return created;
  },

  async resendInvite(email: string): Promise<void> {
    const u = usersStore.find((x) => x.email === email);
    if (!u) return;
    logApiCall("POST", `/api/v1/admin/users/${email}/resend-invite`);
    u.inviteStatus = "pendente";
  },

  async blockUser(email: string): Promise<void> {
    const u = usersStore.find((x) => x.email === email);
    if (!u) return;
    logApiCall("POST", `/api/v1/admin/users/${email}/block`);
    u.status = "bloqueado";
  },
};
