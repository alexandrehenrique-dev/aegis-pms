# Etapa 15 — Domínio `users` (listagem, convite, detalhe, remoção e restauração por tenant)

> Cole este arquivo inteiro numa conversa nova do GPT. Pré-requisito: etapas 06 (SMTP + KeycloakAdminClient), 07 (modelo core + ProductAccessResolver) e 10 (Tenant CRUD + ProductAssignment) concluídas.

## Contexto fixo

Telas `UserTable`, `InviteUserDrawer`, `UserDetailPanel` — hoje 100% mock. Payloads conforme `docs/trace/00_endpoints_esperados.md` (Seção B.5). Esta etapa implementa o ciclo de vida completo do usuário: convite → ativo → bloqueado/removido → restaurado (ADR-0020). O mecanismo de convite via Keycloak Admin API (já documentado na etapa 06 como `KeycloakAdminClient`) é reaproveitado aqui — não recriar.

## Objetivo

Listar usuários (filtrado por papel — ADR-0019), convidar novos usuários, ver/editar detalhe, bloquear, remover (soft delete) e restaurar acesso.

## Tarefas

### A. Endpoints

```txt
GET    /api/v1/tenants/{tenantId}/users
POST   /api/v1/tenants/{tenantId}/users/invite
GET    /api/v1/tenants/{tenantId}/users/{userId}
PUT    /api/v1/tenants/{tenantId}/users/{userId}
POST   /api/v1/tenants/{tenantId}/users/{userId}/resend-invite
POST   /api/v1/tenants/{tenantId}/users/{userId}/block
DELETE /api/v1/tenants/{tenantId}/users/{userId}   ← NOVO — soft delete (ADR-0020)
POST   /api/v1/tenants/{tenantId}/users/{userId}/restore  ← NOVO — restauração (ADR-0020)
```

**`resend-invite`**: só válido para usuário com `inviteStatus: "pendente"` (senão 400); dispara de novo `executeActionsEmail` no Keycloak.

**`block`**: marca `TenantMembership.status = "bloqueado"` (reversível via `unblock` — que é `PUT /users/{userId}` com `status: "ativo"`). Não desabilita no Keycloak, não remove `ProductAssignment`s. Usuário bloqueado é impedido de autenticar via verificação de status da membership no `GET /me`. Sujeito à regra de "não se trancar para fora" (Seção C).

**`DELETE` (soft delete — ADR-0020)**: remoção definitiva de um usuário do tenant:
1. Verifica regra de "não se trancar para fora"
2. `TenantMembership.status = "removido"` (novo status — distinto de `"bloqueado"`)
3. Revoga todos os `ProductAssignment`s ativos do usuário neste tenant (`status = "removido"`)
4. Verifica se o usuário ainda tem `TenantMembership` ativa em **outros** tenants. Se sim: não toca no Keycloak. Se não: `PUT /admin/realms/aegis/users/{keycloakId}` com `{ enabled: false }`
5. Registra evento de auditoria `USER_REMOVED_FROM_TENANT`
6. Envia e-mail informativo: "Seu acesso ao tenant X foi removido" (template simples, sem link de ação)
7. Cria notificação interna: `type: "TENANT_ACCESS_REVOKED"` (se etapa 24 disponível)
8. Resposta: `204 No Content`

**`POST .../restore` (ADR-0020)**: reativação de usuário removido ou bloqueado:
1. Verifica que `TenantMembership.status` é `"removido"` ou `"bloqueado"` (senão 400)
2. `TenantMembership.status = "ativo"`
3. Se Keycloak estava `enabled: false`: reabilitar (`enabled: true`) via Admin API
4. Dispara `executeActionsEmail` com `["UPDATE_PASSWORD"]` — força redefinição de senha na primeira entrada
5. Registra evento de auditoria `USER_RESTORED_TO_TENANT`
6. Envia e-mail: "Seu acesso ao tenant X foi restaurado. Defina uma nova senha para continuar." (mesmo template `executeActions.ftl` da etapa 06)
7. **Não** restaura `ProductAssignment`s automaticamente — o admin precisa re-atribuir produtos manualmente
8. Resposta: `200` com `UserSummary` atualizado

