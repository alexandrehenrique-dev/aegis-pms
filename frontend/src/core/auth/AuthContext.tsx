import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
import type { AuthUser, ProductOption, TenantOption } from "../../shared/types";
import { setAuthTokenProvider } from "../../shared/services/apiClient";
import { logApiCall } from "../../shared/services/devLog";
import { setNotificationsCurrentUser } from "../notifications/services/notificationsService";
import type { DeleteProductRequest, UpdateProductRequest } from "../../domains/products/contracts/requests";
import { AuthContext, type AuthContextValue } from "./authContextDefinition";

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

  const login = useCallback((user: AuthUser, tenants: TenantOption[], products: Record<string, ProductOption[]>) => {
    setAuthUser(user);
    setUserTenants(tenants);
    setUserProducts(products);
  }, []);

  const logout = useCallback(() => {
    setAuthUser(null);
    setSelectedTenant(null);
    setSelectedProduct(null);
    setUserTenants([]);
    setUserProducts({});
  }, []);

  const effectiveTenant = useMemo(
    () => selectedTenant ?? (userTenants.length === 1 ? userTenants[0] : null),
    [selectedTenant, userTenants],
  );
  const tenantProducts = useMemo(
    () => (effectiveTenant ? userProducts[effectiveTenant.id] || [] : []),
    [effectiveTenant, userProducts],
  );
  const effectiveProduct = useMemo(() => {
    const activeProducts = tenantProducts.filter((p) => p.status !== "Arquivado" && p.modules > 0);
    return selectedProduct ?? (activeProducts.length === 1 ? activeProducts[0] : null);
  }, [selectedProduct, tenantProducts]);

  const switchTenant = useCallback((tenantId: string) => {
    const t = userTenants.find((t) => t.id === tenantId);
    if (t) { setSelectedTenant(t); setSelectedProduct(null); }
  }, [userTenants]);

  const switchProduct = useCallback((productId: string) => {
    if (!effectiveTenant) return;
    const p = (userProducts[effectiveTenant.id] || []).find((p) => p.id === productId);
    if (p) setSelectedProduct(p);
  }, [effectiveTenant, userProducts]);

  /**
   * Editar Produto a partir de ProductSelectScreen (botão direito → menu de
   * contexto, mesma jornada de TenantSelectScreen para tenants suspensos) —
   * único jeito de agir sobre um produto bloqueado/sem módulos, que não pode
   * ser aberto. Ponto de integração real (Sprint 07): `PATCH
   * /api/v1/admin/products/{productId}` (docs/AEGIS_PMS_V1.md §8.4).
   */
  const updateProduct = useCallback((productId: string, req: UpdateProductRequest) => {
    if (!effectiveTenant) return;
    logApiCall("PATCH", `/api/v1/admin/products/${productId}`, req);
    const tenantId = effectiveTenant.id;
    const patch = { name: req.name, type: req.type, status: req.status, modulesList: req.modules, modules: req.modules.length };
    setUserProducts((prev) => ({ ...prev, [tenantId]: (prev[tenantId] ?? []).map((p) => (p.id === productId ? { ...p, ...patch } : p)) }));
    setSelectedProduct((sp) => (sp && sp.id === productId ? { ...sp, ...patch } : sp));
  }, [effectiveTenant]);

  /**
   * Favoritar/desfavoritar produto (Sprint 15, Tarefa E.1) — antes só existia
   * a estrela visual em `isFavorite === true`, sem nenhum caminho para
   * desmarcar. Acesso via `ProductContextMenu` (botão direito), disponível a
   * qualquer papel que veja `/select-product`, não só quem gerencia produtos
   * (favoritar é preferência pessoal, não administração).
   */
  const toggleFavorite = useCallback((productId: string) => {
    if (!effectiveTenant) return;
    const tenantId = effectiveTenant.id;
    setUserProducts((prev) => ({
      ...prev,
      [tenantId]: (prev[tenantId] ?? []).map((p) => (p.id === productId ? { ...p, isFavorite: !p.isFavorite } : p)),
    }));
    setSelectedProduct((sp) => (sp && sp.id === productId ? { ...sp, isFavorite: !sp.isFavorite } : sp));
  }, [effectiveTenant]);

  /** Exclusão lógica (soft delete) — `DELETE /api/v1/admin/products/{productId}` (docs/AEGIS_PMS_V1.md §8.4/§8.5: produto nunca é apagado fisicamente). */
  const removeProduct = useCallback((productId: string, req: DeleteProductRequest) => {
    if (!effectiveTenant) return;
    logApiCall("DELETE", `/api/v1/admin/products/${productId}`, req);
    const tenantId = effectiveTenant.id;
    setUserProducts((prev) => ({ ...prev, [tenantId]: (prev[tenantId] ?? []).filter((p) => p.id !== productId) }));
    setSelectedProduct((sp) => (sp && sp.id === productId ? null : sp));
  }, [effectiveTenant]);

  const value = useMemo<AuthContextValue>(() => ({
    authUser, selectedTenant, selectedProduct, userTenants, userProducts,
    effectiveTenant, tenantProducts, effectiveProduct,
    login, logout, selectTenant: setSelectedTenant, selectProduct: setSelectedProduct,
    switchTenant, switchProduct, updateProduct, removeProduct, toggleFavorite,
  }), [
    authUser, selectedTenant, selectedProduct, userTenants, userProducts, effectiveTenant, tenantProducts, effectiveProduct,
    login, logout, switchTenant, switchProduct, updateProduct, removeProduct, toggleFavorite,
  ]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
