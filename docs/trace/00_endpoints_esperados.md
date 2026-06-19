# Trace 00 — Endpoints esperados pelo frontend e reordenação das sprints

> Gerado para servir de insumo à decisão de **ordem de execução** das sprints em `docs/sprints/`. Cobre: (A) o que o backend planejado (`implementation/001` + `docs/sprints/sprint-02-fundacao-backend-gpt/`) já define, (B) o que falta definir para o frontend existente funcionar de ponta a ponta, (C) os endpoints novos exigidos pelo fluxo de Super Admin criado na Sprint 09, e (D) a recomendação de ordenação.

## A. Endpoints já definidos (Sprint 02 / `implementation/001`, Features 001–025)

Cobertura confirmada nos arquivos de `docs/sprints/sprint-02-fundacao-backend-gpt/`. Todos sob `http://localhost:8080`, exceto Keycloak (porta **8282**).

| Endpoint | Método | Usado por (frontend) |
|---|---|---|
| `/actuator/health`, `/actuator/info` | GET | Infra, monitoramento — sem tela própria |
| `/api/v1/me` | GET | `AuthContext`, qualquer tela que exiba o usuário logado |
| `/api/v1/tenants` | GET, POST | `domains/tenants/pages/TenantsManagement.tsx` (nova, Sprint 09), `CreateTenantForm.tsx` (nova) |
| `/api/v1/tenants/{tenantId}` | GET | `TenantSettings.tsx`, `TenantSelectScreen.tsx` |
| `/api/v1/products` | GET, POST | `CreateProductForm.tsx`, `ProductSelectScreen.tsx`, `domains/products` |
| `/api/v1/products/{productId}` | GET | Telas de detalhe de produto em todos os domínios |
| `/api/v1/products/{productId}/modules/{moduleKey}/enable` | POST | Telas de gestão de módulos do produto (`/products/:id/modules`) |
| `/api/v1/products/{productId}/modules/{moduleKey}/disable` | POST | idem |
| `/api/v1/products/{productId}/graph/nodes` | GET, POST | `domains/knowledge` (Knowledge Graph) |
| `/api/v1/products/{productId}/graph/nodes/{nodeId}` | GET | idem |
| `/api/v1/products/{productId}/graph/edges` | POST | idem |
| `/api/v1/products/{productId}/graph/nodes/{nodeId}/neighbors` | GET | idem |
| `/api/v1/products/{productId}/graph/nodes/{nodeId}/related` | GET | idem |
| `http://localhost:8282/realms/aegis/...` | OIDC | Login real (Sprint 06) |

## B. Gap real — domínios do frontend sem nenhum endpoint definido ainda

O frontend (já construído via Figma Make e refatorado na Sprint 01) tem telas funcionando em mock para os domínios abaixo, mas `implementation/001`/Sprint 02 **não define nenhum endpoint REST para eles** — a fundação do backend cobre só o núcleo (tenant, produto, módulo, knowledge graph, auth). Isto é esperado: a fundação foi desenhada para ser mínima. Mas precisa ficar explícito para não ser esquecido: **alguém vai precisar especificar e implementar estes endpoints em uma extensão futura da Sprint 02 (mesma estratégia: via GPT, em etapas pequenas), antes que a Sprint 09 consiga trocar o domínio inteiro de mock para real.**

| Domínio (frontend) | Telas existentes (mock) | Endpoints necessários (propostos) |
|---|---|---|
| `content` | `EditorialDashboard`, `ContentDataGrid`, `WorkflowBoard`, `ContentEditor`, `PublishPanel`, versões | `GET/POST /api/v1/products/{productId}/content`, `GET/PUT /api/v1/products/{productId}/content/{contentId}`, `POST /api/v1/products/{productId}/content/{contentId}/transition` (workflow), `POST /api/v1/products/{productId}/content/{contentId}/publish`, `GET /api/v1/products/{productId}/content/{contentId}/versions` |
| `assets` | `AssetLibrary`, `AssetUpload`, `AssetDetail`, metadados, tags, uso | `GET/POST /api/v1/products/{productId}/assets`, `GET/DELETE /api/v1/products/{productId}/assets/{assetId}`, `PUT /api/v1/products/{productId}/assets/{assetId}/metadata`, `GET /api/v1/products/{productId}/assets/{assetId}/usage` |
| `forms` | `FormsDashboard`, `FormBuilder`, `SubmissionTable`, `SubmissionDetails`, `FormAnalytics` | `GET/POST /api/v1/products/{productId}/forms`, `GET/PUT /api/v1/products/{productId}/forms/{formId}`, `GET /api/v1/products/{productId}/forms/{formId}/submissions`, `GET /api/v1/products/{productId}/forms/{formId}/submissions/{submissionId}` |
| `analytics` | `AnalyticsOverview`, `ProductHealthPanel`, `ContentAnalytics`, `ChannelBreakdown`, `ReportGrid`, `TrendCards` | `GET /api/v1/products/{productId}/analytics/overview`, `GET /api/v1/products/{productId}/analytics/health`, `GET /api/v1/products/{productId}/analytics/trends`, `GET /api/v1/products/{productId}/analytics/reports` |
| `users` | `UserTable`, `InviteUserDrawer`, `UserDetailPanel` | `GET /api/v1/tenants/{tenantId}/users`, `POST /api/v1/tenants/{tenantId}/users/invite`, `GET/PUT /api/v1/tenants/{tenantId}/users/{userId}` |
| `audit` | `AuditTimeline`, `AuditEventDetail` | `GET /api/v1/tenants/{tenantId}/audit-events`, `GET /api/v1/tenants/{tenantId}/audit-events/{eventId}` |
| `settings` | `TenantSettings`, `ProductSettings`, `PermissionMatrixView`, `RoleManagement`, `AccessPreviewPanel`, `SecuritySettingsPanel` | `PUT /api/v1/tenants/{tenantId}`, `PUT /api/v1/products/{productId}/settings`, `GET/PUT /api/v1/tenants/{tenantId}/roles`, `GET /api/v1/tenants/{tenantId}/permission-matrix` |
| `dashboard` | `DashboardGlobal` (KPIs agregados) | Provavelmente um agregador (`GET /api/v1/dashboard/summary`) que combina dados de tenants/produtos/módulos já existentes — avaliar se compensa um endpoint dedicado ou se o frontend agrega client-side a partir dos endpoints de A. |