> **Nota de divergência de path (auditoria de cobertura):** o frontend mock hoje (`usersService.invite`, `productAssignmentsService.assign`) chama `/api/v1/admin/users/invite` e `/api/v1/admin/products/{productId}/assignments` — paths que nunca apareceram em nenhuma etapa e que **não** são o padrão canônico. O padrão correto, usado em todas as etapas, é sempre escopado por `tenantId`/`productId` no path, nunca um prefixo `/admin/...` solto — escopar pelo path é o que sustenta a regra de isolamento da Seção 10 do padrão de qualidade. Reconciliação do frontend: Sprint 07 (toggle mock↔real).

Payloads:

```ts
type UserSummary = {
  name: string; email: string; role: string; products: string;
  // status: "ativo" | "bloqueado" | "convidado" | "removido"
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

> `KeycloakAdminClient` já foi implementado na etapa 06 (`06_auth_proxy_smtp_e_convite.md`). **Não recriar** — injetar a classe existente e chamar os métodos já testados: `createUser(...)` e `executeActionsEmail(keycloakId, ["UPDATE_PASSWORD"])`.

1. Chamar `KeycloakAdminClient.createUser(email, name)` → retorna `keycloakId` (UUID do novo usuário no realm).
2. Criar `TenantMembership` local com `userSubject = keycloakId`, `role` informado, `status = "convidado"`.
3. Chamar `KeycloakAdminClient.executeActionsEmail(keycloakId, ["UPDATE_PASSWORD"])` → Keycloak envia e-mail via SMTP configurado (MailHog em dev).
4. Se o convite veio de `ProductAssignment` (etapa 10) ou de criação de tenant (etapa 10), criar também o registro correspondente apontando para o `keycloakId` recém-criado.
5. `inviteStatus` fica `"pendente"` até o usuário definir senha via link do e-mail (primeiro login bem-sucedido via `POST /auth/login` retorna token válido → `GET /me` atualiza `inviteStatus = "ativo"`).
6. **(Retrofit pós-etapa 24)** Após criar `TenantMembership`, chamar `NotificationService.assignOnboarding(userSubject)`.

### C. Regras de negócio

- E-mail duplicado no mesmo tenant é rejeitado com 409.
- `PUT /users/{userId}` permite editar `role` e `allowedProducts`, nunca o e-mail (e-mail é imutável, é a identidade no Keycloak).
- Usuário não pode editar/remover/bloquear a si mesmo de forma que fique sem nenhum `TENANT_ADMIN`/`SUPER_ADMIN` ativo no tenant (regra de "não se trancar para fora" — aplicável a `PUT`, `block`, `DELETE` e `restore` que mude papel).
- **Isolamento por tenant** (`00_padrao_qualidade_e_arquitetura.md`, Seção 10): usuário de um tenant fora do escopo de quem chama (sem membership ativa, exceto `SUPER_ADMIN`) retorna 404, nunca 403, em qualquer endpoint da Seção A.
- **Listagem filtrada por papel (ADR-0019)** — `GET /tenants/{tenantId}/users` não usa o mesmo filtro para todos:

  | Papel do caller | Usuários retornados |
  |---|---|
  | `SUPER_ADMIN` | Todos os usuários do tenant |
  | `TENANT_ADMIN` | Todos os usuários do tenant |
  | `PRODUCT_MANAGER` | Apenas usuários que compartilham ao menos um produto com o caller (via `ProductAssignment`) |
  | `EDITOR` / `VIEWER` | Idem `PRODUCT_MANAGER` |

  O critério de filtro é determinado pelo `UserService` a partir do papel do `AuthenticatedUser` — nunca recebido como parâmetro de query do cliente.

### D. Padrão de qualidade e entrega (obrigatório)

> Resumo — detalhe completo em `00_padrao_qualidade_e_arquitetura.md`.

- **Java 25** / **Spring Boot 4.1.x**. `KeycloakAdminClient` **já existe** (etapa 06) — não recriar; apenas injetar e testar os novos fluxos com mock. 100% de cobertura nas classes funcionais desta etapa.
- Entregar em rodadas:
  1. `UserMapper` (MapStruct, `TenantMembership` + dados do Keycloak → `UserSummary`, incluindo `status: "removido"`) + testes de mapper.
  2. `UserService` — convite (delegando para `KeycloakAdminClient` existente), regra de e-mail duplicado, regra de "não se trancar para fora", filtro de listagem por papel (Seção C) — testes com mocks para cada regra + filtro por papel.
  3. `UserService` (continuação) — `softDelete`: sequência de 8 passos da Seção A; `restore`: sequência de 8 passos da Seção A — testes cobrindo: remoção do último TENANT_ADMIN bloqueada; usuário com outros tenants não desabilita Keycloak; restore de usuário já ativo retorna 400.
  4. `UserController` (todos os endpoints da Seção A) + testes `@WebMvcTest` + validação via `curl`.

## Critérios de aceite

- [ ] `GET /tenants/{tenantId}/users` com token de `TENANT_ADMIN` retorna todos os usuários do tenant.
- [ ] `GET /tenants/{tenantId}/users` com token de `PRODUCT_MANAGER` retorna apenas usuários que compartilham ao menos um produto com o caller.
- [ ] Convidar usuário cria conta real no Keycloak (via `KeycloakAdminClient` da etapa 06) e `TenantMembership` local.
- [ ] Convite com e-mail duplicado no tenant é rejeitado com 409.
- [ ] Detalhe e edição de usuário funcionam.
- [ ] Remover o último `TENANT_ADMIN`/`SUPER_ADMIN` ativo é bloqueado (DELETE retorna 422 ou 400).
- [ ] `DELETE /users/{userId}` marca `TenantMembership.status = "removido"`, revoga `ProductAssignment`s e desabilita conta no Keycloak se sem outros tenants ativos.
- [ ] `DELETE /users/{userId}` para usuário com membership ativa em outro tenant: **não** desabilita conta no Keycloak.
- [ ] `POST .../restore` reativa `TenantMembership`, reabilita Keycloak se estava desabilitado, dispara `executeActionsEmail(["UPDATE_PASSWORD"])`.
- [ ] `POST .../restore` em usuário já ativo retorna 400.
- [ ] `resend-invite` em usuário já ativo (não pendente) é rejeitado com 400.
- [ ] `block` impede login do usuário nesse tenant; bloquear o último `TENANT_ADMIN`/`SUPER_ADMIN` ativo é rejeitado.
- [ ] Usuário/tenant fora do escopo de quem chama retorna 404, nunca 403.
- [ ] `mvn clean verify` confirma 100% de cobertura nas classes elegíveis desta etapa (JaCoCo).

## Validação

> **Entrega via collection Postman, não só curl** (ver `00_padrao_qualidade_e_arquitetura.md`, Seção 11). Os `curl` abaixo são a especificação exata de cada request — adicione-os à pasta desta etapa em `aegis-postman-collection.json` (collection cumulativa, autenticação via `{{token}}` herdado da pasta "Auth") e devolva o JSON completo atualizado para download.

```bash
# convidar usuário
curl -X POST http://localhost:8080/api/v1/tenants/<tenantId>/users/invite \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"Joao Alves","email":"joao@byop.com","role":"Editor","allowedProducts":"Maestro Beton"}'

