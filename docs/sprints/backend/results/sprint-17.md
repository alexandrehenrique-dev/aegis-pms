# Resultado da Sprint 17 — Domínios `settings` e `dashboard`

> Evidências detalhadas. O resumo consolidado para colar em conversas futuras está em `docs/sprints/backend/SPRINT-RESULTADO.md`.

**Data de fechamento:** 2026-06-29.

## Objetivo

Implementar `docs/sprints/backend/17_dominio_settings_e_dashboard.md`: configurações de produto/tenant editáveis, matriz de permissões por papel/tenant (ADR-0014) e o agregador de KPIs do hub global — sem nenhuma tabela/cache própria para o dashboard.

## Resultado

Objetivo atingido. `settings/overview`, `PUT settings`, `roles`/`permission-matrix` (GET/PUT/preview/restore-defaults) e `dashboard/summary` estão implementados, testados e validados manualmente via Bruno contra a aplicação real. `mvn clean verify` fecha com `BUILD SUCCESS`, 932 testes, JaCoCo 100% e Spring Modulith aprovado.

## Classes criadas

**Domínio `settings` (`br.com.byop.aegis.settings`):**
- `settings/domain/RolePermission.java` — entity com exatamente os campos previstos no artefato (`id`, `tenantId`, `role`, `permissionKey`, `allowed`), sem campo adicional.
- `settings/repository/RolePermissionRepository.java` — Javadoc na interface e em todo método (`findAllByTenantId`, `findAllByTenantIdAndRole`, `findByTenantIdAndRoleAndPermissionKey`, `deleteAllByTenantId`).
- `settings/mapper/RolePermissionMapper.java` (MapStruct) — `toSummary(RolePermission): RolePermissionSummary`, conversão 1:1; o agrupamento por papel (matriz `role × permissionKey`) é responsabilidade do `SettingsService`, não do mapper.
- `settings/dto/RolePermissionSummary.java`, `settings/dto/RoleMatrixEntry.java` (shape único reaproveitado por `GET roles`, `GET permission-matrix` e `POST permission-matrix/preview`), `settings/dto/SettingCard.java`.
- `settings/contract/UpdateProductSettingsRequest.java`, `settings/contract/PermissionMatrixPreviewRequest.java`.
- `settings/service/PermissionMatrixDefaults.java` — catálogo de fábrica (tabela literal, mesmo padrão de `AuditRiskCatalog`), copiado das constantes reais de `frontend/src/core/permissions/roles.ts` (`roleVisibleNav`/`roleBlockedRoutePrefixes`): 31 chaves de permissão, 5 papéis canônicos.
- `settings/service/SettingsService.java` — overview/update de settings de produto, leitura/escrita/preview/restore-defaults da matriz de permissões, sempre via `ProductVisibilityService`/`TenantAccessService` (nunca `ProductAccessResolver`, que é específico de conteúdo — ADR-0018/Seção 10.6).
- `settings/controller/SettingsController.java` — os 8 endpoints da etapa.
- `settings/exception/SettingsNotFoundException.java`, `InsufficientSettingsRoleException.java`, `InvalidSettingsRoleException.java`, `SettingsExceptionHandler.java`.

**Domínio `dashboard` (`br.com.byop.aegis.dashboard`):**
- `dashboard/dto/DashboardSummaryResponse.java` — sem entidade/tabela própria.
- `dashboard/service/DashboardService.java` — agregador puro sobre `product.api`/`content.api`/`asset.api`/`submission.api`, reaproveitando exatamente as mesmas APIs já consumidas pela etapa 14 (analytics), conforme deixado registrado como retrofit pendente naquela etapa.
- `dashboard/controller/DashboardController.java` — único endpoint, sem path param (escopo resolvido pelo papel do caller no token).

**Migration:** `V9__settings_role_permissions.sql` — tabela nova `role_permissions` (FK para `tenants`, `ON DELETE CASCADE`, unique `(tenant_id, role, permission_key)`).

## Toques cross-module (sinalizados na análise antes da implementação)

