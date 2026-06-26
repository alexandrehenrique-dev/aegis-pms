# Sprint 10 — Tenants CRUD completo, escopo por papel e ProductAssignment

## Identificacao

- **Sprint:** 10 — Tenants CRUD completo, escopo por papel e ProductAssignment
- **Status:** Concluida em 2026-06-26
- **Branch:** `sprint/10-rodada-1-product-assignment`
- **Resultado local:** BUILD SUCCESS

## Resumo Executivo

A Sprint 10 fechou o fluxo backend de Super Admin para criar tenant, editar/excluir tenant, listar tenants por escopo de papel e atribuir usuarios a produtos. Tambem integrou convites e e-mails reais usando Keycloak Admin API, JavaMailSender, FreeMarker, tema Aegis e MailHog em desenvolvimento.

## Escopo Entregue

- CRUD completo de tenant: `POST`, `GET`, `PUT` e `DELETE`.
- `GET /api/v1/tenants` retorna todos os tenants para `SUPER_ADMIN` e mantem filtro por membership ativa para os demais papeis.
- `POST /api/v1/tenants` aceita `plan` e `initialAdminEmail`.
- `ProductAssignment` com contrato REST proprio, status `"atribuido"`/`"convidado"` e constraint unica por produto/usuario.
- Endpoints de usuarios atribuidos ao produto: listar, criar e remover.
- Validacoes de XOR `userId`/`inviteEmail`, `productId` divergente, `tenantId` divergente e usuario fora do tenant.
- E-mails de convite, atribuicao e revogacao validados em MailHog.

## Arquivos Criados/Alterados por Area

### Backend — Tenant

- `CreateTenantRequest`, `UpdateTenantRequest`, `DeleteTenantRequest`
- `CreateTenantCommand`
- `Tenant`, `TenantSummary`, `TenantMapper`
- `TenantAccessService`, `TenantReference`
- `TenantService`, `TenantController`
- `InvalidTenantConfirmationException`, `InvalidTenantStatusException`, `TenantExceptionHandler`

### Backend — ProductAssignment

- `AssignProductUserRequest`
- `ProductAssignmentSummary`
- `ProductAssignment`, `ProductAssignmentStatus`
- `ProductAssignmentRepository`
- `ProductAssignmentMapper`
- `ProductAssignmentService`
- `ProductAssignmentController`
- `InvalidProductAssignmentException`, `ProductAssignmentNotFoundException`, `ProductExceptionHandler`

### Backend — Identidade, Convite e E-mail

- `identity.api.IdentityInvitationService`
- `identity.api.IdentityUser`
- `identity.api.IdentityUserDirectory`
- `KeycloakAdminClient`
- `ProductAssignmentInvitePort`
- `KeycloakProductAssignmentInvitePort`
- `ProductAssignmentEmailPort`
- `FreemarkerProductAssignmentEmailPort`
- `ProductAssignmentEmailCommand`
- `ProductAssignmentNotificationPort`
- `StubProductAssignmentInvitePort`
- `StubProductAssignmentEmailPort`
- `StubProductAssignmentNotificationPort`

### Persistencia, Infra e Postman

- `V4__product_assignment_contract.sql`
- `application.yml`
- `infra/keycloak/realm/aegis-realm.json`
- `infra/keycloak/themes/aegis/email/theme.properties`
- `infra/keycloak/themes/aegis/email/messages/messages_en.properties`
- `infra/keycloak/themes/aegis/email/messages/messages_pt_BR.properties`
- `postman/aegis-postman-collection.json`
- `postman/.postman/resources.yaml`
- `postman/postman/collections/Aegis PMS - Backend API Collection/`

### Testes

- Testes de repository, mapper, service, controller, contratos e portas de ProductAssignment.
- Testes de Tenant para update/delete/listagem por papel.
- Testes de Keycloak/Admin client, identity API e renderizacao de e-mail.
- Ajustes de cobertura em testes existentes de core, product e tenant.

## Contratos REST Implementados

### Tenant

- `POST /api/v1/tenants`
  - Request inclui `name`, `slug`, `plan`, `initialAdminEmail`.
  - Response continua no formato de resumo/opcao de tenant.
