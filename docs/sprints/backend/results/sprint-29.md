# Sprint 29 — Tokens de ativação de convite e reset de senha via Aegis

Concluída tecnicamente em 2026-07-03 na branch `sprint/29-auth-action-tokens-finalizacao`.

## Objetivo

Substituir os e-mails de ação do Keycloak por tokens gerenciados pelo Aegis, com links para o frontend (`/invite?token=...` e `/reset-password?token=...`) e confirmação de senha via backend, sem expor a tela nativa do Keycloak ao usuário final.

## Escopo Implementado

- Persistência de tokens de ação (`AuthActionToken`) para convite e reset de senha.
- Endpoints públicos para validar convite, ativar convite, solicitar reset e confirmar reset.
- Fluxos de convite de tenant, resend/restore e convite por produto usando token Aegis.
- Reset de senha com mensagem anti-enumeração, expiração de tokens pendentes anteriores e rate limit manual.
- Integração com Keycloak Admin API para definir senha, habilitar usuário e limpar `requiredActions`.
- Templates de e-mail próprios do Aegis apontando para o frontend.
- Auditoria via evento público de identidade consumido pelo módulo `audit`, sem dependência direta `identity -> audit`.
- Pasta Bruno 29 com fluxo isolado de validação por MailHog.
- Correções SonarQube for IDE reportadas em 2026-07-03.

## Fora de Escopo

- Implementar frontend real para `/invite` e `/reset-password`.
- Trocar política de senha do realm Keycloak.
- Criar endpoint administrativo para emitir token manualmente.
- Remover o método legado `executeActionsEmail` do `KeycloakAdminClient`; ele permanece para debug/compatibilidade, fora dos fluxos produtivos.
- Validação manual de login pós-ativação no frontend real; documentada como pendência operacional por depender das telas `/invite` e `/reset-password`.

## Arquivos Criados

- `backend/src/main/java/br/com/byop/aegis/audit/service/IdentityAuthActionAuditListener.java`
- `backend/src/main/java/br/com/byop/aegis/identity/api/IdentityActionInviteCommand.java`
- `backend/src/main/java/br/com/byop/aegis/identity/api/IdentityActionTokenService.java`
- `backend/src/main/java/br/com/byop/aegis/identity/api/IdentityAuthActionAuditEvent.java`
- `backend/src/main/java/br/com/byop/aegis/identity/auth/controller/AuthActionErrorResponse.java`
- `backend/src/main/java/br/com/byop/aegis/identity/auth/domain/AuthActionStatus.java`
- `backend/src/main/java/br/com/byop/aegis/identity/auth/domain/AuthActionToken.java`
- `backend/src/main/java/br/com/byop/aegis/identity/auth/domain/AuthActionType.java`
- `backend/src/main/java/br/com/byop/aegis/identity/auth/dto/AuthActionTokenRequest.java`
- `backend/src/main/java/br/com/byop/aegis/identity/auth/dto/AuthInviteValidationResponse.java`
- `backend/src/main/java/br/com/byop/aegis/identity/auth/dto/AuthPasswordActionRequest.java`
- `backend/src/main/java/br/com/byop/aegis/identity/auth/dto/AuthPasswordResetRequest.java`
- `backend/src/main/java/br/com/byop/aegis/identity/auth/exception/AuthActionTokenExpiredException.java`
- `backend/src/main/java/br/com/byop/aegis/identity/auth/exception/AuthActionTokenNotFoundException.java`
- `backend/src/main/java/br/com/byop/aegis/identity/auth/exception/AuthActionTokenUsedException.java`
- `backend/src/main/java/br/com/byop/aegis/identity/auth/exception/AuthRateLimitExceededException.java`
- `backend/src/main/java/br/com/byop/aegis/identity/auth/exception/WeakPasswordException.java`
- `backend/src/main/java/br/com/byop/aegis/identity/auth/repository/AuthActionTokenRepository.java`
- `backend/src/main/java/br/com/byop/aegis/identity/auth/service/AuthActionEmailService.java`
- `backend/src/main/java/br/com/byop/aegis/identity/auth/service/AuthActionTokenCleanupJob.java`
- `backend/src/main/java/br/com/byop/aegis/identity/auth/service/AuthActionTokenService.java`
- `backend/src/main/java/br/com/byop/aegis/identity/auth/service/AuthActivationService.java`
- `backend/src/main/java/br/com/byop/aegis/identity/auth/service/PasswordPolicy.java`
- `backend/src/main/resources/db/migration/V16__auth_action_tokens.sql`
- `backend/src/test/java/br/com/byop/aegis/audit/service/IdentityAuthActionAuditListenerTest.java`
- `backend/src/test/java/br/com/byop/aegis/identity/api/IdentityActionTokenServiceTest.java`
- `backend/src/test/java/br/com/byop/aegis/identity/auth/domain/AuthActionTokenTest.java`
- `backend/src/test/java/br/com/byop/aegis/identity/auth/repository/AuthActionTokenRepositoryTest.java`
- `backend/src/test/java/br/com/byop/aegis/identity/auth/service/AuthActionEmailServiceTest.java`
- `backend/src/test/java/br/com/byop/aegis/identity/auth/service/AuthActionTokenCleanupJobTest.java`
- `backend/src/test/java/br/com/byop/aegis/identity/auth/service/AuthActionTokenServiceTest.java`
- `backend/src/test/java/br/com/byop/aegis/identity/auth/service/AuthActivationServiceTest.java`
- `backend/src/test/java/br/com/byop/aegis/identity/auth/service/PasswordPolicyTest.java`
- `bruno/29-auth-action-tokens/**`
- `docs/sprints/backend/results/sprint-29.md`
- `infra/keycloak/themes/aegis/email/html/inviteActivation.ftl`
- `infra/keycloak/themes/aegis/email/html/passwordReset.ftl`

