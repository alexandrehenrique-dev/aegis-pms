# Sprint 15 — Dominio users

Data de fechamento: 2026-06-27

## Resultado

Sprint concluida com o ciclo de vida de usuarios por tenant implementado sobre `TenantMembership`, `ProductAssignment` e Keycloak Admin API existente.

O dominio substitui os mocks das telas `UserTable`, `InviteUserDrawer` e `UserDetailPanel` por endpoints reais tenant-scoped, preservando contratos REST, isolamento por tenant, papeis canonicos e fronteiras do Spring Modulith.

## Escopo

Implementado:

- listagem de usuarios por tenant com filtro por papel;
- convite de usuario;
- detalhe;
- edicao de papel, produtos permitidos e status;
- reenvio de convite;
- bloqueio;
- soft delete;
- restauracao de acesso;
- integracao com Keycloak Admin API existente;
- Bruno cumulativo da Etapa 15;
- correcoes SonarQube for IDE apontadas para a sprint.

Fora do escopo e registrado como retrofit:

- notificacoes internas reais de remocao/restauracao;
- auditoria persistente de eventos `USER_REMOVED_FROM_TENANT` e `USER_RESTORED_TO_TENANT`;
- substituicao futura de `executeActionsEmail` por tokens Aegis gerenciados.

## Endpoints

Confirmados:

- `GET /api/v1/tenants/{tenantId}/users`
- `POST /api/v1/tenants/{tenantId}/users/invite`
- `GET /api/v1/tenants/{tenantId}/users/{userId}`
- `PUT /api/v1/tenants/{tenantId}/users/{userId}`
- `POST /api/v1/tenants/{tenantId}/users/{userId}/resend-invite`
- `POST /api/v1/tenants/{tenantId}/users/{userId}/block`
- `DELETE /api/v1/tenants/{tenantId}/users/{userId}`
- `POST /api/v1/tenants/{tenantId}/users/{userId}/restore`

Garantias preservadas:

- `DELETE /users/{userId}` retorna `204 No Content`;
- ultimo admin ativo continua protegido;
- usuario fora do tenant retorna 404, nunca 403;
- restore de usuario ativo retorna erro;
- HTTP 422 continua sendo 422 no caso de ultimo admin.

## Contratos

Contratos formalizados antes da borda REST:

- `TenantUserSummary`
- `InviteTenantUserRequest`
- `UpdateTenantUserRequest`
- erros padronizados via `CoreErrorResponse`

Status expostos no contrato:

- `ativo`
- `convidado`
- `bloqueado`
- `removido`

Aliases em ingles sao aceitos apenas como entrada tolerante na edicao (`active`, `invited`, `suspended`, `removed`) e normalizados para os valores canonicos acima.

## Arquivos alterados/criados/removidos

### Backend — novos arquivos

- `backend/src/main/java/br/com/byop/aegis/identity/api/IdentityUserLifecycleService.java`
- `backend/src/main/java/br/com/byop/aegis/product/api/ProductUserAccess.java`
- `backend/src/main/java/br/com/byop/aegis/product/api/ProductUserAccessService.java`
- `backend/src/main/java/br/com/byop/aegis/product/user/contract/InviteTenantUserRequest.java`
- `backend/src/main/java/br/com/byop/aegis/product/user/contract/UpdateTenantUserRequest.java`
- `backend/src/main/java/br/com/byop/aegis/product/user/controller/TenantUserController.java`
- `backend/src/main/java/br/com/byop/aegis/product/user/dto/TenantUserSummary.java`
- `backend/src/main/java/br/com/byop/aegis/product/user/exception/InvalidTenantUserOperationException.java`
- `backend/src/main/java/br/com/byop/aegis/product/user/exception/LastTenantAdminException.java`
- `backend/src/main/java/br/com/byop/aegis/product/user/exception/TenantUserAlreadyExistsException.java`
- `backend/src/main/java/br/com/byop/aegis/product/user/exception/TenantUserExceptionHandler.java`
- `backend/src/main/java/br/com/byop/aegis/product/user/exception/TenantUserNotFoundException.java`
- `backend/src/main/java/br/com/byop/aegis/product/user/mapper/TenantUserMapper.java`
- `backend/src/main/java/br/com/byop/aegis/product/user/service/TenantUserService.java`
- `backend/src/main/java/br/com/byop/aegis/tenant/api/TenantMembershipReference.java`
- `backend/src/main/java/br/com/byop/aegis/tenant/api/TenantUserAccessService.java`

