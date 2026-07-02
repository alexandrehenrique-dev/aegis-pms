# Sprint 25 — Dominio `notification`

Finalizada em 2026-07-02 na branch `sprint/25-notification-finalizacao`.

## Objetivo da sprint

Implementar o dominio `notification` com notificacoes direcionadas, fan-out materializado por usuario, onboarding como notificacao real, endpoints de leitura para o usuario autenticado e endpoints administrativos restritos a SUPER_ADMIN.

## Escopo implementado

- Modulo backend `br.com.byop.aegis.notification`.
- CRUD administrativo minimo de notificacoes: criacao e listagem global para SUPER_ADMIN.
- Leitura do historico do usuario autenticado.
- Resolucao de modal pendente para onboarding/avisos `MODAL_ONCE`.
- Marcacao idempotente de notificacao como mostrada e lida.
- Fan-out materializado para `ALL`, `TENANT` e `USERS`.
- Onboarding como notificacao tecnica sem mecanismo paralelo.
- Integracao com convite de usuario e seed local.
- Sanitizacao de markdown compartilhada sem dependencia direta entre modulos internos.
- Pasta Bruno cumulativa `25-notifications`.

## Classes criadas

- `br.com.byop.aegis.notification.domain.Notification`
- `br.com.byop.aegis.notification.domain.UserNotificationStatus`
- `br.com.byop.aegis.notification.domain.NotificationType`
- `br.com.byop.aegis.notification.domain.NotificationPresentationMode`
- `br.com.byop.aegis.notification.repository.NotificationRepository`
- `br.com.byop.aegis.notification.repository.UserNotificationStatusRepository`
- `br.com.byop.aegis.notification.contract.CreateNotificationRequest`
- `br.com.byop.aegis.notification.dto.NotificationResponse`
- `br.com.byop.aegis.notification.dto.NotificationWithStatus`
- `br.com.byop.aegis.notification.mapper.NotificationMapper`
- `br.com.byop.aegis.notification.service.NotificationService`
- `br.com.byop.aegis.notification.controller.NotificationController`
- `br.com.byop.aegis.notification.exception.NotificationExceptionHandler`
- `br.com.byop.aegis.notification.exception.NotificationNotFoundException`
- `br.com.byop.aegis.notification.exception.NotificationTargetUserNotFoundException`
- `br.com.byop.aegis.notification.exception.InvalidNotificationTargetException`
- `br.com.byop.aegis.notification.exception.InsufficientNotificationRoleException`
- `br.com.byop.aegis.notification.exception.OnboardingNotificationNotFoundException`
- `br.com.byop.aegis.notification.api.NotificationOnboardingService`
- `br.com.byop.aegis.shared.markdown.SharedMarkdownSanitizer`

## Classes alteradas

- `br.com.byop.aegis.api.openapi.OpenApiConfig`
- `br.com.byop.aegis.content.service.MarkdownSanitizer`
- `br.com.byop.aegis.pages.service.PageMarkdownSanitizer`
- `br.com.byop.aegis.product.user.service.TenantUserService`
- `br.com.byop.aegis.seed.DemoSeedService`
- `br.com.byop.aegis.tenant.api.TenantUserAccessService`
- `br.com.byop.aegis.tenant.repository.TenantMembershipRepository`
- testes de `TenantUserService`, `DemoSeedService`, `TenantUserAccessService` e `TenantMembershipRepository`

## Arquivos criados

- `backend/src/main/java/br/com/byop/aegis/notification/**`
- `backend/src/main/java/br/com/byop/aegis/shared/markdown/SharedMarkdownSanitizer.java`
- `backend/src/main/java/br/com/byop/aegis/shared/markdown/package-info.java`
- `backend/src/main/resources/db/migration/V13__notifications.sql`
- `backend/src/test/java/br/com/byop/aegis/notification/**`
- `backend/src/test/java/br/com/byop/aegis/shared/markdown/SharedMarkdownSanitizerTest.java`
- `bruno/25-notifications/**`
- `docs/sprints/backend/results/sprint-25.md`

## Arquivos alterados

