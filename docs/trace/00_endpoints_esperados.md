# Trace 00 — Endpoints esperados pelo frontend, payloads completos e reordenação das sprints

> Gerado para servir de insumo à decisão de **ordem de execução** das sprints em `docs/sprints/`, e como **especificação literal** para quem for implementar o backend (via GPT ou não). Cobre, para cada endpoint: método + path, payload de request, payload de response, e o motivo pelo qual ele existe (qual tela do frontend depende dele e por quê). Os shapes de payload abaixo são os mesmos tipos TypeScript já definidos em `frontend/src/**/contracts/{requests,responses}.ts` pela Sprint 09 — não são suposições, são os contratos que os `services/*.ts` mock já implementam hoje sobre dados estáticos, prontos para apontar para `apiClient` (`frontend/src/shared/services/apiClient.ts`) quando o endpoint real existir (Sprint 07 faz essa troca).

Convenção de tipos abaixo: `string`, `number`, `boolean` como em TypeScript; `?` depois do nome = campo opcional; union types entre aspas separadas por `|`.

---

## A. Endpoints já definidos e implementáveis (Sprint 02 / `implementation/001`, Features 001–025)

Cobertura confirmada nos arquivos de `docs/sprints/sprint-02-fundacao-backend-gpt/`. Todos sob `http://localhost:8080/api/v1`, exceto Keycloak (porta **8282**).

| Endpoint | Método | Usado por (frontend) | Motivo |
|---|---|---|---|
| `/actuator/health`, `/actuator/info` | GET | Infra, monitoramento — sem tela própria | Healthcheck de orquestração (Docker/K8s), não consumido pela UI |
| `/me` | GET | `AuthContext`, qualquer tela que exiba o usuário logado, simulador de papéis | Fonte de verdade da identidade e do papel (`UserRole`) do usuário autenticado — substitui o `authUser` mockado de `core/auth/mocks/users.ts` |
| `/tenants` | GET, POST | `TenantSelectScreen.tsx` (lista, Super Admin), `CreateTenantWizardModal.tsx` (criação) | Lista todos os tenants da plataforma (Super Admin) ou os do usuário logado (demais papéis); cria um tenant novo |
| `/tenants/{tenantId}` | GET, PUT, DELETE | `TenantSettings.tsx`, `EditTenantModal.tsx`, `TenantContextMenu.tsx` (Excluir) | Ver/editar/excluir um tenant específico — ver payloads completos na Seção C |
| `/products` | GET, POST | `CreateProductForm.tsx`, `ProductSelectScreen.tsx`, `domains/products` | Lista produtos do tenant atual ou cria um novo produto (opcionalmente já vinculado a um `tenantId`, fluxo do wizard) |
| `/products/{productId}` | GET | Telas de detalhe de produto em todos os domínios | Detalhe de um produto específico |
| `/products/{productId}/modules/{moduleKey}/enable` | POST | `ModuleCatalog.tsx` (Sprint 05 liga o catálogo real) | Habilita um módulo do catálogo para o produto, respeitando dependências |
| `/products/{productId}/modules/{moduleKey}/disable` | POST | idem | Desabilita um módulo |
| `/products/{productId}/graph/nodes` | GET, POST | `domains/knowledge` (Knowledge Graph) | Lista/cria nós do grafo de conhecimento do produto |
| `/products/{productId}/graph/nodes/{nodeId}` | GET | idem | Detalhe de um nó |
| `/products/{productId}/graph/edges` | POST | idem | Cria uma relação entre dois nós |
| `/products/{productId}/graph/nodes/{nodeId}/neighbors` | GET | idem | Vizinhos diretos de um nó (1 hop) |
| `/products/{productId}/graph/nodes/{nodeId}/related` | GET | idem | Entidades relacionadas (N hops) |
| `http://localhost:8282/realms/aegis/...` | OIDC | Login real (Sprint 06) | Autenticação via Keycloak (Authorization Code + PKCE) |

---

## B. Domínios do frontend sem endpoint definido ainda — payloads completos