- `GET /api/v1/tenants`
  - `SUPER_ADMIN`: todos os tenants.
  - Demais papeis: somente memberships ativas do usuario.
- `PUT /api/v1/tenants/{tenantId}`
  - Atualiza `name`, `plan` e `status`.
- `DELETE /api/v1/tenants/{tenantId}`
  - Recebe `confirmationText`.
  - Retorna `204 No Content` quando a confirmacao bate com o nome do tenant.

### ProductAssignment

- `GET /api/v1/products/{productId}/users`
- `POST /api/v1/products/{productId}/users`
  - Request: `tenantId`, `productId`, `userId` ou `inviteEmail`, `inviteName`, `role`.
  - Response: `tenantId`, `productId`, `productName`, `userName`, `userEmail`, `role`, `status`.
- `DELETE /api/v1/products/{productId}/users/{userId}`
  - Remove somente o ProductAssignment.
  - Nao remove `TenantMembership`.
  - Nao remove nem desabilita usuario no Keycloak.

## Endpoints Confirmados

- `GET /api/v1/tenants` com `SUPER_ADMIN`: 200
- `PUT /api/v1/tenants/{tenantId}`: 200
- `DELETE /api/v1/tenants/{tenantId}` com `confirmationText` incorreto: 400
- `GET /api/v1/products/{productId}/users`: 200
- `POST /api/v1/products/{productId}/users` com `userId`: 201
- `POST /api/v1/products/{productId}/users` com `inviteEmail`: 201
- `POST /api/v1/products/{productId}/users` com `userId` + `inviteEmail`: 400
- `POST /api/v1/products/{productId}/users` sem ambos: 400
- `DELETE /api/v1/products/{productId}/users/{userId}`: 204

## Regras de Negocio Implementadas

- `DELETE tenant` exige `confirmationText == tenant.name`.
- Exclusao de tenant grava evento minimo em `audit_events`.
- Cascade remove produtos, modulos, memberships e ProductAssignments ligados ao tenant/produto.
- `ProductAssignment` preserva unicidade `product_id + user_subject`.
- `POST ProductAssignment` exige exatamente um entre `userId` e `inviteEmail`.
- `productId` do path deve ser igual ao `productId` do body.
- `tenantId` do body deve corresponder ao tenant do produto.
- `userId` existente precisa ter `TenantMembership` ativa no mesmo tenant.
- Usuario de outro tenant e rejeitado.
- Status REST nao expoe enum Java cru.

## Decisoes Reais Tomadas

- O enum Java ficou canonico em ingles (`ASSIGNED`, `INVITED`) para manter o padrao interno, com conversao documentada no `ProductAssignmentMapper` para `"atribuido"` e `"convidado"`.
- `ProductAssignment` e entidade propria, nao extensao de `TenantMembership`, pois o papel e por produto.
- `inviteEmail` usa Keycloak Admin API real quando disponivel, nao stub silencioso.
- `userId` existente usa e-mail informativo via SMTP/FreeMarker, sem `requiredActions`.
- Remocao de ProductAssignment envia e-mail de revogacao, sem tocar em membership ou Keycloak.
- MailHog e criterio de aceite no ambiente de desenvolvimento; envio para Gmail real nao e requisito de dev.
- `loginTheme` do realm permanece `null` intencionalmente porque nao ha tema `login/`.
- NotificationService nao foi criado em paralelo; o ponto segue isolado no `StubProductAssignmentNotificationPort` para Sprint 24.

## Migrations e Persistencia

- Migration criada: `V4__product_assignment_contract.sql`.
- `tenants.plan` adicionado com compatibilidade para dados existentes.
- `product_assignments.tenant_id` adicionado ao contrato.
- Constraint unica `product_id + user_subject` preservada.
- Ajustes de cascade delete para `products`, `product_modules`, `tenant_memberships` e `product_assignments`.
- `ProductAssignment` e removido ao excluir tenant/produto.
- `audit_events` existente foi preservado.

## Seguranca e Keycloak

