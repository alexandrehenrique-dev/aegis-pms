# Sprint 30 — Migration de dados reais de homologação e remoção do seed Java

Concluída tecnicamente em 2026-07-03 na branch `feat/etapa-30-seed-homologacao`.

## Objetivo

Substituir o seed Java local por uma migration Flyway idempotente que rode em todos os ambientes com dados reais de homologação do tenant `CLIENTES BETA`, removendo o package `seed` e as APIs públicas criadas exclusivamente para ele.

A sprint também fechou retrofits pendentes das etapas anteriores: auditoria de enable/disable de módulos, auditoria de submissão pública de formulário, enriquecimento de auditoria com `traceId`, `ip` e `userAgent`, Telegram para feedback interno do Aegis, Telegram para submissions de produto e Swagger/OpenAPI com OAuth2 Authorization Code + PKCE.

## Escopo Implementado

- Remoção completa do seed Java local (`DemoSeedRunner`, `DemoSeedService`) e das APIs públicas de seed que só existiam para esse fluxo.
- Migration Flyway `V17__seed_homologacao.sql` com tenant `clientes-beta`, seis produtos reais, módulos, páginas, seções e Knowledge Graph inicial.
- Campos de Telegram em `product_security_settings` para configuração de produto/formulário.
- Configuração global `aegis.telegram.alert.*` para alertas internos do Aegis.
- Feedback interno do Aegis salva no banco e envia Telegram global do Aegis, se ativo.
- Submission pública de formulário salva no banco, registra auditoria e envia Telegram pelo delivery channel do formulário/produto, se configurado.
- Auditoria enriquecida com contexto HTTP (`traceId`, `ip`, `userAgent`) via interceptor web.
- OpenAPI/Swagger passou a expor `oauth2-pkce` além do `bearer-jwt`.
- Realm export do Keycloak atualizado para aceitar o redirect URI do Swagger OAuth2.
- Bruno ganhou a pasta `30-seed-homologacao`.
- Documentações de backend, frontend, qualidade e testes API foram alinhadas para não misturar Telegram interno do Aegis com Telegram de produto externo.
- Apontamentos SonarQube for IDE reportados em `ProductControllerTest` e `ProductModuleService` corrigidos.

## Fora de Escopo

- Criar usuários reais dos clientes beta. A entrada de pessoas reais continua via fluxo de convite.
- Validar envio real para Telegram com token/chat de produção. A implementação e os testes usam `MockRestServiceServer`; a validação real depende de credenciais externas.
- Implementar telas frontend de configuração global do Telegram do Aegis.
- Alterar contratos existentes de feedback, submissions, settings ou OpenAPI além dos campos novos e do security scheme aditivo.
- Resetar ou limpar dados residuais do banco local de desenvolvimento. A migration foi escrita para preservar dados existentes.

## Arquivos Criados

