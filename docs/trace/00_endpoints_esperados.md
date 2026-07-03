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
| `/products/{productId}/modules/{moduleKey}/enable` | POST | `ModuleCatalog.tsx` (Sprint 05 liga o catálogo real) | Habilita um módulo do catálogo para o produto, respeitando dependências (`KNOWLEDGE_GRAPH` exige `CONTENT` — `core/products/moduleDefaults.ts`; validação formalizada na etapa 06 nesta revisão) |
| `/products/{productId}/modules/{moduleKey}/disable` | POST | idem | Desabilita um módulo |
| `/products/{productId}/graph/nodes` | GET, POST | `domains/knowledge` (Knowledge Graph) | Lista/cria nós do grafo de conhecimento do produto — `GET` aceita `?q={label}` para busca por entidade (sustenta `EntityPicker.tsx` ao linkar uma referência durante a autoria, ver ADR-0016) |
| `/products/{productId}/graph/nodes/{nodeId}` | GET | idem | Detalhe de um nó |
| `/products/{productId}/graph/edges` | POST | idem | Cria uma relação entre dois nós — chamado automaticamente ao salvar um `Content` com referência inline `kg-ref` (ADR-0016, Sprint 16), nunca por desenho manual no canvas |
| `/products/{productId}/graph/nodes/{nodeId}/neighbors` | GET | idem | Vizinhos diretos de um nó (1 hop) |
| `/products/{productId}/graph/nodes/{nodeId}/related` | GET | idem | Entidades relacionadas (N hops) |
| `/products/{productId}/graph/orphans/{nodeId}/resolve` | POST | `OrphanEntityTable.tsx` | Aplica uma ação de curadoria (Arquivar/Vincular/Associar/Mesclar/Revisar) a um nó órfão — etapa 17 |
| `/products/{productId}/graph/orphans/resolve` | POST | idem | Mesma ação, em lote, para vários nós órfãos |
| `/products/{productId}/graph/insights/review` | POST | painel de insights do grafo | Marca um insight textual de sugestão de conexão como revisado |
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
**`POST /api/v1/products/{productId}/forms`** — cria uma definição nova em `Draft`. Motivo: `FormBuilder.tsx`/fluxo "novo formulário" precisa iniciar um form real sem seed manual; decisão registrada na execução da Sprint 13 porque a lista inicial só trazia `GET`/`PUT`.

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

**`GET /api/v1/products/{productId}/analytics/trends`** —
```ts
type TrendCard = {
  type: string; text: string; metric: string;
  severity: string; reviewed: boolean;
};
// Response: TrendCard[]
```
Motivo: `TrendCards.tsx` renderiza cards de insight com tipo, texto, métrica, severidade e estado de revisão. A ação recomendada e a rota de navegação são derivadas no próprio componente a partir de `severity`.

**`GET /api/v1/products/{productId}/analytics/reports`** —
```ts
type AnalyticsReport = {
  name: string; description: string; period: string;
  format: string; status: string; lastGenerated: string;
};
// Response: AnalyticsReport[]
```
Motivo: `ReportGrid.tsx` exibe cards de relatório; o frontend original usava arrays posicionais, formalizados aqui como campos nomeados para contrato JSON estável.

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

**`POST /api/v1/tenants/{tenantId}/users/{userId}/resend-invite`**, **`POST .../users/{userId}/block`** — adicionados nesta revisão (auditoria de cobertura): `usersService.resendInvite`/`blockUser` já chamavam esses caminhos sem documentação formal. Motivo: ações de `UserTable.tsx`/`UserDetailPanel.tsx` sobre um usuário pendente/ativo. Ver regras completas na etapa 14.

> **Nota de divergência de path:** o frontend mock hoje chama `/api/v1/admin/users/invite` e `/api/v1/admin/products/{productId}/assignments` (sem `tenantId`/sem escopo no path) — paths que nunca foram o padrão documentado aqui nem nas etapas. O canônico é sempre escopado (`/tenants/{tenantId}/users/...`, `/products/{productId}/users`); a reconciliação do mock para o path correto é tarefa da Sprint 07 (toggle mock↔real), não do backend.

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

