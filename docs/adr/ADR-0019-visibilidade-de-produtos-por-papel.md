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

### 7. Jornada de produto para PRODUCT_MANAGER / EDITOR / VIEWER

Papéis de produto podem pertencer a vários produtos em vários tenants diferentes,
mas a jornada principal deles é **produto-first**, não tenant-first. Para esses
papéis, o tenant é contexto técnico de autorização e auditoria; a UI não deve
exigir que o usuário "entenda" ou selecione tenant antes de escolher o produto.

Regras de UX/autorização:

| Papel | Entrada pós-login | Navegação primária |
|---|---|---|
| `PRODUCT_MANAGER` | Seleção direta de produto atribuído, agregando produtos de todos os tenants onde tem assignment ativo | Workspace do produto (`/content`, `/pages`, `/assets`, `/forms`, `/analytics`, `/knowledge`, settings de produto/equipe) |
| `EDITOR` | Seleção direta de produto atribuído | Workspace editorial (`/content`, `/pages`, `/assets`, `/forms`) |
| `VIEWER` | Seleção direta de produto atribuído | Leitura/consulta (`/content`, `/pages`, `/analytics`) |
| `TENANT_ADMIN` | Seleção de tenant, depois visão administrativa de produtos/usuários/settings do tenant | Hub administrativo do tenant |
| `SUPER_ADMIN` | Visão de plataforma/tenants/produtos, com acesso a conteúdo somente quando também houver `ProductAssignment` explícito (ADR-0018) | Hub de plataforma; workspace de produto apenas no contexto de assignment explícito |

Consequências práticas:
- Um `PRODUCT_MANAGER`, `EDITOR` ou `VIEWER` atribuído a produtos em tenants
  diferentes vê uma lista única de produtos autorizados; selecionar um produto
  seta o tenant internamente para chamadas API, mas não expõe uma jornada de
  troca de tenant como pré-requisito.
- Os testes de fumaça do frontend devem validar esta jornada nova, não o fluxo
  antigo. Para papéis de produto, pós-login válido deve cair em
  `/select-product` ou diretamente no workspace do produto; um teste que espera
  `/select-tenant` para `PRODUCT_MANAGER`/`EDITOR`/`VIEWER` está testando o
  comportamento errado.
- Rotas, botões, cards e menus de configuração devem seguir a mesma autorização
  do backend. Não basta bloquear a chamada HTTP: ações que o papel não pode
  executar não devem aparecer como affordance principal na UI.
- Busca global, timeline lateral, dashboards de produto e qualquer sidebar de
  "atividade recente" seguem o mesmo contexto efetivo. Para papéis de produto,
  o escopo padrão é o produto selecionado; não podem aparecer itens, assets,
  conteúdos, produtos ou eventos de outros produtos do mesmo tenant. Para
  `SUPER_ADMIN`/`TENANT_ADMIN`, a busca pode listar produtos do escopo
  administrativo, mas itens operacionais internos (conteúdo/assets/forms/KG)
  continuam filtrados pelo produto quando houver produto selecionado.
- Telas de settings são contextuais. `EDITOR` e `VIEWER` não veem settings;
  `PRODUCT_MANAGER` vê apenas settings operacionais de produto/equipe que seu
  papel permite; tenant/security/roles/access-preview/audit continuam reservados
  a `TENANT_ADMIN`/`SUPER_ADMIN` conforme ADR-0014/0018.
- `Dashboard Global` é uma tela administrativa de tenant/plataforma.
  `ProductDashboard` também é cockpit administrativo do produto. Para
  `PRODUCT_MANAGER`, `EDITOR` e `VIEWER`, a landing após escolher produto é o
  workspace operacional permitido (ex.: `/content`), não `/dashboard` nem
  `/products/{id}`. Esses papéis não devem ver item de sidebar para dashboard
  global nem KPIs agregados do tenant.
- Módulos de produto são configuração administrativa. A ativação/desativação de
  módulos é permitida para `SUPER_ADMIN` e `TENANT_ADMIN` ativo do tenant; papéis
  de produto operam módulos já habilitados, mas não alteram o catálogo habilitado.