## Arquivos Alterados

- `.env.example`
- `backend/src/main/java/br/com/byop/aegis/identity/api/IdentityUserLifecycleService.java`
- `backend/src/main/java/br/com/byop/aegis/identity/auth/client/KeycloakAdminClient.java`
- `backend/src/main/java/br/com/byop/aegis/identity/auth/controller/AuthController.java`
- `backend/src/main/java/br/com/byop/aegis/identity/auth/controller/AuthExceptionHandler.java`
- `backend/src/main/java/br/com/byop/aegis/identity/auth/service/AuthService.java`
- `backend/src/main/java/br/com/byop/aegis/product/service/KeycloakProductAssignmentInvitePort.java`
- `backend/src/main/java/br/com/byop/aegis/product/service/ProductAssignmentInvitePort.java`
- `backend/src/main/java/br/com/byop/aegis/product/service/ProductAssignmentService.java`
- `backend/src/main/java/br/com/byop/aegis/product/user/service/TenantUserService.java`
- `backend/src/main/resources/application-local.yml`
- `backend/src/test/java/br/com/byop/aegis/api/openapi/OpenApiConfigTest.java`
- `backend/src/test/java/br/com/byop/aegis/identity/auth/client/KeycloakAdminClientTest.java`
- `backend/src/test/java/br/com/byop/aegis/identity/auth/controller/AuthControllerTest.java`
- `backend/src/test/java/br/com/byop/aegis/identity/auth/controller/AuthExceptionHandlerTest.java`
- `backend/src/test/java/br/com/byop/aegis/identity/auth/service/AuthServiceTest.java`
- `backend/src/test/java/br/com/byop/aegis/product/service/KeycloakProductAssignmentInvitePortTest.java`
- `backend/src/test/java/br/com/byop/aegis/product/service/ProductAssignmentPortStubTest.java`
- `backend/src/test/java/br/com/byop/aegis/product/service/ProductAssignmentServiceTest.java`
- `backend/src/test/java/br/com/byop/aegis/product/user/service/TenantUserServiceTest.java`
- `backend/src/test/java/br/com/byop/aegis/security/SecurityConfigTest.java`
- `bruno/collection.bru`
- `docker-compose.yml`
- `docs/api-testing/README.md`
- `docs/sprints/backend/SPRINT-RESULTADO.md`

## Arquivos Removidos

Nenhum arquivo foi removido.

## Endpoints Confirmados

- `POST /api/v1/auth/invite/validate`
- `POST /api/v1/auth/activate`
- `POST /api/v1/auth/reset-password/request`
- `POST /api/v1/auth/reset-password/confirm`

Todos permanecem públicos por estarem sob `/api/v1/auth/**`.

## Contratos Implementados