Para cada domínio: a(s) tela(s) que dependem dele, o(s) endpoint(s) propostos com payload completo (espelhando exatamente `frontend/src/domains/<dominio>/contracts/responses.ts`, já implementado e em uso hoje sobre mock), e o motivo. **Nenhum desses payloads é invenção desta análise — são os contratos TypeScript reais que já governam o frontend hoje.**

### B.1 `content`

Telas: `EditorialDashboard`, `ContentDataGrid`, `WorkflowBoard`, `ContentEditor`, `PublishPanel`, `VersionsPage`, `VersionCompareView`.

**`GET /api/v1/products/{productId}/content`** — lista de conteúdos (alimenta `ContentDataGrid`, `EditorialDashboard`).
```ts
type ContentRow = {
  title: string; type: string; lang: string; author: string;
  status: string; updatedAt: string; publication: string; version: string;
};
// Response: ContentRow[]
```
Motivo: `ContentDataGrid.tsx` precisa da tabela completa para filtrar por nome/tipo/idioma/autor/status; `EditorialDashboard` deriva contadores (rascunhos, em revisão, publicados) do mesmo conjunto.

**`GET /api/v1/products/{productId}/content/{contentId}`**, **`PUT .../content/{contentId}`** — detalhe e edição de um conteúdo. Motivo: `ContentEditor.tsx` carrega e salva um item específico (hoje só decorativo, sem persistência real).

**`POST /api/v1/products/{productId}/content/{contentId}/transition`** — payload `{ from: WFStatus; to: WFStatus; comment?: string }` onde `WFStatus = "Draft" | "In Review" | "Published" | "Archived"`. Motivo: `WorkflowBoard.tsx` move um card de uma coluna para outra via drag-and-drop; o backend precisa validar a transição (`wfAllowed` em `content.mocks.ts` já modela as transições permitidas: Draft→In Review, In Review→Draft/Published, Published→Archived, Archived→Draft) e quem pode publicar (hoje restrito a `super_admin`/`tenant_admin`/`product_manager` no client, mas a regra real deve vir do backend).

**`GET /api/v1/products/{productId}/content/{contentId}/versions`** — Motivo: `VersionsPage.tsx`/`VersionCompareView.tsx` exibem e comparam versões.

**`POST /api/v1/products/{productId}/content/{contentId}/publish`** — Motivo: `PublishPanel.tsx`.

**`GET /api/v1/products/{productId}/content/edit-events`** — response `string[]` (texto livre por enquanto, ex. "Editor criou rascunho da página Agenda."). Motivo: timeline de atividade editorial em `EditorialDashboard.tsx`. Candidato a ser absorvido pelo endpoint de auditoria (B.6) no futuro, em vez de existir isolado.

**`GET /api/v1/products/{productId}/content/workflow-items`** — response `WFItem[]` onde `WFItem = { id: string; title: string; type: string; lang: string; author: string; status: WFStatus; version: string }`. Motivo: estado inicial do quadro Kanban de `WorkflowBoard.tsx` (depois disso, as transições passam a vir de `transition` acima, não de um novo GET).

### B.2 `assets`

Telas: `AssetLibrary`, `AssetUploadScreen`, `AssetDetail`, `AssetMetadataForm`, `AssetTagManager`, `AssetUsageScreen`, `AssetPicker`.

**`GET /api/v1/products/{productId}/assets`** — alimenta `AssetLibrary` e `AssetPicker`.
```ts
type AssetSummary = {
  name: string; type: string; size: string; status: string;
  tags: string; usage: string; uploadedAt: string;
};
// Response: AssetSummary[]
```
**`POST /api/v1/products/{productId}/assets`** (multipart/form-data) — Motivo: `AssetUploadScreen.tsx`.

**`GET /api/v1/products/{productId}/assets/{assetId}`**, **`DELETE .../assets/{assetId}`** — Motivo: `AssetDetail.tsx`.

**`PUT /api/v1/products/{productId}/assets/{assetId}/metadata`** — Motivo: `AssetMetadataForm.tsx`.

**`GET /api/v1/products/{productId}/assets/{assetId}/usage`** — onde o asset está sendo usado (páginas, SEO). Motivo: `AssetUsageScreen.tsx`.

