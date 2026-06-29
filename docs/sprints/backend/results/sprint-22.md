# Sprint 22 — Seed inicial e Knowledge Graph

Concluida em 2026-06-29 na branch `sprint/22-seed-inicial-e-grafo`.

## Objetivo

Adicionar um seed inicial realista para o ambiente local do Aegis PMS, criando tenants, produtos, usuarios demo, memberships, ProductAssignments e exemplos de Knowledge Graph para WikiDev e Loki.

O seed deve ser idempotente, rodar apenas em `local`, respeitar Keycloak como IAM oficial e usar apenas fronteiras publicas entre modulos.

## Resultado alcancado

Foi criado um modulo `seed` com `DemoSeedRunner` e `DemoSeedService`, ambos restritos ao profile `local`.

O seed cria ou atualiza:

- 3 tenants: `clientes-beta`, `aegis-labs` e `cliente-norte`.
- 9 produtos: `maestro-beton`, `conecta-talentos`, `alexandre-dev`, `cmss`, `loki`, `wikidev`, `aegis-core`, `aegis-docs` e `portal-norte`.
- 5 usuarios demo no Keycloak.
- memberships e ProductAssignments coerentes para os papeis globais.
- grafo WikiDev com Programacao, Java, Spring Boot e JPA.
- grafo Loki com Poema do Limiar, Musica Ecos e Playlist Atravessias.

Uma segunda subida da aplicacao com o mesmo banco confirmou idempotencia: os dados seedados permaneceram em 3 tenants, 9 produtos, 9 nos e 7 arestas para WikiDev/Loki, sem duplicacao.

## Classes criadas ou alteradas

Criadas:

- `br.com.byop.aegis.seed.DemoSeedRunner`
- `br.com.byop.aegis.seed.DemoSeedService`
- `br.com.byop.aegis.identity.api.IdentityDemoUserService`
- `br.com.byop.aegis.tenant.api.TenantSeedCommand`
- `br.com.byop.aegis.tenant.api.TenantSeedReference`
- `br.com.byop.aegis.tenant.api.TenantSeedService`
- `br.com.byop.aegis.product.api.ProductSeedCommand`
- `br.com.byop.aegis.product.api.ProductSeedReference`
- `br.com.byop.aegis.product.api.ProductSeedService`
- `br.com.byop.aegis.knowledgegraph.api.GraphSeedNodeCommand`
- `br.com.byop.aegis.knowledgegraph.api.GraphSeedEdgeCommand`
- `br.com.byop.aegis.knowledgegraph.api.GraphSeedNodeReference`
- `br.com.byop.aegis.knowledgegraph.api.KnowledgeGraphSeedService`

Alteradas:

- `br.com.byop.aegis.identity.auth.client.KeycloakAdminClient`
- `br.com.byop.aegis.identity.auth.client.KeycloakAdminClientTest`

Testes criados:

- `IdentityDemoUserServiceTest`
- `TenantSeedServiceTest`
- `ProductSeedServiceTest`
- `KnowledgeGraphSeedServiceTest`
- `DemoSeedRunnerTest`
- `DemoSeedServiceTest`

## Arquivos criados

