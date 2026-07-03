# Sprint 26 — Templates de produto: esqueleto de páginas no `POST /products`

Finalizada em 2026-07-03 na branch `sprint/26-templates-de-produto`.

## Objetivo da sprint

`POST /api/v1/products` passa a pré-habilitar os módulos recomendados por `type` e, para os tipos com esqueleto de páginas definido (`Site Institucional`, `Portal`, `Portfolio`), criar automaticamente páginas/seções vazias. `Custom` continua nascendo 100% em branco. `PUT /tenants/{tenantId}` passa a notificar os usuários do tenant quando o status transiciona entre ativo e suspenso, reaproveitando o domínio `notification` (Sprint 25).

## Escopo implementado

- Catálogo fixo de módulos recomendados por tipo de produto (`product.domain.ProductModuleTemplateCatalog`).
- Catálogo fixo de conteúdo default por `BlockType` (`pages.domain.BlockDefaults`).
- Catálogo fixo de esqueleto de páginas/seções por tipo de produto (`pages.domain.PageTemplateCatalog`).
- Scaffold de páginas/seções disparado por evento, dentro da mesma transação do produto (`pages.service.ProductPageScaffoldService`).
- Habilitação automática dos módulos recomendados em `ProductService.createProduct`, na ordem que resolve dependência (`CONTENT` antes de `KNOWLEDGE_GRAPH`).
- Notificação interna `BELL_ONLY` de suspensão (`WARNING`) e reativação (`GENERAL`) de tenant, disparada por evento a partir de `TenantService.updateTenant`.
- Pasta Bruno cumulativa `26-product-templates`.

## Classes criadas

- `br.com.byop.aegis.product.domain.ProductModuleTemplateCatalog`
- `br.com.byop.aegis.pages.domain.BlockDefaults`
- `br.com.byop.aegis.pages.domain.PageTemplateCatalog`
- `br.com.byop.aegis.pages.domain.PageSkeletonTemplate`
- `br.com.byop.aegis.pages.domain.SectionSkeletonTemplate`
- `br.com.byop.aegis.pages.service.ProductPageScaffoldService`
- `br.com.byop.aegis.tenant.api.TenantLifecycleTransition`
- `br.com.byop.aegis.tenant.api.TenantStatusChangedEvent`

## Classes alteradas

- `br.com.byop.aegis.product.api.ProductCreatedEvent` — novos campos `productType` (nome do enum `ProductTypeKey`) e `defaultLocale`.
- `br.com.byop.aegis.product.service.ProductService` — injeta `ProductModuleService`, habilita módulos recomendados após criar o produto, publica o evento estendido.
- `br.com.byop.aegis.tenant.service.TenantService` — publica `TenantStatusChangedEvent` só nas transições `ACTIVE↔SUSPENDED`.
- `br.com.byop.aegis.notification.service.NotificationService` — novo método `notifyTenantStatusChange` + listener `onTenantStatusChanged`, ambos delegando a um helper privado não-transacional (evita self-invocation, java:S6809).
- `br.com.byop.aegis.pages.service.SectionContentValidationService` — `validateFormReference` e `validateDownload` passam a aceitar o estado "ainda não configurado" (ver Decisões técnicas).

## Arquivos criados

- `backend/src/main/java/br/com/byop/aegis/product/domain/ProductModuleTemplateCatalog.java`
- `backend/src/main/java/br/com/byop/aegis/pages/domain/BlockDefaults.java`
- `backend/src/main/java/br/com/byop/aegis/pages/domain/PageTemplateCatalog.java`
- `backend/src/main/java/br/com/byop/aegis/pages/domain/PageSkeletonTemplate.java`
- `backend/src/main/java/br/com/byop/aegis/pages/domain/SectionSkeletonTemplate.java`
- `backend/src/main/java/br/com/byop/aegis/pages/service/ProductPageScaffoldService.java`
- `backend/src/main/java/br/com/byop/aegis/tenant/api/TenantLifecycleTransition.java`
- `backend/src/main/java/br/com/byop/aegis/tenant/api/TenantStatusChangedEvent.java`
- `backend/src/test/java/br/com/byop/aegis/product/domain/ProductModuleTemplateCatalogTest.java`
- `backend/src/test/java/br/com/byop/aegis/pages/domain/BlockDefaultsTest.java`
- `backend/src/test/java/br/com/byop/aegis/pages/domain/PageTemplateCatalogTest.java`
- `backend/src/test/java/br/com/byop/aegis/pages/service/ProductPageScaffoldServiceTest.java`
- `backend/src/test/java/br/com/byop/aegis/ProductTemplateScaffoldIntegrationTest.java`
- `backend/src/test/java/br/com/byop/aegis/TenantStatusNotificationIntegrationTest.java`
- `bruno/26-product-templates/**` (9 requests)
- `docs/sprints/backend/results/sprint-26.md`

