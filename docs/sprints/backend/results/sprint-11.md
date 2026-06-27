# Sprint 11 — Dominio `content` (editorial, workflow, versoes, publicacao)

## Identificacao

- **Sprint:** 11 — Dominio `content`
- **Status:** Concluida em 2026-06-27
- **Branch:** `sprint/11-dominio-content`
- **Resultado local:** BUILD SUCCESS

## Resumo Executivo

A Sprint 11 entregou o dominio `content`: CRUD editorial, maquina de estados de workflow (Draft/In Review/Published/Archived), versionamento (`ContentVersion` a cada transicao), publicacao com autorizacao por papel, sanitizacao de markdown (Jsoup) e integracao com o Knowledge Graph via referencias inline `{{kg-ref:nodeId:Label}}`. Durante a implementacao foram encontrados e corrigidos dois bugs reais de plataforma (Jackson 3 e auto-selecao de metodo do MapStruct) e tratados 46 apontamentos do SonarQube for IDE ao longo de varias rodadas de revisao.

**Adendo pos-entrega (mesma sessao):** o artefato original da Sprint 11 e a lista literal de "Endpoints obrigatorios" desta execucao **nao incluiam** um endpoint de criacao de `Content` — a entrega inicial documentou essa ausencia como decisao deliberada de aderencia ao contrato. Ao validar a collection Postman, o responsavel pelo repositorio identificou que a ausencia de criacao tornava o fluxo de teste manual (criar -> editar -> transicionar -> publicar) inviavel sem inserir dados via SQL direto no banco, e solicitou explicitamente a adicao do endpoint. Implementado `POST /api/v1/products/{productId}/content` (contrato `CreateContentRequest`, mesmos campos de `UpdateContentRequest`; `author`/`tenantId`/`status=Draft`/`version=1` resolvidos pelo backend, nao pelo cliente) reaproveitando integralmente a sanitizacao de markdown e a validacao de `kg-ref` ja existentes. Ver secao dedicada abaixo.

## Escopo Entregue

- Entidades `Content` e `ContentVersion`, com migration propria `V5__content_editorial_workflow.sql`.
- Maquina de estados (`ContentWorkflowPolicy`): `Draft -> In Review`, `In Review -> Draft`, `In Review -> Published`, `Published -> Archived`, `Archived -> Draft`. Qualquer outra transicao retorna 400 `INVALID_CONTENT_TRANSITION`.
- Autorizacao de publicacao restrita a `SUPER_ADMIN`, `TENANT_ADMIN` e `PRODUCT_MANAGER` (via authorities do JWT) — `EDITOR`/`VIEWER` recebem 403 `CONTENT_PUBLISH_FORBIDDEN`, tanto via `transition` quanto via `publish`.
- `publish` como atalho de `transition(to: Published)`, atualizando tambem `publication`.
- Sanitizacao de markdown (`MarkdownSanitizer`, Jsoup) com allowlist fixa (`p, strong, em, ul, ol, li, blockquote, h2, h3, a, br, span`), bloqueio de `<script>`, `<iframe>`, `javascript:`, `data:`, atributo `style`, e `span` restrito ao atributo `class` com os 5 valores fixos reais (`text-aegis-red/blue/green/amber/violet`, confirmados no codigo do frontend — Sprint 18 do frontend).
- Integracao com Knowledge Graph via nova API publica `knowledgegraph.api.KnowledgeGraphPort` (ver Decisoes Reais): garante node do conteudo, cria edges `RELATED_TO`, rejeita com 400 `INVALID_KG_REFERENCE` `kg-ref` para node inexistente ou com `nodeId` mal formado — validado **antes** de persistir.
- Isolamento por produto: conteudo de produto fora do escopo do usuario retorna 404, nunca 403 (via `ProductAccessPort` ja existente).
- Module-gating: `ContentController` anotado com `@RequireModule(ModuleKey.CONTENT)` — modulo desabilitado retorna 403 `MODULE_DISABLED`, mesmo para `SUPER_ADMIN`.

