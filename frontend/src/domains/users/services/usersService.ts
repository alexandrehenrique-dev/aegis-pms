import { usersRows } from "../mocks/users.mocks";
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
    const created: UserSummary = {
      name: req.name, email: req.email, role: req.role, products: req.allowedProducts,
      status: "convidado", lastAccess: "nunca", inviteStatus: "pendente",
    };
    usersStore.push(created);
    return created;
  },
};