## Arquivos alterados

- `backend/src/main/java/br/com/byop/aegis/product/api/ProductCreatedEvent.java`
- `backend/src/main/java/br/com/byop/aegis/product/service/ProductService.java`
- `backend/src/main/java/br/com/byop/aegis/tenant/service/TenantService.java`
- `backend/src/main/java/br/com/byop/aegis/notification/service/NotificationService.java`
- `backend/src/main/java/br/com/byop/aegis/pages/service/SectionContentValidationService.java`
- `backend/src/test/java/br/com/byop/aegis/product/service/ProductServiceTest.java`
- `backend/src/test/java/br/com/byop/aegis/tenant/service/TenantServiceTest.java`
- `backend/src/test/java/br/com/byop/aegis/notification/service/NotificationServiceTest.java`
- `backend/src/test/java/br/com/byop/aegis/pages/service/SectionContentValidationServiceTest.java`
- `backend/src/test/java/br/com/byop/aegis/asset/service/AssetStorageProvisioningServiceTest.java`
- `docs/sprints/backend/SPRINT-RESULTADO.md`

## Entidades reutilizadas

Nenhuma entidade nova. A sprint reaproveita integralmente:

- `product.domain.Product`, `ProductModule`, `ProductTypeKey` (etapa 06/07).
- `pages.domain.Page`, `PageSection`, `BlockType` (etapa 21/23).
- `tenant.domain.Tenant`, `TenantStatus` (etapa 09).
- `notification.domain.Notification`, `UserNotificationStatus`, `NotificationType`, `NotificationPresentationMode` (Sprint 25).

## Endpoints confirmados

Nenhum endpoint novo — o scaffold e a notificação são efeitos colaterais de `POST /api/v1/products` e `PUT /api/v1/tenants/{tenantId}`, ambos já existentes (etapas 06/07 e 09).

## Contratos envolvidos

Nenhum contrato REST novo (`Request`/`Response`). `ProductCreatedEvent` (contrato interno entre módulos, `product.api`) ganhou dois campos (`productType`, `defaultLocale`); `TenantStatusChangedEvent` (novo, `tenant.api`) e `TenantLifecycleTransition` (novo enum, `tenant.api`) são contratos internos de evento, nunca expostos via HTTP.

## Integrações entre módulos (`product`, `pages`, `tenant`, `notification`)

- `product → pages`: via `ProductCreatedEvent` (publicado por `ProductService`, escutado por `ProductPageScaffoldService`). `pages` não importa nenhum tipo de `product.domain`; o tipo de produto chega como `String` (`productType`).
- `product` (interno): `ProductService` chama `ProductModuleService.enableModule` diretamente — mesmo módulo, sem evento.
- `tenant → notification`: via `TenantStatusChangedEvent` (publicado por `TenantService`, escutado por `NotificationService`). `notification` já dependia de `tenant.api` (`TenantUserAccessService`) — o evento evita a dependência inversa.
- Nenhuma dependência cíclica introduzida; `ModulithArchitectureTest` confirma.

## Comportamento por tipo de produto

| `type` | Módulos habilitados | Páginas criadas |
|---|---|---|
| `Site Institucional` | `PAGES, CONTENT, ASSETS, FORMS, SEO, ANALYTICS` | 7 (`home`, `quem-somos`, `historia`, `agenda`, `galeria`, `apoie`, `contato`) |
| `Portal` | `PAGES, CONTENT, FORMS, SEO, ANALYTICS` | 1 (`home`, com 2× `card-list` rotulado "Vagas"/"Blog") |
| `Portfolio` | `PORTFOLIO, PAGES, CONTENT, ASSETS, SEO, ANALYTICS` | 1 (`home`, com `card-list`/`feature-grid`/`timeline`/`download` rotulados) |
| `Knowledge Base` | `CONTENT, KNOWLEDGE_GRAPH, SEO, ANALYTICS` | nenhuma |
| `Library/Books/Music` | `LIBRARY, BOOKS, MUSIC, CONTENT, KNOWLEDGE_GRAPH, SEO, ANALYTICS` | nenhuma |
| `Produto SaaS` | `CONTENT, ASSETS, FORMS, ANALYTICS, SEO` | nenhuma |
| `Custom` | nenhum | nenhuma |

### Comportamento específico de `Custom`