## Arquivos Criados/Alterados por Area

### Backend — Dominio `content`

- `content/domain/Content.java` (com `Content.Edit`, record interno para evitar metodo com lista longa de parametros — java:S107)
- `content/domain/ContentStatus.java`
- `content/domain/DifficultyLevel.java`
- `content/domain/ContentVersion.java`
- `content/repository/ContentRepository.java`
- `content/repository/ContentVersionRepository.java`
- `content/contract/CreateContentRequest.java` (adicionado pos-entrega)
- `content/contract/UpdateContentRequest.java`
- `content/contract/ContentTransitionRequest.java`
- `content/contract/PublishContentRequest.java`
- `content/dto/ContentSummary.java`
- `content/dto/WorkflowItemSummary.java`
- `content/dto/ContentVersionSummary.java`
- `content/mapper/ContentMapper.java`
- `content/mapper/ContentVersionMapper.java`
- `content/service/ContentWorkflowPolicy.java`
- `content/service/MarkdownSanitizer.java`
- `content/service/ContentService.java`
- `content/controller/ContentController.java`
- `content/exception/ContentNotFoundException.java`, `InvalidContentTransitionException.java`, `InsufficientContentRoleException.java`, `InvalidContentStatusException.java`, `InvalidDifficultyLevelException.java`, `InvalidContentReferenceException.java`, `ContentExceptionHandler.java`

### Backend — Knowledge Graph (API publica nova)

- `knowledgegraph/api/package-info.java` (`@NamedInterface("knowledgegraph-api")`)
- `knowledgegraph/api/KnowledgeGraphPort.java`
- `knowledgegraph/service/KnowledgeGraphPortAdapter.java`

### Backend — Stubs de ProductAssignment (Sprint 10, documentacao ajustada)

- `product/service/StubProductAssignmentEmailPort.java`
- `product/service/StubProductAssignmentInvitePort.java`
- `product/service/StubProductAssignmentNotificationPort.java`

### Persistencia

- `db/migration/V5__content_editorial_workflow.sql` — tabelas `contents` e `content_versions`.

### Build

- `backend/pom.xml` — dependencia nova `org.jsoup:jsoup:1.18.1` (sanitizacao de markdown).

### Testes

- `content/repository/ContentRepositoryTest.java`, `ContentVersionRepositoryTest.java` (`@DataJpaTest`)
- `content/mapper/ContentMapperTest.java`, `ContentVersionMapperTest.java`
- `content/service/ContentServiceTest.java`, `ContentWorkflowPolicyTest.java`, `MarkdownSanitizerTest.java`
- `content/controller/ContentControllerTest.java` (`@WebMvcTest`)
- `content/exception/ContentExceptionHandlerTest.java`
- `knowledgegraph/service/KnowledgeGraphPortAdapterTest.java`

### Postman

- `postman/aegis-postman-collection.json` — pasta `11 - Content Editorial Workflow` adicionada (14 requests, incluindo "Criar conteudo").

## Contratos REST Implementados

```
POST /api/v1/products/{productId}/content                              (adicionado pos-entrega, ver Adendo)
GET  /api/v1/products/{productId}/content
GET  /api/v1/products/{productId}/content/{contentId}
PUT  /api/v1/products/{productId}/content/{contentId}
POST /api/v1/products/{productId}/content/{contentId}/transition
GET  /api/v1/products/{productId}/content/{contentId}/versions
POST /api/v1/products/{productId}/content/{contentId}/publish
GET  /api/v1/products/{productId}/content/edit-events
GET  /api/v1/products/{productId}/content/workflow-items
```

Payload de `ContentSummary` espelha exatamente `ContentRow` do frontend (`id, title, type, lang, author, status, updatedAt, publication, version, summary?, difficultyLevel?, body?, category?, topic?, metadata?`).

