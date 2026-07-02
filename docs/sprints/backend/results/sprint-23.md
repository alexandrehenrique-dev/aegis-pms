# Sprint 23 — Dominio Pages, Secoes e Blocos

Concluida em 2026-07-02 na branch `sprint/23-dominio-pages-secoes-e-blocos`.

## Objetivo

Implementar o dominio `pages` completo: `Page`, `PageSection`, `ProductGlobals` e `Event`, com catalogo fechado de 20 `BlockType`, validacao de conteudo por tipo centralizada em `SectionContentValidationService` (a classe mais critica da sprint), mecanismo generico de `acceptsChildren` (sem `if`/`switch` hardcoded por tipo), sanitizacao de markdown reaproveitando a allowlist da Sprint 11, e as entidades `ProductGlobals` (navbar/footer/redes sociais, ADR-0013) e `Event` (agenda propria, nunca um "content type" generico).

## Resultado alcancado

Modulo `br.com.byop.aegis.pages` criado do zero, seguindo a estrutura de pacotes padrao (`domain`, `repository`, `contract`, `dto`, `mapper`, `service`, `controller`, `exception`). Migration `V12__pages_sections_globals_events.sql` cria as 4 tabelas (`pages`, `page_sections`, `product_globals`, `events`) com FK `ON DELETE CASCADE` de `page_sections` para `pages` (exclusao de pagina remove secoes em cascata) e unique constraints (`product_id, slug` em `pages`; `product_id` em `product_globals`).

`SectionContentValidationService` implementa a tabela de validacao da Secao C via um registry `Map<BlockType, BiConsumer<UUID, Map<String,Object>>>` construido em `buildRules()` — cada `BlockType` com regra especifica aponta para um metodo privado dedicado; tipos sem regra especifica caem no `getOrDefault` (no-op) seguido sempre da validacao generica de `acceptsChildren` via `BlockTypeCatalog`. O mecanismo de sub-blocos (`two-column` → `left`/`right`) e inteiramente orientado por dados (`BlockTypeDefinition.childSlots()`/`acceptsChildren()`), incluindo a regra de recursao (filho do mesmo tipo do pai sempre rejeitado, mesmo se listado em `acceptsChildren`) — nenhum `if (type == BlockType.TWO_COLUMN)` no codigo.

## Classes criadas

### Dominio (`pages.domain`)

- `Page`, `PageSection`, `ProductGlobals`, `Event` — entidades JPA.
- `PageStatus`, `BlockType`, `EventAccessType`, `EventVisibility` — enums de catalogo fechado (`fromContractValue`/`contractValue`).
- `BlockTypeDefinition`, `BlockTypeCatalog` — tabela estatica do mecanismo `acceptsChildren` (hoje so `two-column` preenchido: `left`/`right`, aceita `text`/`rich-text`/`image`/`cta-section`).

### Repositorios (`pages.repository`) — Javadoc completo na interface e em todo metodo

- `PageRepository` (`findAllByProductId`, `findByProductIdAndId`, `findByProductIdAndSlug`).
- `PageSectionRepository` (`findAllByPageIdOrderByOrderAsc`, `findByPageIdAndId`, `existsByFormIdReference` — consulta JSONB nativa `content_json ->> 'formId'`, exposta para o dominio `form` decidir exclusao de `FormDefinition`).
- `ProductGlobalsRepository` (`findByProductId`).
- `EventRepository` (`findAllByProductIdOrderByDatetimeAsc`, `findByProductIdAndId`).

### Contratos e DTOs (`pages.contract`, `pages.dto`)

- Requests: `CreatePageRequest`, `UpdatePageRequest`, `PageSeoRequest`, `CreateSectionRequest`, `UpdateSectionRequest`, `ReorderSectionsRequest`, `UpdateProductGlobalsRequest`, `NavbarRequest`, `FooterRequest`, `NavLinkRequest`, `SocialLinkRequest`, `FloatingWhatsappRequest`, `CreateEventRequest`, `UpdateEventRequest`.
- Responses: `PageSummary`, `PageDetail`, `PageSeoResponse`, `PageSectionResponse`, `ProductGlobalsResponse`, `NavbarResponse`, `FooterResponse`, `NavLinkResponse`, `SocialLinkResponse`, `FloatingWhatsappResponse`, `EventSummary`, `EventDetail`.