**`GET /api/v1/products/{productId}/asset-tags`**, **`POST .../asset-tags`**, **`DELETE .../asset-tags/{tag}`** — response/request `string[]`/`string`. Motivo: `AssetTagManager.tsx` ("Criar tag", "Mesclar tags", "Remover").

### B.3 `forms`

Telas: `FormsDashboard`, `FormsList`, `FormBuilder`, `FormPreviewFrame`, `SubmissionTable`, `SubmissionDetails`, `BasicFormAnalytics`, `PublicationPanel`.

**`GET /api/v1/products/{productId}/forms`** — alimenta `FormsList`/`FormsDashboard`.
```ts
type FormSummary = {
  name: string; type: string; status: string; responses: string;
  conversion: string; lastActivity: string; publication: string;
};
// Response: FormSummary[]
```
**`GET /api/v1/products/{productId}/forms/{formId}`**, **`PUT .../forms/{formId}`** — Motivo: `FormBuilder.tsx` carrega/salva a definição do formulário (campos, ordem, obrigatoriedade).

**`GET /api/v1/products/{productId}/forms/field-types`** — response `string[]` (ex.: "Texto", "Email", "Upload", "Consentimento LGPD"...). Motivo: paleta de campos disponíveis em `FormBuilder.tsx` — provavelmente um catálogo fixo do sistema, não por tenant, mas exposto via API para evitar hardcode no frontend.

**`GET /api/v1/products/{productId}/forms/{formId}/submissions`** e **`GET .../submissions`** (todas as submissions do produto) —
```ts
type SubmissionSummary = {
  date: string; name: string; email: string; source: string;
  status: string; owner: string; score: string;
};
// Response: SubmissionSummary[]
```
Motivo: `SubmissionTable.tsx` lista todas; `FormsList` mostra contagem por formulário.

**`GET /api/v1/products/{productId}/forms/{formId}/submissions/{submissionId}`** — Motivo: `SubmissionDetails.tsx`.

**`POST /api/v1/products/{productId}/forms/{formId}/publish`** — Motivo: `PublicationPanel.tsx`.

### B.4 `analytics`

Telas: `AnalyticsOverview`, `ProductHealthPanel`, `ContentAnalytics`, `FormAnalyticsModule`, `ChannelBreakdown`, `ReportGrid`, `TrendCards`, `AnalyticsStates`.

**`GET /api/v1/products/{productId}/analytics/kpis`** —
```ts
type AnalyticsKpi = { label: string; value: string; comparison: string; note: string; tone: string };
// Response: AnalyticsKpi[]
```
Motivo: grid de KPIs reaproveitado em `AnalyticsOverview` e outras telas (`KPIGrid` em `AnalyticsBits.tsx`).

**`GET /api/v1/products/{productId}/analytics/health`** —
```ts
type HealthSignal = { label: string; status: string; score: string; tone: string };
// Response: HealthSignal[]
```
Motivo: `ProductHealthPanel.tsx`.

**`GET /api/v1/products/{productId}/analytics/channels`** —
```ts
type ChannelRow = { name: string; visits: string; conversion: string; trend: string };
// Response: ChannelRow[]
```
Motivo: `ChannelBreakdown.tsx`.

**`GET /api/v1/products/{productId}/analytics/trends`**, **`GET .../analytics/reports`** — Motivo: `TrendCards.tsx`, `ReportGrid.tsx` (ainda sem contrato formal no frontend — telas hoje 100% estáticas, sem nem mock estruturado; precisarão de levantamento próprio antes de virar endpoint real).

### B.5 `users`

Telas: `UserTable`, `InviteUserDrawer`, `UserDetailPanel`.

**`GET /api/v1/tenants/{tenantId}/users`** —
```ts
type UserSummary = {
  name: string; email: string; role: string; products: string;
  status: string; lastAccess: string; inviteStatus: string;
};
// Response: UserSummary[]
```
Motivo: `UserTable.tsx`.

