# Etapa 14 — Domínio `users` (listagem, convite, detalhe por tenant)

> Cole este arquivo inteiro numa conversa nova do GPT. Pré-requisito: etapas 03 (Keycloak/realm) e 09 (Tenant CRUD + ProductAssignment) concluídas — convite de usuário cria conta no Keycloak, não só registro local.

## Contexto fixo

Telas `UserTable`, `InviteUserDrawer`, `UserDetailPanel` — hoje 100% mock. Payloads conforme `docs/trace/00_endpoints_esperados.md` (Seção B.5). Este endpoint de convite é reaproveitado pela etapa 09 (convite ao criar tenant) e por `ProductAssignment` com `inviteEmail` — implementar aqui de forma que ambas as etapas possam chamá-lo.

## Objetivo

Listar usuários de um tenant, convidar novos usuários (criando conta no Keycloak via Admin API), e ver/editar detalhe de um usuário.

## Tarefas

### A. Endpoints

```txt
GET  /api/v1/tenants/{tenantId}/users
POST /api/v1/tenants/{tenantId}/users/invite
GET  /api/v1/tenants/{tenantId}/users/{userId}
PUT  /api/v1/tenants/{tenantId}/users/{userId}
POST /api/v1/tenants/{tenantId}/users/{userId}/resend-invite
POST /api/v1/tenants/{tenantId}/users/{userId}/block
```

> **`resend-invite`/`block` adicionados nesta revisão** — auditoria de cobertura encontrou que `usersService.resendInvite`/`blockUser` (frontend) já chamavam esses dois caminhos sem nenhuma etapa documentá-los. `resend-invite`: só válido para usuário com `inviteStatus: "pendente"` (senão 400 — usuário já ativo não precisa reenviar convite); dispara de novo o fluxo de verificação do Keycloak (mesmo mecanismo da Seção B). `block`: marca `TenantMembership.status` como inativo/bloqueado (não deleta o registro) — usuário bloqueado não consegue mais autenticar nesse tenant; sujeito à mesma regra de "não se trancar para fora" da Seção C (não pode bloquear o último `TENANT_ADMIN`/`SUPER_ADMIN` ativo).
>
> **Nota de divergência de path (auditoria de cobertura):** o frontend mock hoje (`usersService.invite`, `productAssignmentsService.assign`) chama `/api/v1/admin/users/invite` e `/api/v1/admin/products/{productId}/assignments` — paths que nunca apareceram em nenhuma etapa e que **não** são o padrão canônico. O padrão correto, usado em todas as etapas (06, 09, 14, 15, 16, 23) é sempre escopado por `tenantId`/`productId` no path (`/tenants/{tenantId}/users/...`, `/products/{productId}/users`), nunca um prefixo `/admin/...` solto sem o escopo na URL — escopar pelo path é o que sustenta a regra de isolamento da Seção 10 do padrão de qualidade. Implementar **só** os paths desta etapa (e da etapa 09); a reconciliação do frontend mock para os paths corretos é tarefa da Sprint 07 (toggle mock↔real), não desta etapa de backend.

Payloads:

```ts
type UserSummary = {
  name: string; email: string; role: string; products: string;
  status: string; lastAccess: string; inviteStatus: string;
};
// GET /users → UserSummary[]

type InviteUserRequest = {
  name: string; email: string; role: string;
  allowedProducts: string; message?: string;
};
// POST /invite → UserSummary (status="convidado", inviteStatus="pendente")
```

### B. Mecanismo de convite (via Keycloak Admin API)

1. Criar usuário no Keycloak (Admin REST API, client `aegis-web` ou um client de serviço dedicado com permissão de admin no realm) com `enabled: true`, `emailVerified: false`, e disparar o fluxo de "Update Password"/verificação de e-mail nativo do Keycloak (não inventar um sistema de e-mail próprio nesta fase — usar o que o Keycloak já oferece).
2. Criar `TenantMembership` local vinculando o `subject` do novo usuário do Keycloak ao tenant, com o `role` informado.
3. Se o convite veio de `ProductAssignment` (etapa 09) ou de criação de tenant (etapa 09), criar também o registro correspondente (`ProductAssignment` ou marcar o tenant com o `initialAdminEmail`) apontando para o `subject` recém-criado.
4. `inviteStatus` fica `"pendente"` até o usuário completar o cadastro no Keycloak (primeiro login bem-sucedido marca como `"ativo"` — pode ser um listener de evento do Keycloak ou verificado no próximo `GET /me`).
5. **(Adicionado pela etapa 23, se já estiver implementada quando esta etapa for executada — senão, é um retrofit a fazer depois)** Ao final do passo 2 (criar `TenantMembership`), chamar `NotificationService.assignOnboarding(userSubject)` para o novo usuário já nascer com a notificação de onboarding pendente.