### Mappers MapStruct (`pages.mapper`)

- `PageMapper`, `PageSectionMapper`, `ProductGlobalsMapper`, `EventMapper`.

### Servicos (`pages.service`)

- `SectionContentValidationService` — classe mais critica da sprint; `validateSectionContent(productId, type, content)` (validacao) e `sanitizeContent(content)` (sanitizacao recursiva de markdown, aplicada ao salvar).
- `PageMarkdownSanitizer` — mesma allowlist/regra da Sprint 11 (`content.service.MarkdownSanitizer`), duplicada porque a classe original nao e exportada via `content.api` e o Spring Modulith proibe import direto entre pacotes internos de modulos diferentes. Nome diferente da original (`PageMarkdownSanitizer`, nao `MarkdownSanitizer`) porque duas classes homonimas em pacotes diferentes colidem no component-scan do Spring (`ConflictingBeanDefinitionException` — bug real encontrado na validacao manual, ver Secao "Bugs encontrados").
- `PageService`, `ProductGlobalsService`, `EventService` — regras de negocio, auditoria via `AuditService.recordEvent` em toda criacao/edicao/exclusao.

### Controllers e excecoes (`pages.controller`, `pages.exception`)

- `PageController` (`@RequireModule(ModuleKey.PAGES)`), `EventController` (mesmo `@RequireModule(ModuleKey.PAGES)`, nenhum modulo novo criado), `ProductGlobalsController` (sem `@RequireModule` — estrutura basica do produto, nunca modulo opcional).
- `PageExceptionHandler` — mapeia todas as excecoes do dominio para `CoreErrorResponse`, incluindo `InvalidSectionContentException` (codigo dinamico via `ex.getErrorCode()`).
- Excecoes: `PageNotFoundException`, `PageSectionNotFoundException`, `EventNotFoundException`, `DuplicatePageSlugException`, `InvalidSectionReorderException`, `UnknownBlockTypeException`, `InvalidPageStatusException`, `InvalidEventAccessTypeException`, `InvalidEventVisibilityException`, `InvalidSectionContentException`.

## Arquivos alterados (fora do modulo `pages`)

- `backend/src/main/java/br/com/byop/aegis/asset/api/AssetReference.java` — campo `category` adicionado (aditivo, API interna entre modulos, nao e contrato REST) para permitir a `SectionContentValidationService` validar a categoria do asset nos blocos `audio`/`video`/`download` sem acessar `asset.domain.AssetCategory` (que quebraria o Spring Modulith).
- `backend/src/main/java/br/com/byop/aegis/asset/api/AssetReferenceService.java` — popula o novo campo `category` a partir de `Asset.getCategory().contractValue()`.
- `backend/src/test/java/br/com/byop/aegis/asset/api/AssetReferenceServiceTest.java`, `backend/src/test/java/br/com/byop/aegis/submission/service/SubmissionServiceTest.java` — ajustados para o novo campo do record `AssetReference`.
- `docs/api-testing/README.md` — pasta `23-dominio-pages-secoes-e-blocos` documentada.
- `AGENTS.md` — Secao 4 (novos padroes: `S5838` `containsEntry`, `S5778` estendido a `Map.of()`/`List.of()` inline em lambdas, tecnica para `S1168` que preserva payload/persistencia) e Secao 6 (observacao datada da rodada Sonar desta sprint).

## Endpoints confirmados

