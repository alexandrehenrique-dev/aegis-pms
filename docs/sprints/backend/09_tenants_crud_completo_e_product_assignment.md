# Etapa 09 — Tenants CRUD completo, escopo por papel, e ProductAssignment

> Cole este arquivo inteiro numa conversa nova do GPT. Pré-requisito: etapa 06 concluída (Tenant/Product/Module básicos já existem).

## Contexto fixo

A etapa 06 criou só `GET`/`POST` de tenant. O frontend (Sprint 09, fora do GPT, já concluída) tem um fluxo completo de Super Admin que a fundação ainda não suporta: editar/excluir tenant, listar **todos** os tenants quando quem pergunta é Super Admin, e atribuir um produto específico a um usuário específico com um papel específico — algo que não existe no modelo de domínio hoje (só existe `TenantMembership`, que é usuário↔tenant, nunca usuário↔produto). Os payloads exatos abaixo vêm de `docs/trace/00_endpoints_esperados.md` (Seção C) — são os mesmos tipos TypeScript que os `services/*.ts` mock do frontend já implementam hoje; não são suposições.

## Objetivo

CRUD completo de tenant com escopo por papel, e a entidade nova `ProductAssignment` com seus endpoints, fechando o modelo de domínio que o fluxo "Super Admin cria tenant → cria produto → atribui a um usuário" precisa.

## Tarefas

### A. Tenant — CRUD completo

**`POST /api/v1/tenants`** — já existe (etapa 06), mas o payload precisa ganhar mais campos:
```ts
type CreateTenantRequest = {
  name: string; slug: string; plan: string;
  initialAdminEmail: string; // dispara convite ao Tenant Admin inicial
};
// Response: TenantOption = { id, name, plan, productCount: 0, lastAccess: "—", status: "ativo" }
```
Ao criar, além da membership `TENANT_ADMIN` do criador (regra já existente da etapa 06), disparar um convite (reaproveitar o mecanismo de convite da etapa 14 — pode ser implementado como um TODO/stub aqui se a etapa 14 ainda não foi feita, mas a chamada deve existir).

**`PUT /api/v1/tenants/{tenantId}`** *(novo)*:
```ts
type UpdateTenantRequest = { name: string; plan: string; status: "ativo" | "suspenso" };
// Response: TenantOption atualizado
```

> **(Adicionado pela etapa 24, se já estiver implementada quando esta etapa for executada — senão, é um retrofit a fazer depois)** Se `status` mudar de valor (`"ativo"` → `"suspenso"` ou vice-versa), chamar `NotificationService.create(...)` notificando os usuários do tenant (`target: { type: "TENANT", tenantId }`) — ver etapa 24, Seção D, para o texto/tipo exatos. `update` sem mudança de status não cria notificação nenhuma.

**`DELETE /api/v1/tenants/{tenantId}`** *(novo)*:
```ts
type DeleteTenantRequest = { confirmationText: string }; // nome do tenant, digitado pelo usuário
// Response: 204 No Content
```
Ação destrutiva e **irreversível** — cascateia para produtos, módulos, memberships e `ProductAssignment`s daquele tenant. Validar no servidor que `confirmationText === tenant.name` (não confiar só na confirmação do client) e auditar a ação (gravar em `audit_events`, mesmo que a etapa 15 de auditoria completa ainda não exista — o registro mínimo já deve acontecer aqui).

**`GET /api/v1/tenants`** *(ajuste de escopo, não endpoint novo)*: quando o token tem papel `SUPER_ADMIN`, retornar **todos os tenants da plataforma**; para os demais papéis, manter o comportamento atual (só tenants onde o usuário tem membership ativa). Mesma rota, escopo decidido pelo papel do token — nunca por um parâmetro que o client poderia manipular.

### B. `ProductAssignment` — entidade e endpoints novos

**Entidade `ProductAssignment`**: `id`, `tenantId`, `productId`, `userSubject` (subject do JWT, igual a `TenantMembership`), `role`, `status` (`atribuido` | `convidado`), `createdAt`, `updatedAt`. Constraint única em `(productId, userSubject)`.

> Decisão de schema: `ProductAssignment` é uma tabela própria, não uma extensão de `TenantMembership` — porque o papel de um usuário pode ser diferente por produto (ex.: Product Manager no produto A, Editor no produto B, ambos no mesmo tenant), e `TenantMembership` é por definição um registro por (tenant, usuário).

**`GET /api/v1/products/{productId}/users`** — lista usuários atribuídos ao produto.

**`POST /api/v1/products/{productId}/users`**:
```ts
type AssignProductUserRequest = {
  tenantId: string; productId: string;
  userId?: string;       // usuário já existente no tenant
  inviteEmail?: string;  // OU convite de um usuário novo
  inviteName?: string;
  role: string;          // papel específico NESTE produto
};
// Response:
type ProductAssignmentSummary = {
  tenantId: string; productId: string; productName: string;
  userName: string; userEmail: string; role: string;
  status: "atribuido" | "convidado";
};
```
Validação obrigatória: exatamente um de `userId` ou `inviteEmail` deve vir preenchido — rejeitar (400) se vierem os dois ou nenhum. Se `userId`, o usuário precisa já ter `TenantMembership` ativa no mesmo tenant do produto (senão 404/400 — não dá para atribuir produto a alguém de fora do tenant). Se `inviteEmail`, dispara o mesmo mecanismo de convite usado em `POST /tenants` e em `POST /tenants/{tenantId}/users/invite` (etapa 14).