- `product/api/ProductAccessScope.java` (novo) + `product/api/ProductVisibilityService.java` (novo) — expõe publicamente a mesma resolução de papel já implementada em `ProductService.listProducts` (ADR-0019), sem duplicar a lógica de branching: `ProductVisibilityService` apenas injeta `ProductService` (mesmo módulo, pacotes internos acessíveis) e adapta `ProductSummary` para o contrato público `ProductAccessScope`. Usado por `SettingsService` (resolver se um produto está no escopo do caller) e `DashboardService` (resolver o conjunto de produtos a agregar).
- `product/repository/ProductAssignmentRepository.java` (alterado) — novo método `findAllByProductIdInAndStatus(Collection<UUID>, ProductAssignmentStatus)`.
- `product/api/ProductUserAccessService.java` (alterado) — novos métodos `countDistinctAssignedUsers`/`countDistinctProductManagers(Collection<UUID> productIds)`, usados pelo `DashboardService` para `activeUsers`/`productManagers`.
- `product/api/ProductReferenceService.java` (alterado) — novo método `renameProduct(UUID, String)`, usado por `SettingsService.updateProductSettings` (único campo de `Product` editável previsto nesta etapa).
- `submission/api/SubmissionAnalyticsService.java` (alterado) — novo método aditivo `countToday(UUID productId)` (não altera `SubmissionAnalyticsResponse` nem nenhum call-site existente da etapa 14, para não arriscar regressão nos testes já validados daquela etapa). Usado por `DashboardService` para `formsReceivedToday`.

## Endpoints confirmados

- `GET /api/v1/products/{productId}/settings/overview`
- `PUT /api/v1/products/{productId}/settings`
- `GET /api/v1/tenants/{tenantId}/roles`
- `PUT /api/v1/tenants/{tenantId}/roles`
- `POST /api/v1/tenants/{tenantId}/roles/restore-defaults`
- `GET /api/v1/tenants/{tenantId}/permission-matrix`
- `POST /api/v1/tenants/{tenantId}/permission-matrix/preview`
- `POST /api/v1/tenants/{tenantId}/permission-matrix/restore-defaults`
- `GET /api/v1/dashboard/summary`

Todos os 9 endpoints da lista oficial do artefato — nenhum endpoint extra, nenhum endpoint faltante (incluindo `POST /roles` de criação de papel, que **não** está na lista oficial e foi deliberadamente não implementado — ver Decisões, item 2).

## Decisões de implementação registradas

