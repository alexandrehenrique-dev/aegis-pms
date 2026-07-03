# Sprint 28 — Backup e exportação de dados na exclusão de produto ou tenant

Finalizada em 2026-07-03 na branch `sprint/28-backup-exportacao-exclusao`.

## Objetivo da sprint

Garantir que a exclusão irreversível de produto ou tenant só avance depois de gerar uma cópia completa dos dados do produto, armazenar o ZIP em área permanente de exports, validar que o arquivo armazenado está íntegro e disponibilizar um link de download por streaming.

## Escopo implementado

- `DELETE /api/v1/products/{productId}` com `confirmationText`, autorização contextual, retorno `202 Accepted` e job assíncrono de export/delete.
- `GET /api/v1/exports/{tokenId}/download` público por token temporário, com streaming via `StreamingResponseBody`.
- Retrofit de `DELETE /api/v1/tenants/{tenantId}` para retornar `202 Accepted` e disparar export/delete por produto antes da remoção final do tenant.
- Entidade `ExportToken`, repository, migration `V15__export_tokens.sql`, status de token e cleanup agendado.
- Status novos de produto: `DELETING`, `DELETED`, `EXPORT_FAILED` e `DELETE_FAILED`.
- Serialização do backup em ZIP com JSONs por domínio e arquivos físicos de assets por streaming.
- Validação de integridade do ZIP armazenado antes de qualquer delete.
- E-mail de export pronto/falha via `productExport.ftl`.
- Pasta Bruno 28 com fluxo seguro de validação.

## Fora de escopo

- Reimportação/restauração de ZIP.
- Retry/backoff operacional formal para SMTP/S3.
- Políticas avançadas de expiração por tenant/plano.
- Execução cumulativa completa da collection Bruno nesta finalização; foi executada a pasta 28 isolada.

## Arquivos criados

- `backend/src/main/java/br/com/byop/aegis/asset/api/ProductExportStorageAdapter.java`
- `backend/src/main/java/br/com/byop/aegis/product/api/ProductExportStoragePort.java`
- `backend/src/main/java/br/com/byop/aegis/product/api/ProductExportStoredFile.java`
- `backend/src/main/java/br/com/byop/aegis/product/export/**`
- `backend/src/main/java/br/com/byop/aegis/tenant/api/TenantExportRemovalPort.java`
- `backend/src/main/java/br/com/byop/aegis/tenant/api/TenantExportRemovalService.java`
- `backend/src/main/java/br/com/byop/aegis/tenant/api/TenantProductExportPort.java`
- `backend/src/main/java/br/com/byop/aegis/tenant/dto/TenantDeleteAcceptedResponse.java`
- `backend/src/main/resources/db/migration/V15__export_tokens.sql`
- `backend/src/test/java/br/com/byop/aegis/asset/api/ProductExportStorageAdapterTest.java`
- `backend/src/test/java/br/com/byop/aegis/product/export/**`
- `backend/src/test/java/br/com/byop/aegis/tenant/api/TenantExportRemovalServiceTest.java`
- `bruno/28-backup-exportacao-exclusao/**`
- `infra/keycloak/themes/aegis/email/html/productExport.ftl`
- `docs/sprints/backend/results/sprint-28.md`

## Arquivos alterados

- `backend/pom.xml`
- `backend/src/main/java/br/com/byop/aegis/asset/storage/LocalStorageProvider.java`
- `backend/src/main/java/br/com/byop/aegis/asset/storage/S3StorageProvider.java`
- `backend/src/main/java/br/com/byop/aegis/asset/storage/StorageProvider.java`
- `backend/src/main/java/br/com/byop/aegis/product/domain/Product.java`
- `backend/src/main/java/br/com/byop/aegis/product/domain/ProductStatus.java`
- `backend/src/main/java/br/com/byop/aegis/security/SecurityConfig.java`
- `backend/src/main/java/br/com/byop/aegis/tenant/controller/TenantController.java`
- `backend/src/main/java/br/com/byop/aegis/tenant/service/TenantService.java`
- Testes de storage, produto, security, settings e tenant relacionados ao novo ciclo de export/delete.
- `docs/api-testing/README.md`
- `docs/sprints/backend/SPRINT-RESULTADO.md`

Observação: `docs/sprints/backend/30_seed_homologacao_e_remocao_seed_java.md` aparece modificado no worktree, mas é alteração preexistente e fora do escopo desta sprint.

## Arquivos removidos

Nenhum arquivo foi removido nesta sprint.

## Endpoints confirmados

- `DELETE /api/v1/products/{productId}`: inicia exportação e exclusão assíncrona do produto; retorna `202`.
- `GET /api/v1/exports/{tokenId}/download`: stream público do ZIP por token temporário.
- `DELETE /api/v1/tenants/{tenantId}`: inicia exports/deletes dos produtos do tenant; retorna `202`.

## Contratos implementados