**`GET /api/v1/tenants/{tenantId}/roles`**, **`PUT .../roles`**, **`POST .../roles/restore-defaults`** — Motivo: `RoleManagement.tsx`. `restore-defaults` adicionado nesta revisão (cobria `settingsService.restoreDefaultRoles`, sem endpoint documentado antes).

**`GET /api/v1/tenants/{tenantId}/permission-matrix`**, **`POST .../permission-matrix/restore-defaults`**, **`POST .../permission-matrix/preview`** — Motivo: `PermissionMatrixView.tsx`; `AccessPreviewPanel.tsx` consome **`preview`**, não calcula no client (correção desta revisão — afirmação anterior de "calcula no client" estava errada e contradizia o `logApiCall` real do frontend).

### B.8 `feedback`

Telas: `FeedbackModal.tsx` e Central de Ajuda.

**`POST /api/v1/feedback`** — cria feedback real reportado por qualquer usuário autenticado.
```ts
type CreateFeedbackRequest = {
  productId?: string;
  category: string;
  priority: string;
  description: string;
  screenName?: string;
  attachmentAssetId?: string;
};
```
Response: `FeedbackSummary`, usando `id` legível `AGS-####` como identificador principal.

**`GET /api/v1/feedback`** — lista todos os feedbacks de todos os tenants. Uso exclusivo de `SUPER_ADMIN`.

**`GET /api/v1/tenants/{tenantId}/feedback`** — lista feedbacks de um tenant. Uso por `SUPER_ADMIN` ou `TENANT_ADMIN` do próprio tenant.

**`PUT /api/v1/feedback/{feedbackId}/status`** — atualiza status por `feedbackId` legível (`AGS-####`), exclusivo de `SUPER_ADMIN`.
```ts
type UpdateFeedbackStatusRequest = { status: string };
type FeedbackSummary = {
  id: string;
  category: string;
  priority: string;
  description: string;
  status: string;
  createdBySubject: string;
  tenantId: string;
  productId?: string;
  createdAt: string;
  attachmentAssetId?: string;
};
```

Motivo: o modal de "Reportar problema" deixou de gerar ID fake em memória. O anexo não tem upload próprio: o frontend deve primeiro usar `POST /api/v1/products/{productId}/assets` e depois enviar o `attachmentAssetId` no `POST /feedback`.

### B.9 `dashboard`

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

### B.10 `knowledge` (parcialmente já cobre A, mas o frontend usa shapes mais ricos)

As telas `EntityDetails`, `EntitySearch`, `GraphCanvasView`, `RelationshipExplorer` já usam os endpoints de grafo da Seção A, mas com um shape mais específico do que `nodes`/`edges` genéricos:
```ts
type KGEntityType = "Tenant" | "Produto" | "Página" | "Asset" | "Formulário" | "Submission" | "Lead" | "Categoria" | "Tag" | "Autor" | "SEO";
type KGNode = { id: string; label: string; type: KGEntityType; status: string; x: number; y: number; props: { k: string; v: string }[] };
type KGEdge = { from: string; to: string; verb: string };
```
O campo `x`/`y` (posição no canvas) é persistido em `graph_nodes`; `props` é derivado de `metadataJson` como lista chave/valor ordenada. Motivo: `GraphCanvasView.tsx` precisa de posição persistente para o layout não "saltar" a cada carregamento; `props` é o que popula o painel de detalhe em `EntityDetails.tsx`.