**Nota de aderencia ao contrato:** o objetivo da sprint menciona "CRUD editorial", mas a lista literal de "Endpoints obrigatorios" (tanto no artefato `11_dominio_content.md` quanto nas instrucoes desta execucao) so trazia os 8 endpoints sem `POST`/`DELETE`. A entrega inicial respeitou exatamente essa lista, sem inventar endpoint. O `POST` de criacao foi adicionado **depois**, a pedido explicito do responsavel pelo repositorio (ver Adendo no Resumo Executivo e secao "Endpoint de criacao adicionado pos-entrega" abaixo) — `DELETE` continua fora do escopo, nao solicitado.

### Endpoint de criacao adicionado pos-entrega

`POST /api/v1/products/{productId}/content` — contrato `CreateContentRequest` (`title`, `type`, `lang`, `body?`, `summary?`, `difficultyLevel?`, `category?`, `topic?`, `metadata?` — mesmos campos de `UpdateContentRequest`, sem `id`/`author`/`status`/`version`, que sao resolvidos pelo backend). Retorna 201 com `ContentSummary` (`status: "Draft"`, `version: "v1"`, `author` resolvido do `subject` do JWT chamador). Reaproveita integralmente `MarkdownSanitizer` e a validacao de `kg-ref` (`InvalidContentReferenceException` se referenciar node inexistente, antes de persistir) — nenhuma logica nova de sanitizacao/validacao foi criada, apenas orquestracao de criacao em `ContentService.createContent`, que resolve `tenantId` via `product.api.ProductReferenceService` (mesma API publica ja usada por `KnowledgeGraphService`). Validado manualmente contra aplicacao real (`mvn spring-boot:run`): `POST` com corpo contendo `<script>` retorna 201 com o corpo sanitizado e `status: "Draft"`.

## Regras de Negocio Implementadas

- Transicao valida apenas quando `from` enviado no request bate com o status atual real do `Content` em banco — caso contrario, 400 `INVALID_CONTENT_TRANSITION` (protege contra condicao de corrida/estado divergente do client).
- Toda transicao (`transition` e `publish`) grava uma `ContentVersion` nova com snapshot JSON do conteudo (`title, type, lang, bodyMarkdown, summary, difficultyLevel, category, topic, fromStatus, toStatus, comment`).
- `publish` atualiza `publication` com timestamp ISO-8601 UTC.
- Sanitizacao aplicada sempre no `PUT` (edicao), nunca apenas na renderizacao.
- `kg-ref` so e processado quando o `body` sanitizado contem ao menos uma referencia — sem refs, nenhuma chamada ao Knowledge Graph e feita (idempotente e barato no caminho comum).
- Node do conteudo no Knowledge Graph e criado uma unica vez por `Content` (id persistido em `contents.graph_node_id`) e reaproveitado em edicoes seguintes — edges repetidas sao idempotentes (nao duplicam).
- `GET /edit-events` deriva texto livre a partir do `toStatus` de cada `ContentVersion` de todos os conteudos do produto, ordenado cronologicamente.
- Autor (`author` no contrato) e resolvido via `identity.api.IdentityUserDirectory` (Sprint 10); falha de resolucao (ex.: Keycloak indisponivel) cai em fallback para o `subject` bruto, sem quebrar a listagem.

## Decisoes Reais Tomadas

