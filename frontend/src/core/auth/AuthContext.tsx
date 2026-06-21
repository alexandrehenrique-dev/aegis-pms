import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import type { AuthUser, ProductOption, TenantOption } from "../../shared/types";
import { setAuthTokenProvider } from "../../shared/services/apiClient";
import { logApiCall } from "../../shared/services/devLog";
import { setNotificationsCurrentUser } from "../notifications/services/notificationsService";
import type { DeleteProductRequest, UpdateProductRequest } from "../../domains/products/contracts/requests";

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
  updateProduct: (productId: string, req: UpdateProductRequest) => void;
  removeProduct: (productId: string, req: DeleteProductRequest) => void;
  toggleFavorite: (productId: string) => void;
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
    setNotificationsCurrentUser(authUser?.id ?? null);
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

  /**
   * Editar Produto a partir de ProductSelectScreen (botão direito → menu de
   * contexto, mesma jornada de TenantSelectScreen para tenants suspensos) —
   * único jeito de agir sobre um produto bloqueado/sem módulos, que não pode
   * ser aberto. Ponto de integração real (Sprint 07): `PATCH
   * /api/v1/admin/products/{productId}` (docs/AEGIS_PMS_V1.md §8.4).
   */
  const updateProduct = (productId: string, req: UpdateProductRequest) => {
    if (!effectiveTenant) return;
    logApiCall("PATCH", `/api/v1/admin/products/${productId}`, req);
    const tenantId = effectiveTenant.id;
    const patch = { name: req.name, type: req.type, status: req.status, modulesList: req.modules, modules: req.modules.length };
    setUserProducts((prev) => ({ ...prev, [tenantId]: (prev[tenantId] ?? []).map((p) => (p.id === productId ? { ...p, ...patch } : p)) }));
    setSelectedProduct((sp) => (sp && sp.id === productId ? { ...sp, ...patch } : sp));
  };

  /**
   * Favoritar/desfavoritar produto (Sprint 15, Tarefa E.1) — antes só existia
   * a estrela visual em `isFavorite === true`, sem nenhum caminho para
   * desmarcar. Acesso via `ProductContextMenu` (botão direito), disponível a
   * qualquer papel que veja `/select-product`, não só quem gerencia produtos
   * (favoritar é preferência pessoal, não administração).
   */
  const toggleFavorite = (productId: string) => {
    if (!effectiveTenant) return;
    const tenantId = effectiveTenant.id;
    setUserProducts((prev) => ({
      ...prev,
      [tenantId]: (prev[tenantId] ?? []).map((p) => (p.id === productId ? { ...p, isFavorite: !p.isFavorite } : p)),
    }));
    setSelectedProduct((sp) => (sp && sp.id === productId ? { ...sp, isFavorite: !sp.isFavorite } : sp));
  };

  /** Exclusão lógica (soft delete) — `DELETE /api/v1/admin/products/{productId}` (docs/AEGIS_PMS_V1.md §8.4/§8.5: produto nunca é apagado fisicamente). */
  const removeProduct = (productId: string, req: DeleteProductRequest) => {
    if (!effectiveTenant) return;
    logApiCall("DELETE", `/api/v1/admin/products/${productId}`, req);
    const tenantId = effectiveTenant.id;
    setUserProducts((prev) => ({ ...prev, [tenantId]: (prev[tenantId] ?? []).filter((p) => p.id !== productId) }));
    setSelectedProduct((sp) => (sp && sp.id === productId ? null : sp));
  };

  const value = useMemo<AuthContextValue>(() => ({
    authUser, selectedTenant, selectedProduct, userTenants, userProducts,
    effectiveTenant, tenantProducts, effectiveProduct,
    login, logout, selectTenant: setSelectedTenant, selectProduct: setSelectedProduct,
    switchTenant, switchProduct, updateProduct, removeProduct, toggleFavorite,
  }), [authUser, selectedTenant, selectedProduct, userTenants, userProducts, effectiveTenant, tenantProducts, effectiveProduct]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
