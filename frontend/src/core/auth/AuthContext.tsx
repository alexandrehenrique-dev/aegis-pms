import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import type { AuthUser, ProductOption, TenantOption } from "../../shared/types";
import { setAuthTokenProvider } from "../../shared/services/apiClient";

type AuthContextValue = {
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
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within an AuthProvider");
  return ctx;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [authUser, setAuthUser] = useState<AuthUser | null>(null);
  const [selectedTenant, setSelectedTenant] = useState<TenantOption | null>(null);
  const [selectedProduct, setSelectedProduct] = useState<ProductOption | null>(null);
  const [userTenants, setUserTenants] = useState<TenantOption[]>([]);
  const [userProducts, setUserProducts] = useState<Record<string, ProductOption[]>>({});

  // Registra a fonte do token do apiClient. Hoje e' um token mock derivado do
  // usuario logado; na Sprint 06 (Keycloak real) so' esta funcao muda.
  useEffect(() => {
    setAuthTokenProvider(() => (authUser ? `mock-token-${authUser.id}` : null));
  }, [authUser]);

  const login = (user: AuthUser, tenants: TenantOption[], products: Record<string, ProductOption[]>) => {
    setAuthUser(user);
    setUserTenants(tenants);
    setUserProducts(products);
  };

  const logout = () => {
    setAuthUser(null);
    setSelectedTenant(null);
    setSelectedProduct(null);
    setUserTenants([]);
    setUserProducts({});
  };

  const effectiveTenant = selectedTenant ?? (userTenants.length === 1 ? userTenants[0] : null);
  const tenantProducts = effectiveTenant ? userProducts[effectiveTenant.id] || [] : [];
  const activeProducts = tenantProducts.filter((p) => p.status !== "Arquivado" && p.modules > 0);
  const effectiveProduct = selectedProduct ?? (activeProducts.length === 1 ? activeProducts[0] : null);

  const switchTenant = (tenantId: string) => {
    const t = userTenants.find((t) => t.id === tenantId);
    if (t) { setSelectedTenant(t); setSelectedProduct(null); }
  };

  const switchProduct = (productId: string) => {
    if (!effectiveTenant) return;
    const p = (userProducts[effectiveTenant.id] || []).find((p) => p.id === productId);
    if (p) setSelectedProduct(p);
  };

  const value = useMemo<AuthContextValue>(() => ({
    authUser, selectedTenant, selectedProduct, userTenants, userProducts,
    effectiveTenant, tenantProducts, effectiveProduct,
    login, logout, selectTenant: setSelectedTenant, selectProduct: setSelectedProduct,
    switchTenant, switchProduct,
  }), [authUser, selectedTenant, selectedProduct, userTenants, userProducts, effectiveTenant, tenantProducts, effectiveProduct]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