### C. Regras de negócio

- E-mail duplicado no mesmo tenant é rejeitado com 409.
- `PUT /users/{userId}` permite editar `role` e `allowedProducts`, nunca o e-mail (e-mail é imutável, é a identidade no Keycloak).
- Usuário não pode editar/remover a si mesmo de forma que fique sem nenhum `TENANT_ADMIN`/`SUPER_ADMIN` ativo no tenant (regra de "não se trancar para fora").
- **Isolamento por tenant** (`00_padrao_qualidade_e_arquitetura.md`, Seção 10): usuário de um tenant fora do escopo do usuário autenticado (sem membership ativa, exceto `SUPER_ADMIN`) retorna 404, nunca 403, em qualquer endpoint da Seção A.

### D. Padrão de qualidade e entrega (obrigatório)

> Resumo — detalhe completo em `00_padrao_qualidade_e_arquitetura.md`.

- **Java 25** / **Spring Boot 4.1.x**. Esta etapa **não cria entidade nova** (reaproveita `TenantMembershipRepository` da etapa 06) — a peça nova é o cliente do Keycloak Admin API. 100% de cobertura nas classes funcionais, incluindo `KeycloakAdminClient`/equivalente (testar com o client mockado, nunca chamando o Keycloak real no teste unitário).
- Entregar em rodadas:
  1. `KeycloakAdminClient` (wrapper do Admin REST API — criar usuário, disparar verificação) + testes com `WireMock`/mock HTTP, sem depender do Keycloak real subindo no teste.
  2. `UserMapper` (MapStruct, `TenantMembership` + dados do Keycloak → `UserSummary`) + testes de mapper.
  3. `UserService` (convite, regra de e-mail duplicado, regra de "não se trancar para fora") + testes com mocks — cada regra da Seção C com teste do caminho feliz e da rejeição.
  4. `UserController` (endpoints da Seção A) + testes `@WebMvcTest` + validação via `curl`.

## Critérios de aceite

- [ ] Listar usuários de um tenant funciona.
- [ ] Convidar usuário cria conta real no Keycloak e `TenantMembership` local.
- [ ] Convite com e-mail duplicado no tenant é rejeitado.
- [ ] Detalhe e edição de usuário funcionam.
- [ ] Remover o último `TENANT_ADMIN`/`SUPER_ADMIN` de um tenant é bloqueado.
- [ ] Usuário/tenant fora do escopo de quem chama (sem `SUPER_ADMIN`) retorna 404 (não 403).
- [ ] `resend-invite` em usuário já ativo (não pendente) é rejeitado com 400; em usuário pendente, dispara o fluxo de verificação de novo.
- [ ] `block` impede login subsequente do usuário nesse tenant; bloquear o último `TENANT_ADMIN`/`SUPER_ADMIN` ativo é rejeitado (mesma regra de "não se trancar para fora").
- [ ] `mvn clean verify` confirma 100% de cobertura nas classes elegíveis desta etapa (JaCoCo).

## Validação

> **Entrega via collection Postman, não só curl** (ver `00_padrao_qualidade_e_arquitetura.md`, Seção 11). Os `curl` abaixo são a especificação exata de cada request — adicione-os à pasta desta etapa em `aegis-postman-collection.json` (collection cumulativa, autenticação via `{{token}}` herdado da pasta "Auth") e devolva o JSON completo atualizado para download.

```bash
curl -X POST http://localhost:8080/api/v1/tenants/<tenantId>/users/invite \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"Joao Alves","email":"joao@byop.com","role":"Editor","allowedProducts":"Maestro Beton"}'

curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/tenants/<tenantId>/users
```

## Commit sugerido

```bash
git add backend/
git commit -m "feat(backend): dominio users com convite via keycloak admin api"
```
