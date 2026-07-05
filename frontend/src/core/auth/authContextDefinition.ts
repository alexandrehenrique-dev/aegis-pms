import { createContext } from "react";
import type { AuthUser, ProductOption, TenantOption } from "../../shared/types";
import type { DeleteProductRequest, UpdateProductRequest } from "../../domains/products/contracts/requests";
import type { LoginResult } from "./services/authService";

export type AuthContextValue = {
  authUser: AuthUser | null;
  selectedTenant: TenantOption | null;
  selectedProduct: ProductOption | null;
  userTenants: TenantOption[];
  userProducts: Record<string, ProductOption[]>;
  effectiveTenant: TenantOption | null;
  tenantProducts: ProductOption[];
  effectiveProduct: ProductOption | null;
  /** Chamado pelo `LoginScreen` após `authService.login()` — em modo api busca `/me` + `/tenants`; em modo mock usa os dados já resolvidos em `LoginResult`. */
  initSession: (result: LoginResult) => Promise<void>;
  logout: () => void;
  selectTenant: (tenant: TenantOption | null) => void;
  selectProduct: (product: ProductOption | null) => void;
  switchTenant: (tenantId: string) => void;
  switchProduct: (productId: string) => void;
  updateProduct: (productId: string, req: UpdateProductRequest) => void;
  removeProduct: (productId: string, req: DeleteProductRequest) => void;
  toggleFavorite: (productId: string) => void;
  /**
   * Adiciona um produto dinamicamente ao contexto de autenticação — chamado após
   * `productsService.create()` para que o produto apareça imediatamente no sidebar
   * e em `effectiveProduct` sem necessidade de re-login. Se `tenantId` bater com
   * o tenant efetivo atual, também seleciona o produto como ativo.
   */
  addProduct: (tenantId: string, product: ProductOption) => void;
};

export const AuthContext = createContext<AuthContextValue | undefined>(undefined);