- `backend/src/main/java/br/com/byop/aegis/audit/context/AuditContext.java`
- `backend/src/main/java/br/com/byop/aegis/audit/context/AuditContextHolder.java`
- `backend/src/main/java/br/com/byop/aegis/audit/context/AuditRequestContextInterceptor.java`
- `backend/src/main/java/br/com/byop/aegis/audit/context/AuditWebMvcConfig.java`
- `backend/src/main/java/br/com/byop/aegis/feedback/service/TelegramFeedbackNotifier.java`
- `backend/src/main/java/br/com/byop/aegis/settings/api/TelegramAlertSettings.java`
- `backend/src/main/java/br/com/byop/aegis/settings/api/TelegramAlertSettingsService.java`
- `backend/src/main/java/br/com/byop/aegis/settings/api/package-info.java`
- `backend/src/main/java/br/com/byop/aegis/submission/service/FormSubmissionTelegramNotifier.java`
- `backend/src/main/resources/db/migration/V17__seed_homologacao.sql`
- `backend/src/test/java/br/com/byop/aegis/audit/context/AuditContextHolderTest.java`
- `backend/src/test/java/br/com/byop/aegis/audit/context/AuditRequestContextInterceptorTest.java`
- `backend/src/test/java/br/com/byop/aegis/audit/context/AuditWebMvcConfigTest.java`
- `backend/src/test/java/br/com/byop/aegis/feedback/service/TelegramFeedbackNotifierTest.java`
- `backend/src/test/java/br/com/byop/aegis/knowledgegraph/domain/GraphTypeCatalogTest.java`
- `backend/src/test/java/br/com/byop/aegis/settings/api/TelegramAlertSettingsServiceTest.java`
- `backend/src/test/java/br/com/byop/aegis/submission/service/FormSubmissionTelegramNotifierTest.java`
- `bruno/30-seed-homologacao/00-login-super-admin.bru`
- `bruno/30-seed-homologacao/01-capturar-tenant-clientes-beta.bru`
- `bruno/30-seed-homologacao/02-capturar-produtos-reais.bru`
- `bruno/30-seed-homologacao/03-paginas-maestro-beton.bru`
- `bruno/30-seed-homologacao/04-grafo-loki.bru`
- `bruno/30-seed-homologacao/05-atualizar-telegram-produto.bru`
- `bruno/30-seed-homologacao/06-openapi-oauth2-pkce.bru`
- `bruno/30-seed-homologacao/folder.bru`
- `docs/sprints/backend/results/sprint-30.md`

## Arquivos Alterados

- `.env.example`
- `AGENTS.md`
- `backend/src/main/java/br/com/byop/aegis/api/openapi/OpenApiConfig.java`
- `backend/src/main/java/br/com/byop/aegis/audit/api/AuditRecordCommand.java`
- `backend/src/main/java/br/com/byop/aegis/audit/api/AuditService.java`
- `backend/src/main/java/br/com/byop/aegis/audit/service/AuditRiskCatalog.java`
- `backend/src/main/java/br/com/byop/aegis/feedback/service/FeedbackService.java`
- `backend/src/main/java/br/com/byop/aegis/form/api/FormReference.java`
- `backend/src/main/java/br/com/byop/aegis/form/api/FormReferenceService.java`
- `backend/src/main/java/br/com/byop/aegis/knowledgegraph/domain/GraphEdgeType.java`
- `backend/src/main/java/br/com/byop/aegis/knowledgegraph/domain/GraphNodeType.java`
- `backend/src/main/java/br/com/byop/aegis/product/controller/ProductController.java`
- `backend/src/main/java/br/com/byop/aegis/product/service/ProductModuleService.java`
- `backend/src/main/java/br/com/byop/aegis/settings/contract/UpdateProductSecuritySettingsRequest.java`
- `backend/src/main/java/br/com/byop/aegis/settings/domain/ProductSecuritySettings.java`
- `backend/src/main/java/br/com/byop/aegis/settings/dto/ProductSecuritySettingsResponse.java`
- `backend/src/main/java/br/com/byop/aegis/settings/mapper/ProductSecuritySettingsMapper.java`
- `backend/src/main/java/br/com/byop/aegis/settings/service/ProductSecuritySettingsService.java`
- `backend/src/main/java/br/com/byop/aegis/submission/service/SubmissionService.java`
- `backend/src/main/resources/application-local.yml`
- `backend/src/main/resources/application.yml`
- `backend/src/test/java/br/com/byop/aegis/api/openapi/OpenApiConfigTest.java`
- `backend/src/test/java/br/com/byop/aegis/audit/api/AuditServiceTest.java`
- `backend/src/test/java/br/com/byop/aegis/feedback/service/FeedbackServiceTest.java`
- `backend/src/test/java/br/com/byop/aegis/pages/service/SectionContentValidationServiceTest.java`
- `backend/src/test/java/br/com/byop/aegis/product/controller/ProductControllerTest.java`
- `backend/src/test/java/br/com/byop/aegis/product/service/ProductModuleServiceTest.java`
- `backend/src/test/java/br/com/byop/aegis/settings/controller/ProductSecuritySettingsControllerTest.java`
- `backend/src/test/java/br/com/byop/aegis/settings/mapper/ProductSecuritySettingsMapperTest.java`
- `backend/src/test/java/br/com/byop/aegis/settings/repository/ProductSecuritySettingsRepositoryTest.java`
- `backend/src/test/java/br/com/byop/aegis/settings/service/ProductSecuritySettingsServiceTest.java`
- `backend/src/test/java/br/com/byop/aegis/submission/service/SubmissionServiceTest.java`
- `bruno/24-openapi-testes-checklist-final/openapi-json-completo.bru`
- `docs/api-testing/README.md`
- `docs/sprints/backend/00_padrao_qualidade_e_arquitetura.md`
- `docs/sprints/backend/27_dominio_feedback.md`
- `docs/sprints/backend/30_seed_homologacao_e_remocao_seed_java.md`
- `docs/sprints/backend/SPRINT-RESULTADO.md`
- `docs/sprints/backend/results/sprint-27.md`
- `docs/sprints/frontend/23_inbox_de_feedbacks_super_admin.md`
- `infra/keycloak/realm/aegis-realm.json`