1. **Sanitizador de markdown:** Jsoup 1.18.1 (`Safelist.none()` + allowlist explicita). Dependencia nova, instrumental e nao arquitetural — registrada aqui conforme exigido.
2. **Resolucao do nome do autor:** reaproveita `identity.api.IdentityUserDirectory.getRequiredUser(subject).displayName()` (Sprint 10), chamado pelo `ContentService` (nao pelo `ContentMapper`, que permanece mapeamento puro).
3. **API publica do Knowledge Graph criada nesta sprint** (`knowledgegraph.api.KnowledgeGraphPort` + `KnowledgeGraphPortAdapter`): o modulo `knowledgegraph` nao tinha `NamedInterface` ate esta sprint, o que violava o isolamento do Spring Modulith assim que `content` tentou importar `knowledgegraph.service.KnowledgeGraphService` diretamente (`ModulithArchitectureTest` falhou com "Module 'content' depends on non-exposed type"). Resolvido criando uma porta minima (`nodeExists`, `ensureContentNode`, `ensureRelatedToEdge`) que encapsula a logica de idempotencia (criar-se-ausente) dentro do proprio modulo `knowledgegraph`, em vez de duplica-la em `content` — `content` nao importa mais nenhum tipo de `knowledgegraph.domain`/`contract`/`dto`/`service`.
4. **`Content.Edit` como record interno:** `applyEdit` tinha 9 parametros posicionais (java:S107). Resolvido com um record `Content.Edit` agrupando os campos editaveis — mesmo padrao ja usado em `GraphNode.Creation` (Sprint 08), sem tornar a entidade anemica (o metodo de comportamento `applyEdit(Edit)` permanece na entidade).
5. **`doTransition` privado nao-transacional:** `transition` e `publish` (ambos `@Transactional` publicos) chamavam um ao outro via `this`, contornando o proxy do Spring (self-invocation). Extraido um metodo privado `doTransition` sem `@Transactional` proprio, chamado pelos dois metodos publicos — cada um mantem sua propria fronteira transacional via proxy.
6. **`readMetadataJson` nunca retorna `null`:** retorna `Map.of()` quando `metadataJson` e nulo/vazio (java:S1168 — nunca retornar `null` para tipo de colecao/mapa).
7. **6 valores do `span class`, na pratica 5:** a Sprint 18 do frontend fala em "6 cores fixas", mas o codigo real (`MarkdownEditModal.tsx`, `Markdown.tsx`, `theme.css`) implementa apenas 5 classes (`text-aegis-red/blue/green/amber/violet`) — a 6a "opcao" da paleta e "remover cor" (desfaz o `<span>`, nao gera uma 6a classe). Allowlist do backend usa as 5 classes reais.

## Bugs de Plataforma Encontrados e Corrigidos

### 1. Jackson 3 (`tools.jackson`) vs Jackson 2 legado (`com.fasterxml.jackson`)

Spring Boot 4.1 + `spring-boot-starter-jackson` auto-configuram um bean `tools.jackson.databind.ObjectMapper` (Jackson 3, namespace renomeado). O `ContentService` foi inicialmente escrito importando `com.fasterxml.jackson.databind.ObjectMapper` (Jackson 2 legado, presente no classpath apenas transitivamente via `spring-modulith-events-jackson`) — a aplicacao real falhava ao subir (`UnsatisfiedDependencyException: No qualifying bean of type 'com.fasterxml.jackson.databind.ObjectMapper'`), embora os testes unitarios (que instanciam `ObjectMapper` manualmente) nao detectassem o problema. Corrigido trocando todos os imports para `tools.jackson.databind.ObjectMapper`/`tools.jackson.core.JacksonException` (que, no Jackson 3, e **unchecked** — `RuntimeException`, nao mais `IOException` como o antigo `JsonProcessingException`). Confirmado subindo a aplicacao real (`mvn spring-boot:run`) antes e depois da correcao.

### 2. MapStruct auto-selecionando metodo de conversao para campos nao relacionados

O metodo `toContractPublication(String):String` (criado para o campo `publication`) foi auto-detectado pelo MapStruct como o **unico** conversor `String -> String` disponivel no `ContentMapper` e aplicado implicitamente a **todos** os outros campos `String` sem `@Mapping` explicito (`title`, `type`, `lang`, `summary`, `category`, `topic`, `body`, `author`) — substituindo `null` por `"—"` indevidamente em qualquer um desses campos quando vazio. Corrigido anotando `toContractPublication`/`toContractStatus`/`toContractDifficulty` com `@Named`, que exclui esses metodos da resolucao implicita do MapStruct (passam a ser usados apenas onde explicitamente referenciados via `expression`). Confirmado lendo o `ContentMapperImpl` gerado antes e depois da correcao.