```txt
GET    /api/v1/products/{productId}/pages
POST   /api/v1/products/{productId}/pages
GET    /api/v1/products/{productId}/pages/{pageId}
PUT    /api/v1/products/{productId}/pages/{pageId}
DELETE /api/v1/products/{productId}/pages/{pageId}
POST   /api/v1/products/{productId}/pages/{pageId}/sections
PUT    /api/v1/products/{productId}/pages/{pageId}/sections/{sectionId}
DELETE /api/v1/products/{productId}/pages/{pageId}/sections/{sectionId}
PUT    /api/v1/products/{productId}/pages/{pageId}/sections/reorder

GET    /api/v1/products/{productId}/globals
PUT    /api/v1/products/{productId}/globals

GET    /api/v1/products/{productId}/events
POST   /api/v1/products/{productId}/events
GET    /api/v1/products/{productId}/events/{eventId}
PUT    /api/v1/products/{productId}/events/{eventId}
DELETE /api/v1/products/{productId}/events/{eventId}
```

Exatamente os endpoints do artefato, sem divergencia.

## Decisoes de implementacao registradas

1. **`Event` no mesmo modulo `pages`, nao em modulo top-level proprio.** A unica dependencia cruzada e o bloco `event-list` referenciando `Event` — mesmo modulo, sem cerimonia extra de `NamedInterface`/Spring Modulith. `EventController` reusa o mesmo `@RequireModule(ModuleKey.PAGES)` do `PageController`, conforme o artefato pede explicitamente ("nenhuma nova permissao").
2. **Catalogo `acceptsChildren` de `two-column`: `cta-section`, nao `cta`.** O artefato lista `["text", "rich-text", "image", "cta"]`, mas `"cta"` nunca existiu no catalogo fechado de `BlockType` (Secao B lista `cta-section`) — usar `"cta"` literal tornaria essa entrada da lista permanentemente inalcancavel (qualquer secao com `type: "cta"` ja seria rejeitada por `UNKNOWN_BLOCK_TYPE` antes mesmo de chegar na checagem de `acceptsChildren`). Corrigido para `cta-section`, unica leitura que torna a regra satisfazivel.
3. **`datetime` de `Event` modelado como `LocalDateTime`** (nao `OffsetDateTime`), pois o artefato exige um "instante unico ISO 8601 com data e hora" no formato exemplificado (`2026-07-12T16:00`, sem timezone) — corresponde ao valor que um input HTML `datetime-local` produz.
4. **Auditoria:** `pages`/`sections`/`globals`/`events` chamam `AuditService.recordEvent` em toda criacao/edicao/exclusao, seguindo o padrao ja usado por `content`/`asset` (nao o de `form`, que nunca chamou auditoria — inconsistencia preexistente registrada, nao corrigida nesta sprint por estar fora de escopo). Acoes registradas: `PAGE_CREATED/UPDATED/DELETED`, `PAGE_SECTION_CREATED/UPDATED/DELETED`, `PAGE_SECTIONS_REORDERED`, `PRODUCT_GLOBALS_CREATED/UPDATED`, `EVENT_CREATED/UPDATED/DELETED`.
5. **`ProductGlobals` ganhou `createdAt`** (nao listado no artefato, que so pede `updatedAt`) para cumprir a Constituicao (Artigo X — todo dado deve responder "quando foi criado?"). Campo interno, nunca exposto em `ProductGlobalsResponse` — nao altera o contrato REST.
6. **`AssetReference.category` adicionado como string de contrato** (`AssetCategory.contractValue()`), nao o enum `AssetCategory` em si — evitaria expor um tipo do pacote interno `asset.domain` para outro modulo, violando o Spring Modulith.
7. **`PageSectionRepository.existsByFormIdReference`** usa uma query nativa JSONB (`content_json ->> 'formId'`) em vez de uma coluna dedicada — evita alterar o schema alem do que o artefato pede, e resolve a Secao E ("pages so expoe a consulta, nunca bloqueia a exclusao por conta propria").
8. **`slug` incluido em `UpdatePageRequest`** (nao explicito no artefato para o payload de update, mas a Secao E discute explicitamente "mudar o slug de uma pagina publicada" como cenario real) — validado contra colisao com outra pagina do mesmo produto, permitindo manter o mesmo slug sem falso-positivo de duplicidade.
9. **Bug real encontrado na validacao manual (Bruno):** duas classes homonimas `MarkdownSanitizer` (uma em `content.service`, outra recem-criada em `pages.service`) quebraram o boot da aplicacao real com `ConflictingBeanDefinitionException` — o component-scan do Spring Boot gera o nome do bean a partir do simple name da classe, ignorando o pacote. Não detectado pelos testes `@WebMvcTest`/Mockito (que nunca sobem o contexto Spring completo). Corrigido renomeando a copia do dominio `pages` para `PageMarkdownSanitizer`.
10. **Bug de composicao da collection Bruno encontrado na validacao manual:** a pasta `22-seed-inicial-e-grafo` termina autenticada como o usuario demo `super-admin@byop.io` (sem `ProductAssignment` no `{{productId}}` avulso criado em `02-products`), fazendo toda a pasta `23` falhar com 403 `PRODUCT_CONTENT_ACCESS_DENIED`. Corrigido adicionando `relogar-como-loki` como primeiro request da pasta 23, restaurando `{{token}}` para o usuario com acesso real ao produto — sem alterar nenhuma pasta anterior.