**`DELETE /api/v1/products/{productId}/users/{userId}`** — remove a atribuição.

### C. Padrão de qualidade e entrega (obrigatório)

> Resumo — detalhe completo em `00_padrao_qualidade_e_arquitetura.md`.

- **Java 25** / **Spring Boot 4.1.x**. Javadoc obrigatório na interface e em todo método de `ProductAssignmentRepository` (e nos métodos novos de `TenantRepository`, se algum for adicionado aqui). Mapper via MapStruct (`ProductAssignmentMapper`). 100% de cobertura nas classes funcionais.
- Entregar em rodadas:
  1. `ProductAssignment` (entity) + `ProductAssignmentRepository` (constraint única `(productId, userSubject)`) + testes `@DataJpaTest`.
  2. `ProductAssignmentMapper` (MapStruct) + testes de mapper.
  3. Extensão de `TenantService` (PUT/DELETE, escopo por papel) + `ProductAssignmentService` (validação `userId` XOR `inviteEmail`, etc.) + testes com mocks — cada regra da Seção A/B com teste do caminho feliz e da rejeição.
  4. Extensão de `TenantController` + novo `ProductAssignmentController` + testes `@WebMvcTest` + validação via `curl`.

## Critérios de aceite

- [ ] `PUT`/`DELETE` de tenant funcionam; `DELETE` exige `confirmationText` correto e cascateia.
- [ ] `GET /tenants` com token de `SUPER_ADMIN` retorna todos os tenants; com outro papel, só os do usuário.
- [ ] `ProductAssignment` pode ser criada com `userId` existente no tenant.
- [ ] `ProductAssignment` pode ser criada com `inviteEmail` (dispara convite, status `convidado`).
- [ ] Criar com `userId` E `inviteEmail` ao mesmo tempo (ou nenhum dos dois) é rejeitado com 400.
- [ ] `userId` de um usuário de outro tenant é rejeitado.
- [ ] `DELETE` de `ProductAssignment` remove a atribuição.
- [ ] Excluir um tenant remove em cascata seus produtos, módulos, memberships e `ProductAssignment`s.
- [ ] `mvn clean verify` confirma 100% de cobertura nas classes elegíveis desta etapa (JaCoCo).
- [ ] `ProductAssignmentRepository` tem Javadoc na interface e em todo método.

## Validação

> **Entrega via collection Postman, não só curl** (ver `00_padrao_qualidade_e_arquitetura.md`, Seção 11). Os `curl` abaixo são a especificação exata de cada request — adicione-os à pasta desta etapa em `aegis-postman-collection.json` (collection cumulativa, autenticação via `{{token}}` herdado da pasta "Auth") e devolva o JSON completo atualizado para download.

```bash
# super admin vê todos os tenants
curl -H "Authorization: Bearer $TOKEN_SUPER_ADMIN" http://localhost:8080/api/v1/tenants

# editar tenant
curl -X PUT http://localhost:8080/api/v1/tenants/<tenantId> \
  -H "Authorization: Bearer $TOKEN_SUPER_ADMIN" -H "Content-Type: application/json" \
  -d '{"name":"BYOP Updated","plan":"Enterprise","status":"ativo"}'

# atribuir produto a usuário existente
curl -X POST http://localhost:8080/api/v1/products/<productId>/users \
  -H "Authorization: Bearer $TOKEN_SUPER_ADMIN" -H "Content-Type: application/json" \
  -d '{"tenantId":"<tenantId>","productId":"<productId>","userId":"<userId>","role":"Editor"}'

# atribuir produto convidando um usuário novo
curl -X POST http://localhost:8080/api/v1/products/<productId>/users \
  -H "Authorization: Bearer $TOKEN_SUPER_ADMIN" -H "Content-Type: application/json" \
  -d '{"tenantId":"<tenantId>","productId":"<productId>","inviteEmail":"novo@cliente.com","inviteName":"Novo Usuario","role":"Viewer"}'

# excluir tenant (esperado: 400 sem confirmationText correto)
curl -X DELETE http://localhost:8080/api/v1/tenants/<tenantId> \
  -H "Authorization: Bearer $TOKEN_SUPER_ADMIN" -H "Content-Type: application/json" \
  -d '{"confirmationText":"nome errado"}'
```

## Artefato de continuidade — `SPRINT-RESULTADO.md`

> Ver `00_padrao_qualidade_e_arquitetura.md`, Seção 12. Antes do commit, gere/atualize `docs/sprints/sprint-02-fundacao-backend-gpt/SPRINT-RESULTADO.md` (arquivo inteiro, nunca um diff) com a entrada desta etapa (template fixo da Seção 12.2): classes criadas, endpoints confirmados, qualquer decisão que esta etapa deixou a seu critério (registre a escolha real), e retrofits pendentes para etapas futuras. É o que a próxima conversa do GPT vai receber em vez da memória que ela não tem.

## Commit sugerido

```bash
git add backend/
git commit -m "feat(backend): crud completo de tenant, escopo por papel e entidade product assignment"
```
