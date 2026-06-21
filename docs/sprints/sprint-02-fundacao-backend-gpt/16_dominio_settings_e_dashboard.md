# Etapa 16 — Domínios `settings` e `dashboard` (configurações, permissões, hub global)

> Cole este arquivo inteiro numa conversa nova do GPT. Pré-requisito: etapas 09 (Tenant CRUD), 10 (content), 11 (assets), 12 (forms), 13 (analytics), 14 (users) concluídas — `dashboard` agrega dados de todas elas.

## Contexto fixo

Telas `SettingsOverview`, `ProductSettings`, `TenantSettings`, `PermissionMatrixView`, `RoleManagement`, `AccessPreviewPanel`, `SecuritySettingsPanel` (domínio `settings`) e `DashboardGlobal` (domínio `dashboard`) — hoje 100% mock. Payloads conforme `docs/trace/00_endpoints_esperados.md` (Seções B.7 e B.8).

## Objetivo

Configurações de produto/tenant editáveis, matriz de permissões por role, e o agregador de KPIs do hub global.

## Tarefas

### A. `settings` — endpoints

```txt
GET  /api/v1/products/{productId}/settings/overview
PUT  /api/v1/products/{productId}/settings
PUT  /api/v1/tenants/{tenantId}                      (já existe, etapa 09 — reaproveitar)
GET  /api/v1/tenants/{tenantId}/roles
PUT  /api/v1/tenants/{tenantId}/roles
POST /api/v1/tenants/{tenantId}/roles/restore-defaults
GET  /api/v1/tenants/{tenantId}/permission-matrix
POST /api/v1/tenants/{tenantId}/permission-matrix/restore-defaults
POST /api/v1/tenants/{tenantId}/permission-matrix/preview      body: { role: string }
```

> **`restore-defaults` (roles e matriz) e `permission-matrix/preview` adicionados nesta revisão** — auditoria de cobertura encontrou que o frontend (`settingsService.restoreDefaultRoles`/`restoreDefaultPermissions`/`generateAccessPreview`) já chamava três caminhos que nenhuma etapa documentava, e que a Seção A (parágrafo abaixo) tinha uma afirmação **contraditória** sobre `AccessPreviewPanel` ("calcula no client, sem endpoint próprio") — corrigida agora. `restore-defaults` (ambos): reverte `RolePermission`/roles do tenant para o catálogo padrão de fábrica — idempotente, sem efeito sobre customizações de outros tenants. `permission-matrix/preview`: dado um `role`, devolve a navegação/capacidades resultantes **sem persistir nada** — é uma simulação somente leitura (não confundir com `PUT /permission-matrix`, que de fato altera).

Payloads:

```ts
type SettingCard = {
  name: string; description: string; status: string;
  lastUpdated: string; owner: string; risk: string;
};
// GET /settings/overview → SettingCard[] (Produto, Tenant, Equipe, Permissões, Integrações, Segurança, Auditoria, SEO...)
```

`roles`/`permission-matrix`: modelar como uma matriz `role × permissão` (ex.: `{ role: string; permissions: Record<string, boolean> }[]`), onde as permissões são as mesmas chaves já usadas em `core/permissions/roles.ts` no frontend (`roleVisibleNav`, `roleBlockedRoutePrefixes`) — não inventar uma nomenclatura de permissão diferente da que o frontend já usa, para a Sprint 05 (fora do GPT) conseguir ligar uma na outra sem tradução.

`AccessPreviewPanel.tsx` (simulação de "como ficaria a navegação para o papel X") consome `POST /permission-matrix/preview` (ver acima) — **não** calcula no client; o backend é a fonte de verdade de permissão (mesmo princípio já usado em `implementation/011`, "UI nunca decide permissão").

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

### D. Padrão de qualidade e entrega (obrigatório)

> Resumo — detalhe completo em `00_padrao_qualidade_e_arquitetura.md`.

- **Java 25** / **Spring Boot 4.1.x**. `roles`/`permission-matrix` precisam de persistência própria — criar `RolePermission` (entity: `id`, `tenantId`, `role`, `permissionKey`, `allowed`) + `RolePermissionRepository`, com Javadoc obrigatório na interface e em todo método. `settings/overview` e `dashboard/summary` são agregadores (sem entidade própria). Mapper via MapStruct (`RolePermissionMapper`). 100% de cobertura nas classes funcionais.
- Entregar em rodadas:
  1. `RolePermission` (entity) + `RolePermissionRepository` + testes `@DataJpaTest`.
  2. `RolePermissionMapper` (MapStruct) + testes de mapper.
  3. `SettingsService` (overview, update, roles/permission-matrix) + `DashboardService` (agregador da Seção B) + testes com mocks — incluindo o cenário de 403 para papel não autorizado.
  4. `SettingsController`, `DashboardController` (endpoints das Seções A/B) + testes `@WebMvcTest` + validação via `curl`.

## Critérios de aceite

- [ ] `settings/overview` retorna os cards esperados.
- [ ] `roles`/`permission-matrix` usam as mesmas chaves de permissão do frontend (`core/permissions/roles.ts`).
- [ ] Editar permissões como papel não autorizado retorna 403.
- [ ] `restore-defaults` (roles e matriz) reverte para o catálogo padrão e é idempotente.
- [ ] `permission-matrix/preview` devolve a simulação sem persistir nenhuma alteração.
- [ ] `dashboard/summary` retorna números reais (não fixos) agregados dos outros domínios.
- [ ] `mvn clean verify` confirma 100% de cobertura nas classes elegíveis desta etapa (JaCoCo).
- [ ] `RolePermissionRepository` tem Javadoc na interface e em todo método.

## Validação

> **Entrega via collection Postman, não só curl** (ver `00_padrao_qualidade_e_arquitetura.md`, Seção 11). Os `curl` abaixo são a especificação exata de cada request — adicione-os à pasta desta etapa em `aegis-postman-collection.json` (collection cumulativa, autenticação via `{{token}}` herdado da pasta "Auth") e devolva o JSON completo atualizado para download.

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/products/<productId>/settings/overview
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/tenants/<tenantId>/permission-matrix
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/dashboard/summary
```

## Artefato de continuidade — `SPRINT-RESULTADO.md`

> Ver `00_padrao_qualidade_e_arquitetura.md`, Seção 12. Antes do commit, gere/atualize `docs/sprints/sprint-02-fundacao-backend-gpt/SPRINT-RESULTADO.md` (arquivo inteiro, nunca um diff) com a entrada desta etapa (template fixo da Seção 12.2): classes criadas, endpoints confirmados, qualquer decisão que esta etapa deixou a seu critério (registre a escolha real), e retrofits pendentes para etapas futuras. É o que a próxima conversa do GPT vai receber em vez da memória que ela não tem.

## Commit sugerido

```bash
git add backend/
git commit -m "feat(backend): dominio settings com matriz de permissoes e agregador do dashboard"
```