- `AuthActionTokenRequest`: `{ "token": "<uuid>" }`
- `AuthPasswordActionRequest`: `{ "token": "<uuid>", "password": "<senha>" }`
- `AuthPasswordResetRequest`: `{ "email": "user@example.com" }`
- `AuthInviteValidationResponse`: `userName`, `userEmail`, `tenantName`, `productNames`, `role`, `inviterName`, `expiresAt`
- `AuthMessageResponse`: `message`
- `AuthActionErrorResponse`: `error`, `message`

Erros implementados:

- `TOKEN_ALREADY_USED` com HTTP 400.
- `TOKEN_EXPIRED` com HTTP 410.
- `TOKEN_NOT_FOUND` com HTTP 404.
- `WEAK_PASSWORD` com HTTP 422.
- `TOO_MANY_REQUESTS` com HTTP 429 e header `Retry-After`.

## Entidade Criada

`AuthActionToken` persiste:

- `id`
- `keycloakId`
- `userEmail`
- `userName`
- `type`
- `status`
- `tenantId`
- `tenantName`
- `productNames`
- `role`
- `inviterName`
- `createdAt`
- `expiresAt`
- `usedAt`

## Enums Criados

- `AuthActionType`: `INVITE`, `PASSWORD_RESET`
- `AuthActionStatus`: `PENDING`, `USED`, `EXPIRED`

## Migration Flyway Criada

`V16__auth_action_tokens.sql`

- Cria tabela `auth_action_tokens`.
- Cria índice `idx_aat_status_expires`.
- Cria índice `idx_aat_email_type_created`.

Observação: o artefato da sprint mencionava `V28__auth_action_tokens.sql`, mas o repositório real já estava na migration `V15` após a Sprint 28. O número correto aplicado nesta sprint é `V16`.

## Serviços Criados ou Alterados

Criados:

- `AuthActionTokenService`
- `AuthActionEmailService`
- `AuthActionTokenCleanupJob`
- `AuthActivationService`
- `PasswordPolicy`
- `IdentityActionTokenService`
- `IdentityAuthActionAuditListener`

Alterados:

- `AuthService`
- `KeycloakAdminClient`
- `IdentityUserLifecycleService`
- `TenantUserService`
- `ProductAssignmentService`
- `ProductAssignmentInvitePort`
- `KeycloakProductAssignmentInvitePort`

## Fluxo de Convite

1. Fluxos autenticados de convite chamam a API pública interna `IdentityActionTokenService`.
2. `AuthActionTokenService` cria token `INVITE` pendente com expiração de 48 horas.
3. `AuthActionEmailService` envia `inviteActivation.ftl`.
4. Link aponta para `${AEGIS_APP_BASE_URL}/invite?token={tokenId}`.
5. O token não é retornado no contrato REST de convite.

## Fluxo de Ativação

1. `POST /api/v1/auth/invite/validate` valida token `INVITE` pendente e retorna contexto do convite.
2. `POST /api/v1/auth/activate` valida senha mínima no backend.
3. Token é consumido como `USED`.
4. Backend chama Keycloak Admin API para `reset-password`.
5. Backend habilita usuário no Keycloak.
6. Backend limpa `requiredActions`.
7. Auditoria publica `USER_INVITE_ACTIVATED`.

## Fluxo de Reset Request

1. `POST /api/v1/auth/reset-password/request` recebe e-mail.
2. Resposta é sempre genérica para evitar enumeração.
3. Se usuário existir, tokens `PASSWORD_RESET` pendentes anteriores do mesmo usuário expiram.
4. Novo token `PASSWORD_RESET` é criado com expiração de 1 hora.
5. `passwordReset.ftl` é enviado com link `${AEGIS_APP_BASE_URL}/reset-password?token={tokenId}`.
6. Auditoria publica `PASSWORD_RESET_REQUESTED`.

## Fluxo de Reset Confirm

1. `POST /api/v1/auth/reset-password/confirm` valida senha mínima no backend.
2. Token `PASSWORD_RESET` é validado e consumido.
3. Backend chama Keycloak Admin API para `reset-password`.
4. Auditoria publica `PASSWORD_RESET_COMPLETED`.

## Rate Limit

Implementado sem biblioteca externa:

- 3 tokens `PASSWORD_RESET` por e-mail em janela de 15 minutos.
- A quarta solicitação retorna HTTP 429.
- Header `Retry-After` retorna o tempo de espera em segundos.