**`POST /api/v1/tenants/{tenantId}/users/invite`** —
```ts
type InviteUserRequest = {
  name: string; email: string; role: string;
  allowedProducts: string; message?: string;
};
// Response: UserSummary (status="convidado", inviteStatus="pendente")
```
Motivo: `InviteUserDrawer.tsx`.

**`GET /api/v1/tenants/{tenantId}/users/{userId}`**, **`PUT .../users/{userId}`** — Motivo: `UserDetailPanel.tsx` (hoje só leitura; "Editar permissões" ainda sem ação real).

### B.6 `audit`

Telas: `AuditTimeline`, `AuditEventDetail`.

**`GET /api/v1/tenants/{tenantId}/audit-events`** —
```ts
type AuditEvent = {
  actor: string; action: string; target: string;
  tenant: string; module: string; time: string; risk: string;
};
// Response: AuditEvent[]
```
Motivo: `AuditTimeline.tsx` (tela cheia e modo `compact` embutido em `UserDetailPanel.tsx`).

**`GET /api/v1/tenants/{tenantId}/audit-events/{eventId}`** — response inclui também um diff antes/depois e payload técnico (trace id, IP, user agent — hoje todos "futuro" no mock). Motivo: `AuditEventDetail.tsx`.

### B.7 `settings`

Telas: `SettingsOverview`, `ProductSettings`, `TenantSettings`, `PermissionMatrixView`, `RoleManagement`, `AccessPreviewPanel`, `SecuritySettingsPanel`.

**`GET /api/v1/products/{productId}/settings/overview`** —
```ts
type SettingCard = {
  name: string; description: string; status: string;
  lastUpdated: string; owner: string; risk: string;
};
// Response: SettingCard[]
```
Motivo: grid de cards (Produto, Tenant, Equipe, Permissões, Integrações, Segurança, Auditoria, SEO...) em `SettingsOverview.tsx`.

**`PUT /api/v1/products/{productId}/settings`** — Motivo: `ProductSettings.tsx`.

**`PUT /api/v1/tenants/{tenantId}`** — ver Seção C (mesmo endpoint do CRUD de tenant).

**`GET /api/v1/tenants/{tenantId}/roles`**, **`PUT .../roles`** — Motivo: `RoleManagement.tsx`.

**`GET /api/v1/tenants/{tenantId}/permission-matrix`** — Motivo: `PermissionMatrixView.tsx`, `AccessPreviewPanel.tsx` (simulação de papel sobre a matriz real).

### B.8 `dashboard`

Tela: `DashboardGlobal`.

**`GET /api/v1/dashboard/summary`** —
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
Motivo: KPIs do hub do tenant. Provavelmente um agregador que combina `/tenants/{id}`, `/products`, `/products/{id}/modules` e os endpoints de B.1–B.6 em vez de uma tabela própria — decisão de implementação a critério do backend, o frontend só precisa do shape acima.

### B.9 `knowledge` (parcialmente já cobre A, mas o frontend usa shapes mais ricos)

As telas `EntityDetails`, `EntitySearch`, `GraphCanvasView`, `RelationshipExplorer` já usam os endpoints de grafo da Seção A, mas com um shape mais específico do que `nodes`/`edges` genéricos:
```ts
type KGEntityType = "Tenant" | "Produto" | "Página" | "Asset" | "Formulário" | "Submission" | "Lead" | "Categoria" | "Tag" | "Autor" | "SEO";
type KGNode = { id: string; label: string; type: KGEntityType; status: string; x: number; y: number; props: { k: string; v: string }[] };
type KGEdge = { from: string; to: string; verb: string };
```
O campo `x`/`y` (posição no canvas) e `props` (lista chave/valor livre) precisam existir no backend ou ser calculados/armazenados em algum lugar — hoje são fixos no mock. Motivo: `GraphCanvasView.tsx` precisa de posição persistente para o layout não "saltar" a cada carregamento; `props` é o que populao painel de detalhe em `EntityDetails.tsx`.

**`GET /api/v1/products/{productId}/graph/orphans`** — nós sem nenhuma edge. Motivo: `OrphanEntityTable.tsx` (hoje 100% mockado, nem tipo formal ainda).

