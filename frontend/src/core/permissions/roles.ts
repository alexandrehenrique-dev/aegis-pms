import { getProductSlug } from "../../shared/utils/productSlugs";
import type { ProductOption, UserRole } from "../../shared/types";

/**
 * Para onde o usuário cai depois de selecionar um produto no login,
 * conforme o papel:
 * - super_admin / tenant_admin: hub do tenant (visão ampla, vários produtos).
 * - product_manager: lista de produtos disponíveis (opera vários produtos).
 * - editor / viewer: direto no produto já selecionado (operam um produto
 *   por vez) — viewer cai no mesmo lugar que editor; as restrições de
 *   somente-leitura já são aplicadas pelo RequireRole/ReadOnlyBanner.
 */
export function getPostLoginLandingPath(role: UserRole, product: ProductOption): string {
  if (role === "product_manager") return "/products";
  if (role === "editor" || role === "viewer") {
    const slug = getProductSlug(product.name);
    return slug ? `/products/${slug}` : "/dashboard";
  }
  return "/dashboard";
}

export const roleLabels: Record<UserRole, string> = {
  super_admin: "Super Admin",
  tenant_admin: "Tenant Admin",
  product_manager: "Product Manager",
  editor: "Editor",
  viewer: "Viewer",
};

export const roleDescriptions: Record<UserRole, string> = {
  super_admin: "Acesso total à plataforma",
  tenant_admin: "Administra tenant e equipe",
  product_manager: "Opera produtos e módulos",
  editor: "Cria e revisa conteúdo",
  viewer: "Somente leitura",
};

// Routes visible in the main sidebar nav per role (replaces the old `roleVisibleNav`
// Set<Screen> keyed by nav item key — now keyed by the nav item's route path).
export const roleVisibleNav: Record<UserRole, Set<string>> = {
  super_admin: new Set(["/dashboard", "/products", "/content", "/pages", "/assets", "/forms", "/analytics", "/knowledge", "/settings", "/audit"]),
  tenant_admin: new Set(["/dashboard", "/products", "/content", "/pages", "/assets", "/forms", "/analytics", "/knowledge", "/settings", "/audit"]),
  product_manager: new Set(["/dashboard", "/products", "/content", "/pages", "/assets", "/forms", "/analytics", "/knowledge", "/settings"]),
  editor: new Set(["/dashboard", "/products", "/content", "/pages", "/assets", "/forms", "/analytics"]),
  viewer: new Set(["/dashboard", "/products", "/content", "/pages", "/analytics"]),
};

// Route prefixes blocked per role (replaces the old `roleBlockedScreens` Set<Screen>).
// A route is blocked if it starts with any of these prefixes.
export const roleBlockedRoutePrefixes: Record<UserRole, string[]> = {
  super_admin: [],
  tenant_admin: ["/settings/security"],
  product_manager: [
    "/settings/tenant", "/users", "/settings/permissions", "/settings/roles", "/settings/access-preview",
    "/audit", "/settings/security", "/products/new", "/content/*/publish",
  ],
  editor: [
    "/settings", "/users",
    "/audit", "/products/new",
  ],
  viewer: [
    "/settings", "/users", "/audit", "/products/new",
    "/content/*/editor", "/forms/new", "/assets/upload", "/assets/*/metadata", "/assets/*/tags",
    "/content/*/workflow", "/knowledge/graph", "/knowledge/relationships", "/knowledge/entities",
    "/knowledge/search", "/knowledge/orphans", "/knowledge/insights",
  ],
};

/**
 * Returns true if a given pathname should be blocked for the given role.
 * Supports a single `*` wildcard segment in prefixes (e.g. "/assets/*\/tags").
 */
export function isRouteBlocked(role: UserRole, pathname: string): boolean {
  const prefixes = roleBlockedRoutePrefixes[role];
  return prefixes.some((prefix) => {
    if (!prefix.includes("*")) return pathname.startsWith(prefix);
    const pattern = "^" + prefix.split("*").map(escapeRegExp).join("[^/]+") ;
    return new RegExp(pattern).test(pathname);
  });
}

function escapeRegExp(s: string) {
  return s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

/**
 * Regra geral de permissão por widget (Sprint 13, Tarefa N) — `RequireRole`
 * só bloqueia a rota inteira; nenhum widget de dashboard verifica papel por
 * conta própria. Qualquer widget de dashboard que exiba dado ou ação fora do
 * escopo de `roleVisibleNav`/`roleBlockedRoutePrefixes` do papel atual deve
 * estar dentro de um `PermGate` (`app/guards/PermGate.tsx`) — vale para
 * dashboards futuros desde o início, não só para os já auditados nesta sprint
 * (`DashboardGlobal`, `EditorialDashboard`, `FormsDashboard`,
 * `AnalyticsOverview`, `ProductDashboard`, `KnowledgeOverview`).
 */
export type RoleAction = { label: string; path: string; desc: string };

export const roleActions: Record<UserRole, RoleAction[]> = {
  super_admin: [
    { label: "Configurar tenant BYOP", path: "/settings/tenant", desc: "Identidade, governança e limites" },
    { label: "Convidar Tenant Admin", path: "/users/invite", desc: "Adicionar administrador ao tenant" },
    { label: "Monitorar usuários e convites", path: "/users", desc: "Ver ativações pendentes" },
    { label: "Ver auditoria completa", path: "/audit", desc: "Eventos críticos e rastreabilidade" },
  ],
  tenant_admin: [
    { label: "Criar novo produto", path: "/products/new", desc: "Iniciar produto com módulos" },
    { label: "Liberar módulos do produto", path: "/products/maestro-beton/modules", desc: "Habilitar capacidades operacionais" },
    { label: "Convidar membro da equipe", path: "/users/invite", desc: "Adicionar Editor ou PM" },
    { label: "Ver auditoria do tenant", path: "/audit", desc: "Rastrear decisões e mudanças" },
  ],
  product_manager: [
    { label: "Revisar conteúdo em aprovação", path: "/content/workflow", desc: "Aprovar ou solicitar alteração" },
    { label: "Validar saúde do produto", path: "/analytics/health", desc: "KPIs por módulo" },
    { label: "Ver respostas dos formulários", path: "/forms/submissions", desc: "Qualificar e atribuir leads" },
    { label: "Acompanhar tendências", path: "/analytics/trends", desc: "Insights e anomalias" },
  ],
  editor: [
    { label: "Criar novo conteúdo", path: "/content", desc: "Rascunho para revisão posterior" },
    { label: "Ver rascunhos pendentes", path: "/content", desc: "Lista de conteúdos em edição" },
    { label: "Enviar conteúdo para revisão", path: "/content/workflow", desc: "Mover Draft → In Review" },
    { label: "Gerenciar assets do produto", path: "/assets", desc: "Imagens, vídeos e documentos" },
  ],
  viewer: [
    { label: "Ver relatórios do produto", path: "/analytics/reports", desc: "Relatórios gerados e disponíveis" },
    { label: "Analytics e performance", path: "/analytics", desc: "KPIs e evolução do produto" },
    { label: "Ver conteúdo publicado", path: "/content", desc: "Conteúdo disponível no produto" },
    { label: "Tendências e insights", path: "/analytics/trends", desc: "Variações e oportunidades" },
  ],
};