1. **Catálogo de `permissionKey` copiado literalmente do frontend.** `core/permissions/roles.ts` não tem um catálogo de "permissionKey" dedicado — a chave reaproveitada é a união dos paths já usados em `roleVisibleNav`/`roleBlockedRoutePrefixes` (31 chaves, incluindo wildcards como `/content/*/publish`, tratados como string opaca, nunca interpretados em runtime). O default de `restore-defaults` é uma tabela literal `role × key → boolean` (`PermissionMatrixDefaults`), calculada uma única vez a partir da regra atual do frontend, no mesmo espírito do `AuditRiskCatalog` da Sprint 16.
2. **Não implementada criação de papel (`POST /roles`).** O mock do frontend (`settingsService.createRole`) sugere essa capacidade, mas ADR-0014 fixa um catálogo fechado de exatamente 5 papéis e exige atualizar a própria ADR antes de criar um 6º — e a lista oficial de endpoints da etapa 17 não inclui `POST /roles`. Decisão: seguir a ADR e a lista oficial; `createRole` do mock é tratado como função stale do frontend, fora do escopo desta sprint.
3. **`GET /roles` e `GET /permission-matrix` retornam o mesmo shape (`RoleMatrixEntry[]`).** O artefato descreve literalmente `{ role: string; permissions: Record<string, boolean> }[]` para ambos — telas diferentes (`RoleManagement.tsx`/`PermissionMatrixView.tsx`), mesmo dado. Edição acontece só via `PUT /roles` (não existe `PUT /permission-matrix` na lista oficial); os dois `restore-defaults` (`/roles/...` e `/permission-matrix/...`) chamam o mesmo `SettingsService.restoreDefaultPermissions`, sem lógica duplicada.
4. **Matriz sempre devolve as 31 chaves para os 5 papéis, mesmo com customização parcial.** `buildMatrix`/`matrixEntryForRole` sempre partem do catálogo de fábrica completo e sobrepõem só as linhas que o tenant de fato persistiu — nunca o contrário. **Bug real encontrado durante a validação manual** (não pego pelos testes unitários com mocks): a primeira versão só caía no catálogo de fábrica quando a tabela do tenant estava **totalmente vazia**; assim que uma única chave de um único papel era customizada, as outras 30 chaves daquele papel (e os 4 papéis sem nenhuma linha) desapareciam da resposta em vez de cair no padrão. Corrigido antes do commit.
5. **`restoreDefaultPermissions` precisa de `flush()` explícito entre o `deleteAllByTenantId` e os `save()` seguintes.** **Segundo bug real encontrado durante a validação manual**: o Hibernate ordena INSERTs antes de DELETEs no flush (independente da ordem em que o código os chama), então recriar as mesmas linhas `(tenantId, role, permissionKey)` na mesma transação sem um flush no meio violava a constraint única (`DataIntegrityViolationException`, traduzido pelo filtro do Spring Security em `403 insufficient_scope` — mesma classe de quirk de plataforma já documentada nas Sprints 12/16, mas com uma causa raiz diferente e real desta vez, não cosmética). Corrigido com `rolePermissionRepository.flush()` logo após o delete.
6. **`PUT /products/{productId}/settings` cobre só o campo `name`.** Nem o artefato nem `docs/trace/00_endpoints_esperados.md` definem um payload concreto para este endpoint (diferente de quase todo outro endpoint do trace doc) e o mock do frontend (`productsService.saveSettings()`) não envia dados reais. Implementado o mínimo defensável: `UpdateProductSettingsRequest{ name }`, usando o único mutador já existente em `Product` (`rename`). Branding/SEO/publicação ficam como retrofit pendente — ver Retrofits.
7. **`settings/overview` é um catálogo majoritariamente estático (8 `SettingCard` fixos).** Não existe dado real por trás de "owner"/"lastUpdated" para a maioria dos cards; só o card "Permissões" reflete um sinal real (`customizado` vs `padrão`, conforme a tabela `role_permissions` do tenant tem ou não overrides) e o card "Produto" reflete o `status` real do produto. Os demais campos ficam com placeholder (`—`), documentado aqui em vez de inventar dado sem fonte.
8. **`dashboard/summary` — mapeamento dos campos sem conceito de "approval" existente.** Não há entidade de workflow/aprovação no backend ainda. Mapeamento adotado: `pendingContent = drafts`, `pendingContentNeedingReview = openApprovals = pendingReview` (mesmo número, duas molduras — conteúdo em revisão é literalmente a aprovação em aberto, não há dois conceitos distintos ainda), `criticalApprovals = stalePendingReview` (>30 dias). `conversionRate` fica fixo em `"0%"` — não existe tracking de tráfego real (mesma decisão já tomada para `channels` na Sprint 14/analytics).
9. **Escopo de `dashboard/summary` resolvido pelo mesmo filtro por papel de `ProductService.listProducts` (ADR-0019), via `ProductVisibilityService`.** `SUPER_ADMIN` agrega globalmente (todos os produtos, decisão já explicitada pelo próprio artefato); `TENANT_ADMIN` agrega pelos produtos do(s) seu(s) tenant(s); `PRODUCT_MANAGER`/`EDITOR`/`VIEWER` agregam pelos produtos com `ProductAssignment`. `activeUsers`/`productManagers` usam o mesmo conjunto de produtos resolvido (distintos usuários/PMs com atribuição ativa nesses produtos) — não usa `TenantMembership`, para o número fazer sentido também para papéis sem visão de tenant inteiro.

## Retrofits pendentes para etapas futuras

- **Consumo real da matriz de permissões.** Nenhum outro domínio ainda lê `RolePermission` para de fato gatear endpoints — a matriz hoje é só configurável/consultável, não enforced em nenhum outro lugar do backend. Primeira implementação de uma permissão configurável por tenant (antes só existia hardcoded no frontend); ligar isso à autorização real de outros domínios é trabalho de uma sprint futura, explicitamente fora do escopo desta etapa.
- **`PUT /products/{productId}/settings`** cobre só `name` — branding, SEO e publicação (campos que `ProductSettings.tsx` sugere visualmente) não têm coluna nenhuma hoje; precisam de um payload e schema definidos numa sprint futura.
- **`settings/overview`** — cards "Tenant", "Equipe", "Integrações", "Segurança", "Auditoria", "SEO" são estáticos (sem `owner`/`lastUpdated` reais); uma sprint futura pode querer ligar isso a dados reais (ex.: `lastUpdated` do card "Equipe" vindo do último `TenantUserService`/membership change).
- **`conversionRate`** do dashboard fixo em `"0%"` até existir tracking de tráfego real (mesmo retrofit já registrado pela Sprint 14 para `channels`).

