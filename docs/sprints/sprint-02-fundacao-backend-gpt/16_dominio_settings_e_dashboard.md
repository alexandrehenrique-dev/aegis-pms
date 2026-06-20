# Etapa 16 — Domínios `settings` e `dashboard` (configurações, permissões, hub global)

> Cole este arquivo inteiro numa conversa nova do GPT. Pré-requisito: etapas 09 (Tenant CRUD), 10 (content), 11 (assets), 12 (forms), 13 (analytics), 14 (users) concluídas — `dashboard` agrega dados de todas elas.

## Contexto fixo

Telas `SettingsOverview`, `ProductSettings`, `TenantSettings`, `PermissionMatrixView`, `RoleManagement`, `AccessPreviewPanel`, `SecuritySettingsPanel` (domínio `settings`) e `DashboardGlobal` (domínio `dashboard`) — hoje 100% mock. Payloads conforme `docs/trace/00_endpoints_esperados.md` (Seções B.7 e B.8).

## Objetivo

Configurações de produto/tenant editáveis, matriz de permissões por role, e o agregador de KPIs do hub global.

## Tarefas

### A. `settings` — endpoints

```txt
GET /api/v1/products/{productId}/settings/overview
PUT /api/v1/products/{productId}/settings
PUT /api/v1/tenants/{tenantId}                      (já existe, etapa 09 — reaproveitar)
GET /api/v1/tenants/{tenantId}/roles
PUT /api/v1/tenants/{tenantId}/roles
GET /api/v1/tenants/{tenantId}/permission-matrix
```

Payloads:

```ts
type SettingCard = {
  name: string; description: string; status: string;
  lastUpdated: string; owner: string; risk: string;
};
// GET /settings/overview → SettingCard[] (Produto, Tenant, Equipe, Permissões, Integrações, Segurança, Auditoria, SEO...)
```

`roles`/`permission-matrix`: modelar como uma matriz `role × permissão` (ex.: `{ role: string; permissions: Record<string, boolean> }[]`), onde as permissões são as mesmas chaves já usadas em `core/permissions/roles.ts` no frontend (`roleVisibleNav`, `roleBlockedRoutePrefixes`) — não inventar uma nomenclatura de permissão diferente da que o frontend já usa, para a Sprint 05 (fora do GPT) conseguir ligar uma na outra sem tradução.

`AccessPreviewPanel.tsx` (simulação de "como ficaria a navegação para o papel X") consome o mesmo `GET /permission-matrix`, calculando a simulação no client — não precisa de endpoint próprio.

### B. `dashboard` — endpoint agregador

```txt
GET /api/v1/dashboard/summary
```

```ts
type DashboardSummaryResponse = {
  activeProducts: number; archivedProducts: number;
  pendingContent: number; pendingContentNeedingReview: number;
  openApprovals: number; criticalApprovals: number;
  formsReceived: number; formsReceivedToday: number;
  recentAssets: number; activeUsers: number; productManagers: number;
  conversionRate: string;
};
```

Implementar como agregador puro: consulta `products` (ativos/arquivados), `content` (pendentes/em revisão), `forms`/`submissions` (recebidos/hoje), `assets` (recentes), `users` (ativos/PMs) — sem tabela de fatos própria. Se a etapa 13 (analytics) já implementou algo equivalente, reaproveitar o mesmo código em vez de duplicar.

### C. Regras de negócio

- Só `SUPER_ADMIN`/`TENANT_ADMIN` podem editar `roles`/`permission-matrix` — qualquer outro papel recebe 403 mesmo que tente.
- `PUT /settings` em um produto que não pertence ao tenant do usuário retorna 404 (mesma regra de "não revelar existência" já usada em produto, etapa 06).
- `dashboard/summary` é escopado pelo(s) tenant(s) do usuário — `SUPER_ADMIN` vê agregado de todos os tenants (ou exige um `tenantId` de contexto — decisão de implementação do GPT, documentar a escolha).

## Critérios de aceite

- [ ] `settings/overview` retorna os cards esperados.
- [ ] `roles`/`permission-matrix` usam as mesmas chaves de permissão do frontend (`core/permissions/roles.ts`).
- [ ] Editar permissões como papel não autorizado retorna 403.
- [ ] `dashboard/summary` retorna números reais (não fixos) agregados dos outros domínios.

## Validação

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/products/<productId>/settings/overview
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/tenants/<tenantId>/permission-matrix
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/dashboard/summary
```

## Commit sugerido

```bash
git add backend/
git commit -m "feat(backend): dominio settings com matriz de permissoes e agregador do dashboard"
```