- `backend/src/main/java/br/com/byop/aegis/api/openapi/OpenApiConfig.java`
- `backend/src/main/java/br/com/byop/aegis/content/service/MarkdownSanitizer.java`
- `backend/src/main/java/br/com/byop/aegis/pages/service/PageMarkdownSanitizer.java`
- `backend/src/main/java/br/com/byop/aegis/product/user/service/TenantUserService.java`
- `backend/src/main/java/br/com/byop/aegis/seed/DemoSeedService.java`
- `backend/src/main/java/br/com/byop/aegis/tenant/api/TenantUserAccessService.java`
- `backend/src/main/java/br/com/byop/aegis/tenant/repository/TenantMembershipRepository.java`
- testes relacionados a users, seed, tenant access e notification
- `bruno/collection.bru`
- `bruno/environments/{local,dev,homolog,prod}.bru`
- `bruno/24-openapi-testes-checklist-final/openapi-json-completo.bru`
- `docs/api-testing/README.md`
- `docs/sprints/backend/00_padrao_qualidade_e_arquitetura.md`
- `docs/sprints/backend/SPRINT-RESULTADO.md`
- `docs/sprints/backend/25_dominio_notification.md`
- arquivos posteriores de sprint com numeracao corrigida (`26`, `27`, `28`, `29`)
- `docs/trace/00_endpoints_esperados.md`

## Entidades criadas

### `Notification`

Campos principais: `id`, `type`, `title`, `bodyMarkdown`, `presentationMode`, `createdBySubject`, `createdAt`, `updatedAt`.

Tipos suportados: `ONBOARDING`, `FEATURE`, `WARNING`, `MAINTENANCE`, `GENERAL`.

Modos de apresentacao: `MODAL_ONCE`, `BELL_ONLY`.

### `UserNotificationStatus`

Campos principais: `id`, `notification`, `userSubject`, `autoShown`, `read`, `readAt`, `shownAt`, `createdAt`.

Cada status representa uma notificacao para um unico destinatario.

## Migration criada

- `V13__notifications.sql`

A migration cria:

- tabela `notifications`;
- tabela `user_notification_statuses`;
- constraint unica `(notification_id, user_subject)`;
- indices de consulta por usuario, notificacao e tipo;
- notificacao tecnica `ONBOARDING` com UUID fixo `25000000-0000-0000-0000-000000000001`.

## Endpoints confirmados

- `GET /api/v1/notifications/mine`
- `GET /api/v1/notifications/mine/pending-modal`
- `POST /api/v1/notifications/{notificationId}/mark-shown`
- `POST /api/v1/notifications/{notificationId}/mark-read`
- `POST /api/v1/notifications`
- `GET /api/v1/notifications`

## Contratos REST implementados

### `NotificationWithStatus`

```txt
id
type
title
bodyMarkdown
presentationMode
createdAt
autoShown
read
readAt
```

Usado por `GET /notifications/mine` e `GET /notifications/mine/pending-modal`.

### `NotificationResponse`

```txt
id
type
title
bodyMarkdown
presentationMode
createdBySubject
createdAt
updatedAt
```

Usado pelos endpoints administrativos.

### `CreateNotificationRequest`

```txt
type
title
bodyMarkdown
presentationMode
target.type = ALL | TENANT | USERS
target.tenantId
target.userIds
```

## Integracao com onboarding

Onboarding foi implementado como uma notificacao `ONBOARDING`, nao como mecanismo separado. A notificacao base e criada por Flyway e o status do usuario e criado por `NotificationOnboardingService.assignOnboarding(userSubject)`.

## Integracao com users/convite

`TenantUserService` chama `NotificationOnboardingService.assignOnboarding(...)` apos criar a membership do novo usuario. Isso garante que usuario convidado ja nasce com onboarding pendente.

## Seed e fan-out inicial

`DemoSeedService`, ativo somente em profile `local`, atribui onboarding aos cinco usuarios demo depois de garantir usuarios, memberships e assignments. O fan-out dos usuarios demo nao ficou em Flyway porque o `userSubject` real vem do Keycloak e e dinamico.