- `DeleteProductRequest`: `{ "confirmationText": "nome exato do produto" }`.
- `DeleteAcceptedResponse`: `{ "message": "Exportação iniciada..." }`.
- `TenantDeleteAcceptedResponse`: resposta assíncrona para exclusão de tenant.
- Erros específicos: `INVALID_PRODUCT_DELETE_CONFIRMATION`, `EXPORT_ALREADY_IN_PROGRESS`, `PRODUCT_DELETE_FORBIDDEN`, `EXPORT_DOWNLOAD_NOT_FOUND`, `EXPORT_LINK_EXPIRED`.

## Entidades e migrations

`ExportToken` persiste o token temporário de download com `id`, `productId`, `productKey`, `zipPath`, `storageProvider`, `recipientEmail`, `status`, `createdAt`, `expiresAt` e `downloadedAt`.

Migration criada: `V15__export_tokens.sql`, com tabela `export_tokens` e índice `idx_export_tokens_status_expires_at`.

## Fluxo de exclusão de produto

1. Controller recebe `DELETE /api/v1/products/{productId}`.
2. `ProductDeleteService` valida existência, confirmação textual, permissão e idempotência.
3. Produto é marcado como `DELETING`.
4. Request retorna `202 Accepted`.
5. `ExportAndDeleteService.exportAndDelete(...)` roda no executor assíncrono.
6. O produto só é removido depois de ZIP gerado, armazenado, reaberto, validado e e-mail enviado.

## Fluxo de exclusão de tenant

`TenantService` deixou de executar cascade síncrono direto. O endpoint passa a retornar `202 Accepted`, lista os produtos ativos do tenant e chama `TenantProductExportPort` para disparar export/delete por produto. Quando o último produto é removido, `TenantExportRemovalPort` executa a limpeza final do tenant.

## Fluxo de exportação ZIP

1. `ProductExportSerializer` serializa produto, tenant, módulos, content, pages, forms, submissions, assets, graph, assignments e auditoria.
2. `ExportZipBuilder` cria ZIP temporário em `aegis.export.temp-dir` (`./data/exports/tmp` por padrão).
3. Assets físicos são lidos via `ProductExportStoragePort.openAsset(...)` e escritos no ZIP por streaming.
4. `ExportStorageService` move o ZIP para área permanente de exports.
5. `ExportIntegrityService` reabre o arquivo armazenado por `ExportStorageService.openStream(token)` e valida entradas obrigatórias, assets esperados e tamanho maior que zero.

## Estrutura do ZIP

- `manifest.json`
- `product.json`
- `modules.json`
- `content/entries.json`
- `pages/pages.json`
- `forms/forms.json`
- `forms/submissions.json`
- `assets/metadata.json`
- `assets/files/{category}/{filename}`
- `knowledge-graph/nodes.json`
- `knowledge-graph/edges.json`
- `users/assignments.json`
- `audit/events.json`

## Manifest

O `manifest.json` contém `aegisExportVersion`, `exportedAt`, `trigger`, dados do produto, dados do tenant, `requestedBy`, `entityCounts` e `downloadExpiresAt`.

## Download por token temporário

`GET /api/v1/exports/{tokenId}/download` não exige JWT. O token é a autorização. O endpoint valida existência, status `AVAILABLE` e expiração, registra `downloadedAt` apenas no primeiro download e retorna `Content-Disposition: attachment` com streaming do ZIP.

## Segurança e autorização

`DELETE /products/{productId}` aceita `SUPER_ADMIN`, `TENANT_ADMIN` com membership ativa no tenant do produto ou usuário com `ProductAssignment` `PRODUCT_MANAGER`. Confirmação textual incorreta retorna 400. Produto já em exclusão retorna 409.

`GET /exports/{tokenId}/download` foi liberado em `SecurityConfig` sem JWT, mas protegido por token temporário e status/expiração no banco.

## E-mail de exportação

Template escolhido: `infra/keycloak/themes/aegis/email/html/productExport.ftl`, mantendo os templates de e-mail do Aegis no mesmo diretório já usado por convites e ações de identidade. O ZIP não é anexado; o e-mail contém link temporário de download.

## Storage dos exports

- Local: `exports/{tokenId}/{filename}` fora da árvore de produto.
- S3: `aegis/pms/exports/{tokenId}/{filename}`.

O download usa o mesmo caminho armazenado no `ExportToken`, e a validação de integridade reabre esse caminho antes do delete.

## Executor assíncrono

Bean: `exportTaskExecutor`.

- Thread name prefix: `aegis-export-`
- Core pool size: `2`
- Max pool size: `4`
- Queue capacity: `25`

Justificativa: export/delete é operação potencialmente pesada de I/O e banco. O pool pequeno limita concorrência para não saturar storage/DB, mas permite paralelismo básico em deleções de tenant com múltiplos produtos. A fila comporta picos moderados sem bloquear o request HTTP.

## Cleanup de exports expirados

