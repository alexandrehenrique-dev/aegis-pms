# Etapa 11 — Seed inicial (tenants/produtos) e seed do Knowledge Graph

> Cole este arquivo inteiro numa conversa nova do GPT. Pré-requisito: etapas 06 e 07 concluídas.

## Contexto fixo

Para desenvolver e demonstrar o Aegis sem depender de cadastro manual repetitivo, precisamos de dados iniciais: um tenant, alguns produtos de exemplo do ecossistema BYOP, e um pequeno grafo de conhecimento populado.

## Objetivo

Seed reprodutível de tenant + produtos + módulos habilitados, e seed de grafo para pelo menos dois produtos.

## Tarefas

### A. Seed de tenant e produtos

Tenant: `BYOP`.

Produtos e módulos habilitados:

- `maestro-beton`: CONTENT, PAGES, ASSETS, FORMS, SEO, ANALYTICS, MUSIC, KNOWLEDGE_GRAPH
- `conecta-talentos`: CONTENT, PAGES, FORMS, SUBMISSIONS, JOBS, SEO, ANALYTICS, INTEGRATIONS, KNOWLEDGE_GRAPH
- `alexandre-dev`: PORTFOLIO, CONTENT, PAGES, ASSETS, SEO, ANALYTICS, KNOWLEDGE_GRAPH
- `cmss`: CONTENT, PAGES, ASSETS, FORMS, SEO, ANALYTICS, KNOWLEDGE_GRAPH
- `loki`: LIBRARY, BOOKS, MUSIC, CONTENT, SEO, ANALYTICS, KNOWLEDGE_GRAPH
- `wikidev`: KNOWLEDGE_BASE, CONTENT, COMMENTS, CONTRIBUTORS, FORMS, ANALYTICS, KNOWLEDGE_GRAPH

Estratégia recomendada: dados técnicos globais via migration Flyway; dados de demo via `CommandLineRunner` restrito ao profile `local` (nunca rodar em `prod`).

### B. Seed do Knowledge Graph

**WikiDev**: nodes para o produto, categoria "Programação", tópico "Java", artigo "Spring Boot", artigo "JPA". Edges: `WikiDev CONTAINS Categoria Programação`, `Categoria Programação CONTAINS Tópico Java`, `Tópico Java CONTAINS Artigo Spring Boot`, `Artigo Spring Boot RELATED_TO Artigo JPA`.

**Loki**: nodes para o produto, um poema, uma música, uma playlist. Edges: `Poema INSPIRED_BY Música`, `Música PART_OF Playlist`.

## Critérios de aceite

- [ ] Os 6 produtos aparecem na API com os módulos corretos habilitados.
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