`Custom` não aciona nenhuma etapa do scaffold: `ProductModuleTemplateCatalog.recommendedModulesFor(CUSTOM)` retorna lista vazia (nenhuma chamada a `ProductModuleService.enableModule`) e `PageTemplateCatalog.skeletonFor("CUSTOM")` retorna lista vazia (o listener de `pages` não cria nenhuma `Page`/`PageSection`). O produto nasce exatamente como o `POST /products` já criava antes da Sprint 26. Confirmado por `ProductServiceTest.shouldNotEnableAnyModuleForCustomProduct`, `ProductPageScaffoldServiceTest.shouldNotCreateAnyPageForCustomOrTypesWithoutSkeleton` e `ProductTemplateScaffoldIntegrationTest.shouldCreateEmptyProductForCustomType`.

### Notificação de tenant suspenso/reativado

`TenantService.updateTenant` compara o status antes/depois da atualização. `ACTIVE→SUSPENDED` publica `TenantStatusChangedEvent(tenantId, SUSPENDED, actorSubject)`; `SUSPENDED→ACTIVE` publica `TenantStatusChangedEvent(tenantId, REACTIVATED, actorSubject)`; qualquer outra transição (incluindo para `ARCHIVED`) ou ausência de mudança não publica nada. `NotificationService.onTenantStatusChanged` cria uma `Notification` (`WARNING`/`GENERAL`, sempre `BELL_ONLY`, `createdBySubject = actorSubject`) e faz fan-out para `tenantUserAccessService.listActiveUserSubjects(tenantId)` — mesma mecânica de persistência de `NotificationService.create`, sem o gate de `SUPER_ADMIN` daquele método (ver Decisão técnica 1 e nota abaixo).

## Regras de negócio implementadas

- `POST /products` com `type` recomendando módulos: habilita cada módulo na ordem do catálogo (dependência resolvida antes de precisar dela).
- `POST /products` com `Site Institucional`, `Portal` ou `Portfolio`: cria o esqueleto de páginas/seções completo, status `draft`, conteúdo default vazio/mínimo.
- `POST /products` com `Custom`: não habilita módulo nenhum, não cria página nenhuma — comportamento idêntico ao anterior à Sprint 26.
- Toda a operação (produto + módulos + páginas + seções) é uma única transação — falha em qualquer ponto não deixa nada persistido.
- `PUT /tenants/{tenantId}`: `ativo→suspenso` cria notificação `WARNING`/`BELL_ONLY`; `suspenso→ativo` cria `GENERAL`/`BELL_ONLY`; qualquer outra transição (incluindo `arquivado`) ou ausência de mudança não cria notificação nenhuma.

## Decisões técnicas tomadas

1. **Duas fronteiras de módulo evitadas com eventos, não chamada direta.** `pages` já depende de `product.api`; `notification` já depende de `tenant.api`. Uma chamada direta no sentido contrário (product→pages, tenant→notification) criaria um ciclo rejeitado por `ApplicationModules.verify()`. Resolvido replicando o padrão já usado pelo módulo `asset` (Sprint 12): `product` publica `ProductCreatedEvent` (estendido com `productType`/`defaultLocale`) e `tenant` publica o novo `TenantStatusChangedEvent`; `pages` e `notification` escutam.
2. **`@TransactionalEventListener(phase = BEFORE_COMMIT)`, não `AFTER_COMMIT`.** O padrão do módulo `asset` usa `AFTER_COMMIT` porque é só provisionamento de pasta em disco. Aqui a exigência explícita da etapa é atomicidade real com o banco (produto + módulos + páginas + seções em uma única transação) — `AFTER_COMMIT` já teria commitado o produto antes do listener rodar. `BEFORE_COMMIT` continua na mesma transação física; uma exceção no listener força rollback do produto inteiro. Testado explicitamente em `ProductPageScaffoldServiceTest.shouldPropagateFailureWithoutPersistingAnySection` e nos testes de integração.
3. **`Workflow` (módulo recomendado de `Produto SaaS` na ADR-0017) não existe no catálogo fechado `ModuleKey`.** Não foi inventado um valor de enum novo fora do escopo desta etapa — `Produto SaaS` habilita só os 5 módulos existentes (`CONTENT, ASSETS, FORMS, ANALYTICS, SEO`). `WORKFLOW` fica como retrofit pendente, mesmo tratamento já dado a `ECOMMERCE` no catálogo do frontend.
4. **`validateFormReference`/`validateDownload` (Sprint 23) relaxados para aceitar o estado "ainda não configurado".** `contact`/`form` exigiam sempre um `formId` referenciando um Form real; `download` exigia sempre pelo menos 1 item. Nenhum dos dois tem forma de ser "vazio, sem inventar referência real" sob a validação original — necessário para o esqueleto da página "contato" (Site Institucional) e do bloco "Downloads" (Portfolio). `formId` ausente/em branco e `items` vazio agora são aceitos; `formId` presente mas malformado ou apontando para um Form inexistente continua rejeitado exatamente como antes. Testes antigos que assumiam rejeição do estado ausente foram atualizados para refletir o novo contrato.
5. **`event-list` usa `{title, source: {}}`, não a forma antiga do mock do frontend.** `frontend/src/domains/pages/blockDefaults.ts` ainda usa `selectedEventIds`, que não corresponde ao contrato validado pelo backend (`source` obrigatório, Sprint 23). Usado o mínimo que passa na validação atual, sem tocar em `SectionContentValidationService.validateEventList`.
6. **`gallery`/`feature-grid`/`card-list`/`hero` não precisaram de relaxamento algum.** Um item-placeholder estrutural sem referência real (ex.: `gallery: {items: [{src: "", alt: "Imagem"}]}`, `card-list`/`feature-grid: {items: [{}]}`) já passa na validação existente, mesmo espírito do `DEFAULT_BLOCK_CONTENT` do frontend.
7. **`label` do texto da etapa (`card-list "Vagas"`) mapeado para `PageSection.variant`.** O contrato REST não tem campo `label`; `variant` já existe exatamente para diferenciar duas instâncias do mesmo `BlockType` na mesma página.
8. **Self-invocation de método `@Transactional` evitada em `NotificationService`** (`onTenantStatusChanged` chamando `notifyTenantStatusChange` do mesmo bean, java:S6809) — extraído `doNotifyTenantStatusChange` privado não-transacional, mesmo padrão já usado em `ContentService.doTransition` (Sprint 11).
9. **Literais repetidos extraídos para constantes** (`java:S1192`) em `BlockDefaults` (`KEY_TITLE`, `KEY_BODY`, `KEY_ITEMS`, `KEY_SOURCE`) e `PageTemplateCatalog` (`SLUG_HOME`, `TITLE_HOME`).

