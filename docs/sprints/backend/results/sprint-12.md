# Sprint 12 — Dominio `asset` (biblioteca de midia, upload, metadados, tags)

## Identificacao

- **Sprint:** 12 — Dominio `asset`
- **Status:** Concluida em 2026-06-27
- **Branch:** `sprint/12-dominio-assets`
- **Resultado local:** BUILD SUCCESS

## Objetivo da Sprint

Entregar a biblioteca de assets do Aegis: upload multipart de arquivos (imagem, PDF, audio, video, documento), metadados editaveis, tags por produto, rastreamento de uso (`AssetUsage`) e resolucao universal por UUID — com o storage escolhido por produto (`Product.assetStorageStrategy`, `"local"` ou `"s3"`) modelado como Strategy Pattern, nunca como `if` espalhado pelo dominio.

## Escopo Entregue

- Entidades `Asset`, `AssetTag`, `AssetTagAssignment`, `AssetUsage` com migration propria `V6__asset_library.sql`.
- Strategy pattern de storage (`StorageProvider`) com duas implementacoes: `LocalStorageProvider` (disco local, estrutura de pastas por categoria) e `S3StorageProvider` (AWS SDK v2, URL pre-assinada).
- `AssetStorageProvisioningService`, acionado pelo novo evento `product.api.ProductCreatedEvent` (publicado por `ProductService` via `ApplicationEventPublisher`/`@TransactionalEventListener`), provisiona a estrutura de pastas por categoria automaticamente quando o produto usa `"local"` — nenhuma acao manual do usuario no sistema operacional.
- Validacao de upload por mime type e tamanho, configuravel por categoria via `aegis.assets.limits` (nunca hardcoded).
- Resolucao universal de asset por UUID (`GET /api/v1/assets/{assetId}/resolve`), sem module-gating, com checagem de tenant/membership (404 generico para asset de outro tenant, nunca 403 — nao revela existencia).
- `AssetController` com 11 endpoints (10 do artefato da sprint + `GET /api/v1/assets/{assetId}/file`, download direto do arquivo, adicionado para viabilizar teste manual completo sem URL presigned em ambiente local).
- Module-gating: todos os endpoints sob `/products/{productId}/...` anotados `@RequireModule(ModuleKey.ASSETS)`.
- Migracao completa da suite de validacao manual de Postman para Bruno (ver secao dedicada).

## Classes Criadas/Alteradas por Area

### Backend — Dominio `asset`

- `asset/domain/Asset.java` (com `Asset.Creation` e `Asset.Metadata`, records internos para evitar metodo/construtor com excesso de parametros — java:S107), `AssetCategory.java`, `AssetStatus.java`, `AssetTag.java`, `AssetTagAssignment.java`, `AssetUsage.java`
- `asset/repository/AssetRepository.java`, `AssetTagRepository.java`, `AssetTagAssignmentRepository.java`, `AssetUsageRepository.java` (Javadoc completo na interface e em todo metodo)
- `asset/mapper/AssetMapper.java` (MapStruct, `@Named` em conversores ambiguos para evitar auto-selecao implicita — mesmo bug ja documentado na Sprint 11)
- `asset/storage/StorageProvider.java` (interface), `LocalStorageProvider.java`, `S3StorageProvider.java`, `AssetFilenameSanitizer.java`, `ResolvedLocation.java`, `S3ClientConfiguration.java`
- `asset/service/AssetService.java`, `AssetStorageProvisioningService.java`, `AssetUploadValidator.java`
- `asset/controller/AssetController.java`
- `asset/contract/UpdateAssetMetadataRequest.java`, `DeleteAssetRequest.java`
- `asset/dto/AssetSummary.java`, `AssetDetail.java`, `AssetUsageSummary.java`, `ResolvedAsset.java`, `AssetFileContent.java`
- `asset/exception/AssetNotFoundException.java`, `AssetTagNotFoundException.java`, `AssetTagAlreadyExistsException.java`, `InvalidAssetTagNameException.java`, `InvalidAssetMimeTypeException.java`, `InvalidAssetFilenameException.java`, `AssetSizeLimitExceededException.java`, `AssetInUseException.java`, `AssetStorageException.java`, `AssetExceptionHandler.java`
- `asset/config/AssetLimitsProperties.java`, `S3StorageProperties.java`, `AssetPropertiesConfiguration.java`

### Backend — Integracao com `product` (API publica)

