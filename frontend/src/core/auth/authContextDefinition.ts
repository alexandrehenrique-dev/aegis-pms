import { createContext } from "react";
import type { AuthUser, ProductOption, TenantOption } from "../../shared/types";
import type { DeleteProductRequest, UpdateProductRequest } from "../../domains/products/contracts/requests";

export type AuthContextValue = {
  authUser: AuthUser | null;
  selectedTenant: TenantOption | null;
  selectedProduct: ProductOption | null;
  userTenants: TenantOption[];
  userProducts: Record<string, ProductOption[]>;
  effectiveTenant: TenantOption | null;
  tenantProducts: ProductOption[];
  effectiveProduct: ProductOption | null;
  login: (user: AuthUser, tenants: TenantOption[], products: Record<string, ProductOption[]>) => void;
  logout: () => void;
  selectTenant: (tenant: TenantOption | null) => void;
  selectProduct: (product: ProductOption | null) => void;
  switchTenant: (tenantId: string) => void;
  switchProduct: (productId: string) => void;
  updateProduct: (productId: string, req: UpdateProductRequest) => void;
  removeProduct: (productId: string, req: DeleteProductRequest) => void;
  toggleFavorite: (productId: string) => void;
};

export const AuthContext = createContext<AuthContextValue | undefined>(undefined);