## Riscos remanescentes

- **Quirk de plataforma 403 `insufficient_scope` para erros não mapeados nos meus próprios handlers** (ver Decisões, item 5) — qualquer exceção não tratada por um `@RestControllerAdvice` específico (ex.: uma `DataIntegrityViolationException` não prevista) é traduzida para `403 insufficient_scope` pelo filtro do Spring Security em vez de um 500 claro, dificultando o diagnóstico em produção. Não é um problema introduzido por esta sprint, mas o bug real encontrado (item 5 das Decisões) mostra que vale a pena, numa sprint futura, revisar esse comportamento global do `SecurityConfig`.
- **Catálogo de `PermissionMatrixDefaults` é uma cópia manual da verdade do frontend** — se `core/permissions/roles.ts` mudar no futuro sem atualizar `PermissionMatrixDefaults` na mesma revisão, o "padrão de fábrica" do backend diverge silenciosamente do que o frontend já fazia antes desta sprint. Sem teste de arquitetura automatizado ligando os dois lados.

## Arquivos criados

```
backend/src/main/java/br/com/byop/aegis/dashboard/controller/DashboardController.java
backend/src/main/java/br/com/byop/aegis/dashboard/dto/DashboardSummaryResponse.java
backend/src/main/java/br/com/byop/aegis/dashboard/service/DashboardService.java
backend/src/main/java/br/com/byop/aegis/product/api/ProductAccessScope.java
backend/src/main/java/br/com/byop/aegis/product/api/ProductVisibilityService.java
backend/src/main/java/br/com/byop/aegis/settings/contract/PermissionMatrixPreviewRequest.java
backend/src/main/java/br/com/byop/aegis/settings/contract/UpdateProductSettingsRequest.java
backend/src/main/java/br/com/byop/aegis/settings/controller/SettingsController.java
backend/src/main/java/br/com/byop/aegis/settings/domain/RolePermission.java
backend/src/main/java/br/com/byop/aegis/settings/dto/RoleMatrixEntry.java
backend/src/main/java/br/com/byop/aegis/settings/dto/RolePermissionSummary.java
backend/src/main/java/br/com/byop/aegis/settings/dto/SettingCard.java
backend/src/main/java/br/com/byop/aegis/settings/exception/InsufficientSettingsRoleException.java
backend/src/main/java/br/com/byop/aegis/settings/exception/InvalidSettingsRoleException.java
backend/src/main/java/br/com/byop/aegis/settings/exception/SettingsExceptionHandler.java
backend/src/main/java/br/com/byop/aegis/settings/exception/SettingsNotFoundException.java
backend/src/main/java/br/com/byop/aegis/settings/mapper/RolePermissionMapper.java
backend/src/main/java/br/com/byop/aegis/settings/repository/RolePermissionRepository.java
backend/src/main/java/br/com/byop/aegis/settings/service/PermissionMatrixDefaults.java
backend/src/main/java/br/com/byop/aegis/settings/service/SettingsService.java
backend/src/main/resources/db/migration/V9__settings_role_permissions.sql
backend/src/test/java/br/com/byop/aegis/dashboard/controller/DashboardControllerTest.java
backend/src/test/java/br/com/byop/aegis/dashboard/service/DashboardServiceTest.java
backend/src/test/java/br/com/byop/aegis/product/api/ProductVisibilityServiceTest.java
backend/src/test/java/br/com/byop/aegis/settings/controller/SettingsControllerTest.java
backend/src/test/java/br/com/byop/aegis/settings/mapper/RolePermissionMapperTest.java
backend/src/test/java/br/com/byop/aegis/settings/repository/RolePermissionRepositoryTest.java
backend/src/test/java/br/com/byop/aegis/settings/service/PermissionMatrixDefaultsTest.java
backend/src/test/java/br/com/byop/aegis/settings/service/SettingsServiceTest.java
bruno/17-settings-dashboard/atualizar-roles.bru
bruno/17-settings-dashboard/atualizar-settings-produto.bru
bruno/17-settings-dashboard/dashboard-summary.bru
bruno/17-settings-dashboard/editor-tentando-editar-roles-403.bru
bruno/17-settings-dashboard/folder.bru
bruno/17-settings-dashboard/listar-permission-matrix.bru
bruno/17-settings-dashboard/listar-roles.bru
bruno/17-settings-dashboard/preview-permission-matrix.bru
bruno/17-settings-dashboard/preview-role-invalido-400.bru
bruno/17-settings-dashboard/restaurar-permission-matrix-padrao.bru
bruno/17-settings-dashboard/restaurar-roles-padrao.bru
bruno/17-settings-dashboard/settings-overview-produto.bru
bruno/17-settings-dashboard/settings-produto-inexistente-404.bru
bruno/17-settings-dashboard/tenant-fora-do-escopo-roles-404.bru
```