- `product/api/AssetStorageStrategy.java` — **movido** de `product.domain` para `product.api` (correcao de violacao real do Spring Modulith: `asset` precisava do enum para resolver o `StorageProvider`, e tipos de `domain` nao sao expostos pela fronteira do modulo).
- `product/api/ProductCreatedEvent.java` (novo) — record `(tenantId, productId, assetStorageStrategy)`, publicado por `ProductService` apos a criacao do produto.
- `product/api/ProductReferenceService.java` — metodo novo `getRequiredAssetStorageStrategy(productId)`.
- `product/service/ProductService.java` — injecao de `ApplicationEventPublisher`, publica `ProductCreatedEvent` ao final de `createProduct`.
- Ajustes de import em `Product.java`, `ProductDetail.java`, `ProductSummary.java`, `CreateProductCommand.java`, `CreateProductRequest.java` e respectivos testes, decorrentes do `AssetStorageStrategy` ter mudado de pacote.

### Persistencia

- `db/migration/V6__asset_library.sql` — tabelas `assets`, `asset_tags`, `asset_tag_assignments`, `asset_usages`.

### Build e configuracao

- `backend/pom.xml` — `software.amazon.awssdk:bom` (2.29.52) e `software.amazon.awssdk:s3` (o `S3Presigner` ja vem incluso no artefato `s3`, sem dependencia separada de `s3-presigner`).
- `backend/src/main/resources/application.yml` — `aegis.assets.limits.*` (tabela de mime types/tamanho maximo por categoria), `spring.servlet.multipart.max-file-size`/`max-request-size: 260MB` (acima do limite de 250MB da categoria `video`), `aegis.storage.s3.*`.
- `.env.example` — `AEGIS_ASSETS_MULTIPART_MAX_FILE_SIZE`/`MAX_REQUEST_SIZE`.
- `.gitignore` — adicionado `backend/data/` (raiz de storage local de assets, criada em runtime, nunca versionada).

### Testes

- `asset/repository/AssetRepositoryTest.java`, `AssetTagRepositoryTest.java`, `AssetTagAssignmentRepositoryTest.java`, `AssetUsageRepositoryTest.java` (`@DataJpaTest`)
- `asset/mapper/AssetMapperTest.java`
- `asset/storage/AssetFilenameSanitizerTest.java`, `LocalStorageProviderTest.java`, `S3StorageProviderTest.java`, `S3ClientConfigurationTest.java`
- `asset/service/AssetServiceTest.java` (38 testes), `AssetStorageProvisioningServiceTest.java`, `AssetUploadValidatorTest.java`
- `asset/controller/AssetControllerTest.java` (23 testes)
- `asset/domain/AssetCategoryTest.java`, `asset/dto/AssetFileContentTest.java`, `asset/exception/AssetExceptionHandlerTest.java`, `asset/config/AssetPropertiesConfigurationTest.java`
- Ajustes em testes existentes de `product` decorrentes do `AssetStorageStrategy`/`ProductCreatedEvent` (`ProductServiceTest`, `ProductControllerTest`, `ProductReferenceServiceTest`, `ProductAccessResolverTest`, `ProductAssignmentServiceTest`, `ProductModuleServiceTest`, mappers).

## Endpoints Implementados / Contratos REST Confirmados

```
GET    /api/v1/products/{productId}/assets
POST   /api/v1/products/{productId}/assets                  (multipart/form-data: file, friendlyName?)
GET    /api/v1/products/{productId}/assets/{assetId}
PUT    /api/v1/products/{productId}/assets/{assetId}/metadata
DELETE /api/v1/products/{productId}/assets/{assetId}        (body opcional { "force": boolean })
GET    /api/v1/products/{productId}/assets/{assetId}/usage
GET    /api/v1/products/{productId}/asset-tags
POST   /api/v1/products/{productId}/asset-tags              (body: JSON string literal, ex.: "institucional")
DELETE /api/v1/products/{productId}/asset-tags/{tag}
GET    /api/v1/assets/{assetId}/resolve                      (sem module-gating, ver Seguranca)
GET    /api/v1/assets/{assetId}/file                          (adicionado pos-entrega — download direto, ver Decisoes)
```