## Rodada de correcao SonarQube for IDE (pos-implementacao)

Apos a implementacao inicial, uma rodada dedicada corrigiu 5 apontamentos reais em 5 arquivos (`PageService`, `PageServiceTest`, `ProductGlobalsServiceTest`, `SectionContentValidationService`, `SectionContentValidationServiceTest`), todos via reescrita, nenhum via supressao:

- **`java:S1168`** (nunca retornar `null` de metodo que devolve `Map`) em `SectionContentValidationService.sanitizeContent` e `PageService.readMap` — corrigido sem alterar o payload/persistencia: a decisao de nulidade (`settings` ausente deve continuar gravando `NULL` na coluna, nao a string `"{}"`) foi movida para uma expressao ternaria no *call site*, fora de qualquer declaracao de metodo — os dois metodos agora sempre devolvem uma colecao nao nula.
- **`java:S1135`** (comentario `TODO` pendente) — apontamento real (nao falso positivo desta vez): a palavra "Todo" abria a primeira frase do Javadoc de `sanitizeContent`. Javadoc reescrito para nao comecar a frase com essa palavra.
- **`java:S5778`** (lambda de `assertThatThrownBy` com mais de uma chamada que pode lancar excecao) — 19 ocorrencias corrigidas nos 3 arquivos de teste, a maioria com `Map.of(...)`/`List.of(...)` construidos inline como argumento da chamada sob teste (o Sonar conta essas construcoes como uma segunda invocacao); todas extraidas para variavel local antes do `assertThatThrownBy`.
- **`java:S5838`** (usar `assertThat(map).containsEntry(key, value)` em vez de `assertThat(map.get(key)).isEqualTo(value)`) — 1 ocorrencia real corrigida em `SectionContentValidationServiceTest`.
- **`java:S5976`** (testes duplicados devem virar `@ParameterizedTest`) — os 3 testes de `href` invalida do bloco `social-links` (`shouldRejectSocialLinkWithInvalidHref`/`WithHrefMissingHost`/`WithMalformedHref`, mesma logica variando so a string de entrada) consolidados em um unico `@ParameterizedTest`/`@MethodSource`, com nomes de cenario preservados nos argumentos.

`AGENTS.md` atualizado (Secao 4 e Secao 6) para prevenir recorrencia. Nenhum contrato REST, endpoint, migration ou regra de negocio alterado nesta rodada — confirmado por reexecucao completa de `mvn clean verify` e da collection Bruno apos as correcoes.

## Cobertura de testes