> **Correção de aderência (ADR-0016, Sprint 16):** uma auditoria de uso real encontrou que `knowledgeService.createEdge`/`ensureNodeForContent` (frontend) nunca eram chamadas por nenhum fluxo de autoria de conteúdo, e que `listNodes()`/`listEdges()` liam de um array de mock estático diferente do array que essas duas funções mutavam — ou seja, mesmo uma chamada manual a `createEdge` nunca apareceria em `GraphCanvasView`. A Sprint 16 corrige as duas coisas no frontend (mock); o contrato dos endpoints acima não muda, mas vale registrar que a criação de edge **só** deve ocorrer a partir do salvamento de um `Content` com referência inline `kg-ref` (ver `EntityPicker.tsx` integrado ao editor) — nunca por uma ação de desenho manual no `GraphCanvasView`, que é só visualização/curadoria.

**`PATCH /api/v1/products/{productId}/graph/nodes/{nodeId}/position`** —
```ts
type UpdateGraphNodePositionRequest = { x: number; y: number };
```
Atualiza a posição persistida do nó no canvas.

**`GET /api/v1/products/{productId}/graph/orphans`** — retorna `KGNode[]` acionável para `OrphanEntityTable`: nós sem nenhuma edge e ainda não marcados como resolvidos por curadoria.

```ts
type OrphanEntityRow = KGNode & {
  action?: "Arquivar" | "Vincular" | "Associar" | "Mesclar" | "Revisar";
};
```

**`POST /api/v1/products/{productId}/graph/orphans/{nodeId}/resolve`** —
```ts
type ResolveGraphOrphanRequest = { action: "Arquivar" | "Vincular" | "Associar" | "Mesclar" | "Revisar" };
```
Como o contrato desta etapa não possui `targetNodeId`, a resolução não cria edge real. O backend registra curadoria idempotente em `metadataJson` (`orphanResolved`, `orphanAction`, `status`) e o nó deixa de aparecer em `GET /graph/orphans`.

**`POST /api/v1/products/{productId}/graph/orphans/resolve`** —
```ts
type ResolveGraphOrphansRequest = { ids: string[]; action?: "Arquivar" | "Vincular" | "Associar" | "Mesclar" | "Revisar" };
```
Aplica a mesma curadoria em lote; quando `action` é omitida, usa `Revisar`.

**`POST /api/v1/products/{productId}/graph/insights/review`** —
```ts
type ReviewGraphInsightRequest = { text: string };
type GraphInsightReviewSummary = { id: string; text: string; reviewed: true };
```
Marca um insight textual como revisado de forma idempotente, usando hash determinístico do texto por produto.

### B.11 Domínios de negócio específicos por produto (Sprint 11 — registrado, não implementar ainda)

Levantamento feito a partir de 6 contratos de negócio reais dos primeiros produtos a serem hospedados no Aegis (Maestro Beton, Conecta Talentos, CMSS, Alexandre Dev, Loki, WikiDev — ver `docs/sprints/11_modelo_paginas_blocos_knowledge_graph_e_mocks_produtos.md`). Diferente do restante da Seção B, estes domínios **não têm etapa criada ainda** na Sprint 02 — ficam registrados aqui para uma extensão futura, numerados como próximas etapas (23+) quando forem implementados:

- **`JobPosting`/`CandidateSubmission`/`Lead`** (Conecta Talentos): vaga como conteúdo estruturado publicável (campos: cidade, modalidade, tipo, salário, requisitos, benefícios, diferenciais, status; workflow próprio Draft→Review→Published→Archived); candidatura com upload de currículo (reaproveita o domínio `asset`, com `targetType: "candidate_submission"`, amarrada a um `jobId`); lead comercial simples (empresa, responsável, contato, mensagem).
- **`Comment`/`ContributorApplication`/`ContentSuggestion`/`BugReport` + integração Telegram** (WikiDev): fecha os module keys `COMMENTS`/`CONTRIBUTORS` que já existem no enum de módulos (etapa 06) mas não têm nenhuma entidade implementada. `Comment` precisa de moderação (`pending`/`approved`/`rejected`); os outros três são formulários simples que, além de persistir no banco, disparam notificação a um canal Telegram (tipo `report_bug`/`content_suggestion`/`contributor_application`).
- **Tipos de conteúdo adicionais da Loki**: `manifesto`, `reflection`, `poem`, `book`, `playlist` como `type` válidos dentro do domínio `content` já existente (etapa 10) — não são entidades novas, são variações de `Content` com campos extras guardados em `metadataJson` (`Book` precisa de `isbn`/`pdfUrl`/`epubUrl`/`amazonUrl`/`physicalAvailable`; `Poem`/`Manifesto` podem referenciar uma `MusicReference` via o Knowledge Graph, já coberto pela Seção A — `nodeType: MUSIC_REFERENCE`, `edgeType: INSPIRED_BY`/`PART_OF`).

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