### Backend — arquivos alterados

- `backend/src/main/java/br/com/byop/aegis/identity/api/IdentityInvitationService.java`
- `backend/src/main/java/br/com/byop/aegis/identity/auth/client/KeycloakAdminClient.java`
- `backend/src/main/java/br/com/byop/aegis/identity/user/controller/UserController.java`
- `backend/src/main/java/br/com/byop/aegis/product/domain/ProductAssignment.java`
- `backend/src/main/java/br/com/byop/aegis/product/domain/ProductAssignmentStatus.java`
- `backend/src/main/java/br/com/byop/aegis/product/repository/ProductAssignmentRepository.java`
- `backend/src/main/java/br/com/byop/aegis/tenant/domain/TenantMembership.java`
- `backend/src/main/java/br/com/byop/aegis/tenant/domain/TenantMembershipStatus.java`
- `backend/src/main/java/br/com/byop/aegis/tenant/repository/TenantMembershipRepository.java`

### Testes

- `backend/src/test/java/br/com/byop/aegis/identity/api/IdentityInvitationServiceTest.java`
- `backend/src/test/java/br/com/byop/aegis/identity/api/IdentityUserLifecycleServiceTest.java`
- `backend/src/test/java/br/com/byop/aegis/identity/auth/client/KeycloakAdminClientTest.java`
- `backend/src/test/java/br/com/byop/aegis/identity/user/controller/UserControllerTest.java`
- `backend/src/test/java/br/com/byop/aegis/identity/user/service/UserServiceTest.java`
- `backend/src/test/java/br/com/byop/aegis/product/api/ProductUserAccessServiceTest.java`
- `backend/src/test/java/br/com/byop/aegis/product/user/controller/TenantUserControllerTest.java`
- `backend/src/test/java/br/com/byop/aegis/product/user/exception/TenantUserExceptionHandlerTest.java`
- `backend/src/test/java/br/com/byop/aegis/product/user/mapper/TenantUserMapperTest.java`
- `backend/src/test/java/br/com/byop/aegis/product/user/service/TenantUserServiceTest.java`
- `backend/src/test/java/br/com/byop/aegis/tenant/api/TenantUserAccessServiceTest.java`

### Bruno

- `bruno/15-users/*`
- `bruno/collection.bru`
- `bruno/environments/local.bru`
- `bruno/environments/dev.bru`
- `bruno/environments/homolog.bru`
- `bruno/environments/prod.bru`

### Documentacao

- `docs/sprints/backend/SPRINT-RESULTADO.md`
- `docs/sprints/backend/results/sprint-15.md`

Tambem havia no worktree, e sera incluido no commit final conforme autorizacao do usuario, a reordenacao documental das etapas futuras em `docs/sprints/backend/00_indice_e_instrucoes.md` e nos arquivos `19_...` a `29_...`.

## Migrations

Nenhuma migration nova foi criada nesta sprint.

Motivo: os campos necessarios ja existiam nas entidades/tabelas reaproveitadas. A sprint apenas expandiu enums/status e metodos de dominio para usar `TenantMembership` e `ProductAssignment` existentes.

## Regras de negocio

- E-mail duplicado no mesmo tenant retorna 409.
- Convite usa Keycloak Admin API existente e nao cria autenticacao paralela.
- Edicao permite alterar papel/produtos/status, mas nao e-mail.
- `resend-invite` aceita somente usuario convidado.
- `block` marca membership como `bloqueado` e nao desabilita Keycloak.
- `DELETE` marca membership como `removido`, revoga assignments do tenant e desabilita Keycloak apenas se nao houver outra membership ativa.
- `restore` aceita apenas `bloqueado` ou `removido`, reativa membership, reabilita Keycloak e dispara `UPDATE_PASSWORD`.
- `restore` nao restaura `ProductAssignment`s automaticamente.
- Ultimo admin ativo do tenant nao pode ser bloqueado, removido nem perder papel administrativo.
- Fora do escopo do caller retorna 404, nunca 403.
- Listagem por papel segue ADR-0019.

## Integracao com Keycloak

`KeycloakAdminClient` foi reaproveitado e ampliado para:

- localizar usuario por e-mail;
- convidar usuario por e-mail/nome;
- disparar `executeActionsEmail`;
- habilitar/desabilitar usuario globalmente.

