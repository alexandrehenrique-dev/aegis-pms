/**
 * Usuários do tenant — espelham os `mockUsers` de `users.ts` para que todos
 * os logins de teste apareçam no picker "Usuário existente" do ProductTeamPanel
 * e em qualquer listagem de membros. Formato: [userId, name, email, role,
 * products, status, lastAccess, inviteStatus].
 *
 * IDs com prefixo "kc-subj-" → usuários operacionais (Editor, PM, Viewer).
 * IDs com prefixo "u"         → usuários admin/plataforma (Super Admin,
 * Tenant Admin) que também precisam aparecer na lista para poder ser
 * atribuídos a produtos que eles mesmo gerenciam (o caso do super-admin
 * ter os seus próprios produtos, descrito em ADR-0018).
 */
export const usersRows = [
  // ── Admins de plataforma ──────────────────────────────────────────────────
  ["u5", "Super Admin",    "super-admin@byop.io", "Super Admin",  "Todos",          "ativo",     "há 5 min",  "—"],
  ["u1", "Ana Martins",    "admin@byop.io",       "Tenant Admin", "Todos",          "ativo",     "há 8 min",  "—"],
  // ── Operadores de produto ─────────────────────────────────────────────────
  ["u2", "Marina Costa",   "pm@byop.io",          "Product Manager", "Maestro Beton",  "ativo",  "há 12 min", "—"],
  ["u3", "Rafael Lima",    "editor@byop.io",      "Editor",       "Maestro Beton, Genesis", "ativo", "ontem", "—"],
  ["u6", "João Alves",     "viewer@byop.io",      "Viewer",       "WikiDev",        "ativo",     "3 dias",    "—"],
  // ── Usuários de e-mail antigo (aliases nos mocks legados) ─────────────────
  ["kc-subj-001", "Ana Martins (legacy)",  "ana@byop.com",    "Tenant Admin",    "Todos",           "ativo",    "há 8 min",  "—"],
  ["kc-subj-002", "Marina Costa (legacy)", "marina@byop.com", "Product Manager", "Maestro Beton",   "ativo",    "há 12 min", "—"],
  ["kc-subj-003", "Rafael Lima (legacy)",  "rafael@byop.com", "Editor",          "Maestro Beton, Genesis", "ativo", "ontem","—"],
  ["kc-subj-004", "João Alves (convidado)","joao@byop.com",   "Viewer",          "WikiDev",         "convidado","nunca",     "pendente"],
  ["kc-subj-005", "Beatriz Nunes",         "bea@byop.com",    "Editor",          "Conecta Talentos","bloqueado","21 dias",   "—"],
];