## Integração com Keycloak Admin API

`KeycloakAdminClient` ganhou:

- `resetPassword(String userId, String password)`
- `clearRequiredActions(String userId)`

O fluxo produtivo de convite/reset não chama mais `executeActionsEmail`. A busca por `executeActionsEmail(` em `backend/src/main/java` mostra apenas wrappers/client legado:

- `KeycloakAdminClient.executeActionsEmail(...)`
- `KeycloakAdminClient.sendResetPasswordEmail(...)` legado
- `IdentityUserLifecycleService.executeActionsEmail(...)` legado

## Templates de E-mail

Criados:

- `infra/keycloak/themes/aegis/email/html/inviteActivation.ftl`
- `infra/keycloak/themes/aegis/email/html/passwordReset.ftl`

Alterados/configurados:

- `.env.example`
- `backend/src/main/resources/application-local.yml`
- `docker-compose.yml`

`docker-compose.yml`, `.env.example` e o profile local receberam fallback/configuração `AEGIS_APP_BASE_URL=http://localhost:5173` para links do frontend em ambiente local.

## Segurança e Endpoints Públicos

Os endpoints novos ficam sob `/api/v1/auth/**`, já liberado pelo `SecurityConfig`.

Decisão de segurança:

- Token é enviado no body do `POST /invite/validate`, não em query string.
- Token é a autorização da ação, assim como links temporários de ação.
- Reset de senha não revela se o e-mail existe.
- Convite e reset marcam token como `USED` ao consumir.

## Auditoria

Criado evento público `IdentityAuthActionAuditEvent` no módulo `identity.api`.

O módulo `audit` consome esse evento em `IdentityAuthActionAuditListener`, registrando:

- `USER_INVITE_ACTIVATED`
- `PASSWORD_RESET_REQUESTED`
- `PASSWORD_RESET_COMPLETED`

A integração por evento evitou ciclo Spring Modulith `identity -> audit -> identity`.

## Bruno

Criada a pasta `bruno/29-auth-action-tokens` com fluxo isolado:

1. Login Loki.
2. Criar tenant descartável.
3. Gerar convite de tenant.
4. Capturar token de convite no MailHog.
5. Validar convite.
6. Validar senha fraca.
7. Ativar convite.
8. Solicitar reset de senha.
9. Capturar token de reset no MailHog.
10. Confirmar reset.
11. Validar token inexistente.

`bruno/collection.bru` recebeu defaults para:

- `authActionTenantKey`
- `authActionAdminEmail`
- `authActionInviteEmail`
- `mailhogBaseUrl`
- `inviteActivationToken`
- `passwordResetToken`
- `inviteActivationPassword`
- `newPassword`
- `resetPasswordEmail`

`docs/api-testing/README.md` foi atualizado com a pasta e variáveis da Sprint 29.

## OpenAPI

Não houve alteração em código produtivo de OpenAPI. `OpenApiConfigTest` foi ajustado para manter a suíte cobrindo a classificação/tagueamento após os novos endpoints de auth.

## Testes Executados

- `mvn -q -Dtest='KeycloakAdminClientTest' test`
- `mvn -q -Dtest='AuthControllerTest,AuthExceptionHandlerTest,PasswordPolicyTest,AuthActivationServiceTest,KeycloakProductAssignmentInvitePortTest' test`
- `mvn clean verify`

## Resultado Maven

Último `mvn clean verify` executado em `backend`:

- `BUILD SUCCESS`
- 1502 testes
- 0 falhas
- 0 erros
- 0 ignorados

## Resultado JaCoCo

JaCoCo aprovado:

- `All coverage checks have been met.`

## Resultado Spring Modulith

Spring Modulith aprovado pela suíte completa:

- `ModulithArchitectureTest`
- 1 teste
- 0 falhas
- 0 erros

## Resultado WireMock

`KeycloakAdminClientTest` cobre chamadas HTTP ao Keycloak Admin API com WireMock, incluindo ausência de chamadas produtivas a `execute-actions-email` nos convites novos e chamadas de reset/limpeza necessárias ao fluxo de ação Aegis.

Um cenário de reset público foi ajustado para `MockRestServiceServer` para evitar flake de socket/WireMock observado durante a validação, sem reduzir cobertura do contrato HTTP.