`ExportCleanupJob.expireExports()` roda com `@Scheduled(cron = "0 0 3 * * *")`, localiza tokens expirados, remove o ZIP do storage e marca o token como `EXPIRED`.

## Tratamento de falhas

- Falha antes do e-mail: produto marca `EXPORT_FAILED`, dados são preservados e o sistema tenta enviar e-mail de falha.
- Falha no e-mail pronto: tratada como falha de exportação; produto permanece preservado como `EXPORT_FAILED`.
- Falha no delete depois do export válido: produto marca `DELETE_FAILED`, sem retry silencioso automático.
- Falha no envio do e-mail de falha: apenas logada para não mascarar a falha original.

## Auditoria

Após delete bem-sucedido, `ExportAndDeleteService` registra `USER_DATA_EXPORTED_AND_PRODUCT_DELETED` via `AuditService`, com `exportTokenCreated=true` no diff.

## Cleanup de stubs órfãos

Nenhum stub órfão foi identificado ou removido nesta sprint.

## Bruno

Criada a pasta `bruno/28-backup-exportacao-exclusao` com:

- Login local da própria pasta para execução isolada.
- Download por token inexistente retorna 404.
- Criação de tenant/produto descartável.
- `DELETE /products/{productId}` com `confirmationText` errado retorna 400.
- `DELETE /products/{productId}` com confirmação correta retorna 202.

Validação isolada executada:

```bash
cd bruno
npx @usebruno/cli run 28-backup-exportacao-exclusao --env local
```

Resultado: **PASS**, 6 requests executados, 6 aprovados, 11/11 testes.

## Validação Maven

Executado:

```bash
cd backend
mvn clean verify
```

Resultado: **BUILD SUCCESS**, 1454 testes, 0 failures, 0 errors, 0 skipped.

## JaCoCo e Spring Modulith

JaCoCo aprovado: `All coverage checks have been met`.

Spring Modulith aprovado pela suíte completa, incluindo `ModulithArchitectureTest`.

## SonarQube for IDE

Rodada SonarQube for IDE corrigida em 2026-07-03 sem `@SuppressWarnings` ou `NOSONAR`:

- Removidos `throws` desnecessários em testes.
- Removido import morto em `ExportTokenRepositoryTest`.
- `ExportZipBuilder` passou a usar construtor público com `@Autowired` e temp dir configurável.
- `ProductExportSerializer` removeu campo/import mortos, substituiu reflexão por `instanceof PGobject`, usou API `java.time`, reduziu métodos com parâmetros excessivos e restringiu SQL a enums com queries fixas.
- `ProductExportSerializerTest` ajustou lambdas de `assertThatThrownBy`.

A reanálise final do painel SonarQube for IDE é local da IDE; não há runner CLI do SonarQube for IDE neste ambiente.

## Decisões técnicas tomadas

1. Delete só ocorre depois do ZIP armazenado e validado no mesmo caminho usado pelo download.
2. O arquivo temporário não é fonte de verdade para exclusão.
3. Export local/S3 passa por porta pública do módulo `asset`, preservando Spring Modulith.
4. Tenant chama `TenantProductExportPort`, evitando dependência direta de internals de `product`.
5. `productExport.ftl` fica em `infra/keycloak/themes/aegis/email/html`.
6. O driver PostgreSQL passou para escopo padrão no Maven porque o código de produção referencia `PGobject`.
7. Construtores públicos com construtores auxiliares de teste foram anotados com `@Autowired` quando necessário.

## Divergências de caminhos da documentação

O prompt de finalização citava `README.md` na raiz, `docs/architecture/adr/` e `docs/architecture/blueprints/`. No repositório atual, os equivalentes reais usados na leitura foram `docs/README.md`, `docs/adr/` e os blueprints em `docs/implementation/*blueprint*.md`. A divergência foi registrada aqui conforme solicitado no rito de fechamento.

## Retrofits pendentes

- Definir políticas operacionais de retry/backoff para SMTP/S3, se necessário.
- Avaliar política de retenção configurável por tenant/plano em sprint futura.

## Critérios de aceite atendidos

- [x] `DELETE /products/{productId}` existe e retorna 202.
- [x] `DELETE /tenants/{tenantId}` foi retrofitado para fluxo assíncrono com backup por produto.
- [x] ZIP inclui JSONs por domínio e arquivos físicos de assets por streaming.
- [x] Download por token temporário faz streaming público e registra `downloadedAt`.
- [x] Delete só ocorre depois do ZIP armazenado, reaberto e validado como íntegro.
- [x] Falha antes do delete marca `EXPORT_FAILED` e preserva dados.
- [x] Falha durante delete marca `DELETE_FAILED`.
- [x] E-mail validado no MailHog na porta 8025.
- [x] Bruno da pasta 28 passou isolado.
- [x] `mvn clean verify` com `BUILD SUCCESS`, JaCoCo e Spring Modulith aprovados.