## Arquivos Removidos

- `backend/src/main/java/br/com/byop/aegis/seed/DemoSeedRunner.java`
- `backend/src/main/java/br/com/byop/aegis/seed/DemoSeedService.java`
- `backend/src/main/java/br/com/byop/aegis/tenant/api/TenantSeedCommand.java`
- `backend/src/main/java/br/com/byop/aegis/tenant/api/TenantSeedReference.java`
- `backend/src/main/java/br/com/byop/aegis/tenant/api/TenantSeedService.java`
- `backend/src/main/java/br/com/byop/aegis/product/api/ProductSeedCommand.java`
- `backend/src/main/java/br/com/byop/aegis/product/api/ProductSeedReference.java`
- `backend/src/main/java/br/com/byop/aegis/product/api/ProductSeedService.java`
- `backend/src/main/java/br/com/byop/aegis/knowledgegraph/api/GraphSeedEdgeCommand.java`
- `backend/src/main/java/br/com/byop/aegis/knowledgegraph/api/GraphSeedNodeCommand.java`
- `backend/src/main/java/br/com/byop/aegis/knowledgegraph/api/GraphSeedNodeReference.java`
- `backend/src/main/java/br/com/byop/aegis/knowledgegraph/api/KnowledgeGraphSeedService.java`
- `backend/src/test/java/br/com/byop/aegis/seed/DemoSeedRunnerTest.java`
- `backend/src/test/java/br/com/byop/aegis/seed/DemoSeedServiceTest.java`
- `backend/src/test/java/br/com/byop/aegis/tenant/api/TenantSeedServiceTest.java`
- `backend/src/test/java/br/com/byop/aegis/product/api/ProductSeedServiceTest.java`
- `backend/src/test/java/br/com/byop/aegis/knowledgegraph/api/KnowledgeGraphSeedServiceTest.java`

## Migration Flyway Criada

`backend/src/main/resources/db/migration/V17__seed_homologacao.sql`

A migration:

- adiciona `telegram_alert_bot_token` e `telegram_alert_chat_id` em `product_security_settings`;
- insere o tenant `clientes-beta` com nome `CLIENTES BETA`;
- insere seis produtos reais do tenant;
- insere módulos por produto;
- insere páginas e seções iniciais;
- insere nodes e edges do Knowledge Graph;
- usa `ON CONFLICT DO NOTHING` e `WHERE EXISTS` para preservar bancos já populados;
- roda em todos os ambientes, não só no profile `local`.

Observação importante: em bancos locais antigos que já tinham produtos com a mesma `key` antes da V17, os `ON CONFLICT` preservam os dados existentes. Por isso os dados dependentes com UUID fixo podem não ser inseridos nesses produtos residuais. Esse comportamento foi documentado e tratado na pasta Bruno 30 sem limpar dados locais.

