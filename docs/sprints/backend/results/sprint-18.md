# Sprint 18 — Knowledge Graph: layout persistente, props ricas e nós órfãos

Concluída em 2026-06-29.

## Objetivo

Evoluir o Knowledge Graph além do MVP: persistir layout do canvas (`x`/`y`), expor props estruturadas a partir do metadata, listar e resolver nós órfãos, oferecer preview leve de nós para referências inline e persistir revisão idempotente de insights.

## Resultado alcançado

A Sprint 18 foi entregue com endpoints REST, migration, DTOs, service, repository, mapper, testes automatizados, Bruno cumulativo e documentação de continuidade. O contrato existente de nodes foi expandido de forma compatível, sem remover campos anteriores.

## Classes criadas

- `br.com.byop.aegis.content.api.ContentPreviewLookupService`
- `br.com.byop.aegis.knowledgegraph.api.GraphNodeContentPreview`
- `br.com.byop.aegis.knowledgegraph.api.GraphNodeContentPreviewPort`
- `br.com.byop.aegis.knowledgegraph.contract.ResolveGraphOrphanRequest`
- `br.com.byop.aegis.knowledgegraph.contract.ResolveGraphOrphansRequest`
- `br.com.byop.aegis.knowledgegraph.contract.ReviewGraphInsightRequest`
- `br.com.byop.aegis.knowledgegraph.contract.UpdateGraphNodePositionRequest`
- `br.com.byop.aegis.knowledgegraph.domain.GraphInsightReview`
- `br.com.byop.aegis.knowledgegraph.dto.GraphInsightReviewSummary`
- `br.com.byop.aegis.knowledgegraph.dto.GraphNodePreview`
- `br.com.byop.aegis.knowledgegraph.dto.GraphNodeProp`
- `br.com.byop.aegis.knowledgegraph.exception.InvalidGraphOrphanActionException`
- `br.com.byop.aegis.knowledgegraph.repository.GraphInsightReviewRepository`
- `ContentPreviewLookupServiceTest`
- `GraphInsightReviewRepositoryTest`

## Classes alteradas

- `ContentRepository`
- `ContentService`
- `KnowledgeGraphController`
- `KnowledgeGraphExceptionHandler`
- `GraphNode`
- `GraphNodeDetail`
- `GraphNodeSummary`
- `GraphNodeMapper`
- `GraphNodeRepository`
- `KnowledgeGraphService`
- `ContentServiceTest`
- `KnowledgeGraphControllerTest`
- `GraphNodeMapperTest`
- `GraphNodeRepositoryTest`
- `KnowledgeGraphServiceTest`

## Migration criada

- `V10__knowledge_graph_layout_orphans.sql`

Ela adiciona `x` e `y` em `graph_nodes` com default `0` e cria `graph_insight_reviews`, com unicidade por `product_id + text_hash`.

## Endpoints confirmados

- `PATCH /api/v1/products/{productId}/graph/nodes/{nodeId}/position`
- `GET /api/v1/products/{productId}/graph/orphans`
- `POST /api/v1/products/{productId}/graph/orphans/{nodeId}/resolve`
- `POST /api/v1/products/{productId}/graph/orphans/resolve`
- `GET /api/v1/products/{productId}/graph/nodes/{nodeId}/preview`
- `POST /api/v1/products/{productId}/graph/insights/review`

## Contratos REST atualizados

- `GraphNodeSummary` e `GraphNodeDetail` passaram a devolver `type`, `status`, `x`, `y` e `props`.
- `UpdateGraphNodePositionRequest` valida `{ x, y }`.
- `ResolveGraphOrphanRequest` valida `{ action }`.
- `ResolveGraphOrphansRequest` valida `{ ids, action? }`.
- `ReviewGraphInsightRequest` valida `{ text }`.
- `GraphNodePreview` devolve `{ id, label, type, summary, difficulty, thumbnail }`.
- `GraphInsightReviewSummary` devolve `{ id, text, reviewed }`.

