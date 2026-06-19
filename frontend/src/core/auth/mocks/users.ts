import type { AuthUser, LoginError, ProductOption, TenantOption } from "../../../shared/types";

export const mockUsers: Record<string, { password: string; user: AuthUser; error?: LoginError }> = {
  "super-admin@byop.io": { password: "senha123", user: { id: "u5", name: "Super Admin", email: "super-admin@byop.io", role: "super_admin", initials: "SA" } },
  "admin@byop.io": { password: "senha123", user: { id: "u1", name: "Ana Martins", email: "admin@byop.io", role: "tenant_admin", initials: "AM" } },
  "pm@byop.io": { password: "senha123", user: { id: "u2", name: "Marina Costa", email: "pm@byop.io", role: "product_manager", initials: "MC" } },
  "editor@byop.io": { password: "senha123", user: { id: "u3", name: "Rafael Lima", email: "editor@byop.io", role: "editor", initials: "RL" } },
  "viewer@byop.io": { password: "senha123", user: { id: "u6", name: "João Alves", email: "viewer@byop.io", role: "viewer", initials: "JA" } },
  "blocked@byop.io": { password: "senha123", user: { id: "u4", name: "Bloqueado", email: "blocked@byop.io", role: "viewer", initials: "BL" }, error: "blocked" },
};

export const mockTenantsByUser: Record<string, TenantOption[]> = {
  u5: [
    { id: "t1", name: "BYOP", plan: "Pro", productCount: 6, lastAccess: "há 8 min", status: "ativo" },
    { id: "t2", name: "Aegis Labs", plan: "Enterprise", productCount: 3, lastAccess: "ontem", status: "ativo" },
    { id: "t3", name: "Cliente Norte", plan: "Starter", productCount: 1, lastAccess: "3 dias", status: "suspenso" },
  ],
  u1: [
    { id: "t1", name: "BYOP", plan: "Pro", productCount: 6, lastAccess: "há 8 min", status: "ativo" },
    { id: "t2", name: "Aegis Labs", plan: "Enterprise", productCount: 3, lastAccess: "ontem", status: "ativo" },
    { id: "t3", name: "Cliente Norte", plan: "Starter", productCount: 1, lastAccess: "3 dias", status: "suspenso" },
  ],
  u2: [{ id: "t1", name: "BYOP", plan: "Pro", productCount: 6, lastAccess: "há 12 min", status: "ativo" }],
  u3: [{ id: "t1", name: "BYOP", plan: "Pro", productCount: 6, lastAccess: "ontem", status: "ativo" }],
  u6: [{ id: "t1", name: "BYOP", plan: "Pro", productCount: 6, lastAccess: "3 dias", status: "ativo" }],
};

export const mockProductsByUser: Record<string, Record<string, ProductOption[]>> = {
  u5: {
    t1: [
      { id: "p1", name: "Maestro Beton", type: "Site Institucional", status: "Ativo", modules: 6, isFavorite: true, isRecent: true },
      { id: "p2", name: "Aion Logbook", type: "Jogo / Experimento", status: "Pendente", modules: 4 },
      { id: "p3", name: "Eirene UI", type: "Design System", status: "Ativo", modules: 5, isRecent: true },
      { id: "p4", name: "Genesis", type: "Produto SaaS", status: "Ativo", modules: 7, isFavorite: true },
      { id: "p5", name: "WikiDev", type: "Knowledge Base", status: "Arquivado", modules: 3 },
      { id: "p6", name: "Conecta Talentos", type: "Portal", status: "Sem módulos", modules: 0 },
    ],
    t2: [
      { id: "p7", name: "Aegis Core", type: "Produto SaaS", status: "Ativo", modules: 4, isFavorite: true },
      { id: "p8", name: "Aegis Docs", type: "Knowledge Base", status: "Ativo", modules: 2 },
      { id: "p9", name: "Aegis Labs Site", type: "Site Institucional", status: "Pendente", modules: 1 },
    ],
    t3: [{ id: "p10", name: "Portal Norte", type: "Portal", status: "Ativo", modules: 3 }],
  },
  u1: {
    t1: [
      { id: "p1", name: "Maestro Beton", type: "Site Institucional", status: "Ativo", modules: 6, isFavorite: true, isRecent: true },
      { id: "p2", name: "Aion Logbook", type: "Jogo / Experimento", status: "Pendente", modules: 4 },
      { id: "p3", name: "Eirene UI", type: "Design System", status: "Ativo", modules: 5, isRecent: true },
      { id: "p4", name: "Genesis", type: "Produto SaaS", status: "Ativo", modules: 7, isFavorite: true },
      { id: "p5", name: "WikiDev", type: "Knowledge Base", status: "Arquivado", modules: 3 },
      { id: "p6", name: "Conecta Talentos", type: "Portal", status: "Sem módulos", modules: 0 },
    ],
    t2: [
      { id: "p7", name: "Aegis Core", type: "Produto SaaS", status: "Ativo", modules: 4, isFavorite: true },
      { id: "p8", name: "Aegis Docs", type: "Knowledge Base", status: "Ativo", modules: 2 },
      { id: "p9", name: "Aegis Labs Site", type: "Site Institucional", status: "Pendente", modules: 1 },
    ],
    t3: [{ id: "p10", name: "Portal Norte", type: "Portal", status: "Ativo", modules: 3 }],
  },
  u2: {
    t1: [
      { id: "p1", name: "Maestro Beton", type: "Site Institucional", status: "Ativo", modules: 6, isFavorite: true, isRecent: true },
      { id: "p2", name: "Aion Logbook", type: "Jogo / Experimento", status: "Pendente", modules: 4 },
      { id: "p3", name: "Eirene UI", type: "Design System", status: "Ativo", modules: 5 },
      { id: "p4", name: "Genesis", type: "Produto SaaS", status: "Ativo", modules: 7 },
    ],
  },
  u3: { t1: [{ id: "p1", name: "Maestro Beton", type: "Site Institucional", status: "Ativo", modules: 6, isFavorite: true }] },
  u6: { t1: [{ id: "p1", name: "Maestro Beton", type: "Site Institucional", status: "Ativo", modules: 6, isFavorite: true }] },
};