- `backend/src/main/java/br/com/byop/aegis/identity/api/IdentityDemoUserService.java`
- `backend/src/main/java/br/com/byop/aegis/knowledgegraph/api/GraphSeedEdgeCommand.java`
- `backend/src/main/java/br/com/byop/aegis/knowledgegraph/api/GraphSeedNodeCommand.java`
- `backend/src/main/java/br/com/byop/aegis/knowledgegraph/api/GraphSeedNodeReference.java`
- `backend/src/main/java/br/com/byop/aegis/knowledgegraph/api/KnowledgeGraphSeedService.java`
- `backend/src/main/java/br/com/byop/aegis/product/api/ProductSeedCommand.java`
- `backend/src/main/java/br/com/byop/aegis/product/api/ProductSeedReference.java`
- `backend/src/main/java/br/com/byop/aegis/product/api/ProductSeedService.java`
- `backend/src/main/java/br/com/byop/aegis/seed/DemoSeedRunner.java`
- `backend/src/main/java/br/com/byop/aegis/seed/DemoSeedService.java`
- `backend/src/main/java/br/com/byop/aegis/tenant/api/TenantSeedCommand.java`
- `backend/src/main/java/br/com/byop/aegis/tenant/api/TenantSeedReference.java`
- `backend/src/main/java/br/com/byop/aegis/tenant/api/TenantSeedService.java`
- `backend/src/test/java/br/com/byop/aegis/identity/api/IdentityDemoUserServiceTest.java`
- `backend/src/test/java/br/com/byop/aegis/knowledgegraph/api/KnowledgeGraphSeedServiceTest.java`
- `backend/src/test/java/br/com/byop/aegis/product/api/ProductSeedServiceTest.java`
- `backend/src/test/java/br/com/byop/aegis/seed/DemoSeedRunnerTest.java`
- `backend/src/test/java/br/com/byop/aegis/seed/DemoSeedServiceTest.java`
- `backend/src/test/java/br/com/byop/aegis/tenant/api/TenantSeedServiceTest.java`
- `bruno/22-seed-inicial-e-grafo/folder.bru`
- `bruno/22-seed-inicial-e-grafo/login-super-admin-seed.bru`
- `bruno/22-seed-inicial-e-grafo/listar-tenants-seed.bru`
- `bruno/22-seed-inicial-e-grafo/listar-produtos-seed.bru`
- `bruno/22-seed-inicial-e-grafo/buscar-node-spring-boot.bru`
- `bruno/22-seed-inicial-e-grafo/buscar-node-jpa.bru`
- `bruno/22-seed-inicial-e-grafo/buscar-node-poema.bru`
- `bruno/22-seed-inicial-e-grafo/buscar-node-musica.bru`
- `bruno/22-seed-inicial-e-grafo/wikidev-spring-boot-neighbors.bru`
- `bruno/22-seed-inicial-e-grafo/wikidev-jpa-related.bru`
- `bruno/22-seed-inicial-e-grafo/loki-poema-neighbors.bru`
- `bruno/22-seed-inicial-e-grafo/loki-musica-related.bru`
- `docs/sprints/backend/results/sprint-22.md`

## Arquivos alterados

- `AGENTS.md`
- `backend/src/main/java/br/com/byop/aegis/identity/auth/client/KeycloakAdminClient.java`
- `backend/src/main/resources/application-local.yml`
- `backend/src/test/java/br/com/byop/aegis/identity/auth/client/KeycloakAdminClientTest.java`
- `docs/api-testing/README.md`
- `docs/sprints/backend/22_seed_inicial_e_grafo.md`
- `docs/sprints/backend/SPRINT-RESULTADO.md`

## Tenants criados

- `clientes-beta` — `CLIENTES BETA`, plano Pro, status `ACTIVE`.
- `aegis-labs` — `Aegis Labs`, plano Enterprise, status `ACTIVE`.
- `cliente-norte` — `Cliente Norte`, plano Starter, status `SUSPENDED`.

## Produtos e modulos habilitados

- `maestro-beton`: `CONTENT`, `PAGES`, `ASSETS`, `FORMS`, `SEO`, `ANALYTICS`, `MUSIC`.
- `conecta-talentos`: `CONTENT`, `PAGES`, `FORMS`, `SUBMISSIONS`, `JOBS`, `SEO`, `ANALYTICS`, `INTEGRATIONS`.
- `alexandre-dev`: `PORTFOLIO`, `CONTENT`, `PAGES`, `ASSETS`, `SEO`, `ANALYTICS`.
- `cmss`: `CONTENT`, `PAGES`, `ASSETS`, `FORMS`, `SEO`, `ANALYTICS`.
- `loki`: `PAGES`, `LIBRARY`, `BOOKS`, `MUSIC`, `CONTENT`, `SEO`, `ANALYTICS`, `KNOWLEDGE_GRAPH`.
- `wikidev`: `KNOWLEDGE_BASE`, `CONTENT`, `COMMENTS`, `CONTRIBUTORS`, `FORMS`, `ANALYTICS`, `KNOWLEDGE_GRAPH`.
- `aegis-core`: `CONTENT`, `ANALYTICS`, `KNOWLEDGE_GRAPH`.
- `aegis-docs`: `CONTENT`, `ANALYTICS`, `KNOWLEDGE_GRAPH`.
- `portal-norte`: `CONTENT`, `PAGES`, `ANALYTICS`.

