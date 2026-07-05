export type InviteUserRequest = {
  name: string;
  email: string;
  role: string;
  allowedProducts: string;
  allowedProductIds?: string[];
  message?: string;
};