## D. Endpoints novos da Sprint 11 (modelo de páginas e correção do Knowledge Graph)

Resultado da simulação dos fluxos do PMS contra os 6 contratos de produto reais (`docs/sprints/11_modelo_paginas_blocos_knowledge_graph_e_mocks_produtos.md`). **Já implementados nas etapas correspondentes da Sprint 02 (GPT)** — listados aqui só para consolidar no mesmo lugar que os demais endpoints deste relatório.

### D.1 Domínio `pages` (novo — etapa 21)

Faltava um jeito de modelar uma página institucional como lista ordenada de seções tipadas (`hero`, `card-list`, `gallery`, `contact`...) — `content` (Seção B.1) modela artigo/post linear, não página composta. Necessário para Maestro Beton, CMSS, Conecta Talentos (home) e Alexandre Dev (home).

```txt
GET    /api/v1/products/{productId}/pages
GET    /api/v1/products/{productId}/pages/{pageId}
POST   /api/v1/products/{productId}/pages
PUT    /api/v1/products/{productId}/pages/{pageId}
DELETE /api/v1/products/{productId}/pages/{pageId}
POST   /api/v1/products/{productId}/pages/{pageId}/sections
PUT    /api/v1/products/{productId}/pages/{pageId}/sections/{sectionId}
DELETE /api/v1/products/{productId}/pages/{pageId}/sections/{sectionId}
PUT    /api/v1/products/{productId}/pages/{pageId}/sections/reorder
```

Catálogo fechado de `BlockType` (atualizado pela Sprint 13 — `footer`/`navbar` saíram, `audio`/`social-links`/`form`/`download` entraram, ver D.6 — e nesta revisão, `video`/`video-gallery`): `hero, text, rich-text, two-column, image, image-text, feature-grid, card-list, gallery, timeline, event-list, cta-section, faq, contact, form, download, audio, social-links, video, video-gallery`. Especificação completa de payloads e regras de validação por tipo de bloco: `docs/sprints/sprint-02-fundacao-backend-gpt/21_dominio_pages_secoes_e_blocos.md`.

**`video`/`video-gallery` (adicionados nesta revisão)** — caso de uso real confirmado (Maestro Beton, `docs/AEGIS_PMS_V1.md` §17) e categoria de asset `"video"` já provisionada (Seção B.2) sem nenhum bloco que a usasse:
```ts
type VideoContent = { title?: string; source: "upload" | "youtube"; fileAssetId: string; youtubeUrl: string; autoplay: boolean };
type VideoGalleryItem = { title: string; source: "upload" | "youtube"; fileAssetId: string; youtubeUrl: string };
type VideoGalleryContent = { items: VideoGalleryItem[] };
```
Motivo: mesmo padrão do bloco `audio` (Seção D.6) — card único (`video`) ou lista (`video-gallery`), upload OU link externo (YouTube em vez de Spotify). Sem endpoint próprio — é `contentJson` de uma `PageSection` como qualquer outro `BlockType`.

### D.2 Preview leve de nó do grafo (extensão da etapa 17 — caso WikiDev)

A WikiDev precisa de um popup leve ao passar o mouse sobre uma referência dentro do corpo de um artigo — diferente da tela de detalhe completa do nó.