O Aegis continua responsavel por autorizacao contextual por tenant/produto. Keycloak permanece como IAM/autenticacao, conforme ADR-0005.

## Testes automatizados

Cobertura adicionada para:

- mapper `TenantUserMapper`;
- service `TenantUserService`;
- controller `TenantUserController` com `@WebMvcTest`;
- exception handler;
- regras de negocio de duplicidade, ultimo admin, filtro por papel, isolamento por tenant, convite, reenvio, bloqueio, soft delete e restore;
- APIs publicas de tenant/product/identity usadas para manter fronteiras modulares;
- novos fluxos do `KeycloakAdminClient`.

## Bruno

Nova pasta `bruno/15-users`:

- `convidar-usuario.bru`
- `convidar-email-duplicado-409.bru`
- `listar-usuarios-do-tenant.bru`
- `detalhar-usuario.bru`
- `editar-usuario.bru`
- `reenviar-convite.bru`
- `bloquear-usuario.bru`
- `restaurar-usuario-bloqueado.bru`
- `remover-usuario.bru`
- `restaurar-usuario-removido.bru`

Variavel `tenantUserInviteEmail` adicionada para manter a collection idempotente entre execucoes.

## Evidencias dos comandos executados

Executados durante a validacao da sprint:

```bash
cd backend
mvn -Dtest='KeycloakAdminClientTest,TenantUserControllerTest,TenantUserServiceTest' test
mvn -Dtest=TenantUserControllerTest test
mvn clean verify

cd ../bruno
npx @usebruno/cli run --env local
```

Resultados consolidados:

- `mvn -Dtest='KeycloakAdminClientTest,TenantUserControllerTest,TenantUserServiceTest' test`: `BUILD SUCCESS`, 57 testes, 0 falhas, 0 erros.
- `mvn -Dtest=TenantUserControllerTest test`: `BUILD SUCCESS`, 5 testes, 0 falhas, 0 erros.
- `mvn clean verify`: `BUILD SUCCESS`, 809 testes, 0 falhas, 0 erros, JaCoCo aprovado (`All coverage checks have been met`) e Spring Modulith aprovado.
- `npx @usebruno/cli run --env local`: `PASS`, 121 requests, 121 aprovados, 231/231 testes aprovados.

## SonarQube for IDE

Correcoes aplicadas:

- `java:S5778`: lambdas de `assertThatThrownBy`/`assertThrows` refatoradas para conter apenas a chamada ao metodo sob teste.
- `java:S1874`: removido uso de APIs deprecated para HTTP 422; teste do controller usa `status().is(422)`.
- `java:S1192`: literais duplicados de status centralizados em constantes privadas semanticas no service.
- `java:S4144`: helper duplicado de visibilidade em testes removido/extrato para metodo comum.

Sem `@SuppressWarnings`, sem falso positivo marcado e sem relaxar regra de negocio.

## Decisoes tomadas

- `users` foi tratado como dominio de orquestracao, nao como autenticacao local.
- Nao foi criada entidade duplicada de usuario local.
- `TenantMembership` permanece a fonte local do vinculo do usuario ao tenant.
- `ProductAssignment` permanece a fonte do vinculo do usuario aos produtos.
- A API REST foi mantida tenant-scoped, sem paths `/admin/...` novos.
- A implementacao usa APIs publicas entre modulos para preservar Spring Modulith.
- `block` e `remove` continuam semanticamente diferentes.
- Restore de usuario removido nao restaura assignments.

## Pendencias e retrofits

- Integrar eventos persistentes ao dominio `audit` quando a etapa 16 for implementada.
- Integrar notificacoes internas quando o dominio `notification` estiver disponivel.
- Substituir `executeActionsEmail` por tokens Aegis gerenciados na etapa de ativacao/reset.
- Enriquecer e-mails informativos de remocao/restauracao quando os templates definitivos estiverem fechados.
- Atualizar o frontend na sprint de integracao mock/real para consumir exclusivamente os paths tenant-scoped canonicos.

## Riscos remanescentes

- A diferenca entre usuario global do Keycloak e memberships por tenant precisa continuar clara em futuras sprints.
- Qualquer novo fluxo que altere status de usuario deve preservar a regra do ultimo admin.
- A collection Bruno depende de ambiente local ativo com Keycloak, PostgreSQL, MailHog e backend em execucao.
- As etapas futuras devem manter o contrato de 404 para fora do tenant e nao transformar esse caso em 403.