- Métricas e listas de usuários devem falar a mesma língua: "usuários ativos"
  considera somente sujeitos com membership de tenant ativa e vínculo de produto
  ativo. Usuários removidos, convites expirados e vínculos removidos podem ser
  auditados/filtrados explicitamente, mas não entram em KPIs ativos nem aparecem
  por padrão nas listas operacionais do produto.
- Relatórios de analytics devem respeitar o formato anunciado pela UI
  (`PDF`, `CSV`, `XLSX`) e o contexto do produto selecionado. Arquivos de
  download genéricos, sem extensão correta ou contendo apenas data de geração
  não satisfazem a jornada de relatório.

Essa decisão segue padrões similares observados em produtos consolidados:
- GitLab documenta que usuários recebem um papel ao serem adicionados a um grupo
  ou projeto, e esse papel determina permissões naquele escopo.
- GitLab também trata membros de projeto como usuários/grupos que têm acesso a
  um projeto específico, com ações de membros dependentes do papel do projeto.
- Microsoft Entra documenta o princípio de usar o menor papel privilegiado por
  tarefa administrativa, reforçando a separação entre operação cotidiana e
  administração sensível.

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
- A UI precisa manter um mapa explícito de rotas/ações por papel efetivo
  (plataforma + assignment de produto no contexto selecionado), para evitar
  dashboards, botões e settings administrativos em jornadas de editor/viewer

## Alternativas Consideradas

- **Todos os papéis usam TenantMembership como filtro**: rejeitado — expõe todos os produtos e usuários do tenant a quem não deveria ver
- **PRODUCT_MANAGER vê todos os produtos mas não pode editar os que não são seus**: rejeitado — ver dado que não é do seu escopo já é o problema, mesmo sem poder editar
- **Filtro via parâmetro de query (`?scope=mine`)**: rejeitado — o cliente poderia omitir o parâmetro e ver tudo; o critério deve ser determinado pelo backend com base no papel do token
- **PRODUCT_MANAGER sem permissão para gerenciar equipe nenhuma**: rejeitado — contradiz a responsabilidade operacional do papel sobre o produto e forçaria Tenant Admin a executar tarefas de equipe que pertencem ao dono do produto
- **PRODUCT_MANAGER/EDITOR/VIEWER escolherem tenant antes de produto**: rejeitado — o tenant é necessário para a API e auditoria, mas não é o objeto mental desses papéis; forçar a escolha aumenta confusão e expõe estrutura organizacional desnecessária

## Impactos

- **Backend**: `ProductService.listProducts` recebe o `AuthenticatedUser` como parâmetro de filtro (etapa 07); `UserService.listUsers` adiciona a mesma lógica (etapa 15); `ProductModuleService` bloqueia alteração de módulos para papéis de produto; testes cobrem filtro por papel, assignment multi-produto e bloqueio de módulos.
- **Frontend**: a seleção de produto para `PRODUCT_MANAGER`/`EDITOR`/`VIEWER` agrega produtos de todos os tenants autorizados e seta tenant internamente; sidebar, rotas, botões e settings são filtrados pelo papel efetivo no produto selecionado.
- **Seed** (etapa 21): incluir seeds de `ProductAssignment` que permitam validar o filtro — ex.: `editor_joao` atribuído só ao produto A, e confirmar que `GET /products` para ele não retorna o produto B (mesmo que ambos sejam do mesmo tenant).
- **Bruno/smoke**: collections de RBAC devem validar `/me` canônico, listagem de produtos com `callerAssignedRole`, bloqueios de convite/remoção/módulos e limpeza de usuários temporários no teardown.

## Links Relacionados

- ADR-0018 (escopo do SUPER_ADMIN).
- ADR-0014 (modelo canônico de papéis).
- `docs/sprints/backend/07_modelo_core_tenant_product_modulos.md` (ProductService.listProducts).
- `docs/sprints/backend/15_dominio_users.md` (UserService.listUsers).
- `docs/sprints/backend/10_tenants_crud_completo_e_product_assignment.md` (ProductAssignment).
- GitLab Docs — Roles and permissions: https://docs.gitlab.com/user/permissions/
- GitLab Docs — Members of a project: https://docs.gitlab.com/user/project/members/
- Microsoft Learn — Least privileged roles by task: https://learn.microsoft.com/en-us/entra/identity/role-based-access-control/delegate-by-task
