# ADR-0019 — Visibilidade de produtos e usuários por papel

## Status

ACCEPTED

## Contexto

A etapa 07 do backend define que `GET /api/v1/products` retorna "só produtos de tenants onde o usuário tem membership ativa". Isso usa `TenantMembership` como filtro — o que significa que um `PRODUCT_MANAGER` com membership no tenant BYOP veria **todos** os produtos do tenant BYOP, mesmo os que não foram atribuídos a ele.

Na prática, o modelo correto é diferente por papel:

- **TENANT_ADMIN**: deve ver todos os produtos do tenant — ele administra o tenant inteiro.
- **PRODUCT_MANAGER, EDITOR, VIEWER**: devem ver **apenas os produtos para os quais têm um `ProductAssignment`** — são papéis de produto, não de tenant.
- **SUPER_ADMIN**: vê metadados de todos os produtos (para gerenciar infraestrutura), mas não acessa conteúdo — ver ADR-0018.

A mesma lógica se aplica à listagem de usuários:
- `GET /tenants/{tenantId}/users`: um PRODUCT_MANAGER não deve ver todos os usuários do tenant — só os usuários que compartilham algum produto com ele.
- `GET /products/{productId}/users`: restrito aos usuários atribuídos àquele produto específico.

Sem esta distinção documentada, o backend implementaria um único filtro "por tenant" para todos os papéis, expondo dados de produtos e usuários fora do escopo de cada papel — violando o princípio de menor privilégio e, potencialmente, a LGPD (um editor de um produto não precisa saber quem são os editores dos outros produtos do mesmo tenant).

## Decisão

### 1. `GET /api/v1/products` — filtro por papel

| Papel | O que vê |
|---|---|
| `SUPER_ADMIN` | Todos os produtos de todos os tenants (metadados; conteúdo bloqueado — ADR-0018) |
| `TENANT_ADMIN` | Todos os produtos do seu tenant |
| `PRODUCT_MANAGER` | Apenas produtos com `ProductAssignment` ativo para seu `userSubject` |
| `EDITOR` | Apenas produtos com `ProductAssignment` ativo para seu `userSubject` |
| `VIEWER` | Apenas produtos com `ProductAssignment` ativo para seu `userSubject` |

Implementação: `ProductService.listProducts(AuthenticatedUser caller)` resolve o filtro a partir do papel do caller:
- `SUPER_ADMIN`: `findAll()` (sem filtro de tenant)
- `TENANT_ADMIN`: `findAllByTenantId(caller.tenantId)` (via TenantMembership)
- `PRODUCT_MANAGER | EDITOR | VIEWER`: `findAllByUserSubject(caller.subject)` (via ProductAssignment)

A seleção do critério de filtro é determinada no `Service`, nunca passada como parâmetro do cliente (nunca `?filter=all` que o frontend poderia manipular).

### 2. `GET /api/v1/products/{productId}` — detalhe de produto

Aplica as mesmas regras de `ProductAccessResolver` (ADR-0018):
- SUPER_ADMIN: metadados sim, conteúdo não
- TENANT_ADMIN: acesso total
- PRODUCT_MANAGER/EDITOR/VIEWER: exige `ProductAssignment` para este `productId`

### 3. `GET /api/v1/tenants/{tenantId}/users` — listagem de usuários do tenant

| Papel | O que vê |
|---|---|
| `SUPER_ADMIN` | Todos os usuários do tenant |
| `TENANT_ADMIN` | Todos os usuários do tenant |
| `PRODUCT_MANAGER` | Apenas usuários que compartilham pelo menos um produto com ele (interseção de `ProductAssignment.userSubject` dos produtos do caller) |
| `EDITOR` / `VIEWER` | Mesma regra do PRODUCT_MANAGER |

Razão: um PRODUCT_MANAGER gerencia equipes de produtos específicos. Ver todos os usuários do tenant (incluindo os de outros produtos) é dado além do seu escopo.

### 4. `GET /api/v1/products/{productId}/users` — usuários de um produto específico

Restrito a usuários com `ProductAssignment` para aquele `productId`. Qualquer chamador (incluindo PRODUCT_MANAGER) só vê os usuários do produto que ele próprio está autorizado a gerenciar.

### 5. Consistência de escopo no convite

`POST /tenants/{tenantId}/users/invite`:
- SUPER_ADMIN / TENANT_ADMIN: podem convidar para qualquer produto do tenant
- PRODUCT_MANAGER: pode convidar apenas para produtos onde **ele** tem `ProductAssignment` ativo com papel `PRODUCT_MANAGER` (não para produtos que ele não gerencia)
- EDITOR / VIEWER: não podem convidar

### 6. Gestão de equipe de produto pelo PRODUCT_MANAGER

O PRODUCT_MANAGER é responsável pela equipe dos produtos que gerencia, mas não
pela base inteira de usuários do tenant. Portanto:

