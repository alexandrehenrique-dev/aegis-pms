# Etapa 07 — Knowledge Graph MVP (modelo relacional + regras de consistência)

> Cole este arquivo inteiro numa conversa nova do GPT. Pré-requisito: etapa 06 concluída (Product já existe).

## Contexto fixo

O Aegis precisa de um Knowledge Graph funcional já no MVP, modelado como grafo lógico relacional em PostgreSQL (não Neo4j obrigatório nesta fase), mas preparado para migração futura. Este é o módulo que a Sprint 05 do plano de frontend (fora do GPT) vai expor como "funcionalidade atribuível a um produto" — ou seja, o Knowledge Graph é só mais um `moduleKey` (`KNOWLEDGE_GRAPH`) do catálogo da etapa 06, com seu próprio modelo de dados.

## Objetivo

Criar `GraphNode` e `GraphEdge`, os endpoints de grafo, e as regras de consistência que impedem o grafo de virar um depósito caótico de relações.

## Tarefas

### A. Entidades

**GraphNode**: `id`, `tenantId`, `productId`, `nodeType`, `refType`, `refId`, `label`, `slug`, `metadataJson`, `createdAt`, `updatedAt`.

**GraphEdge**: `id`, `tenantId`, `productId`, `sourceNodeId`, `targetNodeId`, `edgeType`, `weight`, `metadataJson`, `createdAt`, `updatedAt`.

Tipos de node iniciais: `PRODUCT, PAGE, CONTENT, ARTICLE, CATEGORY, TOPIC, TAG, ASSET, FORM, JOB_POSTING, CANDIDATE, BOOK, POEM, MANIFESTO, REFLECTION, MUSIC_REFERENCE, PROJECT, SKILL, SERVICE, EVENT, CONTRACT`.

Tipos de edge iniciais: `CONTAINS, BELONGS_TO, REFERENCES, RELATED_TO, INSPIRED_BY, USES, IMPLEMENTS, PUBLISHED_AS, SUBMITTED_TO, TAGGED_WITH, PART_OF, DEPENDS_ON`.

Índices obrigatórios: `tenantId`, `productId`, `nodeType`, `(refType, refId)`, `sourceNodeId`, `targetNodeId`, `edgeType`, único em `(productId, refType, refId)`.

### B. Endpoints

```txt
POST /api/v1/products/{productId}/graph/nodes
GET  /api/v1/products/{productId}/graph/nodes
GET  /api/v1/products/{productId}/graph/nodes/{nodeId}
POST /api/v1/products/{productId}/graph/edges
GET  /api/v1/products/{productId}/graph/nodes/{nodeId}/neighbors
GET  /api/v1/products/{productId}/graph/nodes/{nodeId}/related
```

### C. Regras de consistência (obrigatórias)

- Edge só pode ligar nodes do mesmo tenant.
- Edge só pode ligar nodes do mesmo produto (cross-product fica para o futuro, explicitamente fora de escopo agora).
- Edge exige `sourceNodeId` e `targetNodeId` existentes — senão 404/400 com erro claro.
- `refId` e `label` não podem ser vazios.
- `edgeType`/`nodeType` devem estar nos catálogos acima.
- Duplicidade `(productId, refType, refId)` bloqueada.
- Duplicidade `(sourceNodeId, targetNodeId, edgeType)` bloqueada ou tratada por upsert.
- **Isolamento por produto** (`00_padrao_qualidade_e_arquitetura.md`, Seção 10): qualquer endpoint da Seção B para um produto fora do escopo do usuário autenticado retorna 404, nunca 403 (mesmo princípio já aplicado à rejeição cross-tenant acima).
- **Module-gating** (Seção 9.2 do padrão): `KnowledgeGraphController` anotado com `@RequireModule(ModuleKey.KNOWLEDGE_GRAPH)` (mecanismo implementado na etapa 06) — produto com módulo `KNOWLEDGE_GRAPH` desabilitado (a maioria dos produtos seed, etapa 20, exceto WikiDev/Loki) retorna 403 `MODULE_DISABLED` em qualquer endpoint deste controller.

### D. Padrão de qualidade e entrega (obrigatório)

> Resumo — detalhe completo em `00_padrao_qualidade_e_arquitetura.md`.

- **Java 25** / **Spring Boot 4.1.x**. Javadoc obrigatório na interface e em todo método de `GraphNodeRepository`/`GraphEdgeRepository`. Mappers via MapStruct (`GraphNodeMapper`, `GraphEdgeMapper`). 100% de cobertura nas classes funcionais — incluindo `GraphConsistencyPolicy` (já citada nos critérios) — DTOs de transporte ficam fora da régua.
- Entregar em rodadas:
  1. `GraphNode`, `GraphEdge` (entities) + `GraphNodeRepository`, `GraphEdgeRepository` + testes `@DataJpaTest` (incluindo os índices/constraints únicos da Seção A).
  2. `GraphNodeMapper`, `GraphEdgeMapper` (MapStruct) + testes de mapper.
  3. `GraphConsistencyPolicy` + `KnowledgeGraphService` (regras da Seção C) + testes com mocks — cada regra de consistência tem teste do caminho feliz e teste da rejeição.
  4. `KnowledgeGraphController` (endpoints da Seção B) + testes `@WebMvcTest` (`@MockitoBean` do service) + validação via `curl` (Seção "Validação").

## Critérios de aceite

- [ ] Criar node e criar edge funcionam.
- [ ] Listar neighbors e related funcionam.
- [ ] Edge com node inexistente é rejeitada (404/400, não 500).
- [ ] Edge cross-tenant é rejeitada.
- [ ] Duplicidade é bloqueada conforme as regras acima.
- [ ] Produto fora do escopo do usuário retorna 404 (não 403).
- [ ] Produto com módulo `KNOWLEDGE_GRAPH` desabilitado retorna 403 `MODULE_DISABLED`.
- [ ] Testes unitários cobrem as regras de consistência (`GraphConsistencyPolicyTest`).
- [ ] `mvn clean verify` confirma 100% de cobertura nas classes elegíveis desta etapa (JaCoCo).
- [ ] `GraphNodeRepository`/`GraphEdgeRepository` têm Javadoc na interface e em todo método.

## Validação

```bash
curl -X POST http://localhost:8080/api/v1/products/<productId>/graph/nodes \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nodeType":"ARTICLE","refType":"ARTICLE","refId":"spring-boot-intro","label":"Introducao ao Spring Boot","slug":"introducao-spring-boot"}'

curl -X POST http://localhost:8080/api/v1/products/<productId>/graph/nodes \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nodeType":"TOPIC","refType":"TOPIC","refId":"spring-boot","label":"Spring Boot","slug":"spring-boot"}'

curl -X POST http://localhost:8080/api/v1/products/<productId>/graph/edges \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"sourceNodeId":"<article-node-id>","targetNodeId":"<topic-node-id>","edgeType":"TAGGED_WITH","weight":1}'

curl http://localhost:8080/api/v1/products/<productId>/graph/nodes/<article-node-id>/neighbors \
  -H "Authorization: Bearer $TOKEN"
```

Testar caso de erro:

```bash
curl -X POST http://localhost:8080/api/v1/products/<productId>/graph/edges \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"sourceNodeId":"00000000-0000-0000-0000-000000000000","targetNodeId":"00000000-0000-0000-0000-000000000001","edgeType":"RELATED_TO"}'
# esperado: 404 ou 400 com erro claro
```

## Commit sugerido

```bash
git add backend/
git commit -m "feat(backend): knowledge graph mvp com modelo relacional e regras de consistencia"
```