## Regras de negocio implementadas

- Apenas `ROLE_SUPER_ADMIN` cria notificacoes e lista todas as notificacoes.
- `ALL` resolve todos os subjects distintos com membership ativa em qualquer tenant.
- `TENANT` resolve subjects distintos com membership ativa no tenant informado.
- `USERS` valida todos os subjects via `IdentityUserDirectory` antes de persistir qualquer dado.
- `USERS` com usuario inexistente retorna 404 e nao cria fan-out parcial.
- `pending-modal` considera somente `MODAL_ONCE`, `autoShown=false`, ordenado pela notificacao mais antiga.
- `BELL_ONLY` nunca aparece em `pending-modal`.
- `mark-shown` e `mark-read` sao idempotentes.
- `mark-read` nao exige `autoShown=true`.
- Leitura/marcacao usa sempre o `userSubject` do token, nunca parametro de usuario no body/path.
- Status inexistente para o caller retorna 404, mesmo se a notificacao existir para outro usuario.
- `bodyMarkdown` e sanitizado antes de persistir.

## Decisoes tecnicas tomadas

- Fan-out e materializado em `user_notification_statuses`; nao ha calculo dinamico em leitura.
- Onboarding e uma notificacao tecnica unica, criada por migration.
- Sanitizacao comum foi extraida para `shared.markdown.SharedMarkdownSanitizer`, exposta por `@NamedInterface("markdown")`, evitando dependencia de `pages` em classe interna de `content`.
- `MarkdownSanitizer` e `PageMarkdownSanitizer` permanecem como wrappers de dominio, delegando ao sanitizer compartilhado.
- `NotificationExceptionHandler` foi restrito a `NotificationController` para nao capturar excecoes de outros modulos.
- Queries sobre `notification.id` em `UserNotificationStatusRepository` usam JPQL explicito, nao query derivada por propriedade inexistente.
- Queries JPQL com parametros nomeados usam `@Param` explicito.

## Correcoes de numeracao de etapa/sprint

Durante a sprint foram corrigidas referencias e titulos posteriores que ainda estavam com numeracao desalinhada. A sequencia final documentada ficou:

- Sprint 25: dominio `notification`
- Sprint 26: templates de produto e seed de esqueleto
- Sprint 27: dominio `feedback`
- Sprint 28: backup/exportacao na exclusao
- Sprint 29: tokens de ativacao e reset de senha

Tambem foram ajustadas referencias antigas em resultados e trace docs que apontavam para a numeracao anterior.

## Apontamentos Sonar corrigidos

Rodada final corrigiu os apontamentos reportados pelo SonarQube for IDE sem `@SuppressWarnings`, `NOSONAR` ou desativacao de regra:

- `java:S6829`: `MarkdownSanitizer` e `PageMarkdownSanitizer` mantiveram construtor de teste/default, mas o construtor de injecao recebeu `@Autowired` explicito.
- `java:S7467`: `NotificationService` trocou variavel de excecao nao usada por unnamed pattern no `catch`.
- `java:S5778`: lambdas de `assertThatThrownBy` em `NotificationServiceTest` passaram a conter apenas a chamada sob teste.
- `java:S5976`: cenarios repetidos de target invalido foram consolidados em `@ParameterizedTest` com `@MethodSource`.

## Documentacao de padroes atualizada

- `docs/sprints/backend/00_padrao_qualidade_e_arquitetura.md`: sequencia de etapas e registro de `notification` como dominio nao gateado por modulo de produto.
- `docs/api-testing/README.md`: pasta Bruno `25-notifications`.
- `docs/sprints/backend/SPRINT-RESULTADO.md`: entrada cumulativa da Sprint 25.
- `docs/sprints/backend/results/sprint-25.md`: artefato unico de continuidade da sprint.
- Arquivos de sprint 25-29 e trace docs: numeracao real corrigida.

## Bugs encontrados e corrigidos