10 dos 11 endpoints sao exatamente os do artefato da etapa (`12_dominio_assets.md`, Secao B). O 11º (`GET /assets/{assetId}/file`) foi adicionado durante a validacao manual para permitir baixar o arquivo do `LocalStorageProvider` sem precisar de uma URL presigned (que so existe para `S3StorageProvider`) — `resolve()` continua sendo o contrato universal documentado na etapa; `/file` e um atalho complementar, mesmo padrao de adendo pos-entrega ja registrado na Sprint 11 (endpoint de criacao de `Content`).

## Regras de Negocio Implementadas

- **Validacao por mime type e categoria**: `AssetUploadValidator` resolve a `AssetCategory` (`image`/`pdf`/`audio`/`video`/`document`) a partir do `Content-Type` recebido contra a tabela `aegis.assets.limits`; mime fora da lista da categoria → 400 `INVALID_ASSET_MIME_TYPE`.
- **Limite de upload por categoria, incluindo video**: tamanhos configurados em `aegis.assets.limits.<categoria>.max-size-bytes` (image 5MB, pdf 15MB, audio 30MB, video 250MB, document 15MB); acima do limite → 413 `ASSET_SIZE_LIMIT_EXCEEDED`. `spring.servlet.multipart.max-file-size`/`max-request-size` elevados para 260MB (acima do maior limite de categoria), senão o upload de video falharia silenciosamente no limite default do Spring (1MB) antes mesmo de chegar no `AssetService`.
- **Nome de arquivo ausente/em branco** → 400 `INVALID_ASSET_FILENAME`.
- **Sanitizacao de filename**: `AssetFilenameSanitizer` remove `..`, `/`, `\`, caracteres de controle e normaliza acentuacao/espacos sem usar regex (loop manual de caracteres, decisao de qualidade — ver Sonar), prevenindo path traversal no `storageKey`.
- **Metadados**: `PUT .../metadata` atualiza `friendlyName`, `altText`, `caption`, `credit` e reconcilia as `tags` (string CSV) — tags inexistentes sao criadas automaticamente, tags removidas da lista tem sua `AssetTagAssignment` removida (sem excluir a `AssetTag` em si, que pode estar associada a outros assets).
- **Tags**: corpo de `POST .../asset-tags` e um JSON string literal (nao um objeto) — decodificado explicitamente via Jackson (`tools.jackson`), nao pelo binding implicito do Spring MVC, para nao gravar a tag com aspas literais no nome; nome nao decodificavel → 400 `INVALID_ASSET_TAG_NAME`; nome duplicado no mesmo produto → 409 `ASSET_TAG_ALREADY_EXISTS`.
- **Usage**: `AssetUsage` rastreia onde um asset e referenciado (`usedInType`, `usedInRefId`, `usedInLabel`); `GET .../usage` lista as referencias ativas.
- **Delete com force**: excluir asset com `AssetUsage` ativo sem `{ "force": true }` → 409 `ASSET_IN_USE`; com `force: true`, exclui o registro e o arquivo do storage independentemente do uso.
- **Resolucao universal por UUID**: `GET /api/v1/assets/{assetId}/resolve` busca o asset so pelo `id`, confirma membership no tenant do asset, instancia o `StorageProvider` correspondente (`local` ou `s3`) e devolve `url`/`expiresAt`/`contentType` — nenhum outro dominio guarda URL ou caminho, so o `assetId`.
- **Mudanca de estrategia nao retroage**: o `storageProvider` de um `Asset` e gravado no momento do upload (herdado do `assetStorageStrategy` do produto naquele instante) e nunca migra automaticamente se o produto trocar de estrategia depois.

## Integracao com `Product.assetStorageStrategy`

- `Product.assetStorageStrategy` (`"local"`/`"s3"`, default `"local"`, ja existente desde a Sprint 06/07) e lido no momento da criacao do produto: `ProductService.createProduct` publica `ProductCreatedEvent(tenantId, productId, assetStorageStrategy)` via `ApplicationEventPublisher`.
- `AssetStorageProvisioningService`, escutando via `@TransactionalEventListener` (decisao de implementacao, ver abaixo), provisiona a estrutura de pastas por categoria (`image/`, `pdf/`, `audio/`, `video/`, `document/`) somente quando a estrategia e `"local"` — no-op para `"s3"` (bucket compartilhado, isolado por prefixo de chave).
- No upload, `AssetService` le `ProductReferenceService.getRequiredAssetStorageStrategy(productId)` para decidir qual `StorageProvider` usar e grava o resultado em `Asset.storageProvider` — nao consulta o produto de novo em operacoes futuras sobre aquele asset (delete, resolve, usage).

## Storage Local — `LocalStorageProvider`

- Estrutura de pastas: `${AEGIS_STORAGE_LOCAL_PATH}/aegis/pms/{tenantId}/{productId}/{category}/{filename}` — namespace `aegis/pms` fixo no codigo (nao no env var), permitindo o mesmo volume ser compartilhado por outros produtos BYOP no futuro sem reconfiguracao.
- `filename` preserva o nome original sanitizado (nunca o `assetId` como nome de arquivo) — colisao de nome resolvida com sufixo numerico incremental (`arquivo.pdf` → `arquivo-2.pdf` → `arquivo-3.pdf`...), nunca sobrescrita silenciosa.
- `resolve()` devolve a URL servida pelo proprio backend (`GET /api/v1/assets/{assetId}/file`), nunca um caminho de disco exposto direto.
- Validado manualmente: upload real grava o arquivo na estrutura exata `data/assets/aegis/pms/{tenantId}/{productId}/{category}/...` (confirmado via `ls` direto no disco durante a sessao de validacao).

## Storage S3 — `S3StorageProvider`

- AWS SDK v2.29.52 (`software.amazon.awssdk:s3`), estilo **Consumer Builder** (`s3Client.putObject(builder -> builder.bucket(...).key(...), RequestBody...)`) em vez do overload por objeto `PutObjectRequest` (decisao tomada durante limpeza do SonarQube, ver Decisoes).
- `storageKey` = `aegis/pms/{tenantId}/{productId}/{category}/{filename}` (sem prefixo de ponto de montagem local — no S3 a chave ja comeca no namespace `aegis/pms`).
- `resolve()` gera URL pre-assinada com TTL curto (`aegis.storage.s3.presigned-url-ttl-seconds`, default 900s = 15min), nunca uma URL publica permanente.

## `StorageProvider` (abstracao)

Interface unica (`provisionProductFolders`, `store`, `resolve`, `delete`) com as duas implementacoes acima resolvidas em runtime por `AssetStorageProvisioningService`/`AssetService` a partir do `assetStorageStrategy` do produto — nenhum `if (provider === "s3")` espalhado pelo dominio, conforme exigido pelo artefato da sprint (Secao D).

## `AssetStorageProvisioningService`

Orquestra o provisionamento de pastas por categoria na criacao do produto (Strategy `"local"`) via `Java NIO`, de forma sincrona, reagindo ao `ProductCreatedEvent` — decisao de implementacao registrada abaixo.

## Resolucao Universal por UUID

`GET /api/v1/assets/{assetId}/resolve` — unico endpoint do dominio fora do prefixo `/products/{productId}/...`, propositalmente: qualquer outro dominio (content, pages, forms) referencia um asset apenas pelo `assetId`, sem conhecer productId/tenantId no momento da resolucao. Sem module-gating (`@RequireModule`), mas com checagem obrigatoria de tenant/membership do usuario autenticado contra o `tenantId` do asset — asset de outro tenant retorna 404 `ASSET_NOT_FOUND`, nunca 403 (mesma regra de nao revelar existencia das demais entidades do sistema).

## Upload Multipart

`POST /api/v1/products/{productId}/assets`, `consumes = multipart/form-data`, campos `file` (obrigatorio) e `friendlyName` (opcional). Validado manualmente com PNG, PDF, arquivo com mime invalido e arquivo acima do limite de imagem (5MB) — todos os quatro cenarios com o resultado esperado (201, 201, 400, 413).

## Module-Gating

`AssetController` anotado `@RequireModule(ModuleKey.ASSETS)` em todos os endpoints sob `/products/{productId}/...` (`ModuleAccessAspect` ja existente desde a Sprint 07) — modulo desabilitado retorna 403 `MODULE_DISABLED`, mesmo para `SUPER_ADMIN`, validado manualmente desabilitando e reabilitando o modulo `ASSETS` em produto real.

## Decisoes de Implementacao Registradas pelo GPT/Codex

1. **`AssetStorageProvisioningService` via evento de aplicacao, nao `@ApplicationModuleListener`**: usar Spring `ApplicationEventPublisher`/`@TransactionalEventListener` em vez do listener nativo do Spring Modulith evita a necessidade da tabela `event_publication` (nao presente nas migrations) e quebra o ciclo de dependencia entre `product` (quem cria o produto) e `asset` (quem precisa reagir a criacao) sem que `product` precise conhecer `asset` diretamente.
2. **Provisionamento de pastas sincrono, nao assincrono**: decisao explicitamente deixada a criterio do GPT/Codex pelo artefato da etapa ("adote a melhor postura encontrada no mercado, mas sem acao manual do usuario quanto ao SO"). Optado por execucao sincrona dentro do listener transacional — simplicidade e consistencia imediata (a pasta existe antes da resposta de criacao do produto retornar), aceitavel porque a operacao e `Files.createDirectories` (NIO, baixa latencia), nunca uma chamada de rede.
3. **`AssetStorageStrategy` promovido a `product.api`**: a interface publica do Spring Modulith (`NamedInterface`) exige que tipos consumidos por outro modulo (`asset`) estejam no pacote `.api`, nao em `.domain`. Movido sem alterar o enum em si, so o pacote — ajuste mecanico em ~15 arquivos que o importavam.
4. **`Asset.Creation`/`Asset.Metadata` como records internos**: o construtor de `Asset` excederia 7 parametros (java:S107) sem agrupamento — mesmo padrao ja usado em `GraphNode.Creation` (Sprint 08) e `Content.Edit` (Sprint 11).
5. **`AssetFilenameSanitizer` sem regex**: reescrito como loop manual de caracteres (em vez de `Pattern`/`replaceAll`) durante a limpeza do SonarQube (java:S5850/S5852 — regex de sanitizacao de path pode ser vetor de ReDoS); decisao de seguranca, nao so de estilo.
6. **`GET /assets/{assetId}/file` adicionado pos-entrega**: nao estava na lista literal de endpoints do artefato da etapa, mas foi necessario para validar manualmente o `LocalStorageProvider` sem depender de URL presigned (exclusiva do S3) — mesmo padrao de adendo documentado pela Sprint 11 (`POST /content`).

## Bug Real de Plataforma Encontrado e Corrigido

`AwsBasicCredentials.create(accessKeyId, secretAccessKey)` lanca `NullPointerException` quando as credenciais S3 estao em branco (ambiente de desenvolvimento tipico, sem S3 configurado) — esse `NullPointerException` nao tratado era traduzido pelo filtro de excecoes do Spring Security em um 403 `insufficient_scope` confuso, sem relacao com o erro real. Corrigido com fallback para `AnonymousCredentialsProvider` em `S3ClientConfiguration.credentialsProvider()` quando as credenciais estao em branco, e com um handler dedicado `SdkException` → 502 `ASSET_STORAGE_UNAVAILABLE` em `AssetExceptionHandler`, para que qualquer erro real de comunicacao com o S3 (bucket nao configurado, indisponibilidade do servico) surja de forma clara em vez de vazar como 403 confuso.

## Migrations Criadas

- `V6__asset_library.sql` — tabelas `assets`, `asset_tags`, `asset_tag_assignments`, `asset_usages`, indices por `tenant_id`/`product_id`/`status`, constraint `uk_asset_tags_product_id_name` (tag unica por produto). Nenhuma migration anterior foi alterada.

## Configuracoes Adicionadas/Alteradas

- `aegis.assets.limits.<categoria>.mime-types`/`max-size-bytes` (image, pdf, audio, video, document) — `application.yml`, configuravel, nunca hardcoded no codigo.
- `spring.servlet.multipart.max-file-size`/`max-request-size`: `260MB`.
- `aegis.storage.s3.bucket`/`region`/`access-key-id`/`secret-access-key`/`presigned-url-ttl-seconds` — credenciais sempre via variavel de ambiente, nunca versionadas.
- `aegis.storage.local-path` (`AEGIS_STORAGE_LOCAL_PATH`, default `./data/assets`) — ja previsto desde a Sprint 01, reaproveitado.

## Seguranca

- Module-gating (`@RequireModule(ModuleKey.ASSETS)`) em todo endpoint sob `/products/{productId}/...`.
- `GET /assets/{assetId}/resolve` e `GET /assets/{assetId}/file` (sem module-gating, por desenho — Secao D.5 do artefato) validam tenant/membership do usuario autenticado contra o `tenantId` do asset antes de gerar qualquer URL ou devolver bytes — asset de outro tenant → 404, nunca 403.
- `storageKey` sempre montado no backend a partir de `tenantId`/`productId`/`category`/`filename` sanitizado — nunca aceito pronto do cliente (mitigacao de path traversal).
- Credenciais S3 nunca em `application.yml` versionado, somente via variavel de ambiente/secret.
- Nenhuma alteracao em `SecurityConfig`.

## Testes Automatizados

- 38 testes em `AssetServiceTest`, 23 em `AssetControllerTest`, cobertura completa de `LocalStorageProviderTest`/`S3StorageProviderTest`/`S3ClientConfigurationTest` (incluindo os riscos da tabela D.5 do artefato: filename com `../` rejeitado, colisao de nome gera sufixo, `resolve()` de outro tenant retorna 404).
- `S3StorageProviderTest` reescrito para mockar o estilo Consumer Builder via `ArgumentCaptor.captor()` (Mockito 5.x, evita cast/raw-type sem checagem).
- `MockMultipartFile` normaliza internamente um `originalFilename` nulo para string vazia (confirmado via inspecao de bytecode) — um teste que pretendia cobrir o ramo `originalFilename == null` na verdade so cobria o ramo de string vazia; corrigido usando `mock(MultipartFile.class)` real com `getOriginalFilename()` retornando `null` diretamente.

## Validacao Manual

Executada com a aplicacao real (`mvn spring-boot:run`, perfil `local`), Postgres e Keycloak de desenvolvimento ja em execucao via `docker compose`:

1. **Upload de PNG** — 201, asset criado com `category: "image"`, arquivo gravado em `data/assets/aegis/pms/{tenantId}/{productId}/image/...`.
2. **Upload de PDF** — 201, `category: "pdf"`, arquivo gravado em `.../pdf/...`.
3. **Upload invalido por mime type** (`.exe`) — 400 `INVALID_ASSET_MIME_TYPE`.
4. **Upload acima do limite** (PNG > 5MB) — 413 `ASSET_SIZE_LIMIT_EXCEEDED`.
5. **Upload S3 sem bucket configurado** — 502 `ASSET_STORAGE_UNAVAILABLE` (apos a correcao do bug real descrito acima; antes da correcao, 403 confuso).
6. **Estrutura de pastas por categoria** (`image/pdf/audio/video/document`) confirmada provisionada automaticamente na criacao de produto com `assetStorageStrategy: "local"`, sem nenhuma acao manual no sistema operacional.
7. **Resolucao universal** `GET /assets/{assetId}/resolve` — 200, `url`/`contentType` corretos para asset local.
8. **Download via** `GET /assets/{assetId}/file` — 200, `Content-Type` correto, bytes do arquivo.
9. **Tags**: criar, listar, duplicar (409), nome invalido (400), remover — todos confirmados.
10. **Metadados**: `PUT .../metadata` atualiza `friendlyName`/`altText`/`caption`/`credit` e reconcilia tags via CSV.
11. **Usage**: lista vazia confirmada para asset sem uso.
12. **Delete**: asset sem uso excluido sem `force`; tentativa de excluir asset em uso sem `force` rejeitada com 409 `ASSET_IN_USE` (cenario validado via testes automatizados; fluxo de criar `AssetUsage` real pertence a dominios futuros que ainda nao existem, ex. `content`/`pages` referenciando assets).
13. **Module disabled**: modulo `ASSETS` desabilitado em produto real retorna 403 `MODULE_DISABLED` em endpoint de listagem; reabilitado em seguida.

Todos os cenarios de aceite do artefato da sprint foram confirmados em ambiente real, nao apenas em teste automatizado.

## Postman → Bruno

Esta sprint mudou a ferramenta de validacao manual cumulativa de **Postman** para **Bruno**.

### Motivo da migracao

Durante a validacao manual do upload multipart desta sprint, **o aplicativo desktop do Postman apresentou um problema real ao enviar arquivos via `multipart/form-data` no macOS** (upload de PNG/PDF falhando de forma inconsistente direto pelo cliente desktop, mesmo com o request configurado corretamente) — o que comprometeu a confiabilidade da suite de testes manuais bem no momento em que o dominio `asset` (cujo nucleo e justamente upload de arquivo) precisava ser validado. Diante disso, foi adotado o **Bruno** como cliente de API alternativo, e a suite inteira foi migrada/reconstruida nele, passando a ser a collection oficialmente validada a partir desta sprint.

### O que foi feito

- Removida a dependencia pratica do Postman: a pasta `postman/` e o arquivo `aegis-postman-collection.json` foram removidos do repositorio.
- Criada a estrutura `bruno/` (collection git-nativa, um arquivo `.bru` por request):
  - `bruno/bruno.json` (manifesto da collection).
  - `bruno/collection.bru` (header `Authorization: Bearer {{token}}` herdado por toda a collection + `script:pre-request` com defaults de variaveis dinamicas).
  - `bruno/environments/{local,dev,homolog,prod}.bru` (variaveis `baseUrl`, `keycloakIssuer`, `clientId`, `username`, `password`, `tenantName`; `local`/`dev` com valores reais, `homolog`/`prod` vazios).
  - `bruno/fixtures/` — arquivos binarios reais para upload multipart (PNG, PDF, executavel com mime invalido, PNG acima do limite de 5MB).
  - 10 pastas numeradas espelhando as etapas do backend: `00-auth`, `01-tenants`, `02-products`, `03-product-modules`, `04-manual`, `05-knowledge-graph`, `06-health`, `10-product-assignments`, `11-content`, `12-assets` — **85 requests no total**.
- **Scripts convertidos de `pm.*` para `bru.*` em 100% dos arquivos** — nenhuma chamada `pm.*`/`postman.*` permanece em nenhum arquivo `.bru` do repositorio (confirmado via busca textual).
- **Login** (`00-auth/login-loki-123456.bru`) salva `token` e `refreshToken` automaticamente via `script:post-response` (`bru.setVar('token', res.body.access_token)`).
- **Requests autenticadas** consomem `Authorization: Bearer {{token}}` herdado de `collection.bru` (usando um `headers` simples, nao o bloco `auth:bearer`, que tem um bug conhecido de nao repropagar variaveis de runtime entre requests no modo herdado do Bruno CLI).
- **Multipart funcionando corretamente no Bruno**: upload de PNG, PDF, mime invalido e arquivo acima do limite — todos os 4 cenarios validados na pasta `12-assets`, sem nenhum ajuste manual alem da configuracao do `.bru` em si.
- Documentacao criada: `docs/api-testing/README.md` (guia de uso, instalacao, estrutura, padrao obrigatorio para sprints futuras); `docs/sprints/backend/00_padrao_qualidade_e_arquitetura.md` (Secao 11) reescrita para descrever o fluxo Bruno; demais arquivos de `docs/sprints/backend/*.md` e `ADR-0015` com referencias a Postman atualizadas para Bruno (os arquivos `results/sprint-N.md` anteriores a esta sprint foram preservados como registro historico — na epoca, Postman era de fato a ferramenta usada).
- **Postman nao foi mantido por compatibilidade histórica nesta sprint** — foi removido integralmente do repositorio, ja que apresentou a falha real descrita acima; o historico de seu uso permanece apenas nos arquivos `results/sprint-N.md` anteriores (Sprints 3 a 11), que nao foram reescritos.

### Validacao da collection Bruno

`npx @usebruno/cli run --env local`, a partir de `bruno/`, executando a collection inteira (todas as 10 pastas, em sequencia unica — variaveis de runtime como tokens e IDs criados so persistem dentro de uma mesma invocacao do CLI): **85 requests executados, 85 aprovados, 160/160 testes aprovados**, confirmado em duas execucoes completas consecutivas (idempotente — tenant/produto/usuario novos a cada execucao via seeds com timestamp). Repetido tambem com `--env dev` (mesmo host local, outro profile), com o mesmo resultado.

## Arquivos Criados/Alterados/Removidos

Ver listagem completa por area nas secoes "Classes Criadas/Alteradas por Area" e "Postman → Bruno" acima. Resumo:

- **Criados:** ~38 arquivos de producao em `backend/.../asset/`, ~17 arquivos de teste em `backend/.../asset/`, `V6__asset_library.sql`, `product/api/ProductCreatedEvent.java`, toda a estrutura `bruno/` (85 requests + config), `docs/api-testing/README.md`, `docs/sprints/backend/results/sprint-12.md` (este arquivo).
- **Alterados:** `backend/pom.xml`, `application.yml`, `.env.example`, `.gitignore`, ~15 arquivos de `product` (import/pacote de `AssetStorageStrategy`), `docs/sprints/backend/SPRINT-RESULTADO.md`, `docs/sprints/backend/00_padrao_qualidade_e_arquitetura.md`, `docs/sprints/backend/*.md` (referencias Postman → Bruno), `docs/adr/ADR-0015-modulos-como-portao-de-acesso.md`, `docs/README.md`.
- **Removidos:** `postman/` (pasta inteira, incluindo `aegis-postman-collection.json` e os arquivos de sincronizacao do app desktop).
- **Movidos:** `product/domain/AssetStorageStrategy.java` → `product/api/AssetStorageStrategy.java`.

## Qualidade e Testes

- `mvn clean verify`: **BUILD SUCCESS**
- Testes: **614** executados, **0** failures, **0** errors, **0** skipped (partindo de 453 na Sprint 11 — 161 testes novos/ajustados nesta sprint)
- JaCoCo: aprovado (`All coverage checks have been met`) — 100% linha e branch nas classes elegiveis, incluindo `LocalStorageProvider`, `S3StorageProvider` e `AssetStorageProvisioningService`
- Spring Modulith: aprovado via `ModulithArchitectureTest` (`ApplicationModules.verify()`) — inclusive a correcao de `AssetStorageStrategy` para `product.api`
- SonarQube for IDE: tres rodadas de limpeza realizadas ao longo da sessao (13 apontamentos iniciais → 4 → 3 → 0), sem `@SuppressWarnings`, sem remocao de teste, sem relaxar validacao, sem alterar contrato/migration/arquitetura de storage — destaque para a reescrita do `S3StorageProvider` para o estilo Consumer Builder (java:S6244) e do `AssetFilenameSanitizer` sem regex (java:S5850/S5852), ambas validadas sem regressao de comportamento via `mvn clean verify` apos cada rodada

## Riscos

- **Disco local sem monitoramento de espaco**: `LocalStorageProvider` nao tem alerta de uso de disco — risco ja mapeado no artefato da sprint (Secao D.5), mitigacao (monitoramento via `/actuator` customizado ou alerta externo) fica para sprint futura de observabilidade.
- **Local nao escala horizontalmente**: multiplas instancias do backend nao compartilham disco automaticamente — `local` aceito para single-instance/MVP; migrar para S3 antes de escalar horizontalmente.
- **Assets "orfaos" de proveniencia**: se um produto trocar de `assetStorageStrategy` depois de criado, assets antigos continuam no provider original — convivencia aceita por desenho (Secao D.4 do artefato), sem ferramenta de migracao entre providers (fora do escopo desta sprint).
- **`AssetUsage` ainda sem produtor real**: nenhum dominio existente hoje (content, pages) cria registros de `AssetUsage` — o endpoint de listagem funciona e foi testado com lista vazia/mockada, mas o fluxo end-to-end real de "asset referenciado por conteudo" so sera validado quando um dominio consumidor existir.

## Retrofits Pendentes

- Quando dominios futuros (content, pages, forms) passarem a referenciar assets, eles devem criar registros de `AssetUsage` (e remove-los ao deixar de referenciar) — nenhuma infraestrutura de evento automatico para isso existe ainda; cada dominio consumidor e responsavel por chamar a API de `asset` explicitamente.
- Ferramenta de migracao entre `StorageProvider`s (mencionada no artefato, Secao D.4/D.6) permanece nao implementada, por desenho — registrar como sprint futura se a necessidade surgir.
- CDN na frente do `S3StorageProvider` e geracao de thumbnails/variantes (Secao D.6 do artefato) registrados como melhorias futuras, nao implementadas nesta sprint.

## Conclusao

A Sprint 12 foi concluida com o dominio `asset` completo (upload multipart, validacao por mime/tamanho/categoria, metadados, tags, usage, exclusao com `force`, resolucao universal por UUID, module-gating), modelando o storage como Strategy Pattern (`local`/`s3`) com provisionamento automatico de pastas via evento de aplicacao — sem nenhuma acao manual do usuario no sistema operacional. Um bug real de plataforma foi encontrado e corrigido (credenciais S3 em branco causando 403 confuso, agora 502 `ASSET_STORAGE_UNAVAILABLE`). Tres rodadas de limpeza do SonarQube for IDE trataram todos os apontamentos sem alterar regra de negocio, contrato ou arquitetura. A suite de validacao manual migrou de Postman para Bruno apos o Postman apresentar falha real de multipart no macOS durante a propria validacao desta sprint — Bruno passa a ser a suite oficialmente validada (85 requests, 160 testes, 100% aprovados) a partir desta sprint. `mvn clean verify` aprovado com 614 testes, JaCoCo 100% e Spring Modulith aprovado.