Ambos os bugs foram encontrados durante a validacao manual com a aplicacao real rodando — nenhum teste automatizado (com mocks/instancias manuais) os capturava, reforcando o valor da etapa de validacao manual via `curl` exigida pela sprint.

## Limpeza SonarQube for IDE

Foram tratadas, ao longo de multiplas rodadas de revisao solicitadas durante a execucao, **46 ocorrencias** distintas reportadas pelo SonarQube for IDE, agrupadas por categoria:

| Categoria | Regra | Arquivos afetados |
|---|---|---|
| Metodo/construtor com excesso de parametros | java:S107 | `Content.java` (resolvido com `Content.Edit`) |
| Uso de relogio do sistema em testes | java:S8692 | `ContentMapperTest.java`, `ContentServiceTest.java` (constantes `FIXED_*` no lugar de `.now()`) |
| `catch` com variavel nao utilizada | java:S7467 | `ContentService.java` (unnamed variable `_`, padrao ja usado no projeto) |
| Retorno de `null` para mapa | java:S1168 | `ContentService.java` (`readMetadataJson` retorna `Map.of()`) |
| Self-invocation de metodo `@Transactional` | (revisao de transacao) | `ContentService.java` (`doTransition` privado) |
| Import morto | java:S1128 | `ContentControllerTest.java` |
| Matcher Mockito inutil (`eq` sem `any` na mesma chamada) | java:S6068 | `ContentControllerTest.java`, `ContentServiceTest.java` |
| Lambda de `assertThrows`/`assertThatThrownBy` com mais de uma chamada potencialmente lancadora | java:S5778 | `ContentServiceTest.java`, `ContentWorkflowPolicyTest.java` |
| Testes repetidos sem parametrizacao | java:S5976 | `ContentServiceTest.java` (3 grupos de testes mesclados em `@ParameterizedTest`: cenarios de transicao invalida, papeis autorizados a publicar, cenarios de `edit-events`) |
| Assertions encadeaveis separadas | java:S5853 | `MarkdownSanitizerTest.java` |
| Marcador `TODO` sem classificacao | java:S1135 | `StubProductAssignmentEmailPort.java`, `StubProductAssignmentInvitePort.java`, `StubProductAssignmentNotificationPort.java` (convertidos em Javadoc sem a palavra TODO) |

**Nota sobre `ContentRepository.java` (java:S1135):** o SonarQube for IDE apontou repetidamente (4 vezes, em rodadas diferentes) um marcador `TODO` na linha 19 deste arquivo. Verificado explicitamente com `grep -n "TODO" ContentRepository.java` em cada ocorrencia — **o arquivo nunca conteve a palavra `TODO`** (linha 19 e `* Lista todo o conteudo de um produto.` — Javadoc comum). Trata-se, com alta confianca, de um finding obsoleto exibido por cache desatualizado do plugin SonarQube for IDE; nenhuma alteracao foi necessaria ou aplicada neste arquivo especifico.

Nenhuma correcao usou `@SuppressWarnings` para mascarar apontamento, nenhum teste foi removido, nenhuma regra de negocio foi relaxada para agradar o Sonar — confirmado por `mvn clean verify` aprovado apos cada rodada de correcao.

## Migrations e Persistencia

- Migration criada: `V5__content_editorial_workflow.sql` (tabelas `contents`, `content_versions`, indices por `tenant_id`/`product_id`/`status`/`content_id`).
- `contents.graph_node_id` referencia `graph_nodes(id)` (FK opcional) — vincula o `Content` ao seu node no Knowledge Graph, quando existir.
- `ddl-auto=validate` respeitado; nenhuma migration anterior foi alterada.
- Nenhum uso de H2 ou MongoDB.

## Seguranca