## Dados Seedados por Migration

Tenant:

- `clientes-beta` — `CLIENTES BETA`, plano `PRO`, status `ACTIVE`.

Dados técnicos complementares:

- `product_security_settings.telegram_alert_chat_id`
- `product_security_settings.telegram_alert_bot_token`

## Produtos Criados

- `maestro-beton` — `Maestro Beton`, tipo `SITE_INSTITUCIONAL`, status `ACTIVE`.
- `conecta-talentos` — `Conecta Talentos`, tipo `PORTAL`, status `ACTIVE`.
- `alexandre-dev` — `Alexandre Dev`, tipo `PORTFOLIO`, status `ACTIVE`.
- `cmss` — `CMSS`, tipo `SITE_INSTITUCIONAL`, status `ACTIVE`.
- `wikidev` — `WikiDev`, tipo `KNOWLEDGE_BASE`, status `ARCHIVED`.
- `loki` — `Loki`, tipo `LIBRARY_BOOKS_MUSIC`, status `ACTIVE`.

Módulos principais:

- Maestro Beton: `CONTENT`, `PAGES`, `ASSETS`, `FORMS`, `SEO`, `ANALYTICS`, `KNOWLEDGE_GRAPH`.
- Conecta Talentos: `CONTENT`, `PAGES`, `FORMS`, `SEO`, `ANALYTICS`, com `SUBMISSIONS` e `JOBS` desabilitados.
- Alexandre Dev: `PORTFOLIO`, `CONTENT`, `PAGES`, `ASSETS`, `SEO`, `ANALYTICS`.
- CMSS: `CONTENT`, `PAGES`, `ASSETS`, `FORMS`, `SEO`, `ANALYTICS`.
- WikiDev: `CONTENT`, `KNOWLEDGE_GRAPH`, `SEO`, `ANALYTICS`.
- Loki: `LIBRARY`, `BOOKS`, `MUSIC`, `CONTENT`, `KNOWLEDGE_GRAPH`, `SEO`, `ANALYTICS`.

## Páginas e Seções Criadas

Maestro Beton:

- `home`: `HERO`, `CARD_LIST`, `GALLERY`, `EVENT_LIST`, `CONTACT`.
- `quem-somos`: `IMAGE_TEXT`, `TWO_COLUMN`.
- `historia`: `TIMELINE`.
- `agenda`: `EVENT_LIST`.
- `galeria`: `GALLERY`.
- `apoie`: `RICH_TEXT`, `FAQ`.
- `contato`: `CONTACT`.

CMSS:

- `home`: `HERO`, `FEATURE_GRID`, `EVENT_LIST`, `CTA_SECTION`.
- `quem-somos`: `IMAGE_TEXT`, `TWO_COLUMN`.
- `historia`: `TIMELINE`.
- `agenda`: `EVENT_LIST`.
- `apoie`: `RICH_TEXT`, `FAQ`.
- `contato`: `CONTACT`.

Conecta Talentos:

- `home`: `HERO`, `RICH_TEXT`, `CARD_LIST` para vagas e `CARD_LIST` para blog.

Alexandre Dev:

- `home`: `HERO`, `CARD_LIST`, `FEATURE_GRID`, `TIMELINE`, `DOWNLOAD`.

## Knowledge Graph Criado

WikiDev:

- Nodes: `Spring Boot`, `JPA`, `Docker`.
- Edges: `Spring Boot -> JPA` (`INTEGRATES_WITH`) e `Spring Boot -> Docker` (`PACKAGED_BY`).

Loki:

- Nodes: `Vigília`, `Clair de Lune - Debussy`, `Manifesto do Silêncio`, `Playlist: Introspecção`, `Fragmentos`.
- Edges: `Vigília -> Clair de Lune - Debussy` (`INSPIRED_BY`), `Manifesto do Silêncio -> Playlist: Introspecção` (`INSPIRED_BY`) e `Vigília -> Fragmentos` (`PART_OF`).