## Apontamentos Sonar corrigidos (rodada SonarQube for IDE)

Rodada dedicada corrigiu os 4 apontamentos reais reportados pelo SonarQube for IDE sem `@SuppressWarnings`, `NOSONAR` ou desativação de regra:

- `java:S125` em `BlockDefaults.java`: bloco de comentário explicando por que `AUDIO`/`VIDEO`/`VIDEO_GALLERY`/`SOCIAL_LINKS` ficam fora do escopo do esqueleto foi reescrito em prosa contínua — a forma anterior (linhas curtas com nomes de tipo separados por `/` e um `;` de pontuação ao final de uma delas) disparava o heurístico de "código comentado" do Sonar.
- `java:S5838` em `BlockDefaultsTest.java`: `assertThat(map.get("src")).isEqualTo("")` trocado por `assertThat((String) map.get("src")).isEmpty()`.
- `java:S5778` em `ProductServiceTest.java`: a lambda de `assertThatThrownBy` em `shouldPropagateFailureFromModuleEnablingWithoutPublishingEvent` continha três invocações (`user(...)`, `createCommand(...)`, `createProduct(...)`) — `caller`/`command` extraídos para variáveis locais antes da asserção, deixando só a chamada sob teste dentro da lambda.
- `java:S5853` em `ProductTemplateScaffoldIntegrationTest.java`: duas asserções separadas sobre o mesmo sujeito (`pages`/`homeSections`) encadeadas em uma única cadeia AssertJ (`hasSize(...).extracting(...)`/`hasSize(...).allSatisfy(...)`).

## Bugs encontrados

Nenhum bug de plataforma real encontrado nesta sprint (nenhum comportamento inesperado do Spring/Hibernate/Jackson como os registrados em sprints anteriores — ex. Sprint 11/12). As descobertas desta etapa foram conflitos entre o texto literal do artefato da sprint e regras já existentes de outras etapas (validação da Sprint 23, module-gating da etapa 06/ADR-0015) — todas registradas como Decisões técnicas acima, não como bug.

## Retrofits pendentes para etapas futuras

- Adicionar `WORKFLOW` ao catálogo fechado `ModuleKey` (e recomendá-lo para `Produto SaaS`) quando esse módulo for de fato implementado.
- `frontend/src/domains/pages/blockDefaults.ts` está desatualizado em relação ao contrato validado do backend para `event-list` (`selectedEventIds` vs `source`) — vale uma sprint de frontend dedicada a alinhar os dois.

## Testes executados