```txt
GET /api/v1/products/{productId}/graph/nodes/{nodeId}/preview
```
```ts
type GraphNodePreview = { id: string; label: string; type: string; summary: string; difficulty?: "beginner"|"intermediate"|"advanced"; thumbnail?: string };
```

### D.3 `summary`/`difficultyLevel` em `Content` (extensão da etapa 10)

`ContentRow` (Seção B.1) ganhou dois campos opcionais — `summary: string` e `difficultyLevel: "beginner"|"intermediate"|"advanced"` — como campos de primeira classe (não metadata solta), porque a WikiDev precisa filtrar/ordenar artigos por dificuldade.

### D.4 Regra de ativação do Knowledge Graph por tipo de produto

A criação de produto hoje não permite selecionar módulos (`CreateProductForm.tsx` usa uma lista fixa, sem Knowledge Graph) e o seed da etapa 20 habilitava `KNOWLEDGE_GRAPH` em produtos que não precisam dele (ex.: Maestro Beton). Regra correta, definida na Sprint 11:

| Tipo de produto | Knowledge Graph |
|---|---|
| Site Institucional (Maestro Beton, CMSS) | Desligado por padrão |
| Portal (Conecta Talentos) | Desligado por padrão (opcional no blog) |
| Knowledge Base (WikiDev) | **Ligado por padrão** |
| Portfolio (Alexandre Dev) | Desligado por padrão, ligável manualmente |
| Library/Books/Music (Loki) | **Ligado por padrão** |

### D.5 Estratégia de armazenamento de assets (Sprint 13)

Escolhida por produto, no momento da criação (`Product.assetStorageStrategy: "local"|"s3"`, default `"local"`). Especificação completa (Strategy pattern, provisionamento de pastas, riscos, melhorias futuras): `docs/sprints/sprint-02-fundacao-backend-gpt/11_dominio_assets.md`, Seção D. A pasta raiz de storage local é criada automaticamente — em Java multiplataforma na etapa 04 (sem Docker) e via volume Docker nomeado na etapa 19 (containerizado) — nunca uma etapa manual.

```txt
GET /api/v1/assets/{assetId}/resolve
```
```ts
type ResolvedAsset = { id: string; url: string; expiresAt?: string; contentType: string };
```
Motivo: qualquer domínio que referencia um asset (`pages`, `content`, `forms`) guarda só o `assetId` (UUID) — nunca uma URL ou caminho — e resolve a URL de uso (local ou S3 pré-assinada) só na hora de renderizar, via este endpoint.

### D.6 Domínio `pages` — `ProductGlobals` (navbar, footer, redes sociais)

Navbar, footer e redes sociais deixaram de ser seção de página e passaram a ser configuração única por produto. Especificação completa: `docs/sprints/sprint-02-fundacao-backend-gpt/21_dominio_pages_secoes_e_blocos.md`, Seção F.

```txt
GET /api/v1/products/{productId}/globals
PUT /api/v1/products/{productId}/globals
```
```ts
type ProductGlobals = {
  navbar: { logoAssetId?: string; links: { label: string; href: string }[] };
  footer: { addressText?: string; links: { label: string; href: string }[] };
  socialLinks: { platform: string; href: string }[];
  floatingWhatsapp?: { enabled: boolean; number: string; message: string };
};
```
Motivo: `BlockRenderer`/preview do frontend renderiza isso automaticamente no topo/rodapé de qualquer página do produto, sem precisar que cada página declare seu próprio navbar/footer.

### D.7 Domínio `forms` — entrega de respostas por múltiplos canais

```txt
PUT /api/v1/products/{productId}/forms/{formId}/delivery
```
```ts
type FormDeliveryChannel = { type: "email"|"whatsapp"|"telegram"|"webhook"; enabled: boolean; config: Record<string,string> };
type UpdateFormDeliveryRequest = { channels: FormDeliveryChannel[] };
```
Motivo: cliente pode querer receber submissions por mais de um canal simultâneo (não só notificação interna). Especificação completa: `docs/sprints/sprint-02-fundacao-backend-gpt/12_dominio_forms.md`, Seção D.