## Decisões de implementação

- `props` estruturadas são derivadas de `metadataJson` como lista ordenada `{ k, v }[]`, sem tabela `graph_node_props`. Motivo: a sprint precisava enriquecer o contrato sem criar um subdomínio de props; `metadataJson` já era o campo flexível oficial do node.
- `status` é derivado da chave `status` do metadata, com fallback `ativo`.
- `x`/`y` ficam persistidos em `graph_nodes`, porque posição de layout é estado próprio do canvas e não deve depender de metadata livre.
- `GET /graph/orphans` retorna apenas nodes sem edge e ainda acionáveis; nodes marcados como resolvidos por curadoria deixam de aparecer.
- `resolve` não cria edge, porque o contrato da sprint não possui `targetNodeId`; criar edge real exigiria inventar payload fora do escopo.
- `reviewInsight` é idempotente por `product_id + text_hash`, persistindo o texto revisado uma única vez por produto.

## Comportamento real das actions de órfão

Todas as actions registram curadoria em `metadataJson` com `orphanResolved=true`, `orphanAction` normalizada em maiúsculas e `status` correspondente. Como não há `targetNodeId` no contrato, nenhuma action cria edge.

- `Arquivar` / `ARQUIVAR` -> `status=arquivado`
- `Vincular` / `VINCULAR` -> `status=vinculado`
- `Associar` / `ASSOCIAR` -> `status=associado`
- `Mesclar` / `MESCLAR` -> `status=mesclado`
- `Revisar` / `REVISAR` -> `status=revisado`
- Batch sem `action` usa `REVISAR`.
- Action inválida retorna `400 INVALID_GRAPH_ORPHAN_ACTION`.

## Preview leve e integração com Content

O preview tenta primeiro `GraphNodeContentPreviewPort`, implementado por `ContentPreviewLookupService` em `content.api`. A busca ocorre por `productId + graphNodeId` e, se necessário, por `refId` quando ele é UUID de conteúdo. Quando há `Content`, o preview usa `summary`, `difficultyLevel.contractValue()` e `thumbnail` vindo de metadata do conteúdo. Se não houver conteúdo associado, o fallback usa `summary`, `difficulty` e `thumbnail` do `metadataJson` do node.

A porta fica em `knowledgegraph.api`, e o adapter em `content.api`, preservando a direção modular e evitando ciclo `knowledgegraph -> content`.

## Isolamento por produto e module-gating

Os endpoints novos ficam em `KnowledgeGraphController`, já anotado com `@RequireModule(ModuleKey.KNOWLEDGE_GRAPH)`. Todos os métodos chamam `ProductAccessPort.assertAccessible(productId, caller)`, preservando o isolamento por produto e as regras de acesso de conteúdo de produto.

## Testes criados/alterados

- `KnowledgeGraphServiceTest`: cobre posição, órfãos, actions individuais/lote, preview com Content, preview via metadata, insight review idempotente e erros.
- `KnowledgeGraphControllerTest`: cobre endpoints novos, validação e erros.
- `GraphNodeMapperTest`: cobre `x`/`y`, `type`, `status`, `props`, metadata inválido/nulo e ordenação.
- `GraphNodeRepositoryTest`: cobre query de órfãos.
- `GraphInsightReviewRepositoryTest`: cobre busca e unicidade por hash.
- `ContentPreviewLookupServiceTest`: cobre preview por graph node, fallback por refId, metadata inválido e campos opcionais; os cenários similares foram parametrizados por `@ParameterizedTest`/`@MethodSource`.
- `ContentServiceTest`: ajustado para leitura tipada de metadata via `TypeReference`.

## Bruno criado/alterado

Pasta `bruno/18-knowledge-graph` criada com:

- `criar-node-orfao.bru`
- `atualizar-posicao-node.bru`
- `listar-orfaos.bru`
- `preview-node-orfao.bru`
- `resolver-orfao-individual.bru`
- `listar-orfaos-apos-resolve.bru`
- `criar-node-orfao-lote.bru`
- `resolver-orfaos-lote.bru`
- `revisar-insight.bru`
- `revisar-insight-idempotente.bru`

