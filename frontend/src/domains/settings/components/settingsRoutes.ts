const SETTINGS_ROUTES: Record<string, string> = {
  "Produto": "/settings/product",
  "Tenant": "/settings/tenant",
  "Equipe": "/users",
  "Permissões": "/settings/permissions",
  "Integrações": "/settings/security",
  "Segurança": "/settings/security",
  "Auditoria": "/audit",
  "SEO": "/settings/product",
  "Domínios futuros": "/settings/product",
};

export function routeForSettingCard(name: string): string {
  return SETTINGS_ROUTES[name] ?? "/settings";
}
