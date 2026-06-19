import { BarChart3, Boxes, FileText, Image, LayoutDashboard, Settings, ShieldCheck, Sparkles, Workflow, type LucideIcon } from "lucide-react";

export type NavItem = { path: string; icon: LucideIcon; label: string };

/**
 * Main sidebar navigation, in display order. Replaces the old `nav`
 * Screen-keyed array. /admin/tenants (Super Admin) is intentionally NOT
 * here — gestão de tenants é cross-tenant por natureza, não mais uma seção
 * dentro do workspace do tenant atual. O único acesso é o card "Gestão de
 * Tenants" em DashboardGlobal (visível só para super_admin), e de lá em
 * diante a navegação é por BackLink ("Voltar para...") dentro do próprio
 * fluxo de administração — não pelo menu lateral regular.
 */
export const nav: NavItem[] = [
  { path: "/dashboard", icon: LayoutDashboard, label: "Dashboard" },
  { path: "/products", icon: Boxes, label: "Produtos" },
  { path: "/content", icon: FileText, label: "Conteúdo" },
  { path: "/assets", icon: Image, label: "Assets" },
  { path: "/forms", icon: Workflow, label: "Forms" },
  { path: "/analytics", icon: BarChart3, label: "Analytics" },
  { path: "/knowledge", icon: Sparkles, label: "Knowledge Graph" },
  { path: "/settings", icon: Settings, label: "Configurações" },
  { path: "/audit", icon: ShieldCheck, label: "Auditoria" },
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