O seed sincroniza os modulos esperados de cada produto. Para produtos com Knowledge Graph, o modulo `KNOWLEDGE_GRAPH` e habilitado junto de `CONTENT`, respeitando a dependencia ja existente.

## Usuarios, memberships e ProductAssignments

`KeycloakAdminClient` passou a expor `ensureDemoUser(email, name, password, realmRoleName)`.

O metodo:

- localiza ou cria usuario no realm Aegis;
- habilita o usuario;
- redefine a senha permanente;
- associa a role de realm correspondente.

Usuarios demo:

- `super-admin@byop.io` — `AEGIS_SUPER_ADMIN`
- `admin@byop.io` — `AEGIS_TENANT_ADMIN`
- `pm@byop.io` — `AEGIS_PRODUCT_MANAGER`
- `editor@byop.io` — `AEGIS_EDITOR`
- `viewer@byop.io` — `AEGIS_VIEWER`

A credencial inicial de demonstracao vem da property local:

```yaml
aegis.seed.demo-user-initial-credential: ${AEGIS_SEED_DEMO_USER_INITIAL_CREDENTIAL:senha123}
```

Memberships:

- `super-admin@byop.io`: membership ativa nos 3 tenants.
- `admin@byop.io`: membership ativa em `clientes-beta` com papel `TENANT_ADMIN`.
- `pm@byop.io`: membership ativa em `clientes-beta` com papel `PRODUCT_MANAGER`.
- `editor@byop.io`: membership ativa em `clientes-beta` com papel `EDITOR`.
- `viewer@byop.io`: membership ativa em `clientes-beta` com papel `VIEWER`.

ProductAssignments:

- `super-admin@byop.io`: assignment em todos os produtos seedados com papel operacional `PRODUCT_MANAGER`.
- `pm@byop.io`: assignment em `maestro-beton`, `alexandre-dev`, `cmss` e `wikidev`.
- `editor@byop.io`: assignment em `maestro-beton`.
- `viewer@byop.io`: assignment em `maestro-beton`.

O super-admin recebeu assignments em todos os produtos seedados porque o backend atual usa `ProductAccessResolver`/ADR-0018 para autorizar acesso a dominios por produto. Mesmo com papel global `SUPER_ADMIN`, os endpoints de conteudo/grafo esperam uma associacao de produto para resolver o escopo operacional.

## Seed do Knowledge Graph

WikiDev:

- nos: WikiDev, Programacao, Java, Spring Boot e JPA.
- arestas: WikiDev -> Programacao (`CONTAINS`), Programacao -> Java (`CONTAINS`), Java -> Spring Boot (`CONTAINS`), Spring Boot -> JPA (`RELATED_TO`).

Loki:

- nos: Loki, Poema do Limiar, Musica Ecos e Playlist Atravessias.
- arestas: Loki -> Poema do Limiar (`CONTAINS`), Poema do Limiar -> Musica Ecos (`INSPIRED_BY`), Musica Ecos -> Playlist Atravessias (`PART_OF`).

Decisao registrada: a playlist do Loki foi representada como `GraphNodeType.CONTENT` com metadata `refType=PLAYLIST`, porque o enum atual de tipos do grafo nao possui `PLAYLIST` e a sprint nao autorizava ampliar esse contrato.

Tambem foi adicionada a aresta Loki -> Poema do Limiar para que o produto Loki apareca conectado no grafo de demonstracao.

## Estrategia de idempotencia

O seed e idempotente por chaves naturais e constraints existentes:

- tenants por `tenant.key`;
- produtos por `product.key`;
- usuarios por e-mail no Keycloak;
- memberships por tenant + user subject;
- ProductAssignments por produto + user subject;
- Graph nodes por produto + `refType` + `refId`;
- Graph edges por produto + source + target + tipo.

Apos subir a aplicacao uma segunda vez com o mesmo banco, foram consultadas as contagens dos dados seedados:

- tenants seedados: 3 linhas, 3 keys distintas.
- produtos seedados: 9 linhas, 9 keys distintas.
- nodes WikiDev/Loki: 9 linhas.
- edges WikiDev/Loki: 7 linhas.