---

## C. Endpoints novos exigidos pelo fluxo de Super Admin (Sprint 09)

O fluxo "Super Admin cria Tenant → cria Produto → atribui Produto a um Usuário" (implementado como modal de 3 passos em `CreateTenantWizardModal.tsx`, ver `TenantSelectScreen.tsx`) expõe uma lacuna de **modelo de domínio**, não só de endpoint: hoje o `implementation/001` só tem `TenantMembership` (usuário↔tenant). Não existe o conceito de usuário atribuído a um **produto específico** com um papel específico.

### C.1 Tenants — CRUD completo

**`POST /api/v1/tenants`**
```ts
type CreateTenantRequest = {
  name: string; slug: string; plan: string;
  initialAdminEmail: string; // dispara convite ao Tenant Admin inicial
};
// Response: TenantOption (= { id, name, plan, productCount: 0, lastAccess: "—", status: "ativo" })
```
Motivo: passo 1 do wizard (`CreateTenantWizardModal.tsx`, step 1).

**`PUT /api/v1/tenants/{tenantId}`**
```ts
type UpdateTenantRequest = { name: string; plan: string; status: "ativo" | "suspenso" };
// Response: TenantOption atualizado
```
Motivo: `EditTenantModal.tsx`, acessível por botão "Editar" no card ou pelo menu de contexto (botão direito) em `TenantSelectScreen.tsx`.

**`DELETE /api/v1/tenants/{tenantId}`**
```ts
type DeleteTenantRequest = { confirmationText: string }; // nome do tenant, digitado pelo usuário para confirmar
// Response: 204 No Content
```
Motivo: ação destrutiva e **irreversível** — remove o tenant e cascateia para produtos/usuários associados. A UI sempre confirma antes via `ConfirmDialog` com aviso de severidade explícito ("Todos os N produtos... serão removidos imediatamente"), acessível pelo botão "Excluir" ou pelo menu de contexto. O backend deve auditar esta ação (Artigo X da Constituição) e, idealmente, validar `confirmationText === tenant.name` também no servidor, não só no client.

**`GET /api/v1/tenants`** (já citado na Seção A) — quando chamado por um `super_admin`, deve retornar **todos os tenants da plataforma**, não só os do usuário logado; é a mesma rota, o escopo do retorno é decidido pelo papel do token, não por um parâmetro separado.

### C.2 Atribuição de produto a usuário (`ProductAssignment`) — entidade nova

**`GET /api/v1/products/{productId}/users`** — lista usuários atribuídos a um produto específico.

**`POST /api/v1/products/{productId}/users`**
```ts
type AssignProductUserRequest = {
  tenantId: string; productId: string;
  userId?: string;       // usuário já existente no tenant
  inviteEmail?: string;  // OU convite de um usuário novo
  inviteName?: string;
  role: string;          // papel específico NESTE produto (pode diferir do papel no tenant)
};
// Response:
type ProductAssignmentSummary = {
  tenantId: string; productId: string; productName: string;
  userName: string; userEmail: string; role: string;
  status: "atribuido" | "convidado";
};
```
Motivo: passo 3 do wizard de onboarding (`CreateTenantWizardModal.tsx`, step 3) — exatamente um de `userId` ou `inviteEmail` deve vir preenchido, nunca os dois nem nenhum.

**`DELETE /api/v1/products/{productId}/users/{userId}`** — remove a atribuição.

Esta lacuna de modelo (`ProductAssignment`) deve ser resolvida **antes** ou **durante** a extensão da Sprint 02 que cobrir o domínio `users` (ver Seção B.5) — não é algo que o frontend possa contornar sozinho, porque é uma decisão de schema do backend: ou uma tabela nova `product_assignment(product_id, user_id, role)`, ou estender `TenantMembership` com `product_id` nulável.

### C.3 Configuração do Keycloak (preparação, sem endpoint novo)