A documentação da collection Bruno foi atualizada com a pasta 18 e as variáveis `orphanNodeId`, `orphanBatchNodeId` e `reviewedInsightId`.

## Documentação alterada

- `AGENTS.md`
- `docs/api-testing/README.md`
- `docs/trace/00_endpoints_esperados.md`
- `docs/sprints/backend/SPRINT-RESULTADO.md`
- `docs/sprints/backend/results/sprint-18.md`

## SonarQube for IDE

- `java:S6809` em `KnowledgeGraphService`: `resolveOrphans(...)` chamava `resolveOrphan(...)` diretamente no mesmo bean, ambos transacionais. Causa: self-invocation em Spring ignora o proxy transacional. Correção: ambos continuam entradas públicas transacionais, mas delegam para o helper privado não transacional `resolveOrphanNode(...)`.
- `java:S5976` em `ContentPreviewLookupServiceTest`: três testes exercitavam a mesma lógica variando apenas entrada e saída esperada. Correção: consolidação em `@ParameterizedTest` com `@MethodSource`, mantendo os três cenários nomeados.
- Ajuste preventivo adicional: removido `@SuppressWarnings("unchecked")` legado em `ContentService`, substituído por `TypeReference<Map<String, Object>>`.

Recomendações adicionadas ao `AGENTS.md`:

- Não chamar método `@Transactional` do próprio bean via `this` ou chamada direta interna.
- Usar `@ParameterizedTest` com `@MethodSource`/`@CsvSource` quando testes variam apenas entrada e saída esperada.

Não foi usado `@SuppressWarnings`, `NOSONAR`, proxy manual, `ApplicationContext` ou desabilitação de regra Sonar.

## Comandos executados

- `mvn -q -Dtest='KnowledgeGraphServiceTest,KnowledgeGraphControllerTest,GraphNodeMapperTest,GraphNodeRepositoryTest,GraphInsightReviewRepositoryTest,ContentPreviewLookupServiceTest' test`
- `mvn -q -Dtest='KnowledgeGraphServiceTest,ContentServiceTest,ContentPreviewLookupServiceTest' test`
- `mvn -q -Dtest='KnowledgeGraphServiceTest,GraphNodeMapperTest,ContentPreviewLookupServiceTest,ContentServiceTest' test jacoco:report`
- `mvn -Dtest=ContentPreviewLookupServiceTest test`
- `mvn clean verify`
- `cd bruno && npx @usebruno/cli run --env local`

## Resultado do `mvn clean verify`

- `BUILD SUCCESS`
- 967 testes
- 0 falhas
- 0 erros
- 0 ignorados
- JaCoCo aprovado: `All coverage checks have been met`
- Spring Modulith aprovado via suíte completa

## Resultado do Bruno

`cd bruno && npx @usebruno/cli run --env local`

- Status: PASS
- Requests: 156 executados, 156 aprovados
- Tests: 301/301 aprovados
- Sem regressão na collection cumulativa

## Bugs reais encontrados

- A primeira implementação de preview criou ciclo Modulith `content -> knowledgegraph -> content`; corrigido por inversão de dependência com `GraphNodeContentPreviewPort`.
- A query de órfãos precisou de `@Param("productId")` explícito para evitar falha de binding em teste JPA.
- JaCoCo expôs branches não cobertos em metadata/preview/actions; testes foram ampliados até `mvn clean verify` aprovar 100%.

## Retrofits pendentes

- Criar edge real em `resolve` só deve ser feito em sprint futura se o contrato passar a carregar um `targetNodeId` ou outra referência explícita de destino.
- Sugestões automáticas reais de insights continuam fora do escopo; esta sprint entrega apenas a marcação idempotente de revisão de insight textual.

## Arquivos criados