- `mvn clean verify`: **BUILD SUCCESS**, **1230 testes**, 0 falhas, 0 erros, 0 ignorados.
- JaCoCo: `All coverage checks have been met` (100% linhas e branches, regra `CLASS`, excluindo `*Request`/`*Response`/`*Summary`/`*Dto`/`*Application`).
- Spring Modulith: `ModulithArchitectureTest` aprovado (fronteiras respeitadas; `pages` nunca acessa `asset.domain`/`form.domain`/entidades internas de outro modulo, so `asset.api`/`form.api`).
- `SectionContentValidationServiceTest`: 68 cenarios — cada um dos 13 `BlockType` com regra especifica cobre pelo menos 1 caminho valido e 1 de rejeicao (varios com multiplos cenarios de rejeicao); mecanismo `acceptsChildren` coberto com 6 cenarios dedicados (filho valido, slot ausente, recursao, tipo fora da lista, tipo desconhecido, slot que nao e objeto); sanitizacao recursiva coberta em profundidade (corpo/descricao em qualquer nivel de aninhamento).
- Reforço de cobertura pos-Sonar: testes adicionais para os dois ramos da expressao ternaria de `settings` (presente/ausente) em `createSection`/`updateSection`, cobrindo tambem o ramo `writeJson(null)`.
- Testes de repositorio (`@DataJpaTest`, 21 cenarios): unicidade de slug por produto, unicidade de `ProductGlobals` por produto, cascata de exclusao (`pages` → `page_sections` via FK), consulta JSONB `existsByFormIdReference`.
- Testes de mapper (16 cenarios) e de controller (`@WebMvcTest`, 37 cenarios): modulo `PAGES` confirmado via reflexao (`RequireModule.value()`), `ProductGlobalsController` confirmado sem anotacao `@RequireModule`.

## Bruno

Pasta `23-dominio-pages-secoes-e-blocos` criada com 39 arquivos `.bru` (38 requests + `folder.bru`), cobrindo: os 4 `curl` exatos da Secao "Validacao" do artefato, os 20 `BlockType` (pelo menos 1 caso valido para os com regra especifica, mais casos de rejeicao para hero/image/two-column/contact/audio/video/video-gallery), CRUD completo de `Page`/`Event`, reordenar, editar, excluir (cascata), `ProductGlobals` (objeto vazio antes de configurar, upsert, acessivel com `PAGES` desabilitado), isolamento por produto (404) e module-gating (403 `MODULE_DISABLED` em `PageController` e `EventController`).

Validado via `cd bruno && npx @usebruno/cli run --env local`: **218 requests executados, 218 aprovados, 427/427 testes aprovados**, confirmado em multiplas execucoes consecutivas (idempotente) antes e depois da rodada de correcao Sonar, sem regressao em nenhuma pasta anterior (00 a 22).

## Retrofits pendentes para etapas futuras

- `form` (etapa 13) ainda nao chama `AuditService` — inconsistencia preexistente, nao corrigida aqui por estar fora do escopo literal desta sprint.
- Nenhuma integracao com Knowledge Graph foi criada para `Page`/`Event` nesta sprint (o artefato nao pede) — `GraphNodeType.PAGE`/`GraphNodeType.EVENT` ja existem no catalogo (Sprint 08) caso uma sprint futura decida conectar paginas/eventos ao grafo.
- Redirecionamento basico ao trocar o `slug` de uma pagina publicada e explicitamente fora de escopo desta sprint (o artefato so pede impedir colisao, ja implementado).

## Comandos executados (implementacao + rodada Sonar + finalizacao)

```bash
# Implementacao, por rodada (Entities/Repository -> Mapper -> Service/Validation -> Controller)
mvn -o compile
mvn -o test-compile
mvn -o -Dtest='PageRepositoryTest,PageSectionRepositoryTest,ProductGlobalsRepositoryTest,EventRepositoryTest' test
mvn -o -Dtest='PageMapperTest,PageSectionMapperTest,ProductGlobalsMapperTest,EventMapperTest' test
mvn -o -Dtest='SectionContentValidationServiceTest' test
mvn -o -Dtest='PageServiceTest,ProductGlobalsServiceTest,EventServiceTest' test
mvn -o -Dtest='PageControllerTest,ProductGlobalsControllerTest,EventControllerTest' test
mvn -o test jacoco:report -Dtest='br.com.byop.aegis.pages.**'

# Validacao manual (Bruno) contra a aplicacao real
docker compose up -d aegis-postgres keycloak-postgres keycloak mailhog
mvn -o spring-boot:run -Dspring-boot.run.profiles=local
cd bruno && npx @usebruno/cli run --env local

# Rodada de correcao SonarQube for IDE
mvn -o -Dtest='PageServiceTest,ProductGlobalsServiceTest,SectionContentValidationServiceTest' test
mvn -o test jacoco:report -Dtest='br.com.byop.aegis.pages.**'
mvn -o clean verify
mvn -o -Dtest=ModulithArchitectureTest test
cd bruno && npx @usebruno/cli run --env local   # reexecutado apos a rodada Sonar

# Finalizacao
cd backend && mvn -o clean verify
cd ../bruno && npx @usebruno/cli run --env local
git status --short
git diff --check
git diff --stat
```