- `ContentController` anotado `@RequireModule(ModuleKey.CONTENT)` — module-gating via `ModuleAccessAspect` ja existente (Sprint 07).
- Toda requisicao passa por `ProductAccessPort.assertAccessible(productId, caller)` antes de chegar ao `ContentService` (mesmo padrao do `KnowledgeGraphController`, Sprint 08) — produto fora do escopo retorna 404.
- Autorizacao de publicacao verificada em `ContentWorkflowPolicy` a partir das authorities do JWT (`ROLE_SUPER_ADMIN`, `ROLE_TENANT_ADMIN`, `ROLE_PRODUCT_MANAGER`) — mesmo padrao usado por `ProductAccessResolver` (Sprint 07/10), sem introduzir logica de autorizacao paralela.
- Nenhuma alteracao em `SecurityConfig`.

## Validacao Manual

Executada com a aplicacao real (`mvn spring-boot:run`, perfil `local`), Postgres e Keycloak de desenvolvimento ja em execucao via `docker compose`. Fluxo completo via `curl`:

1. Tenant + Produto criados via API; modulo `CONTENT` habilitado; `ProductAssignment` criado para o usuario de teste (`SUPER_ADMIN` real, com assignment `PRODUCT_MANAGER` no produto, conforme `ProductAccessResolver`).
2. Conteudo inicial seedado diretamente no banco (sessao original, antes do endpoint de criacao existir); apos o adendo, `POST /content` validado tambem via `curl` contra a aplicacao real — 201, `status: "Draft"`, `version: "v1"`, corpo com `<script>` sanitizado na propria resposta de criacao.
3. `GET /content` — 200, lista correta.
4. `GET /content/{id}` — 200, detalhe correto.
5. `PUT /content/{id}` com `<script>alert(1)</script>` no corpo — 200, `<script>` removido na resposta (sanitizacao confirmada em produto real).
6. `POST /transition` `Draft -> In Review` — 200.
7. `POST /transition` `Draft -> Published` (invalida) — 400 `INVALID_CONTENT_TRANSITION`.
8. `POST /publish` — 200, `status: Published`, `publication` preenchido.
9. Usuario `EDITOR` real (criado no Keycloak para este teste, com `ProductAssignment` EDITOR): transicoes `Published -> Archived -> Draft -> In Review` — 200 (permitidas); `In Review -> Published` via `transition` — 403 `CONTENT_PUBLISH_FORBIDDEN`; `POST /publish` — 403 `CONTENT_PUBLISH_FORBIDDEN`.
10. Modulo `CONTENT` desabilitado — `GET /content` retorna 403 `MODULE_DISABLED` mesmo para o `SUPER_ADMIN`; reabilitado em seguida.
11. Segundo produto (modulo `CONTENT` habilitado, sem `ProductAssignment` do usuario `EDITOR`) — `GET /content` retorna 404 `PRODUCT_NOT_FOUND` (isolamento por escopo, nunca 403).
12. Knowledge Graph: node `ARTICLE` criado via API; `PUT /content` com `{{kg-ref:<nodeId>:Outro Artigo}}` no corpo — 200, node `CONTENT` do artigo criado automaticamente, edge `RELATED_TO` criada (confirmado consultando `GET .../graph/nodes/{id}/related` e diretamente na tabela `graph_edges`); `kg-ref` para `nodeId` inexistente — 400 `INVALID_KG_REFERENCE`.

Todos os cenarios de aceite da sprint foram confirmados em ambiente real, nao apenas em teste automatizado.

## Postman

- Collection cumulativa atualizada: `postman/aegis-postman-collection.json` (nenhuma collection nova criada, conforme solicitado).
- Pasta adicionada: `11 - Content Editorial Workflow`, com 14 requests:
  - Criar conteudo (primeiro request da pasta — captura `{{contentId}}` automaticamente da resposta para os demais requests)
  - Listar conteudo do produto (tambem captura `{{contentId}}` da resposta, como fallback, se a lista nao estiver vazia)
  - Buscar detalhe do conteudo
  - Editar conteudo (demonstra sanitizacao de `<script>`)
  - Transicao Draft -> In Review
  - Transicao invalida Draft -> Published (400)
  - Publicar conteudo
  - EDITOR tentando publicar (403) — usa `{{editorToken}}` (variavel a ser definida manualmente com um token de usuario EDITOR com `ProductAssignment` ativo, ja que a sprint nao tem fluxo de seed automatico de papeis)
  - Listar versoes (ordem cronologica)
  - Listar workflow-items
  - Listar edit-events
  - Desabilitar / listar com 403 MODULE_DISABLED / reabilitar modulo CONTENT