## Resultado MailHog

Validado em 2026-07-03 com backend local (`mvn spring-boot:run -Dspring-boot.run.profiles=local`), Keycloak em `localhost:8282` e MailHog em `localhost:8025`.

Confirmados links gerados nos e-mails:

- `http://localhost:5173/invite?token=<uuid>`
- `http://localhost:5173/reset-password?token=<uuid>`

A pasta Bruno 29 captura tokens pela API do MailHog (`/api/v2/messages`) e normaliza quoted-printable antes de extrair o UUID do link.

## Resultado Bruno

A pasta `bruno/29-auth-action-tokens` foi executada contra a stack local real em 2026-07-03:

```bash
cd bruno
npx @usebruno/cli run 29-auth-action-tokens --env local --output results --format json
```

Resultado:

- `PASS`
- 11 requests executados
- 11 requests aprovados
- 22 testes executados
- 22 testes aprovados
- 0 failed requests
- 0 error requests

## Resultado SonarQube for IDE

Rodada reportada em 2026-07-03 corrigiu 7 apontamentos sem `@SuppressWarnings`, `NOSONAR` ou desativação de regra:

- `java:S2068` em `AuthActionEmailService` e `PasswordPolicy`: nomes internos contendo `PASSWORD` foram trocados por nomes semânticos neutros.
- `java:S7467` em `AuthActivationService`: `catch` com variável não usada trocado por unnamed variable.
- `java:S1874` em `AuthControllerTest` e `AuthExceptionHandler`: `UNPROCESSABLE_ENTITY`/`isUnprocessableEntity` trocados por `UNPROCESSABLE_CONTENT`/`isUnprocessableContent`.
- `java:S1128` em `AuthService`: import morto removido.
- `java:S1161` em `KeycloakProductAssignmentInvitePort`: `@Override` adicionado ao overload contextual.

Após essas correções, `mvn clean verify` foi executado novamente com sucesso.

## Decisões Técnicas Tomadas

- Token Aegis substitui o e-mail de ação do Keycloak, mas Keycloak continua sendo IAM.
- `AuthActionToken` usa UUID como token de ação.
- Convite e reset têm expirações diferentes: 48h e 1h.
- O token é validado por `POST` com body para reduzir vazamento em logs de URL.
- A decisão de nulidade/ausência de usuário no reset fica no fluxo de serviço, preservando resposta anti-enumeração.
- Auditoria cruza módulo por evento público, não por chamada direta de service interno.
- `executeActionsEmail` permanece apenas como legado/debug para não quebrar APIs internas existentes.

## Divergências Encontradas na Documentação

- O prompt de finalização pedia `aegis-pms/README.md`, mas a raiz do repositório real não possui `README.md`. A documentação de entrada existente e lida foi `docs/README.md`.
- A sprint mencionava migration `V28__auth_action_tokens.sql`; o repositório real estava em `V15` após a Sprint 28, portanto a migration correta criada foi `V16__auth_action_tokens.sql`.
- A documentação da Sprint 15 ainda mencionava restauração via `executeActionsEmail`; nesta sprint o fluxo produtivo foi substituído por token Aegis.

## Retrofits Pendentes

- Implementar/validar telas frontend reais `/invite` e `/reset-password`.
- Validar login real no frontend após ativação e após reset quando as telas existirem.
- Avaliar remoção futura dos wrappers legados `executeActionsEmail` quando não forem mais necessários para debug.

## Critérios de Aceite Atendidos

- Endpoint de validação de convite implementado.
- Erros de token usado, expirado e inexistente implementados.
- Ativação com senha válida implementada com Keycloak Admin API.
- Senha fraca retorna `WEAK_PASSWORD`.
- Reset request retorna resposta genérica anti-enumeração.
- Rate limit de reset implementado com HTTP 429.
- Reset confirm consome token e chama Keycloak Admin API.
- Job de expiração implementado e testado.
- Fluxos produtivos de convite/reset não chamam mais `executeActionsEmail`.
- `mvn clean verify` aprovado.
- JaCoCo aprovado.
- Spring Modulith aprovado.
- SonarQube for IDE corrigido conforme apontamentos reportados.
- Bruno 29 aprovado contra backend, Keycloak e MailHog locais.
- MailHog validado com links `localhost:5173` para convite e reset.