Maestro Beton:

- Nodes: `Maestro Beton`, `Página Home`, `Form: Orçamento`, `Institucional`, `Marina Costa`.
- Edges: `CONTAINS`, `BELONGS_TO` e `WRITTEN_BY` conectando produto, página, form, categoria e autora.

## GraphNodeType

Valores adicionados ao enum Java:

- `TENANT`
- `PLAYLIST`
- `MUSIC_REF`
- `MANIFEST`

Valores já existentes e usados pela migration:

- `TOPIC`
- `POEM`
- `BOOK`
- `PRODUCT`
- `PAGE`
- `FORM`
- `CATEGORY`
- `CONTENT`

Também foram adicionados valores em `GraphEdgeType` para suportar o grafo real:

- `INTEGRATES_WITH`
- `PACKAGED_BY`
- `WRITTEN_BY`

## Seed Java Removido

O package `br.com.byop.aegis.seed` foi removido por completo:

- `DemoSeedRunner`
- `DemoSeedService`

Também foi removido de `application-local.yml` o bloco `aegis.seed.*`, incluindo a senha local do seed. O seed não depende mais do profile `local` nem de execução Java em runtime; os dados reais passam a entrar por Flyway.

## NamedInterfaces Removidas ou Mantidas

Removidas por serem exclusivas do seed Java:

- `tenant.api.TenantSeedService`
- `tenant.api.TenantSeedCommand`
- `tenant.api.TenantSeedReference`
- `product.api.ProductSeedService`
- `product.api.ProductSeedCommand`
- `product.api.ProductSeedReference`
- `knowledgegraph.api.KnowledgeGraphSeedService`
- `knowledgegraph.api.GraphSeedNodeCommand`
- `knowledgegraph.api.GraphSeedEdgeCommand`
- `knowledgegraph.api.GraphSeedNodeReference`

Mantida:

- `notification.api.NotificationOnboardingService`, porque não era exclusiva do seed: continua usada por `TenantUserService` no onboarding de usuários convidados.

Criada:

- `settings.api.TelegramAlertSettingsService` como API pública do módulo `settings`, exposta por `@NamedInterface("settings-api")`.

## Retrofits Concluídos

### Auditoria de módulos

`ProductModuleService` passou a receber `AuditService` e os endpoints de enable/disable em `ProductController` passaram a repassar o caller autenticado. As ações registradas são:

- `MODULE_ENABLED`
- `MODULE_DISABLED`

`AuditRiskCatalog` foi atualizado com essas ações. A correção Sonar `java:S6809` foi feita sem self-invocation transacional: os overloads transacionais delegam para métodos privados não anotados.

### Auditoria de submissões

`SubmissionService` passou a registrar `FORM_SUBMISSION_RECEIVED` quando uma submissão pública é aceita. Essa auditoria ocorre sem criar dependência modular indevida, preservando o padrão de integração já usado pelo projeto.

### AuditRequestContext

Criado o contexto HTTP de auditoria:

- `AuditContext`
- `AuditContextHolder`
- `AuditRequestContextInterceptor`
- `AuditWebMvcConfig`

`AuditService` passa a enriquecer eventos com:

- `traceId`
- `ip`
- `userAgent`

Os testes cobrem fallback de valores em branco e limpeza do holder ao final da request.

### Feedback Telegram

`TelegramFeedbackNotifier` implementa o fluxo interno do Aegis:

`usuário do Aegis -> Reportar problema -> POST /api/v1/feedback -> banco -> Telegram global do Aegis`

Configuração:

- `aegis.telegram.alert.enabled`
- `aegis.telegram.alert.chat-id`
- `aegis.telegram.alert.bot-token`

Variáveis `.env.example`:

- `AEGIS_TELEGRAM_ALERT_ENABLED`
- `AEGIS_TELEGRAM_ALERT_CHAT_ID`
- `AEGIS_TELEGRAM_ALERT_BOT_TOKEN`