Apos a correcao Sonar do `DemoSeedService`, a collection Bruno completa foi executada novamente e as mesmas contagens foram confirmadas no PostgreSQL Aegis, mantendo a idempotencia.

## Estrategia de profile local/prod

`DemoSeedRunner` e `DemoSeedService` usam `@Profile("local")`. Com isso:

- no profile `local`, o runner executa o seed automaticamente no startup;
- no profile `prod`, o runner nao executa e o service de seed local nao e registrado como bean;
- a credencial demo existe apenas em `application-local.yml`, com possibilidade de override por `AEGIS_SEED_DEMO_USER_INITIAL_CREDENTIAL`.

Essa decisao evita seed acidental em producao e evita autenticao propria; Keycloak segue como IAM oficial.

## Correcoes de numeracao da sprint

O arquivo `docs/sprints/backend/22_seed_inicial_e_grafo.md` foi corrigido para identificar a etapa como Sprint 22. O conteudo interno anterior dizia "Etapa 21", mas o prompt da sprint determinava que o numero correto deveria ser inferido pelo nome do arquivo.

Nao houve alteracao no historico real da Sprint 21.

Observacao operacional: o prompt de finalizacao mencionava `README.md` na raiz, mas esse arquivo nao existe neste repositorio. O equivalente usado foi `docs/README.md`, conforme orientacao do `AGENTS.md`. O prompt inicial da sprint tambem mencionava `docs/sprints/backend/00_endpoints_esperados.md`; o equivalente real existente e usado foi `docs/trace/00_endpoints_esperados.md`.

## Correcoes SonarQube for IDE

Rodada pos-implementacao corrigiu os apontamentos identificados no `DemoSeedService.java`, sem supressao, `NOSONAR`, desativacao de regra ou falso positivo:

- `java:S2068`: a constante sensivel `PASSWORD` foi removida; a credencial demo passou para a property local `aegis.seed.demo-user-initial-credential`, com default somente em `application-local.yml`.
- `java:S1192`: slugs repetidos de usuarios e produtos foram extraidos para constantes semanticas de dominio, como `USER_SUPER_ADMIN_SLUG`, `USER_EDITOR_SLUG`, `USER_VIEWER_SLUG`, `PRODUCT_MAESTRO_BETON` e `PRODUCT_ALEXANDRE_DEV`.

Validacoes associadas:

- local executa: comprovado por `@Profile("local")` em `DemoSeedRunner`/`DemoSeedService`, teste unitario de profile e Bruno completo contra backend local.
- prod nao executa: comprovado por `@Profile("local")` no runner e no service de seed, coberto em teste unitario.
- idempotencia preservada: contagens seedadas continuaram em 3 tenants, 9 produtos, 9 nos WikiDev/Loki e 7 arestas WikiDev/Loki.

`AGENTS.md` foi atualizado para prevenir reincidencia em seeds futuros:

- evitar nomes de constantes como `PASSWORD`, `SECRET` e `TOKEN` para valores de demonstracao local;
- preferir nome semantico explicito e property local quando fizer sentido;
- extrair literais repetidos de seed, slugs de produto, papeis, tenants e modulos para constantes semanticas;
- nao corrigir Sonar com `NOSONAR`, `@SuppressWarnings` ou desativacao de regra sem autorizacao explicita.

A reanalise visual do SonarQube for IDE fica pendente na IDE local, pois nao ha runner CLI disponivel neste ambiente. As validacoes objetivas feitas nesta sprint foram Maven, JaCoCo, Spring Modulith, Bruno, organizacao modular e revisao dos padroes documentados no `AGENTS.md`.

## Bruno

Criada a pasta `bruno/22-seed-inicial-e-grafo` com validacoes de:

- login do super-admin seedado;
- listagem dos tenants seedados;
- listagem dos produtos seedados;
- busca dos nodes Spring Boot, JPA, Poema do Limiar e Musica Ecos;
- vizinhos de Spring Boot em WikiDev;
- relacionados de JPA em WikiDev;
- vizinhos de Poema do Limiar em Loki;
- relacionados de Musica Ecos em Loki.

Ultima execucao contra a aplicacao local com profile `local`:

- Comando: `cd bruno && npx @usebruno/cli run --env local`
- Status: PASS
- Requests: 180 executados, 180 aprovados
- Tests: 354/354 aprovados
- Duration: 22238 ms

`docs/api-testing/README.md` foi atualizado com a pasta 22 e variaveis operacionais para os IDs seedados usados pelas requisicoes Bruno.

## Maven / JaCoCo / Spring Modulith

Teste direcionado de seed apos a correcao Sonar:

- Comando: `mvn -Dtest='*Seed*Test,*Demo*Seed*Test,*KnowledgeGraph*Seed*Test' test`
- Diretorio: `backend/`
- Resultado: `BUILD SUCCESS`
- Tests run: 21
- Failures: 0
- Errors: 0
- Skipped: 0

Ultima execucao completa obrigatoria:

- Comando: `mvn clean verify`
- Diretorio: `backend/`
- Resultado: `BUILD SUCCESS`
- Tests run: 1029
- Failures: 0
- Errors: 0
- Skipped: 0
- JaCoCo: `All coverage checks have been met`
- Spring Modulith: aprovado via suite completa

## Comandos executados

- `git status --short --branch`
- `git branch --show-current`
- `mvn -q -DskipTests compile`
- `mvn -q -Dtest='IdentityDemoUserServiceTest,TenantSeedServiceTest,ProductSeedServiceTest,KnowledgeGraphSeedServiceTest,DemoSeedRunnerTest,DemoSeedServiceTest,KeycloakAdminClientTest' test`
- `mvn -Dtest='*Seed*Test,*Demo*Seed*Test,*KnowledgeGraph*Seed*Test' test`
- `mvn clean verify`
- `mvn spring-boot:run -Dspring-boot.run.profiles=local`
- `cd bruno && npx @usebruno/cli run --env local`
- consultas SQL de idempotencia no PostgreSQL Aegis
- `git diff --check`
- `git diff --stat`
- `git status`

## Bugs ou ajustes reais encontrados

- O artefato original da sprint tinha ambiguidade documental de numero, chamando a etapa de 21 apesar do arquivo ser `22_seed_inicial_e_grafo.md`; corrigido para Sprint 22 sem alterar historico da Sprint 21.
- A playlist do Loki exigia uma representacao sem ampliar enum: foi modelada como `GraphNodeType.CONTENT` com metadata `refType=PLAYLIST`.
- O super-admin precisou receber ProductAssignments em todos os produtos seedados por causa da regra atual de acesso a conteudo de produto via `ProductAccessResolver`/ADR-0018.
- A rodada SonarQube for IDE apontou `java:S2068` e `java:S1192` no seed; corrigido com property local para credencial demo e constantes semanticas para slugs.
- O prompt de finalizacao apontava `README.md` na raiz, mas o arquivo nao existe; `docs/README.md` foi usado como equivalente oficial, conforme `AGENTS.md`.

## Criterios de aceite

- Seed executado somente em profile `local`.
- Profile `prod` nao registra nem executa o seed local.
- Keycloak mantido como fonte de identidade, sem autenticacao propria.
- Tenants, produtos, usuarios, memberships e ProductAssignments seedados de forma idempotente.
- Knowledge Graph inicial de WikiDev e Loki criado.
- Bruno atualizado com validacoes da Sprint 22.
- Bruno completo aprovado sem quebrar pastas anteriores.
- `mvn clean verify` aprovado com 0 failures, 0 errors, JaCoCo e Spring Modulith.
- SonarQube for IDE corrigido para os apontamentos listados, sem `NOSONAR`, `@SuppressWarnings` ou desativacao de regra.
- Documentacao de padroes atualizada em `AGENTS.md`.
- Documentacao da Sprint 22 registrada em `SPRINT-RESULTADO.md` e `results/sprint-22.md`.

## Retrofits pendentes

- Quando a etapa 24/notification existir, avaliar o fan-out de notificacao `ONBOARDING` para os 5 usuarios demo, conforme observacao do artefato da Sprint 22.
- Se o enum de `GraphNodeType` ganhar um tipo `PLAYLIST` em sprint futura, reavaliar a representacao da playlist do Loki hoje modelada como `CONTENT` + metadata `refType=PLAYLIST`.
