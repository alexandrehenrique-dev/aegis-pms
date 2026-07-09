# ADR-0018 — Escopo do SUPER_ADMIN: operador de plataforma, não de produto

## Status

ACCEPTED

## Contexto

Uma auditoria de cenários reais de uso (após a execução da etapa 06 do backend) revelou que o papel `SUPER_ADMIN` tem acesso irrestrito a **todos** os domínios de produto hoje documentados — incluindo content, pages, assets, forms, analytics e knowledge graph — sem nenhuma restrição de escopo. Isso cria dois problemas distintos:

**Problema 1 — LGPD / privacidade**: o SUPER_ADMIN é o operador da plataforma Aegis (a BYOP, ou quem opera a instância). Os produtos criados dentro de cada tenant pertencem a empresas/pessoas físicas distintas (clientes). O conteúdo editorial desses produtos (artigos, páginas, formulários, assets de marca, dados de formulários submetidos) é dado dos clientes, não do operador da plataforma. Pela LGPD, o operador da plataforma não tem base legal para acessar o conteúdo dos produtos dos seus clientes sem autorização explícita — o que um `SUPER_ADMIN` operacional jamais teria por default.

**Problema 2 — responsabilidade operacional errada**: o SUPER_ADMIN não é um "super-editor" — ele nunca vai criar conteúdo, publicar páginas ou gerenciar formulários de um produto de cliente. Mostrar todas as rotas de domínio de produto na navegação do SUPER_ADMIN cria confusão sobre o que ele pode e deve fazer, e aumenta a superfície de erro operacional (ex.: editar conteúdo de um tenant por engano enquanto debugava outro).

Nenhuma ADR anterior havia escrito explicitamente a separação entre **escopo de plataforma** (criar infraestrutura — tenants, produtos, infraestrutura de usuários) e **escopo de produto** (operar o conteúdo de um produto específico). Essa ambiguidade resulta em:
- Backend sem restrição de conteúdo para SUPER_ADMIN nos domínios de produto
- Frontend com SUPER_ADMIN vendo rotas de content/pages/assets/forms/analytics/knowledge na navbar
- Nenhuma etapa documentando o que SUPER_ADMIN pode ou não acessar nos domínios de produto

## Decisão

O papel `SUPER_ADMIN` é primariamente um **operador de plataforma** — gerencia a infraestrutura (tenants, produtos, usuários, configurações). Para os produtos que ele mesmo cria ou para os quais é explicitamente atribuído, tem **acesso total ao conteúdo** desses produtos. Para os produtos de outros clientes/tenants, o acesso ao conteúdo é bloqueado por padrão (LGPD).

### Regra fundamental: `ProductAssignment` automático ao criar produto

Quando um `SUPER_ADMIN` cria um produto (`POST /api/v1/products`), um `ProductAssignment` com `role: "product_manager"` é **criado automaticamente** para o `userSubject` do criador. Isso garante:
- Acesso imediato e completo ao conteúdo do produto recém-criado
- Rastreabilidade: há um registro explícito de "quem tem acesso a este produto"
- Simetria com outros papéis: o criador é sempre o primeiro membro do produto

> Esta regra se aplica igualmente quando um TENANT_ADMIN cria um produto no seu tenant — ele também recebe um `ProductAssignment` automático como `product_manager` do produto criado.

### O que SUPER_ADMIN pode fazer

| Domínio | Operação |
|---|---|
| Tenants | CRUD completo (criar, listar todos, editar, suspender, excluir) |
| Produtos | Criar (+ recebe ProductAssignment automático), listar metadados de todos, arquivar |
| Conteúdo de produtos **próprios** (com ProductAssignment) | Acesso total — content, pages, assets, forms, analytics, knowledge graph |
| Usuários de tenant | Convidar, bloquear, remover, restaurar — ao nível de tenant |
| Módulos de produto | Habilitar/desabilitar em qualquer produto (incluindo os de clientes) |
| Configurações | Acesso total às configurações da plataforma |
| Auditoria | Leitura de todos os eventos de todos os tenants |
| Feedback/bug-reports | Leitura de todos os bug-reports |
| Notificações | Criar e enviar notificações para qualquer tenant/produto |

### O que SUPER_ADMIN NÃO pode fazer (sem ProductAssignment no produto)

| Domínio | Operação bloqueada |
|---|---|
| Content | Criar, ler, editar, publicar conteúdo de produtos de clientes |
| Pages | Criar, ler, editar páginas de produtos de clientes |
| Assets | Upload, leitura, edição de assets de produtos de clientes |
| Forms | Criar, ler, editar formulários ou submissions de produtos de clientes |
| Analytics | Métricas/KPIs de produtos de clientes |
| Knowledge Graph | Nós/edges de produtos de clientes |

"Produto de cliente" = produto para o qual o SUPER_ADMIN não tem `ProductAssignment`. Mesmo que o SUPER_ADMIN tenha criado o tenant do cliente, se não criou o produto (ou não foi explicitamente atribuído), o acesso ao conteúdo é bloqueado.

### Atribuição explícita para acesso a produto de cliente

Se o SUPER_ADMIN precisar acessar o conteúdo de um produto de cliente (ex.: suporte técnico), um `TENANT_ADMIN` ou o próprio SUPER_ADMIN (via operação de plataforma) pode criar explicitamente um `ProductAssignment` para o produto em questão. Nesse contexto, o SUPER_ADMIN opera com o papel do `ProductAssignment`, não com o papel `super_admin` — para os domínios de produto, o `ProductAssignment.role` prevalece.

### Resposta HTTP ao SUPER_ADMIN sem atribuição

