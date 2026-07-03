# Sprint 27 — Dominio `feedback`

Finalizada em 2026-07-03 na branch `sprint/27-dominio-feedback`.

## Objetivo da sprint

Persistir o feedback submetido pelo usuario pelo fluxo "Reportar problema", com id legivel `AGS-####`, contexto de tenant/produto quando disponivel, anexo opcional reaproveitando o dominio `asset` e endpoints administrativos para triagem.

## Escopo implementado

- Novo modulo `br.com.byop.aegis.feedback`, organizado por `domain`, `repository`, `contract`, `dto`, `mapper`, `service`, `controller` e `exception`.
- Migration `V14__feedback.sql`, com tabela `feedback`, sequence `feedback_public_id_seq`, constraints de categoria/prioridade/status/id publico e indices de tenant/status/criacao.
- Endpoint publico autenticado `POST /api/v1/feedback`.
- Endpoints administrativos `GET /api/v1/feedback`, `GET /api/v1/tenants/{tenantId}/feedback` e `PUT /api/v1/feedback/{feedbackId}/status`.
- API publica `TenantUserAccessService.listActiveMemberships(String userSubject)` para resolver contexto de tenant quando o feedback nasce fora de produto.
- OpenAPI/Swagger atualizado com tag `Feedback` e classificacao dos paths novos.
- Porta publica `notification.api.FeedbackNotificationService` para disparar sino `WARNING`/`BELL_ONLY` quando o feedback nasce com prioridade `crítica`.
- Pasta Bruno `27-feedback`, cobrindo criacao com/sem anexo, anexo cross-tenant, notificacao de feedback critico, listagem global, listagem por tenant, bloqueios por papel e atualizacao de status.

## Classes criadas

- `feedback.domain.Feedback`
- `feedback.domain.FeedbackCategory`
- `feedback.domain.FeedbackPriority`
- `feedback.domain.FeedbackStatus`
- `feedback.repository.FeedbackRepository`
- `feedback.contract.CreateFeedbackRequest`
- `feedback.contract.UpdateFeedbackStatusRequest`
- `feedback.dto.FeedbackSummary`
- `feedback.mapper.FeedbackMapper`
- `feedback.service.FeedbackService`
- `feedback.controller.FeedbackController`
- `feedback.exception.FeedbackNotFoundException`
- `feedback.exception.FeedbackForbiddenException`
- `feedback.exception.FeedbackContextRequiredException`
- `feedback.exception.FeedbackAttachmentNotFoundException`
- `feedback.exception.FeedbackExceptionHandler`
- `notification.api.CriticalFeedbackNotificationRequest`
- `notification.api.FeedbackNotificationService`

## Classes alteradas

- `tenant.api.TenantUserAccessService` — adicionadas consultas publicas de memberships ativas por subject e de subjects `SUPER_ADMIN` ativos.
- `tenant.repository.TenantMembershipRepository` — query publica do modulo tenant para subjects ativos por papel.
- `notification.service.NotificationService` — metodo `notifyCriticalFeedback(...)` com fan-out para super-admins ativos.
- `api.openapi.OpenApiConfig` — tag e paths de `Feedback`.
- `api.openapi.OpenApiConfigTest` — cobertura da classificacao OpenAPI de feedback.
- `tenant.api.TenantUserAccessServiceTest` — cobertura da nova API publica.

## Endpoints confirmados

- `POST /api/v1/feedback` — qualquer usuario autenticado cria feedback.
- `GET /api/v1/feedback` — somente `SUPER_ADMIN`, todos os tenants.
- `GET /api/v1/tenants/{tenantId}/feedback` — `SUPER_ADMIN` ou `TENANT_ADMIN` do proprio tenant.
- `PUT /api/v1/feedback/{feedbackId}/status` — somente `SUPER_ADMIN`, usando o id legivel `AGS-####`.

## Decisoes tecnicas tomadas