Não é um endpoint, mas uma config exposta via `frontend/.env.example` e lida por `core/config/keycloakConfig.ts`:
```env
VITE_API_MODE=mock
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_KEYCLOAK_URL=http://localhost:8282
VITE_KEYCLOAK_REALM=aegis
VITE_KEYCLOAK_CLIENT_ID=aegis-web
```
Motivo: nenhuma porta deve ser hardcoded no código (já mudou uma vez nesta sessão de planejamento, de 8181 para 8282).

---

## D. Recomendação de ordenação das sprints

1. **01** — Refactor do frontend + setup git. *(concluída.)*
2. **02** (via GPT, `sprint-02-fundacao-backend-gpt/`) — Fundação do backend: Docker Compose, Postgres dedicado, Keycloak dedicado (porta 8282), Spring Boot, `/api/v1/me`, tenants, produtos, módulos, knowledge graph, build do React servido pelo Spring Boot.
3. **09** — Substitui 03 e 04. *(concluída — services/contracts em todos os 10 domínios existentes, fluxo Super Admin completo via modal de 3 passos em `/select-tenant`, `.env.example` com Keycloak na porta 8282, script de build, este relatório.)*
   - ~~03~~, ~~04~~ — obsoletas, mantidas só como histórico.
4. **10** — Refinamento de ações pendentes na UI (`10_refinamento_acoes_pendentes_ui.md`) — corrige `SelectLike`/`Field` para serem editáveis de verdade e liga ~140 botões sem `onClick` a alguma ação real. Não depende de backend.
5. **Nova sprint a criar (recomendado, ainda não escrita)** — "Extensão da Sprint 02: endpoints de domínio" — mesma estratégia via GPT em etapas pequenas, cobrindo toda a Seção B (`content`, `assets`, `forms`, `analytics`, `users`, `audit`, `settings`, `dashboard`) mais a Seção C inteira (`tenants` CRUD e `ProductAssignment`). Sem isso, a Sprint 07 (mock↔real) só liga de fato os domínios já cobertos pela fundação (tenants, produtos, módulos, knowledge graph) — os demais continuam mockados mesmo com a infraestrutura de toggle pronta. Esta sprint deve usar os payloads exatos desta Seção B/C como especificação, não reinventar shapes.
6. **05** — Roles/permissões por produto (liga `ModuleCatalog` ao backend real).
7. **06** — Keycloak login UI custom (login real via Authorization Code + PKCE, porta 8282).
8. **07** — Modo mock vs. real via `.env` (`VITE_API_MODE`) — só plenamente eficaz depois que a sprint #5 acima existir.
9. **08** — Gaps pós Sprint 19 (Figma Make) — reavaliar depois da 09, já que o gap do Super Admin "Criar Tenant" foi resolvido por ela.

## Observação sobre o login mock de Super Admin e demais papéis

Para permitir simular qualquer fluxo sem backend/Keycloak, `frontend/src/core/auth/mocks/users.ts` tem uma conta funcional por papel, todas com senha `senha123`:

| E-mail | Papel | Tenant(s) | Produto(s) |
|---|---|---|---|
| `super-admin@byop.io` | Super Admin | BYOP, Aegis Labs, Cliente Norte | todos |
| `admin@byop.io` | Tenant Admin | BYOP, Aegis Labs, Cliente Norte | todos (em BYOP) |
| `pm@byop.io` | Product Manager | BYOP | Maestro Beton, Aion Logbook, Eirene UI, Genesis |
| `editor@byop.io` | Editor | BYOP | Maestro Beton |
| `viewer@byop.io` | Viewer | BYOP | Maestro Beton |
| `blocked@byop.io` | Viewer (bloqueado) | — | — (login sempre falha com `error: "blocked"`, para testar esse estado de erro) |

O destino pós-login também é específico por papel (`core/permissions/roles.ts`, `getPostLoginLandingPath`): Super Admin e Tenant Admin caem no Dashboard (hub multi-produto); Product Manager cai na lista de produtos (`/products`, opera vários); Editor e Viewer caem direto no produto já selecionado (`/products/maestro-beton`, operam um produto por vez) — Viewer com as mesmas restrições de somente-leitura já aplicadas pelo `RequireRole`/`ReadOnlyBanner`.