## C. Endpoints novos exigidos pelo fluxo de Super Admin (Sprint 09)

O fluxo "Super Admin cria Tenant → cria Produto → atribui Produto a um Usuário" expõe uma lacuna de **modelo de domínio**, não só de endpoint: hoje o `implementation/001` só tem `TenantMembership` (usuário↔tenant). Não existe o conceito de usuário atribuído a um **produto específico** com um papel específico nesse produto.

| Endpoint | Método | Observação |
|---|---|---|
| `/api/v1/tenants` | POST | Já existe em A — reaproveitado como passo 1 do wizard |
| `/api/v1/products` | POST | Já existe em A — reaproveitado como passo 2 do wizard, com `tenantId` no body |
| `/api/v1/products/{productId}/users` | GET, POST | **Novo.** Lista/atribui usuários a um produto específico com um papel (`POST` recebe `{ userId ou email, role }`). Requer uma entidade nova no backend, ex. `ProductAssignment` (ou estender `TenantMembership` com `productId` nulável — decisão de modelagem a ser tomada na extensão da Sprint 02) |
| `/api/v1/products/{productId}/users/{userId}` | DELETE | **Novo.** Remove a atribuição do usuário ao produto |

Esta lacuna de modelo (`ProductAssignment`) deve ser resolvida **antes** ou **durante** a extensão da Sprint 02 que cobrir o domínio `users` (ver tabela B) — não é algo que o frontend possa contornar sozinho, porque é uma decisão de schema do backend.

## D. Recomendação de ordenação das sprints

1. **01** — Refactor do frontend + setup git. *(já concluída, conforme indicado pelo repositório atual já estar na estrutura `src/app`/`src/core`/`src/domains`/`src/shared`.)*
2. **02** (via GPT, `sprint-02-fundacao-backend-gpt/`) — Fundação do backend: Docker Compose, Postgres dedicado, Keycloak dedicado (porta 8282), Spring Boot, `/api/v1/me`, tenants, produtos, módulos, knowledge graph, build do React servido pelo Spring Boot.
3. **09** — Substitui 03 e 04. Camada `services/`+`contracts/` em todos os domínios (inicialmente sobre mock, pronta para o toggle real da Sprint 07), fluxo Super Admin completo (criar tenant → criar produto → atribuir usuário) com navegação de volta, `.env.example` com Keycloak na porta 8282, script de build (`npm run build:backend`) e este relatório.
   - ~~03~~, ~~04~~ — não executar; obsoletas, mantidas só como histórico do diagnóstico original (que estava correto, só desatualizado nos caminhos de arquivo).
4. **Nova sprint a criar (recomendado, ainda não escrita)** — "Extensão da Sprint 02: endpoints de domínio" — mesma estratégia de execução via GPT em etapas pequenas, cobrindo a tabela B inteira (`content`, `assets`, `forms`, `analytics`, `users`, `audit`, `settings`) mais o endpoint de `ProductAssignment` da tabela C. Sem isso, a Sprint 07 (mock↔real) só vai conseguir ligar de fato os domínios já cobertos pela fundação (tenants, produtos, módulos, knowledge graph) — os demais continuarão mockados mesmo com a infraestrutura de toggle pronta.
5. **05** — Roles/permissões por produto.
6. **06** — Keycloak login UI custom (login real via Authorization Code + PKCE, porta 8282).
7. **07** — Modo mock vs. real via `.env` (`VITE_API_MODE`) — só plenamente eficaz depois que a sprint de extensão (#4 acima) existir, mas pode ser executada antes para os domínios já cobertos por A.
8. **08** — Gaps pós Sprint 19 (Figma Make) — reavaliar depois da 09, já que pelo menos um gap relatado ali (Super Admin "Criar Tenant" marcado como ✅ sem existir) é resolvido pela própria Sprint 09.

## Observação sobre o login mock de Super Admin

Para permitir simular o fluxo de Super Admin antes do backend/Keycloak existirem, já foi adicionado um usuário mock em `frontend/src/core/auth/mocks/users.ts`:

```
super-admin@byop.io / senha123
```

Com acesso a todos os tenants mockados (`t1`, `t2`, `t3`) e seus respectivos produtos.