| Rota / tela | PRODUCT_MANAGER |
|---|---|
| `GET /api/v1/tenants/{tenantId}/users` / `/users` | permitido apenas como visão filtrada por usuários que compartilham produto com ele; a UI deve tratar como consulta/leitura, não como gestão ampla do tenant |
| `POST /api/v1/tenants/{tenantId}/users/invite` | permitido somente quando `allowedProductIds` é subconjunto dos produtos em que o caller é `PRODUCT_MANAGER`; convites sem produto explícito são rejeitados para PRODUCT_MANAGER |
| `GET /api/v1/products/{productId}/users` / `/settings/team` | permitido para produto onde o caller tem qualquer `ProductAssignment` ativo, respeitando a visibilidade do produto |
| `POST /api/v1/products/{productId}/users` | permitido para SUPER_ADMIN, TENANT_ADMIN ou PRODUCT_MANAGER ativo daquele produto; EDITOR/VIEWER não gerenciam equipe |
| `DELETE /api/v1/products/{productId}/users/{userId}` | mesma regra de gestão: SUPER_ADMIN, TENANT_ADMIN ou PRODUCT_MANAGER ativo daquele produto |

Essa regra resolve a ambiguidade entre "PRODUCT_MANAGER não vê todos os usuários
do tenant" e "PRODUCT_MANAGER gerencia equipe de produto": o escopo administrativo
dele é o produto, nunca o tenant inteiro.

Exclusão de produto não é exclusão de usuário. Quando um produto é deletado,
somente os vínculos daquele produto são removidos; a identidade Keycloak e o
membership do tenant devem permanecer enquanto o usuário tiver qualquer outro
vínculo ativo em outro produto ou tenant. A regra vale para todos os papéis de
produto (`PRODUCT_MANAGER`, `EDITOR`, `VIEWER`).

## Consequências

Positivas:
- Menor privilégio: cada papel vê exatamente o que precisa para suas responsabilidades
- Privacidade: membros de um produto não ficam expostos para membros de outros produtos do mesmo tenant
- Clareza de UX: o PRODUCT_MANAGER vê uma lista focada, sem "ruído" de produtos que não são dele
- Coerência operacional: Product Manager consegue montar e manter a equipe do produto que administra, sem ganhar poderes de Tenant Admin

Negativas / trade-offs:
- `GET /products` precisa de lógica de filtro condicional por papel — mais complexo que um único `findAllByTenantId`; mitigado pelo fato de ser localizado no `ProductService`, nunca espalhado
- `GET /tenants/{tenantId}/users` para PRODUCT_MANAGER precisa de um JOIN entre `ProductAssignment` do caller e `ProductAssignment` de todos os outros usuários — query mais complexa, mas correta
- Convites e alterações de equipe precisam validar `allowedProductIds`/`productId` no backend, mesmo quando a UI já restringe as opções

## Alternativas Consideradas

- **Todos os papéis usam TenantMembership como filtro**: rejeitado — expõe todos os produtos e usuários do tenant a quem não deveria ver
- **PRODUCT_MANAGER vê todos os produtos mas não pode editar os que não são seus**: rejeitado — ver dado que não é do seu escopo já é o problema, mesmo sem poder editar
- **Filtro via parâmetro de query (`?scope=mine`)**: rejeitado — o cliente poderia omitir o parâmetro e ver tudo; o critério deve ser determinado pelo backend com base no papel do token
- **PRODUCT_MANAGER sem permissão para gerenciar equipe nenhuma**: rejeitado — contradiz a responsabilidade operacional do papel sobre o produto e forçaria Tenant Admin a executar tarefas de equipe que pertencem ao dono do produto

## Impactos

- **Backend**: `ProductService.listProducts` passa a receber o `AuthenticatedUser` como parâmetro de filtro (etapa 07); `UserService.listUsers` adiciona a mesma lógica (etapa 15); testes adicionais em Rodada 3 (service) cobrindo o filtro por papel para cada um dos 5 papéis.
- **Frontend**: nenhuma mudança de UI necessária — o backend já devolverá a lista filtrada; o frontend renderiza o que recebe (Sprint 07 de integração mock→real).
- **Seed** (etapa 21): incluir seeds de `ProductAssignment` que permitam validar o filtro — ex.: `editor_joao` atribuído só ao produto A, e confirmar que `GET /products` para ele não retorna o produto B (mesmo que ambos sejam do mesmo tenant).

## Links Relacionados

- ADR-0018 (escopo do SUPER_ADMIN).
- ADR-0014 (modelo canônico de papéis).
- `docs/sprints/backend/07_modelo_core_tenant_product_modulos.md` (ProductService.listProducts).
- `docs/sprints/backend/15_dominio_users.md` (UserService.listUsers).
- `docs/sprints/backend/10_tenants_crud_completo_e_product_assignment.md` (ProductAssignment).