Esse fluxo não consulta settings de produto, tenant ou formulário.

### Telegram de submissions de produto

`FormSubmissionTelegramNotifier` implementa o fluxo externo de produto:

`plataforma externa -> formulário/submission -> banco -> Telegram configurado no formulário/produto`

O notifier lê `deliveryChannelsJson` do `FormReference` e envia para canais Telegram habilitados com `chatId` e `botToken`. O envio é best-effort: falha externa é logada e não quebra persistência nem response.

### Swagger OAuth2 PKCE

`OpenApiConfig` passou a expor:

- `oauth2-pkce` com authorization code flow;
- `bearer-jwt`, preservado para uso direto com token.

O profile `local` configura Swagger UI com:

- `springdoc.swagger-ui.oauth.client-id`
- `springdoc.swagger-ui.oauth.use-pkce-with-authorization-code-grant=true`
- `springdoc.swagger-ui.oauth2-redirect-url=http://localhost:8080/swagger-ui/oauth2-redirect.html`

## Keycloak Realm Export

`infra/keycloak/realm/aegis-realm.json` foi atualizado para incluir o redirect URI explícito do Swagger UI:

- `http://localhost:8080/swagger-ui/oauth2-redirect.html`

Isso permite validar OAuth2 Authorization Code + PKCE pela UI do Swagger em ambiente local/dev.

## Bruno

Criada a pasta `bruno/30-seed-homologacao`:

- `00-login-super-admin.bru`
- `01-capturar-tenant-clientes-beta.bru`
- `02-capturar-produtos-reais.bru`
- `03-paginas-maestro-beton.bru`
- `04-grafo-loki.bru`
- `05-atualizar-telegram-produto.bru`
- `06-openapi-oauth2-pkce.bru`

Validação executada:

```bash
cd bruno
npx @usebruno/cli run 30-seed-homologacao --env local
```

Resultado:

- 7 requests executados.
- 7 requests aprovados.
- 16/16 testes aprovados.

A pasta também documenta o cenário de banco local antigo com produtos de mesma `key` anteriores à V17. Nesse caso, a migration preserva dados existentes e a pasta valida que os endpoints seguem respondendo, sem exigir que o produto residual tenha as páginas/grafo dos UUIDs fixos da migration.

Execução cumulativa também realizada contra a aplicação local já ativa em `localhost:8080`:

```bash
cd bruno
npx @usebruno/cli run --env local
```

Resultado cumulativo: falhou por divergências já conhecidas em pastas antigas afetadas por estado residual/contratos alterados em sprints anteriores, com 292 requests executados, 267 requests aprovados, 25 requests falhos e 530/569 testes aprovados. A pasta `30-seed-homologacao` passou dentro da execução cumulativa e também isoladamente.

## Testes Executados

Durante a sprint foram executados:

```bash
mvn -DskipTests compile
```

Resultado: `BUILD SUCCESS`.

Também foram executados testes focados cobrindo OpenAPI, feedback, Telegram, settings, submissões, auditoria, módulos, grafo e validação de blocos. Resultado: `BUILD SUCCESS`.

Validação final:

```bash
cd backend
mvn clean verify
```

Resultado final registrado:

- `BUILD SUCCESS`
- 1504 testes
- 0 failures
- 0 errors
- 0 skipped

## Resultado Maven

`mvn clean verify` aprovado com `BUILD SUCCESS`.

## Resultado JaCoCo

JaCoCo aprovado:

```text
All coverage checks have been met.
```

O build analisou 418 classes no check final.

## Resultado Spring Modulith

Spring Modulith aprovado pela suíte completa, incluindo `ModulithArchitectureTest`. As novas integrações entre módulos usam APIs públicas:

- `form.api.FormReferenceService`
- `settings.api.TelegramAlertSettingsService`
- `audit.api.AuditService`
- `product.api` para acesso/módulos já existente