Classes de teste novas: `ProductModuleTemplateCatalogTest`, `BlockDefaultsTest`, `PageTemplateCatalogTest`, `ProductPageScaffoldServiceTest`, `ProductTemplateScaffoldIntegrationTest` (`@SpringBootTest`, Postgres real), `TenantStatusNotificationIntegrationTest` (`@SpringBootTest`, Postgres real).

Classes de teste alteradas: `ProductServiceTest` (módulos recomendados por tipo, ordem de dependência, `Custom`, falha transacional), `TenantServiceTest` (transições de status e publicação/ausência de evento), `NotificationServiceTest` (`notifyTenantStatusChange`, listener), `SectionContentValidationServiceTest` (novo comportamento de `formId`/`items` ausentes), `AssetStorageProvisioningServiceTest` (assinatura estendida de `ProductCreatedEvent`).

## Validação Maven

`mvn clean verify` (execução final, backend/): **BUILD SUCCESS**, 1337 testes, 0 falhas, 0 erros, 0 ignorados.

## Validação JaCoCo

`jacoco:check` (bundle, regra `LINE`/`BRANCH` mínimo 1.00, excludes já existentes `*Dto`/`*Command`/`*Request`/`*Response`/`*Detail`/`*Config`): `All coverage checks have been met`, 353 classes analisadas.

## Validação Spring Modulith

`ModulithArchitectureTest` (`ApplicationModules.of(AegisApplication.class).verify()`): 1 teste, 0 falhas, 0 erros. Nenhuma dependência cíclica introduzida pelos dois novos eventos (ver Decisão técnica 1 e Integrações entre módulos).

## Validações com Bruno

Pasta criada: `bruno/26-product-templates` (9 requests): criar tenant próprio da pasta, criar produto Site Institucional + validar 7 páginas, criar produto Custom + validar 403 `MODULE_DISABLED` ao listar páginas (módulo `PAGES` nunca habilitado para `Custom` — prova mais forte que um 200/`[]`), suspender tenant + validar notificação `WARNING`/`BELL_ONLY`, reativar tenant + validar notificação `GENERAL`/`BELL_ONLY`.

Resultado da pasta 26 isoladamente: 9 requests, 9 aprovados, 100% dos testes aprovados.

Execução da collection cumulativa inteira (`npx @usebruno/cli run --env local`, todas as pastas 00-26): 245 requests executados. As pastas 23 (`dominio-pages-secoes-e-blocos`) e 24 (`openapi-testes-checklist-final`) apresentaram falhas em cascata (409 de slug duplicado, originado por dados residuais de execuções anteriores contra o Postgres local de longa duração) — confirmado como pré-existente e não relacionado a esta sprint (mesma falha já presente antes de qualquer alteração da Etapa 26; reset do volume de dados local não foi executado por ser ação destrutiva fora do escopo desta sprint). Todas as demais pastas, incluindo a nova pasta 26, passaram 100%.

## Riscos identificados

- `event-list`/`contact`/`download` no scaffold usam conteúdo estruturalmente diferente do mock do frontend (ver Decisão técnica 5) — o editor de páginas no frontend precisa ser revisado para não confundir isso com um bug quando exibir esses defaults.
- A relaxação de `validateFormReference`/`validateDownload` (Decisão técnica 4) altera o contrato de validação já usado por qualquer chamador de `POST/PUT .../sections`, não só pelo scaffold — clientes existentes que dependiam do erro em caso de `formId`/`items` ausentes deixam de recebê-lo.

## Critérios de aceite atendidos

- [x] Criar produto `"Site Institucional"` gera as 7 páginas do esqueleto, cada seção com o `BlockType` certo e conteúdo vazio/mínimo.
- [x] `gallery`/`download`/`contact` nascem sempre com listas/referências vazias — nunca um asset/form inventado.
- [x] Criar produto `"Knowledge Base"`/`"Library/Books/Music"`/`"Produto SaaS"` habilita os módulos recomendados existentes, mas não cria nenhuma página.
- [x] Criar produto `"Custom"` não habilita nenhum módulo nem cria nenhuma página.
- [x] Habilitar módulo com dependência (`KNOWLEDGE_GRAPH`) durante o scaffold nunca é rejeitado — ordem resolve a dependência primeiro.
- [x] Falha em qualquer parte do scaffold não deixa o produto criado parcialmente.
- [x] Suspender um tenant cria notificação `BELL_ONLY`/`WARNING`; reativar cria `BELL_ONLY`/`GENERAL`.
- [x] `mvn clean verify` com `BUILD SUCCESS`, JaCoCo e Spring Modulith aprovados.
- [x] Bruno cumulativo com a nova pasta 26 passando 100% (falhas pré-existentes em 23/24 documentadas como fora de escopo).