- `inviteEmail` cria usuario no Keycloak se ele ainda nao existir.
- O usuario convidado recebe `requiredActions: ["UPDATE_PASSWORD"]`.
- O fluxo chama `execute-actions-email`.
- `ProductAssignment.userSubject` grava o ID real do usuario no Keycloak.
- Para usuario existente, dados de e-mail/nome sao lidos via API de identidade/Keycloak.
- Realm exportado em `infra/keycloak/realm/aegis-realm.json` com `emailTheme: "aegis"`.
- `loginTheme` permanece `null`.
- Nenhuma alteracao em `SecurityConfig` foi feita nas rodadas proibidas.

## E-mails e Convites

- Convite por `inviteEmail`: Keycloak Admin API + `execute-actions-email`.
- E-mail de usuario existente: `JavaMailSender` + FreeMarker + `productAssignment.ftl`.
- E-mail de revogacao: `JavaMailSender` + FreeMarker + `productAccessRevoked.ftl`.
- Tema Aegis carregado pelo Keycloak.
- Mensagens localizadas adicionadas:
  - `messages_en.properties`
  - `messages_pt_BR.properties`
- Validacao manual confirmou e-mail no MailHog com template Aegis esperado.

## TODOs e Stubs

- `StubProductAssignmentInvitePort`: fallback/teste, nao bean ativo principal.
- `StubProductAssignmentEmailPort`: fallback/teste, nao bean ativo principal.
- `StubProductAssignmentNotificationPort`: TODO futuro da Sprint 24/NotificationService.
- Nao foi criado SMTP paralelo.
- Nao foi criado NotificationService paralelo.

## Postman

- Collection cumulativa atualizada: `postman/aegis-postman-collection.json`.
- Nome consolidado: `Aegis PMS - Backend API Collection`.
- Pasta adicionada: `10 - Tenant e Product Assignments`.
- Requests oficiais incluidos:
  - editar tenant;
  - excluir tenant com `confirmationText` errado;
  - listar usuarios do produto;
  - atribuir produto a usuario existente;
  - atribuir produto convidando novo usuario;
  - rejeicoes de XOR;
  - remover atribuicao.
- Mantido uso de `{{baseUrl}}`, Bearer `{{token}}`, `{{tenantId}}`, `{{productId}}`, `{{userId}}`, `{{inviteEmail}}`, `{{tenantName}}`.

## Validacao Manual

- Validados endpoints com curl conforme a sprint.
- Validado MailHog para:
  - e-mail informativo de atribuicao;
  - e-mail de convite com template Aegis;
  - e-mail de revogacao.
- Confirmado que MailHog e o criterio de aceite em dev, nao entrega real no Gmail.
- Logs do Keycloak verificados sem erro de template FreeMarker.

## Qualidade e Testes

- `mvn clean verify`: BUILD SUCCESS
- Testes: 340 executados
- Failures: 0
- Errors: 0
- JaCoCo: aprovado (`All coverage checks have been met`)
- Spring Modulith: aprovado via auditoria do build.
- Imports mortos e organizacao de pacotes revisados durante as rodadas de correcao.

## Sonar/SonarQube for IDE

- Corrigidos apontamentos conhecidos:
  - `java:S5778` em asserts de excecao;
  - `java:S7467` em catch sem variavel usada;
  - teste sem assertion real em stub;
  - regex desnecessario/vulneravel na montagem de URL de produto.
- SonarQube for IDE nao foi executado via CLI.
- TODOs restantes sao intencionais e classificados para sprint futura.

## Riscos

- A etapa 28 devera substituir o uso produtivo de `executeActionsEmail` por tokens Aegis para evitar redirecionamento visual ao Keycloak.
- A etapa 24 ainda precisa fornecer NotificationService real para notificacoes internas.
- A etapa 27 alterara o comportamento de exclusao de tenant/produto para backup/export antes de apagar dados.

## Retrofits Pendentes

- Sprint 24: integrar `ProductAssignmentNotificationPort` ao NotificationService real.
- Etapa 27: alterar exclusoes destrutivas para fluxo de backup/export assíncrono.
- Etapa 28: substituir convites/reset por `AuthActionToken` e links Aegis no frontend.

## Conclusao

A Sprint 10 foi concluida com CRUD completo de tenant, escopo por papel, ProductAssignment funcional, convites/e-mails reais em dev, Postman cumulativo atualizado, documentacao consolidada, JaCoCo aprovado, Spring Modulith aprovado e validacao manual por curl/MailHog.