- Boot real falhou inicialmente porque queries derivadas em `UserNotificationStatusRepository` tentavam resolver `notificationId` como propriedade JPA direta. Corrigido com JPQL explicito sobre `status.notification.id` e `@Param`.
- Criacao de notificacao `ALL` retornou 500 na aplicacao real porque queries JPQL de `TenantMembershipRepository` usavam parametros nomeados sem `@Param`. Corrigido e coberto por teste de repository.
- Bruno da pasta 25 inicialmente usava `form-urlencoded` em vez de `formUrlEncoded` e senhas demo antigas; ajustado para o formato do Bruno CLI e para a senha local `senha123`.
- A pasta 24 passou a esperar a nova tag `Notifications` no OpenAPI.
- O handler de excecao de notification estava global e poderia traduzir erro de outro modulo como `INVALID_NOTIFICATION_REQUEST`; restringido ao controller de notification.

## Validacoes com Maven

- `mvn -Dtest='NotificationServiceTest,SharedMarkdownSanitizerTest,MarkdownSanitizerTest,PageMarkdownSanitizerTest' test`: **BUILD SUCCESS**, 44 testes, 0 falhas, 0 erros.
- `mvn clean test`: **BUILD SUCCESS**, 1285 testes, 0 falhas, 0 erros.
- `mvn clean verify`: **BUILD SUCCESS**, 1285 testes, 0 falhas, 0 erros.

## Validacoes com JaCoCo

- JaCoCo executado no `mvn clean verify`.
- Resultado: `All coverage checks have been met`.

## Validacoes com Spring Modulith

- `ModulithArchitectureTest`: 1 teste, 0 falhas, 0 erros.
- Fronteiras preservadas por APIs publicas expostas via `NamedInterface`, especialmente `notification.api` e `shared.markdown`.

## Validacoes com Bruno

Pasta criada: `bruno/25-notifications`.

Cenarios cobertos:

- login como super admin;
- criacao `ALL`;
- rejeicao de usuario inexistente em `USERS`;
- criacao `BELL_ONLY`;
- login de usuario comum;
- pending modal com conteudo;
- `mark-shown`;
- pending modal 204;
- listagem `mine`;
- `mark-read`;
- 403 para editor criar/listar admin;
- convite de usuario com onboarding atribuido.

Resultado validado contra aplicacao real em profile `local`:

```txt
Collection cumulativa: 236 requests, 236 aprovados, 459/459 testes aprovados.
```

## Riscos identificados

- O fan-out e materializado no momento da criacao; usuarios adicionados depois nao recebem notificacoes antigas automaticamente, exceto onboarding atribuido explicitamente no convite.
- Notificacoes internas para remocao/restauracao de usuario ainda nao foram implementadas.
- O seed local depende dos subjects reais do Keycloak; por isso o fan-out dos usuarios demo fica em `DemoSeedService`, nao em Flyway.
- A collection Bruno depende de Keycloak/local stack saudavel para logins e convite real.

## Retrofits pendentes

- Implementar notificacoes internas para remocao/restauracao de usuario quando a decisao de produto for tomada.
- A etapa 29 ainda deve substituir `executeActionsEmail(["UPDATE_PASSWORD"])` por tokens Aegis gerenciados.
- Avaliar exemplos OpenAPI/schema mais ricos para notification em sprint futura, se necessario.

## Criterios de aceite atendidos

- [x] Criar notificacao `ALL` gera status para usuarios ativos sem duplicar subjects.
- [x] Criar notificacao `USERS` com usuario inexistente retorna 404 e nao faz fan-out parcial.
- [x] `pending-modal` retorna conteudo quando ha `MODAL_ONCE` pendente e 204 quando nao ha.
- [x] `mark-shown` e `mark-read` sao idempotentes.
- [x] `BELL_ONLY` nunca aparece em `pending-modal`.
- [x] Usuario nao SUPER_ADMIN recebe 403 nos endpoints administrativos.
- [x] Usuario criado por convite recebe onboarding pendente.
- [x] `bodyMarkdown` passa pela mesma sanitizacao compartilhada usada por content/pages.
- [x] Usuario nao consegue marcar notificacao sem `UserNotificationStatus` proprio.
- [x] Spring Modulith preservado sem importar classe interna de outro modulo.
