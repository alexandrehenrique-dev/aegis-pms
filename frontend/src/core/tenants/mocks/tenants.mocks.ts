import type { TenantOption } from "../../../shared/types";

// Lista canônica de tenants da plataforma para a tela de administração do
// Super Admin (/admin/tenants). Hoje espelha manualmente os tenants do mock
// de login (core/auth/mocks/users.ts, usuário super-admin@byop.io) — quando o
// backend existir, um único /api/v1/tenants substitui as duas fontes.
export const allTenants: TenantOption[] = [
  { id: "t1", name: "BYOP", plan: "Pro", productCount: 6, lastAccess: "há 8 min", status: "ativo" },
  { id: "t2", name: "Aegis Labs", plan: "Enterprise", productCount: 3, lastAccess: "ontem", status: "ativo" },
  { id: "t3", name: "Cliente Norte", plan: "Starter", productCount: 1, lastAccess: "3 dias", status: "suspenso" },
];
