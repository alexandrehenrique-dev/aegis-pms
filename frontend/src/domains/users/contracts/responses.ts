// ADR-0020: soft delete de usuário — "removido" não é permanente, a conta
// pode ser restaurada via `usersService.restoreUser`.
export type UserStatus = "ativo" | "bloqueado" | "convidado" | "removido";

export type UserSummary = {
  userId: string;
  name: string;
  email: string;
  role: string;
  products: string;
  status: UserStatus;
  lastAccess: string;
  inviteStatus: string;
};

export type ListUsersResponse = UserSummary[];
