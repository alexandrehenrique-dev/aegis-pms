export type InviteUserRequest = {
  name: string;
  email: string;
  role: string;
  allowedProducts: string;
  message?: string;
};