---

## F. Domínio `notification` (Sprint 14 — onboarding real + notificações direcionadas)

Onboarding deixou de ser um modal client-side (`localStorage`) e passou a ser **um tipo de notificação** entre vários — mesmo mecanismo serve para avisos de feature, manutenção, etc. Especificação completa: `docs/sprints/backend/25_dominio_notification.md`.

```txt
GET  /api/v1/notifications/mine
GET  /api/v1/notifications/mine/pending-modal
POST /api/v1/notifications/{notificationId}/mark-shown
POST /api/v1/notifications/{notificationId}/mark-read
POST /api/v1/notifications                                  (Super Admin)
GET  /api/v1/notifications                                  (Super Admin)
```

```ts
type NotificationWithStatus = {
  id: string; type: "ONBOARDING" | "FEATURE" | "WARNING" | "MAINTENANCE" | "GENERAL";
  title: string; bodyMarkdown: string; presentationMode: "MODAL_ONCE" | "BELL_ONLY";
  createdAt: string; autoShown: boolean; read: boolean; readAt?: string;
};
type CreateNotificationRequest = {
  type: "ONBOARDING" | "FEATURE" | "WARNING" | "MAINTENANCE" | "GENERAL";
  title: string; bodyMarkdown: string; presentationMode: "MODAL_ONCE" | "BELL_ONLY";
  target: { type: "ALL" } | { type: "TENANT"; tenantId: string } | { type: "USERS"; userIds: string[] };
};
```

Motivo: `core/notifications/components/NotificationModal.tsx` (substitui `DemoWelcomeModal.tsx`) precisa saber o que mostrar automaticamente ao entrar num produto (`pending-modal`); `shared/components/Notifications.tsx` (sino) precisa do histórico completo (`mine`); `TenantSelectScreen.tsx` ganha o botão "Criar Notificação" (Super Admin) que chama `POST /notifications` com fan-out para os destinatários.

---

## G. Recomendação de ordenação das sprints

1. **01** — Refactor do frontend + setup git. *(concluída.)*
2. **02** (via GPT, `sprint-02-fundacao-backend-gpt/`) — Fundação do backend: Docker Compose, Postgres dedicado, Keycloak dedicado (porta 8282), Spring Boot, `/api/v1/me`, tenants, produtos, módulos, knowledge graph, build do React servido pelo Spring Boot.
3. **09** — Substitui 03 e 04. *(concluída — services/contracts em todos os 10 domínios existentes, fluxo Super Admin completo via modal de 3 passos em `/select-tenant`, `.env.example` com Keycloak na porta 8282, script de build, este relatório.)*
   - ~~03~~, ~~04~~ — obsoletas, mantidas só como histórico.
