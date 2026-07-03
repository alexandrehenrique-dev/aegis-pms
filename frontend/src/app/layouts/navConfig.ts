import { BarChart3, Bug, FileText, Image, LayoutDashboard, Layers, Settings, ShieldCheck, Sparkles, Workflow, type LucideIcon } from "lucide-react";

export type NavItem = { path: string; icon: LucideIcon; label: string; moduleKey?: string };

/**
 * Main sidebar navigation, in display order. Replaces the old `nav`
 * Screen-keyed array. /admin/tenants (Super Admin) is intentionally NOT
 * here — gestão de tenants é cross-tenant por natureza, não mais uma seção
 * dentro do workspace do tenant atual. O único acesso é o card "Gestão de
 * Tenants" em DashboardGlobal (visível só para super_admin), e de lá em
 * diante a navegação é por BackLink ("Voltar para...") dentro do próprio
 * fluxo de administração — não pelo menu lateral regular.
 *
 * /products também foi removido por um motivo análogo: trocar/gerir o
 * conjunto de produtos do tenant é uma ação cross-produto, que já acontece
 * em /select-product (ProductSelectScreen, mesma tela que resolve o caso de
 * produto bloqueado/sem módulos com botão direito → Editar/Excluir) — não
 * faz sentido reaparecer como item fixo dentro do workspace de um produto
 * já selecionado.
 */
/**
 * `moduleKey` (Sprint 15, Tarefa B) mapeia 1:1 com as chaves de
 * `PRODUCT_TYPE_MODULE_DEFAULTS`/`ModuleOption.key` (`core/products/moduleDefaults.ts`)
 * — é o que `AppShell` usa para também checar `effectiveProduct.modulesList`
 * (via `resolveEnabledModules`) antes de mostrar o item, além do papel do
 * usuário. Item sem `moduleKey` (Dashboard, Configurações, Auditoria) é
 * estrutural do workspace e nunca depende de módulo — só de papel. Ao
 * adicionar um item novo aqui que corresponda a um módulo opcional, declare
 * o `moduleKey` correspondente ou ele ficará visível mesmo com o módulo
 * desabilitado no produto (era exatamente esse o gap do ADR-0015).
 */
export const nav: NavItem[] = [
  { path: "/dashboard", icon: LayoutDashboard, label: "Dashboard" },
  { path: "/pages", icon: Layers, label: "Páginas", moduleKey: "Páginas" },
  { path: "/content", icon: FileText, label: "Conteúdo", moduleKey: "Conteúdo" },
  { path: "/assets", icon: Image, label: "Assets", moduleKey: "Assets" },
  { path: "/forms", icon: Workflow, label: "Forms", moduleKey: "Forms" },
  { path: "/analytics", icon: BarChart3, label: "Analytics", moduleKey: "Analytics" },
  { path: "/knowledge", icon: Sparkles, label: "Knowledge Graph", moduleKey: "Knowledge Graph" },
  { path: "/settings", icon: Settings, label: "Configurações" },
  { path: "/audit", icon: ShieldCheck, label: "Auditoria" },
  // Sprint 23 — visível só para super_admin (roleVisibleNav), badge de
  // contagem de feedbacks "aberto" renderizado à parte em AppShell.
  { path: "/admin/feedback", icon: Bug, label: "Feedbacks" },
];

/** Module tabs shown under the breadcrumb for each top-level section. */
export const moduleTabs: Record<string, [string, string][]> = {
  "/content": [["/content", "Dashboard"], ["/content/list", "Lista"], ["/content/workflow", "Workflow"]],
  "/assets": [["/assets", "Biblioteca"], ["/assets/tags", "Tags"]],
  "/forms": [["/forms", "Dashboard"], ["/forms/submissions", "Submissions"], ["/forms/publication", "Publicação"], ["/forms/analytics", "Analytics"]],
  "/analytics": [["/analytics", "Overview"], ["/analytics/health", "Saúde"], ["/analytics/content", "Conteúdo"], ["/analytics/forms", "Forms"], ["/analytics/channels", "Canais"], ["/analytics/reports", "Relatórios"], ["/analytics/trends", "Tendências"]],
  "/knowledge": [["/knowledge", "Overview"], ["/knowledge/graph", "Graph"], ["/knowledge/relationships", "Relações"], ["/knowledge/search", "Busca"], ["/knowledge/orphans", "Órfãos"], ["/knowledge/insights", "Insights"]],
  "/settings": [["/settings", "Visão Geral"], ["/settings/product", "Produto"], ["/settings/tenant", "Tenant"], ["/settings/permissions", "Permissões"], ["/settings/security", "Segurança"]],
  "/users": [["/users", "Usuários"]],
  "/audit": [["/audit", "Timeline"]],
};

/** Returns the module-tab set (if any) that applies to the given pathname. */
export function tabsForPath(pathname: string): [string, string][] | undefined {
  const root = "/" + pathname.split("/")[1];
  return moduleTabs[root];
}
