export type UserSummary = {
  name: string;
  email: string;
  role: string;
  products: string;
  status: string;
  lastAccess: string;
  inviteStatus: string;
};

export type ListUsersResponse = UserSummary[];
