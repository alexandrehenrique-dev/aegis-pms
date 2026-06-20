# Etapa 20 — Seed inicial (tenants/produtos/usuários) e seed do Knowledge Graph

> Cole este arquivo inteiro numa conversa nova do GPT. Pré-requisito: etapas 06, 07, 09 e 17 concluídas (Tenant/Product/Modules, Knowledge Graph, CRUD de tenant + `ProductAssignment`, e os campos extras de grafo).

## Contexto fixo

Para desenvolver e demonstrar o Aegis sem depender de cadastro manual repetitivo, precisamos de dados iniciais: tenants, produtos de exemplo do ecossistema BYOP, um pequeno grafo de conhecimento populado, e usuários/memberships/atribuições de produto que espelhem o que o frontend já usa em modo mock (`frontend/src/core/auth/mocks/users.ts`) — assim o login real (Sprint 06, fora do GPT) encontra os mesmos dados que hoje só existem no mock, sem exigir recadastro manual para demonstrar o fluxo de Super Admin.

## Objetivo

Seed reprodutível de tenants + produtos + módulos habilitados + usuários/memberships/atribuições de produto, e seed de grafo para pelo menos dois produtos.

## Tarefas

### A. Seed de tenants e produtos

Tenants: `BYOP` (plano Pro), `Aegis Labs` (plano Enterprise), `Cliente Norte` (plano Starter, status `suspenso`) — mesmos três tenants já usados no mock do frontend (`mockTenantsByUser`), para o login de `super-admin@byop.io` mostrar os mesmos dados em modo mock e em modo real.

Produtos e módulos habilitados (tenant `BYOP`, salvo indicação contrária):

- `maestro-beton`: CONTENT, PAGES, ASSETS, FORMS, SEO, ANALYTICS, MUSIC — **sem** KNOWLEDGE_GRAPH (Site Institucional, ver Sprint 11 Tarefa A.2/A.3: nenhum dos contratos pede cross-referência de conteúdo)
- `conecta-talentos`: CONTENT, PAGES, FORMS, SUBMISSIONS, JOBS, SEO, ANALYTICS, INTEGRATIONS — **sem** KNOWLEDGE_GRAPH (Portal, desligado por padrão)
- `alexandre-dev`: PORTFOLIO, CONTENT, PAGES, ASSETS, SEO, ANALYTICS — **sem** KNOWLEDGE_GRAPH (Portfolio, desligado por padrão, ligável manualmente)
- `cmss`: CONTENT, PAGES, ASSETS, FORMS, SEO, ANALYTICS — **sem** KNOWLEDGE_GRAPH (Site Institucional, desligado por padrão)
- `loki`: LIBRARY, BOOKS, MUSIC, CONTENT, SEO, ANALYTICS, KNOWLEDGE_GRAPH — **com** KNOWLEDGE_GRAPH ligado (módulo central do relacionamento obra↔música)
- `wikidev`: KNOWLEDGE_BASE, CONTENT, COMMENTS, CONTRIBUTORS, FORMS, ANALYTICS, KNOWLEDGE_GRAPH — **com** KNOWLEDGE_GRAPH ligado (módulo central do produto)
- tenant `Aegis Labs`: `aegis-core` (Produto SaaS), `aegis-docs` (Knowledge Base) — CONTENT, ANALYTICS, KNOWLEDGE_GRAPH em ambos.
- tenant `Cliente Norte`: `portal-norte` (Portal) — CONTENT, PAGES, ANALYTICS.

Estratégia recomendada: dados técnicos globais via migration Flyway; dados de demo via `CommandLineRunner` restrito ao profile `local` (nunca rodar em `prod`).

### A.1 Seed de usuários, memberships e atribuições de produto

Usuários e papéis, espelhando `frontend/src/core/auth/mocks/users.ts` (mesma senha de teste em todos, `senha123`, criada via Keycloak admin API na etapa 03):

- `super-admin@byop.io` — `TenantMembership` ativa em `BYOP`, `Aegis Labs` e `Cliente Norte`, papel `SUPER_ADMIN`.
- `admin@byop.io` — `TENANT_ADMIN` em `BYOP`.
- `pm@byop.io` — `PRODUCT_MANAGER` em `BYOP`, com `ProductAssignment` (papel `PRODUCT_MANAGER`) em `maestro-beton`, `alexandre-dev`(?), `cmss`, `wikidev` (ajustar à mesma lista de produtos do mock `mockProductsByUser.u2`).
- `editor@byop.io` — `EDITOR` em `BYOP`, `ProductAssignment` em `maestro-beton`.
- `viewer@byop.io` — `VIEWER` em `BYOP`, `ProductAssignment` em `maestro-beton`.

### B. Seed do Knowledge Graph

**WikiDev**: nodes para o produto, categoria "Programação", tópico "Java", artigo "Spring Boot", artigo "JPA". Edges: `WikiDev CONTAINS Categoria Programação`, `Categoria Programação CONTAINS Tópico Java`, `Tópico Java CONTAINS Artigo Spring Boot`, `Artigo Spring Boot RELATED_TO Artigo JPA`.

**Loki**: nodes para o produto, um poema, uma música, uma playlist. Edges: `Poema INSPIRED_BY Música`, `Música PART_OF Playlist`.

## Critérios de aceite

- [ ] Os 3 tenants (`BYOP`, `Aegis Labs`, `Cliente Norte`) e os 8 produtos aparecem na API com os módulos corretos habilitados.
- [ ] Os 5 usuários de teste existem no Keycloak (etapa 03) e suas memberships/atribuições de produto existem no backend.
- [ ] `super-admin@byop.io` autenticado vê os 3 tenants via `GET /api/v1/tenants`; os demais usuários veem só os seus.
- [ ] Seed roda apenas em profile `local` (não em `prod`).
- [ ] Grafo do WikiDev e do Loki existe e responde a consultas de `neighbors`/`related`.

## Validação

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/products

curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/products/<wikidev-id>/graph/nodes/<spring-boot-node-id>/neighbors
```

## Commit sugerido

```bash
git add backend/
git commit -m "feat(backend): seed inicial de tenant/produtos e seed do knowledge graph"
```