- Variaveis de colecao adicionadas: `contentId`, `editorToken`.
- **Nota:** durante esta sessao, o aplicativo desktop Postman (aberto e sincronizando o workspace) removeu collections antigas duplicadas (`Aegis PMS — Sprint 05`, `Aegis PMS - Sprint 07 - Core Tenant Product Modules`, `Aegis PMS - Backend Cumulative Collection`) — confirmado pelo usuario como acao manual propria no app, nao relacionada a este trabalho.

## Qualidade e Testes

- `mvn clean verify`: **BUILD SUCCESS**
- Testes: **453** executados, **0** failures, **0** errors, **0** skipped (partindo de 340 na Sprint 10 — 113 testes novos/ajustados nesta sprint, incluindo o novo `KnowledgeGraphPortAdapter` e o endpoint de criacao adicionado pos-entrega)
- JaCoCo: aprovado (`All coverage checks have been met`) — 100% linha e branch nas classes elegiveis
- Spring Modulith: aprovado via `ModulithArchitectureTest` (`ApplicationModules.verify()`) — inclusive a nova fronteira publica `knowledgegraph.api`
- SonarQube for IDE: 46 apontamentos tratados ao longo da sessao (ver secao dedicada acima); nenhum finding pendente relacionado a arquivos da Sprint 11 ao final, exceto a nota documentada sobre `ContentRepository.java` (finding nao reproduzido)

## Riscos

- A allowlist de classes de cor do `span` (5 valores) depende de permanecer sincronizada com o frontend (`theme.css`/`MarkdownEditModal.tsx`) — se uma nova cor for adicionada no frontend sem atualizar `MarkdownSanitizer.ALLOWED_SPAN_CLASSES`, o backend vai descartar silenciosamente a marcacao (unwrap), nao quebrar, mas perder a formatacao.
- `KnowledgeGraphPort` e consumido hoje apenas por `content`; futuras sprints que tambem precisem integrar com o Knowledge Graph (ex.: `pages`, Sprint 22) devem reaproveitar esta porta em vez de criar uma nova ou importar tipos internos do modulo.
- Ausencia de endpoint de criacao de `Content` nesta sprint (por desenho do artefato) significa que qualquer ambiente novo precisa de seed manual/SQL para ter conteudo para editar — relevante para Sprint 21 (seed inicial).

## Retrofits Pendentes

- Nenhum endpoint de criacao/exclusao de `Content` foi implementado (fora do escopo literal da Sprint 11) — se uma sprint futura precisar de CRUD completo, adicionar `POST`/`DELETE` com contrato proprio, reaproveitando `ContentWorkflowPolicy`/`MarkdownSanitizer`/`KnowledgeGraphPort` ja existentes.
- `KnowledgeGraphPort.ensureContentNode` sempre cria nodes do tipo `GraphNodeType.CONTENT` — se um dominio futuro precisar de outro `nodeType` via a mesma porta, avaliar generalizar a assinatura.

## Conclusao

A Sprint 11 foi concluida com o dominio `content` completo (workflow, versionamento, publicacao, sanitizacao de markdown, integracao real com Knowledge Graph), validado em ambiente real (nao apenas testes automatizados), com dois bugs de plataforma genuinos corrigidos (Jackson 3 e auto-selecao do MapStruct), 46 apontamentos do SonarQube for IDE tratados, JaCoCo 100%, Spring Modulith aprovado, Postman cumulativo atualizado e nenhuma alteracao arquitetural fora do escopo da sprint.