4. **10** — Refinamento de ações pendentes na UI (`10_refinamento_acoes_pendentes_ui.md`) — corrige `SelectLike`/`Field` para serem editáveis de verdade e liga ~140 botões sem `onClick` a alguma ação real. Não depende de backend.
5. **11** — Modelo de páginas/blocos, correção do Knowledge Graph e mocks dos 6 produtos (`11_modelo_paginas_blocos_knowledge_graph_e_mocks_produtos.md`) — seleção real de módulos na criação de produto, domínio `pages`, preview leve de nó, `summary`/`difficultyLevel` em `Content`. *(Mocks dos 6 produtos como produto selecionável já criados; conteúdo profundo por produto ainda pendente — ver critérios de aceite daquela sprint.)*
6. **12** — Correções pós-teste manual da Sprint 11 (`12_correcoes_pos_teste_editor_blocos_e_grafo.md`) — logging de chamadas de API, fluxo "Novo conteúdo" corrigido, `two-column`/`contact` alinhados com o backend, CRUD de itens em blocos de lista, preview real, `edgeType` do grafo corrigido, mobile e upload de PDF.
7. **13** — Engine de blocos genérica, entidades globais e responsividade (`13_engine_de_blocos_entidades_globais_e_responsividade.md`) — decisões de modelo (tipos de conteúdo, `ProductGlobals`, sub-blocos genéricos, permissões por widget, estratégia de storage), markdown, mídia, drag-and-drop, entrega de formulário multicanal, Central de Ajuda, e varredura responsiva ampla. **Ainda não executada** (mas o markdown que ela pedia já está em produção, confirmado na investigação da Sprint 14).
8. **14** — Onboarding real e sistema de notificações remodelado (`14_onboarding_real_e_sistema_de_notificacoes.md`) — substitui o modal de boas-vindas baseado em `localStorage` por onboarding persistido por usuário, e cria o fluxo de notificações direcionadas do Super Admin (criar notificação, escolher destinatários, fan-out, sino remodelado). **Ainda não executada.**
9. **Extensão da Sprint 02 — concluída e integrada** como etapas `09` a `17`, `21`, `23`, `24` e `25` de `docs/sprints/backend/` (renumeração feita após a Sprint 11: as etapas antigas `09`-`12` — build do frontend, docker compose, seed, openapi/checklist — agora são `18`-`20` e `22`; a etapa `23`, domínio `pages`, foi adicionada pela Sprint 11; a etapa `24`, OpenAPI/Swagger, concluiu o checklist final; a etapa `25`, domínio `notification`, foi adicionada pela Sprint 14, depois do checklist final). Cobre toda a Seção B (`content`=11, `assets`=12, `forms`=13, `analytics`=14, `users`=15, `audit`=16, `settings`+`dashboard`=17) e a Seção C inteira (`tenants` CRUD + `ProductAssignment`=10, extras do Knowledge Graph=18). **Atualizada pela Sprint 13** (ECOMMERCE/assetStorageStrategy, markdown/sanitização, storage local/S3, formulário multicanal, ProductGlobals/audio/social-links/acceptsChildren genérico) **e pela Sprint 14** (domínio `notification`, etapa 25).
10. **05** — Roles/permissões por produto (liga `ModuleCatalog` ao backend real).
11. **06** — Keycloak login UI custom (login real via Authorization Code + PKCE, porta 8282).
12. **07** — Modo mock vs. real via `.env` (`VITE_API_MODE`) — só plenamente eficaz depois que a sprint #10 acima existir.
13. **08** — Gaps pós Sprint 19 (Figma Make) — reavaliar depois da 09, já que o gap do Super Admin "Criar Tenant" foi resolvido por ela.

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

## Observação sobre os produtos mockados (atualizado pela Sprint 11)

O tenant BYOP (`t1`) agora tem 9 produtos no mock (`mockProductsByUser`/`products.mocks.ts`), cobrindo os 6 contratos de negócio reais analisados na Sprint 11, mais os produtos técnicos pré-existentes:

| Produto | Tipo | Contrato de origem |
|---|---|---|
| Maestro Beton | Site Institucional | `contrato-json-site-maestro-beton.md` |
| Conecta Talentos | Portal | `contrato-negocio-site-conecta-talentos-rh.md` |
| CMSS | Site Institucional | `levantamento-site-institucional-cms-first.md` |
| Alexandre Dev | Portfolio | `contrato-cms.md` / `documento-negocios.md` |
| Loki | Biblioteca Filosófica | `contratos-cms.md` / `documento-negocios-974819bc.md` |
| WikiDev | Knowledge Base | `documenta-negocio-cms.md` |
| Aion Logbook, Eirene UI, Genesis | (técnicos, pré-existentes) | — |

Todos os 6 produtos de negócio já existem como produto selecionável. Popular cada um com conteúdo (páginas/artigos) fiel ao respectivo contrato é a Tarefa E da Sprint 11, ainda pendente — depende do domínio `pages` (Tarefa B) existir em código no frontend.