## Resultado `mvn clean verify` (execucao final)

```
BUILD SUCCESS
Tests run: 1230, Failures: 0, Errors: 0, Skipped: 0
jacoco:check -> All coverage checks have been met.
```

## Resultado Bruno (execucao final)

```
Status: PASS
Requests: 218 (218 Passed)
Tests: 427/427
```

## Resultado JaCoCo

`All coverage checks have been met` — 100% de linhas e branches na regra `CLASS` (excluindo `*Request`/`*Response`/`*Summary`/`*Dto`/`*Application`, conforme padrao do projeto). Nenhuma exclusao nova adicionada ao `pom.xml`.

## Resultado Spring Modulith

`ModulithArchitectureTest` (`ApplicationModules.of(AegisApplication.class).verify()`) aprovado — o modulo `pages` consome somente `asset.api`/`form.api`/`product.api`/`audit.api`/`security`, nunca pacotes internos (`domain`/`repository`/`service`) de outro modulo.

## Criterios de aceite atendidos

- [x] Criar pagina, adicionar secoes, reordenar e editar funcionam.
- [x] Secao com `type` fora do catalogo e rejeitada com 400 (`UNKNOWN_BLOCK_TYPE`) — `footer`/`navbar` nao sao mais aceitos como `type` de secao.
- [x] `hero` sem `title` e rejeitado; imagem sem `alt` e rejeitada em qualquer bloco que tenha imagem.
- [x] `two-column` com um `two-column` aninhado dentro e rejeitado (regra generica via `acceptsChildren`, nao hardcoded).
- [x] `contact` exige `formId` de um formulario existente do mesmo produto; payload com `fields[]` solto e rejeitado.
- [x] `audio` com `source: "upload"` exige `fileAssetId` de um asset de audio existente; com `source: "spotify-track"` exige `spotifyUrl` valida.
- [x] Reordenar secoes persiste a nova ordem corretamente.
- [x] Excluir pagina remove as secoes em cascata (FK `ON DELETE CASCADE`, migration V12).
- [x] `GET /products/{productId}/globals` nunca retorna 404 (objeto vazio se ainda nao configurado); `PUT` faz upsert.
- [x] Pagina/secao de produto fora do escopo do usuario retorna 404 (nao 403).
- [x] Produto com modulo `PAGES` desabilitado retorna 403 `MODULE_DISABLED` (nao afeta `ProductGlobalsController`).
- [x] `Event` e entidade propria (`EventController`), nunca resolvido como "content type"; CRUD completo funciona; `datetime` e um unico valor ISO com data e hora.
- [x] Bloco `event-list` referencia `Event` via filtro (periodo/visibilidade), nunca um `contentType`.
- [x] `video` com `source: "upload"` exige `fileAssetId` de um asset `category: "video"` existente; com `source: "youtube"` exige `youtubeUrl` valida (regex); URL que nao bate e rejeitada com 400.
- [x] `video-gallery` com mais de 50 itens e rejeitado; item sem `title` e rejeitado.
- [x] `mvn clean verify` confirma 100% de cobertura nas classes elegiveis, incluindo `SectionContentValidationService` (JaCoCo).
- [x] `PageRepository`/`PageSectionRepository`/`ProductGlobalsRepository`/`EventRepository` tem Javadoc na interface e em todo metodo.
- [x] Collection Bruno cumulativa validada 100% (218/218 requests, 427/427 testes), sem regressao nas pastas 00–22.
- [x] Rodada de correcao SonarQube for IDE concluida sem `@SuppressWarnings`/`NOSONAR`, sem alteracao de contrato/endpoint/payload/regra de negocio.