## Arquivos alterados

```
backend/src/main/java/br/com/byop/aegis/product/api/ProductReferenceService.java
backend/src/main/java/br/com/byop/aegis/product/api/ProductUserAccessService.java
backend/src/main/java/br/com/byop/aegis/product/repository/ProductAssignmentRepository.java
backend/src/main/java/br/com/byop/aegis/submission/api/SubmissionAnalyticsService.java
backend/src/main/java/br/com/byop/aegis/system/controller/SpaFallbackController.java
backend/src/test/java/br/com/byop/aegis/product/api/ProductReferenceServiceTest.java
backend/src/test/java/br/com/byop/aegis/product/api/ProductUserAccessServiceTest.java
backend/src/test/java/br/com/byop/aegis/product/repository/ProductAssignmentRepositoryTest.java
backend/src/test/java/br/com/byop/aegis/submission/api/SubmissionAnalyticsServiceTest.java
docs/api-testing/README.md
docs/sprints/backend/SPRINT-RESULTADO.md
```

## Arquivos removidos

Nenhum.

## Cobertura de testes

`mvn clean verify` final: **BUILD SUCCESS**, **932 testes**, 0 falhas, 0 erros, 0 ignorados. JaCoCo: `All coverage checks have been met` (regra `BUNDLE`, linha e branch 100%, 252 classes elegíveis analisadas). Spring Modulith: `ModulithArchitectureTest` aprovado — incluindo os novos módulos `settings`/`dashboard` e os métodos novos expostos em `product.api`/`submission.api`.

Testes novos por camada: `RolePermissionRepositoryTest` (`@DataJpaTest`, 6 casos), `RolePermissionMapperTest` (sem contexto Spring, 3 casos), `PermissionMatrixDefaultsTest` (8 casos, incluindo a regra LGPD do SUPER_ADMIN e o catálogo das 31 chaves), `SettingsServiceTest` (Mockito, 18 casos, incluindo os 5 cenários de papel/escopo e os dois bugs corrigidos), `SettingsControllerTest` (`@WebMvcTest`, 13 casos), `DashboardServiceTest` (Mockito, 2 casos), `DashboardControllerTest` (`@WebMvcTest`, 2 casos), `ProductVisibilityServiceTest` (Mockito, 2 casos). Testes existentes estendidos: `ProductReferenceServiceTest` (+2, `renameProduct`), `ProductUserAccessServiceTest` (+4, contagens distintas), `ProductAssignmentRepositoryTest` (+1, `findAllByProductIdInAndStatus`), `SubmissionAnalyticsServiceTest` (+2, `countToday`).

## SonarQube for IDE

Rodada de correção pós-implementação (apontamentos reais encontrados pelo usuário via SonarQube for IDE), nenhum resolvido com `@SuppressWarnings`/`NOSONAR`/desabilitação de regra — todos via reescrita, sem alterar nenhum contrato REST, payload, migration, regra de autorização ou comportamento de teste:

- `java:S1192` (literal duplicada) em `PermissionMatrixDefaults` — as 31 chaves de permissão (antes literais repetidas em `PERMISSION_KEYS` e em cada `Set.of(...)` de `ALLOWED_KEYS_BY_ROLE`) extraídas para 31 constantes privadas com nome semântico (`SETTINGS_ROLES`, `KNOWLEDGE_GRAPH`, `ASSETS_UPLOAD` etc.).
- `java:S1192` em `SettingsService` — `"SETTINGS"` (módulo do `AuditRecordCommand`, repetido 3x), `"configurado"` (6x), `"medio"`/`"alto"` (3x cada) e o placeholder `"—"` (16x) extraídos para `MODULE_SETTINGS`, `STATUS_CONFIGURADO`, `RISK_MEDIO`, `RISK_ALTO`, `OVERVIEW_PLACEHOLDER`.
- `java:S6862` (teste não deve depender do relógio do sistema) em `ProductVisibilityServiceTest` — os dois `OffsetDateTime.now()` do helper `summary(...)` (campos `createdAt`/`updatedAt`, não asserados pelo teste) substituídos por uma constante `FIXED_TIMESTAMP`.
- `java:S4144` (métodos com implementação idêntica) em `SettingsController` — `getRoles`/`getPermissionMatrix` (idênticos: resolver caller + `getRoleMatrix`) e `restoreDefaultRoles`/`restoreDefaultPermissionMatrix` (idênticos: resolver caller + `restoreDefaultPermissions`) passaram a delegar para dois métodos privados novos (`resolveRoleMatrix`, `resolveRestoreDefaults`) — os 4 endpoints públicos continuam com a mesma assinatura, mesma URL, mesmo comportamento.
- `java:S6068` (uso de matcher Mockito sem necessidade) em `SettingsServiceTest` — `eq(TENANT_ID)` num `verify(...)` com um único argumento substituído pelo valor direto `TENANT_ID`.
- `java:S5778` (lambda de `assertThatThrownBy` com mais de uma chamada que pode lançar) em `SettingsServiceTest` — 3 ocorrências corrigidas extraindo a construção do objeto auxiliar (`UpdateProductSettingsRequest`, `List<RoleMatrixEntry>`, `PermissionMatrixPreviewRequest`) para uma variável local antes do `assertThatThrownBy`, deixando só a chamada ao método sob teste dentro da lambda — mesmo padrão já adotado nas Sprints 11/12/15/16.
- `java:S3752` (mapeamento HTTP sem método explícito) em `SpaFallbackController` — `@RequestMapping(value = {...})` (aceitava qualquer verbo HTTP) substituído por `@GetMapping(value = {...})`; o fallback da SPA só responde a navegação de browser (sempre `GET`), confirmado pelos testes existentes (`SpaFallbackControllerTest`, só usa `get(...)`).

Validação desta rodada: `mvn clean verify` (`BUILD SUCCESS`, 932 testes, JaCoCo 100%, Modulith aprovado, sem nenhuma regressão) e `cd bruno && npx @usebruno/cli run --env local` (146/146 requests, 282/282 testes). A confirmação de "zero apontamentos" para os 6 arquivos depende de uma nova varredura do usuário via SonarQube for IDE no próprio IntelliJ — não há scanner de SonarQube configurado no `pom.xml` deste repositório (nem `sonar-maven-plugin`, nem `sonar-project.properties`) para rodar uma análise equivalente fora da IDE.

## Bruno

Pasta `17-settings-dashboard` criada com 13 requests: overview feliz, update feliz, produto inexistente (404), roles feliz (GET/PUT/restore-defaults), permission-matrix feliz (GET/preview/restore-defaults), preview com papel inválido (400), 403 de papel insuficiente e 404 cross-tenant (mesmo padrão de `nonMemberToken`/`editorToken` já usado em `11-content`/`16-audit`, sem seed automático de segundo usuário) e dashboard/summary feliz.

**Dois bugs reais de implementação encontrados e corrigidos durante esta validação** (não pegos pelos testes unitários com mocks, só pela execução contra a aplicação real e o Postgres real) — ver Decisões, itens 4 e 5: matriz incompleta quando parcialmente customizada, e violação de constraint única no `restore-defaults` por ordenação de flush do Hibernate.

Validado via `cd bruno && npx @usebruno/cli run --env local`: **146 requests executados, 146 aprovados, 282/282 testes aprovados**, confirmado em duas execuções completas consecutivas (idempotente).