Não foi introduzida dependência direta por entidade, repository ou service interno de outro módulo.

## Resultado SonarQube for IDE

Apontamentos reportados e corrigidos:

- `ProductControllerTest.java` — `java:S6068`: removidos `eq(...)` inúteis em mocks; os valores agora são passados diretamente.
- `ProductModuleService.java` — `java:S6809`: removida chamada direta entre métodos transacionais do mesmo bean; lógica comum extraída para métodos privados sem `@Transactional`.

Não foram adicionados `@SuppressWarnings` ou `NOSONAR`. As duas supressões existentes no projeto (`SecurityConfig` e `AuditEventQueryService`) são preexistentes e fora do escopo desta sprint.

## Divergências Encontradas

- O prompt da sprint sugeria que a migration fizesse `ALTER TYPE graph_node_type`, mas o schema real usa strings/enums Java mapeados por JPA; a implementação necessária foi adicionar os valores ao enum Java `GraphNodeType`.
- O prompt da sprint citava a remoção de NamedInterfaces de seed. No código real não havia `@NamedInterface` dedicado por seed; havia classes dentro dos pacotes `*.api` já expostos. As classes de seed foram removidas, e os `package-info.java` públicos existentes foram preservados porque continuam servindo outras APIs do módulo.
- `NotificationOnboardingService` não foi removido porque continua sendo usado no fluxo real de convite/onboarding.
- Em banco local antigo, `ON CONFLICT (tenant_id, key) DO NOTHING` preserva produtos já existentes com a mesma `key`; por isso páginas/grafo fixos da V17 só são garantidos em banco limpo ou quando os produtos fixos foram de fato inseridos.
- A pasta Bruno 30 foi ajustada para validar o cenário limpo e também o cenário residual local sem exigir reset destrutivo do banco.
- A collection Bruno cumulativa permanece sensível a estado residual de pastas antigas; a execução desta sprint registrou falhas fora do escopo em pastas anteriores, enquanto a pasta 30 passou integralmente.

## Pendências e Recomendações

- Validar manualmente recebimento real no Telegram global do Aegis com `AEGIS_TELEGRAM_ALERT_ENABLED=true`, token e chat reais.
- Validar manualmente submission externa com canal Telegram real configurado no formulário/produto.
- Validar manualmente OAuth2 PKCE na UI do Swagger com Keycloak local aberto no navegador.
- Em homologação limpa, conferir visualmente que os produtos `CLIENTES BETA` apresentam páginas/grafo conforme a migration.
- Se houver necessidade de IDs fixos também em bancos locais antigos, a ação correta é limpeza/migração dirigida por humano; a sprint não executou reset destrutivo.

## Critérios de Aceite Atendidos

- Seed Java removido.
- APIs públicas exclusivas de seed removidas.
- `NotificationOnboardingService` preservado por uso real fora do seed.
- Migration `V17__seed_homologacao.sql` criada e validada pelo Flyway.
- Dados reais de `CLIENTES BETA` seedados por migration.
- Produtos reais de homologação registrados.
- Páginas, seções e Knowledge Graph iniciais registrados.
- Novos valores de `GraphNodeType` e `GraphEdgeType` adicionados e testados.
- Auditoria de enable/disable de módulos implementada.
- Auditoria de submissão pública implementada.
- Contexto HTTP de auditoria (`traceId`, `ip`, `userAgent`) implementado.
- Feedback Telegram global do Aegis implementado.
- Telegram de submission por formulário/produto implementado.
- Swagger OAuth2 PKCE implementado e Bearer JWT preservado.
- Realm export do Keycloak atualizado.
- Bruno 30 criado e validado.
- Documentação de sprint, qualidade, feedback, frontend e API testing atualizada.
- SonarQube for IDE corrigido para os apontamentos reportados.
- `mvn clean verify` aprovado com 1504 testes, 0 falhas, 0 erros, JaCoCo aprovado e Spring Modulith aprovado.
