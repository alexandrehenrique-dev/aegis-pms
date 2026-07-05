import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { Loader2 } from "lucide-react";
import type { AuthUser, ProductOption, TenantOption } from "../../shared/types";
import { setAuthTokenProvider, setRefreshHandler } from "../../shared/services/apiClient";
import { logApiCall } from "../../shared/services/devLog";
import { setNotificationsCurrentUser } from "../notifications/services/notificationsService";
import type { DeleteProductRequest, UpdateProductRequest } from "../../domains/products/contracts/requests";
import { IS_API_MODE } from "../../infra/apiMode";
import { AuthContext, type AuthContextValue } from "./authContextDefinition";
import { authService, type LoginResult } from "./services/authService";
import { meService } from "./services/meService";
import { tenantsService } from "../tenants/services/tenantsService";
import { productsService } from "../../domains/products/services/productsService";
import { toUserRole } from "./utils/roleMapper";
import { setCurrentProductId, setCurrentProductSlug } from "../products/currentProductContext";
import { slugify } from "../../shared/utils/slugify";

const ACCESS_TOKEN_KEY = "access_token";
const REFRESH_TOKEN_KEY = "refresh_token";

function initialsOf(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  const first = parts[0]?.[0] ?? "";
  const last = parts.length > 1 ? parts[parts.length - 1][0] : "";
  return (first + last).toUpperCase();
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [authUser, setAuthUser] = useState<AuthUser | null>(null);
  const [selectedTenant, setSelectedTenant] = useState<TenantOption | null>(null);
  const [selectedProduct, setSelectedProduct] = useState<ProductOption | null>(null);
  const [userTenants, setUserTenants] = useState<TenantOption[]>([]);
  const [userProducts, setUserProducts] = useState<Record<string, ProductOption[]>>({});
  // true enquanto restauramos sessão de um token existente no sessionStorage (F5/reabertura)
  const [restoring, setRestoring] = useState(() => IS_API_MODE && !!sessionStorage.getItem(ACCESS_TOKEN_KEY));
  const restorationAttempted = useRef(false);

  // Registra a fonte do token do apiClient a partir do sessionStorage —
  // preenchido por `initSession`/limpo por `logout`, independente do modo
  // (mock ou api). Registrado uma única vez: o valor é sempre lido em tempo
  // de chamada, não capturado no fechamento do efeito.
  useEffect(() => {
    setAuthTokenProvider(() => sessionStorage.getItem(ACCESS_TOKEN_KEY));
    setRefreshHandler(async () => {
      const refreshToken = sessionStorage.getItem(REFRESH_TOKEN_KEY);
      if (!refreshToken) return false;
      try {
        const tokens = await authService.refresh(refreshToken);
        sessionStorage.setItem(ACCESS_TOKEN_KEY, tokens.accessToken);
        sessionStorage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken);
        return true;
      } catch {
        sessionStorage.removeItem(ACCESS_TOKEN_KEY);
        sessionStorage.removeItem(REFRESH_TOKEN_KEY);
        setAuthUser(null);
        window.location.href = "/login";
        return false;
      }
    });
  }, []);

  // Restauração de sessão: se há token no sessionStorage (F5 / reabertura da aba),
  // rebusca /me para recriar o authUser sem exigir novo login.
  useEffect(() => {
    if (!IS_API_MODE || restorationAttempted.current) return;
    restorationAttempted.current = true;
    const token = sessionStorage.getItem(ACCESS_TOKEN_KEY);
    if (!token) { setRestoring(false); return; }

    meService.getMe()
      .then(async (me) => {
        setAuthUser({ id: me.subject, name: me.name, email: me.email, role: toUserRole(me.role), initials: initialsOf(me.name) });
        const tenants = await tenantsService.listTenants();
        setUserTenants(tenants);
        const products = await productsService.listProducts().catch(() => [] as Awaited<ReturnType<typeof productsService.listProducts>>);
        const visibleTenantIds = new Set(tenants.map((t) => t.id));
        const productsByTenant = products.reduce<Record<string, ProductOption[]>>((acc, product) => {
          if (!product.id || !product.tenantId || !visibleTenantIds.has(product.tenantId)) return acc;
          (acc[product.tenantId] ??= []).push({ id: product.id, key: product.key, name: product.name, type: product.type, status: product.status, modules: product.modules, modulesList: product.modulesList });
          return acc;
        }, {});
        // Enriquece productCount com o número real de produtos carregados —
        // o DTO do backend não retorna esse campo, então derivamos do que já temos.
        setUserTenants(tenants.map(t => ({ ...t, productCount: (productsByTenant[t.id] ?? []).length })));
        setUserProducts(productsByTenant);
      })
      .catch(() => {
        sessionStorage.removeItem(ACCESS_TOKEN_KEY);
        sessionStorage.removeItem(REFRESH_TOKEN_KEY);
        window.location.href = "/login";
      })
      .finally(() => setRestoring(false));
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    setNotificationsCurrentUser(authUser?.id ?? null);
  }, [authUser]);

  const initSession = useCallback(async (result: LoginResult) => {
    sessionStorage.setItem(ACCESS_TOKEN_KEY, result.accessToken);
    sessionStorage.setItem(REFRESH_TOKEN_KEY, result.refreshToken);

    if (IS_API_MODE) {
      const me = await meService.getMe();
      setAuthUser({ id: me.subject, name: me.name, email: me.email, role: toUserRole(me.role), initials: initialsOf(me.name) });

      // Carrega tenants e produtos de forma independente — se produtos falhar,
      // o usuário ainda consegue logar e ver seus tenants (degradação suave).
      const tenants = await tenantsService.listTenants();
      setUserTenants(tenants);

      const products = await productsService.listProducts().catch(() => [] as Awaited<ReturnType<typeof productsService.listProducts>>);
      const visibleTenantIds = new Set(tenants.map((tenant) => tenant.id));
      const productsByTenant = products.reduce<Record<string, ProductOption[]>>((acc, product) => {
        if (!product.id || !product.tenantId) return acc;
        // O escopo real vem do backend (`GET /products`, ADR-0019). Este
        // guard impede que o estado de navegação amplie escopo caso uma
        // resposta inconsistente traga produto de tenant invisível ao usuário.
        if (!visibleTenantIds.has(product.tenantId)) return acc;
        const tenantProducts = acc[product.tenantId] ?? (acc[product.tenantId] = []);
        tenantProducts.push({
          id: product.id,
          key: product.key,
          name: product.name,
          type: product.type,
          status: product.status,
          modules: product.modules,
          modulesList: product.modulesList,
        });
        return acc;
      }, {});
      // Enriquece productCount com o número real de produtos carregados —
      // o DTO do backend não retorna esse campo, então derivamos do que já temos.
      setUserTenants(tenants.map(tenant => ({ ...tenant, productCount: (productsByTenant[tenant.id] ?? []).length })));
      setUserProducts(productsByTenant);
    } else {
      setAuthUser(result.user ?? null);
      setUserTenants(result.tenants ?? []);
      setUserProducts(result.products ?? {});
    }
  }, []);

  const logout = useCallback(() => {
    const refreshToken = sessionStorage.getItem(REFRESH_TOKEN_KEY);
    authService.logout(refreshToken).catch(() => { /* best-effort — limpa a sessão local de qualquer forma */ });
    sessionStorage.removeItem(ACCESS_TOKEN_KEY);
    sessionStorage.removeItem(REFRESH_TOKEN_KEY);
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

  // Espelha `effectiveProduct.id` para services de baixo nível que hoje não
  // recebem `productId` como argumento (analyticsService, boa parte de
  // assetsService/knowledgeService) — ver core/products/currentProductContext.ts.
  useEffect(() => {
    setCurrentProductId(effectiveProduct?.id ?? null);
    setCurrentProductSlug(effectiveProduct ? slugify(effectiveProduct.name) : null);
  }, [effectiveProduct]);

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

  /**
   * Adiciona produto criado em tempo-real ao userProducts (e seleciona como ativo
   * se o tenant for o efetivo atual). Permite que super-admin/tenant-admin vejam o
   * produto recém-criado no sidebar imediatamente, sem re-login.
   */
  const addProduct = useCallback((tenantId: string, product: ProductOption) => {
    setUserProducts((prev) => ({
      ...prev,
      [tenantId]: [...(prev[tenantId] ?? []), product],
    }));
    if (effectiveTenant?.id === tenantId) {
      setSelectedProduct(product);
    }
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
    initSession, logout, selectTenant: setSelectedTenant, selectProduct: setSelectedProduct,
    switchTenant, switchProduct, updateProduct, removeProduct, toggleFavorite, addProduct,
  }), [
    authUser, selectedTenant, selectedProduct, userTenants, userProducts, effectiveTenant, tenantProducts, effectiveProduct,
    initSession, logout, switchTenant, switchProduct, updateProduct, removeProduct, toggleFavorite, addProduct,
  ]);

  if (restoring) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <Loader2 size={32} className="animate-spin text-primary" />
      </div>
    );
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