Diferente da regra geral de isolamento cross-tenant (404 — "não revelar existência"), o SUPER_ADMIN sabe que o produto existe (ele mesmo pode ter criado). Por isso, a resposta para SUPER_ADMIN sem `ProductAssignment` tentando acessar domínios de produto é **403** com corpo padronizado:

```json
{
  "error": "PRODUCT_CONTENT_ACCESS_DENIED",
  "message": "SUPER_ADMIN role requires explicit ProductAssignment to access product content"
}
```

### Implementação no backend

Em cada domínio de produto gateado (content, pages, assets, forms, analytics, knowledge graph), o `Service` aplica a seguinte lógica de autorização **antes** do module-gating:

```
1. SUPER_ADMIN com ProductAssignment para este productId → usa o papel do ProductAssignment
2. SUPER_ADMIN sem ProductAssignment → 403 PRODUCT_CONTENT_ACCESS_DENIED
3. TENANT_ADMIN com TenantMembership no tenant do produto → acesso total (passa)
4. PRODUCT_MANAGER/EDITOR/VIEWER com ProductAssignment para este productId → acesso pelo papel
5. Qualquer outro caso → 404 (nem tenantId conhecido → não revelar existência)
```

Um helper de autorização (`ProductAccessResolver` ou similar, implementado na etapa 07 junto ao modelo core) centraliza esta lógica e é chamado por todos os services de domínio — nunca duplicado em cada service.

### Implementação no frontend

O frontend reflete este escopo:
- `roleVisibleNav["super_admin"]` inclui apenas: `/dashboard`, `/products`, `/settings`, `/audit`
- `roleBlockedRoutePrefixes["super_admin"]` bloqueia: `/content`, `/pages`, `/assets`, `/forms`, `/analytics`, `/knowledge`
- A tela de produtos para SUPER_ADMIN mostra metadados (nome, tipo, status, módulos) mas não tem botão "Entrar no produto" — substituído por "Gerenciar" (módulos, usuários, configurações)

Quando o `SUPER_ADMIN` também possui `ProductAssignment` explícito em um produto
selecionado, o frontend deve mesclar a navegação/ações do papel de plataforma
com o papel de produto daquele assignment. Ex.: um Super Admin que também é
`EDITOR` de um produto específico pode acessar o workspace editorial desse
produto, mas continua sem acesso ao conteúdo dos demais produtos sem assignment.
A decisão efetiva é sempre contextual ao produto selecionado e nunca transforma
`SUPER_ADMIN` em super-editor global.

Dashboards e widgets de plataforma não podem reaproveitar componentes de
produto sem escopo explícito. O dashboard global do `SUPER_ADMIN` pode mostrar
metadados e saúde operacional agregada, mas qualquer timeline, busca, lista de
assets/conteúdos/forms ou Knowledge Graph exibida enquanto um produto está
selecionado precisa respeitar o produto selecionado. Isso evita que a UI dê ao
operador de plataforma a impressão de que ele está vendo ou podendo agir sobre
conteúdo de produtos de cliente sem assignment explícito.

Essa regra vale também na direção oposta: `PRODUCT_MANAGER`, `EDITOR` e
`VIEWER` não devem receber o `Dashboard Global` como fallback de navegação. Se
o usuário tem somente papéis de produto, a experiência principal é o workspace
do produto selecionado; dashboards globais, listas amplas de tenant e ações de
criação/configuração de produto ficam reservadas aos papéis administrativos.

## Consequências

Positivas:
- Conformidade LGPD: operador da plataforma não acessa dados dos clientes por default
- Clareza de responsabilidade: SUPER_ADMIN sabe exatamente o que ele gerencia
- Superfície de erro reduzida: impossível editar conteúdo de cliente por engano ao operar a plataforma
- Auditabilidade: qualquer acesso de SUPER_ADMIN a conteúdo fica rastreável (é sempre via ProductAssignment explícito)

Negativas / trade-offs:
- Se o BYOP precisar debugar um produto de cliente (ex.: investigar um bug report), precisa criar um ProductAssignment temporário — um passo a mais, mas um passo auditável
- A distinção "SUPER_ADMIN age como PRODUCT_MANAGER quando tem ProductAssignment" adiciona uma camada de resolução de papel por contexto — implementada uma única vez em `ProductAccessResolver`, não espalhada

## Alternativas Consideradas

- **SUPER_ADMIN com acesso total (modelo atual)**: rejeitado — viola LGPD e cria risco operacional
- **SUPER_ADMIN sem acesso nenhum a produtos (nem metadados)**: rejeitado — o operador da plataforma precisa listar produtos para gerenciar módulos, usuários e infraestrutura; apenas o conteúdo editorial é restrito
- **SUPER_ADMIN com acesso somente-leitura a conteúdo**: rejeitado — somente-leitura ainda viola a LGPD (o operador não tem base legal para ler dados dos clientes sem autorização)

## Impactos

- **Backend**: etapa 07 adiciona `ProductAccessResolver`; todas as etapas de domínio de produto (11-22 para content, pages, assets, forms, analytics, knowledge graph) adicionam a verificação de `ProductAccessResolver` antes do module-gating nos critérios de aceite.
- **Frontend**: `core/permissions/roles.ts` tem `roleVisibleNav["super_admin"]` e `roleBlockedRoutePrefixes["super_admin"]` revisados; Sprint 19 do frontend executa essas mudanças.
- **Seed** (etapa 21): usuário de teste `super_admin` não recebe `ProductAssignment` para nenhum produto por default — propositalmente, para validar que o 403 funciona.

## Links Relacionados

- ADR-0014 (modelo canônico de papéis).
- ADR-0019 (visibilidade de produtos por papel).
- `docs/sprints/backend/07_modelo_core_tenant_product_modulos.md` (ProductAccessResolver).
- `docs/sprints/frontend/19_escopo_super_admin_e_validacoes.md`.