1. O id legivel e sequencial via PostgreSQL (`feedback_public_id_seq`) e formatado como `AGS-%04d`. O UUID interno existe apenas para persistencia e relacionamento, nunca como identificador principal do contrato REST.
2. `POST /feedback` com `productId` resolve tenant pelo proprio produto usando `ProductAccessPort.assertAccessible(...)` e `ProductReferenceService.getRequiredReference(...)`.
3. `POST /feedback` sem `productId` resolve o tenant pelas memberships ativas do usuario. Se houver exatamente uma membership ativa, ela define o tenant; se houver zero ou mais de uma, o backend retorna 400 `FEEDBACK_CONTEXT_REQUIRED` para evitar atribuir o feedback ao tenant errado.
4. `attachmentAssetId` e validado pelo `asset.api.AssetReferenceService`; o tenant do asset e derivado pelo produto do asset via `ProductReferenceService`. Asset de outro tenant retorna 404 `FEEDBACK_ATTACHMENT_NOT_FOUND`.
5. `GET /tenants/{tenantId}/feedback` usa isolamento por papel: `SUPER_ADMIN` lista apos validar que o tenant existe; `TENANT_ADMIN` precisa ter authority `ROLE_TENANT_ADMIN` e membership ativa `TENANT_ADMIN` naquele tenant. Ausencia de membership retorna 404 para nao vazar existencia/escopo.
6. Categorias e prioridades aceitam variantes sem acento/case no input, mas respondem no contrato canonico pedido pelo artefato (`"média"`, `"crítica"`, `"UX confusa"`, etc.).
7. Feedback nao e module-gated. Reportar problema continua sendo capacidade transversal do produto, nao dependente de `PAGES`, `CONTENT` ou outro modulo.
8. Feedback critico (`priority = "crítica"`) dispara notificacao interna `WARNING`/`BELL_ONLY` para os subjects com membership ativa `SUPER_ADMIN`, via a porta publica `notification.api.FeedbackNotificationService`. O modulo `feedback` nao chama `NotificationService.create(...)` diretamente nem acessa internals de `notification` ou `tenant`; `notification` resolve destinatarios por `tenant.api.TenantUserAccessService.listActiveSuperAdminSubjects()`.

## Bruno

Criada a pasta `bruno/27-feedback` com 23 requests. A pasta e autossuficiente para o seed local: captura o tenant `clientes-beta` pela API de tenants antes de autenticar `editor@byop.io`/`admin@byop.io`.

Resultado da pasta 27 isolada contra a aplicacao local atual (`baseUrl=http://localhost:8081`, backend iniciado a partir do codigo desta branch): **23 requests, 23 aprovados, 40/40 testes aprovados**.

Execucao cumulativa completa (`npx @usebruno/cli run --env local`) foi iniciada contra o Postgres local de longa duracao e apresentou falhas pre-existentes em pastas antigas por estado residual/dados ja criados, como ja documentado na Sprint 26. A validacao especifica desta sprint foi confirmada pela pasta 27 isolada contra a build atual.

## Validação Maven

Execucao final obrigatoria:

```bash
mvn clean verify
```

Resultado: **BUILD SUCCESS**, **1378 testes**, 0 falhas, 0 erros, 0 ignorados.

## JaCoCo e Spring Modulith

JaCoCo aprovado: `All coverage checks have been met`.

Spring Modulith aprovado pela suite completa, incluindo `ModulithArchitectureTest`; nenhuma dependencia ciclica introduzida. O modulo `feedback` consome somente APIs publicas de `product`, `asset`, `tenant` e `notification`.

## Sonar/imports/pacotes

Nenhum `@SuppressWarnings` ou `NOSONAR` foi adicionado. Os testes novos evitam `@MockBean`/`@SpyBean` e seguem os padroes de lambdas do AssertJ registrados no `AGENTS.md`.

A reanalise SonarQube for IDE fica pendente na IDE local, pois nao ha runner CLI do SonarQube for IDE neste ambiente. A implementacao foi revisada contra os padroes ja registrados no repositorio.

## Retrofits pendentes

- Dispatch Telegram do feedback interno fica para a etapa 30 e deve usar configuracao global do Aegis (`aegis.telegram.alert.*`), nao configuracao de produto.
- Configuracao Telegram por produto/formulario fica em fluxo separado de submission/canais de entrega; nao controla o `POST /feedback` interno do Aegis.
- Ajustar o frontend `FeedbackModal.tsx` em sprint frontend para usar `<input type="file">` real, fazer upload via `POST /products/{productId}/assets` quando houver produto e enviar o `attachmentAssetId` no `POST /feedback`.

## Critérios de aceite atendidos

- [x] `POST /feedback` funciona para qualquer papel autenticado e persiste o registro.
- [x] Resposta de criacao devolve id legivel `AGS-####`, nao UUID interno.
- [x] `attachmentAssetId` valido do mesmo tenant e aceito; asset de outro tenant e rejeitado.
- [x] `GET /feedback` global so funciona para `SUPER_ADMIN`.
- [x] `GET /tenants/{tenantId}/feedback` funciona para `SUPER_ADMIN`/`TENANT_ADMIN` do proprio tenant; tenant alheio retorna 404.
- [x] `PUT .../status` so funciona para `SUPER_ADMIN`.
- [x] Feedback `priority = "crítica"` cria notificacao `WARNING`/`BELL_ONLY` para super-admins ativos.
- [x] `mvn clean verify` com `BUILD SUCCESS`, JaCoCo e Spring Modulith aprovados.