- `backend/src/main/java/br/com/byop/aegis/content/api/ContentPreviewLookupService.java`
- `backend/src/main/java/br/com/byop/aegis/knowledgegraph/api/GraphNodeContentPreview.java`
- `backend/src/main/java/br/com/byop/aegis/knowledgegraph/api/GraphNodeContentPreviewPort.java`
- `backend/src/main/java/br/com/byop/aegis/knowledgegraph/contract/ResolveGraphOrphanRequest.java`
- `backend/src/main/java/br/com/byop/aegis/knowledgegraph/contract/ResolveGraphOrphansRequest.java`
- `backend/src/main/java/br/com/byop/aegis/knowledgegraph/contract/ReviewGraphInsightRequest.java`
- `backend/src/main/java/br/com/byop/aegis/knowledgegraph/contract/UpdateGraphNodePositionRequest.java`
- `backend/src/main/java/br/com/byop/aegis/knowledgegraph/domain/GraphInsightReview.java`
- `backend/src/main/java/br/com/byop/aegis/knowledgegraph/dto/GraphInsightReviewSummary.java`
- `backend/src/main/java/br/com/byop/aegis/knowledgegraph/dto/GraphNodePreview.java`
- `backend/src/main/java/br/com/byop/aegis/knowledgegraph/dto/GraphNodeProp.java`
- `backend/src/main/java/br/com/byop/aegis/knowledgegraph/exception/InvalidGraphOrphanActionException.java`
- `backend/src/main/java/br/com/byop/aegis/knowledgegraph/repository/GraphInsightReviewRepository.java`
- `backend/src/main/resources/db/migration/V10__knowledge_graph_layout_orphans.sql`
- `backend/src/test/java/br/com/byop/aegis/content/api/ContentPreviewLookupServiceTest.java`
- `backend/src/test/java/br/com/byop/aegis/knowledgegraph/repository/GraphInsightReviewRepositoryTest.java`
- `bruno/18-knowledge-graph/*`
- `docs/sprints/backend/results/sprint-18.md`

## Arquivos alterados

- `AGENTS.md`
- `backend/src/main/java/br/com/byop/aegis/content/repository/ContentRepository.java`
- `backend/src/main/java/br/com/byop/aegis/content/service/ContentService.java`
- `backend/src/main/java/br/com/byop/aegis/knowledgegraph/controller/KnowledgeGraphController.java`
- `backend/src/main/java/br/com/byop/aegis/knowledgegraph/controller/KnowledgeGraphExceptionHandler.java`
- `backend/src/main/java/br/com/byop/aegis/knowledgegraph/domain/GraphNode.java`
- `backend/src/main/java/br/com/byop/aegis/knowledgegraph/dto/GraphNodeDetail.java`
- `backend/src/main/java/br/com/byop/aegis/knowledgegraph/dto/GraphNodeSummary.java`
- `backend/src/main/java/br/com/byop/aegis/knowledgegraph/mapper/GraphNodeMapper.java`
- `backend/src/main/java/br/com/byop/aegis/knowledgegraph/repository/GraphNodeRepository.java`
- `backend/src/main/java/br/com/byop/aegis/knowledgegraph/service/KnowledgeGraphService.java`
- `backend/src/test/java/br/com/byop/aegis/content/service/ContentServiceTest.java`
- `backend/src/test/java/br/com/byop/aegis/knowledgegraph/controller/KnowledgeGraphControllerTest.java`
- `backend/src/test/java/br/com/byop/aegis/knowledgegraph/mapper/GraphNodeMapperTest.java`
- `backend/src/test/java/br/com/byop/aegis/knowledgegraph/repository/GraphNodeRepositoryTest.java`
- `backend/src/test/java/br/com/byop/aegis/knowledgegraph/service/KnowledgeGraphServiceTest.java`
- `docs/api-testing/README.md`
- `docs/sprints/backend/SPRINT-RESULTADO.md`
- `docs/trace/00_endpoints_esperados.md`

## Arquivos removidos

Nenhum.