# listar usuários (TENANT_ADMIN vê todos; PRODUCT_MANAGER vê apenas compartilhados)
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/tenants/<tenantId>/users

# remover usuário do tenant (soft delete)
curl -X DELETE http://localhost:8080/api/v1/tenants/<tenantId>/users/<userId> \
  -H "Authorization: Bearer $TOKEN"
# esperado: 204 No Content

# restaurar usuário removido
curl -X POST http://localhost:8080/api/v1/tenants/<tenantId>/users/<userId>/restore \
  -H "Authorization: Bearer $TOKEN"
# esperado: 200 com UserSummary atualizado (status="ativo", inviteStatus="pendente" — nova senha obrigatória)
```

## Artefato de continuidade — `SPRINT-RESULTADO.md`

> Ver `00_padrao_qualidade_e_arquitetura.md`, Seção 12. Antes do commit, gere/atualize `docs/sprints/sprint-02-fundacao-backend-gpt/SPRINT-RESULTADO.md` (arquivo inteiro, nunca um diff) com a entrada desta etapa (template fixo da Seção 12.2): classes criadas, endpoints confirmados, qualquer decisão que esta etapa deixou a seu critério (registre a escolha real), e retrofits pendentes para etapas futuras. É o que a próxima conversa do GPT vai receber em vez da memória que ela não tem.

## Commit sugerido

```bash
git add backend/
git commit -m "feat(backend): dominio users com convite, soft-delete, restore e listagem por papel"
```
