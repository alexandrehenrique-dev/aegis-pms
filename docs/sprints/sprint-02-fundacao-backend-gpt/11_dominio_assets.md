# Etapa 11 — Domínio `asset` (biblioteca de mídia, upload, metadados, tags)

> Cole este arquivo inteiro numa conversa nova do GPT. Pré-requisito: etapa 06 concluída. Usa o package `asset` já previsto na etapa 04.

## Contexto fixo

Telas `AssetLibrary`, `AssetUploadScreen`, `AssetDetail`, `AssetMetadataForm`, `AssetTagManager`, `AssetUsageScreen`, `AssetPicker` — hoje 100% mock. Payloads conforme `docs/trace/00_endpoints_esperados.md` (Seção B.2), espelhando `frontend/src/domains/assets/contracts/responses.ts`.

## Objetivo

Upload e gestão de assets (arquivos), com metadados editáveis, tags do produto, e rastreamento de onde cada asset é usado.

## Tarefas

### A. Entidades

**Asset**: `id` (UUID — é o identificador universal usado por qualquer service que referencia um asset, ver Seção D), `tenantId`, `productId`, `name`, `friendlyName`, `altText`, `caption`, `credit`, `type` (mime/categoria), `category` (`"image"|"pdf"|"audio"|"video"|"document"`, derivado de `type` na criação — usado para escolher a subpasta no storage local e para o limite de tamanho por categoria), `sizeBytes`, `status`, `storageProvider` (`"local"|"s3"`, herdado de `Product.assetStorageStrategy` no momento do upload — **não muda retroativamente se o produto trocar de estratégia depois**, ver Seção D.4), `storageKey` (caminho relativo no provider — nunca um caminho absoluto de disco nem uma URL pronta; a resolução para URL acontece em runtime, Seção D.2), `uploadedBySubject`, `createdAt`, `updatedAt`.

**AssetTag**: `id`, `productId`, `name` (único por produto).

**AssetTagAssignment**: tabela de junção `assetId` ↔ `assetTagId`.

**AssetUsage**: `id`, `assetId`, `usedInType` (ex.: `CONTENT`, `SEO`), `usedInRefId`, `usedInLabel` — para responder "onde este asset está sendo usado".

### B. Endpoints

```txt
GET    /api/v1/products/{productId}/assets
POST   /api/v1/products/{productId}/assets                  (multipart/form-data)
GET    /api/v1/products/{productId}/assets/{assetId}
GET    /api/v1/assets/{assetId}/resolve                     (ver Seção D.2 — resolução universal por UUID, fora do prefixo de produto porque qualquer domínio pode chamar com só o assetId)
DELETE /api/v1/products/{productId}/assets/{assetId}
PUT    /api/v1/products/{productId}/assets/{assetId}/metadata
GET    /api/v1/products/{productId}/assets/{assetId}/usage
GET    /api/v1/products/{productId}/asset-tags
POST   /api/v1/products/{productId}/asset-tags
DELETE /api/v1/products/{productId}/asset-tags/{tag}
```

Payloads:

```ts
type AssetSummary = {
  name: string; type: string; size: string; status: string;
  tags: string; usage: string; uploadedAt: string;
};
// GET /assets → AssetSummary[]

type UpdateAssetMetadataRequest = {
  friendlyName: string; altText: string; caption: string; credit: string; tags: string;
};
// PUT /assets/{assetId}/metadata

// GET /asset-tags → string[]
// POST /asset-tags body: string (nome da tag) → string[] atualizado
```

### C. Regras de negócio

- **Asset não é só imagem (confirmado pela Sprint 13 do frontend):** `type` já é genérico (mime/categoria) — o upload precisa aceitar PDF, áudio (mp3/wav), vídeo e documentos de verdade, não só imagem. Casos confirmados nos contratos de negócio: currículo em PDF (Alexandre Dev, WikiDev), upload de currículo de candidato (Conecta Talentos, registrado em B.10), upload de música avulsa para o bloco de áudio (Loki), vídeo de apresentações/shows (Maestro Beton, `docs/AEGIS_PMS_V1.md` §17 — bloco `video`/`video-gallery`, etapa 21). Limite de tamanho varia por categoria — tabela de referência (configurável via `application.yml`, nunca hardcoded):

  | `category` | Mime types aceitos | Limite default |
  |---|---|---|
  | `image` | `image/jpeg`, `image/png`, `image/webp`, `image/svg+xml` | 5MB |
  | `pdf` | `application/pdf` | 15MB |
  | `audio` | `audio/mpeg`, `audio/wav`, `audio/ogg` | 30MB |
  | `video` | `video/mp4`, `video/webm`, `video/quicktime` | 250MB |
  | `document` | demais mime types de documento (`.docx`, `.xlsx`, `.csv`, ...) | 15MB |

  **`video` precisa de configuração extra**: 250MB é muito acima do default do Spring (`spring.servlet.multipart.max-file-size`/`max-request-size`, default 1MB) — esta etapa **precisa** elevar os dois para pelo menos o limite da categoria `video` na configuração do `application.yml` (etapa 04), senão o upload de vídeo falha silenciosamente com 413 antes mesmo de chegar no `AssetService`. Documentar a escolha do limite exato no código (comentário citando esta tabela).
- Upload valida tipo/tamanho contra a tabela acima — rejeitar com 400/413 se o mime type não estiver na lista da categoria ou o arquivo exceder o limite.
- Excluir um asset com `usage` não vazio exige confirmação explícita no payload (`{ force: true }`) — replicando o cuidado já aplicado em exclusão de tenant na etapa 09.
- Tag duplicada (mesmo nome, mesmo produto) é rejeitada com 409; "mesclar tags" (ação do frontend `AssetTagManager.tsx`) é implementável como: criar a tag destino se não existir, mover todas as `AssetTagAssignment` da tag origem para a destino, excluir a tag origem.

### D. Estratégia de armazenamento (escolhida na criação do produto — Sprint 13 do frontend)

Decisão de produto: o **storage de assets é escolhido por produto**, no momento da criação (campo `Product.assetStorageStrategy`, etapa 06), com default `"local"`. Isso precisa ser modelado como uma abstração desde já — não como um `if (provider === "s3")` espalhado pelo código — porque a estratégia pode mudar de novo no futuro (outro provedor, ex. Cloudflare R2/GCS) sem reescrever o domínio inteiro.

#### D.1 Strategy pattern: `StorageProvider`

Interface única, duas implementações iniciais:

```txt
interface StorageProvider {
  provisionProductFolders(productId): void      // só relevante para "local"; no-op para "s3"
  store(productId, category, file): storageKey
  resolve(storageKey): { url: string; expiresAt?: DateTime }
  delete(storageKey): void
}
```

- **Convenção de caminho (decisão confirmada — namespace `aegis/pms` fixo no código, não no env var, para o mesmo volume/bucket poder ser compartilhado por outros produtos BYOP no futuro sem reconfigurar nada):**
  ```txt
  {AEGIS_STORAGE_LOCAL_PATH}/aegis/pms/{tenantId}/{productId}/{category}/{filename}
  ```
  Exemplo real: `/app/assets/aegis/pms/{tenantId}/{productId}/pdf/curriculo-alexandre.pdf`.
- **`LocalStorageProvider`**: grava em `${AEGIS_STORAGE_LOCAL_PATH}/aegis/pms/{tenantId}/{productId}/{category}/{filename}`, onde `filename` é o **nome original do arquivo, sanitizado** (sem espaços/acentos/caracteres especiais — `slugify`, mantendo a extensão). `resolve()` retorna uma URL servida pelo próprio backend (ex.: `/api/v1/assets/{assetId}/file`, autenticada e com checagem de membership — nunca um caminho de disco exposto direto).
  - **Colisão de nome**: como o nome original é preservado (em vez de usar `assetId` como nome de arquivo), dois uploads com o mesmo nome no mesmo produto/categoria colidiriam. Regra: se `filename` já existe naquela pasta, anexar um sufixo curto antes da extensão (`curriculo-alexandre.pdf` → `curriculo-alexandre-2.pdf` → `-3.pdf`...), nunca sobrescrever silenciosamente. O `assetId` (UUID) continua sendo a chave de busca/exclusão/auditoria — o nome do arquivo em disco é só para o caminho ficar legível para quem inspeciona o servidor diretamente, não é usado pela API para localizar o asset.
- **`S3StorageProvider`**: grava no bucket configurado, `storageKey` = chave do objeto S3 (`aegis/pms/{tenantId}/{productId}/{category}/{filename}` — sem o prefixo `{AEGIS_STORAGE_LOCAL_PATH}`/`/app`, que é só o ponto de montagem do disco local; no S3 a chave já começa no namespace `aegis/pms`). Mesma regra de colisão de nome do `LocalStorageProvider`. `resolve()` gera uma **URL pré-assinada com TTL curto** (ex.: 15 minutos) — nunca uma URL pública permanente, mesmo que o bucket seja público, para manter o mesmo nível de controle de acesso que o `local` tem.
- `AssetStorageProvisioningService.provisionFor(productId, strategy)` (chamado pela etapa 06 na criação do produto): se `strategy === "local"`, cria a estrutura de pastas do produto — uma subpasta por categoria (`image/`, `pdf/`, `audio/`, `video/`, `document/`) — antes de qualquer upload acontecer. Se `strategy === "s3"`, não faz nada no provisionamento (o bucket já existe, é compartilhado entre produtos, isolado só pelo prefixo da chave).

#### D.2 Resolução universal por UUID

Todo asset é referenciado externamente só pelo seu `id` (UUID) — nenhum service de outro domínio (content, pages, forms) guarda URL nem caminho, só `assetId`. Para obter a URL de uso:

```txt
GET /api/v1/assets/{assetId}/resolve
```
```ts
type ResolvedAsset = { id: string; url: string; expiresAt?: string; contentType: string };
```

Implementação: busca o `Asset` pelo `id`, confirma que o usuário autenticado tem membership no `tenantId` daquele asset (nunca resolver asset de tenant alheio — mesma regra de não-revelar-existência das demais entidades), lê `storageProvider`, instancia o `StorageProvider` correspondente, chama `.resolve(storageKey)`. O consumidor da API nunca precisa saber se o arquivo está em disco local ou em um bucket — só recebe `url` (e, se for S3, `expiresAt`, para saber quando precisa resolver de novo).

#### D.3 Variáveis de ambiente (complementa a etapa 01)

```env
AEGIS_STORAGE_LOCAL_PATH=./data/assets   # já existe na etapa 01 — usado pelo LocalStorageProvider; código sempre acrescenta /aegis/pms
AEGIS_STORAGE_S3_BUCKET=
AEGIS_STORAGE_S3_REGION=
AEGIS_STORAGE_S3_ACCESS_KEY_ID=
AEGIS_STORAGE_S3_SECRET_ACCESS_KEY=
AEGIS_STORAGE_S3_PRESIGNED_URL_TTL_SECONDS=900
```
Credenciais S3 nunca vão para `application.yml` versionado — só via variável de ambiente/secret do orquestrador (mesma regra já aplicada a `KEYCLOAK_ADMIN_PASSWORD`).

#### D.4 Mudança de estratégia depois de criado o produto

Fora de escopo desta etapa **implementar** a migração, mas a regra precisa existir desde já para não gerar inconsistência: se o `assetStorageStrategy` do produto for alterado depois (ex.: começou `local`, virou `s3`), **assets já existentes mantêm seu `storageProvider` original** — só uploads novos seguem a estratégia nova. Migrar os assets antigos para o novo provider é uma operação explícita e separada (futura sprint de "migração de storage"), nunca implícita na troca de configuração.

#### D.5 Riscos mapeados

| Risco | Mitigação |
|---|---|
| **Local**: disco do servidor enche sem aviso | monitorar uso de disco via `/actuator` customizado ou alerta externo; não é parte desta etapa, mas registrar a necessidade |
| **Local**: não escala horizontalmente (múltiplas instâncias do backend não compartilham disco automaticamente) | documentar que `local` exige volume compartilhado (NFS/EFS) se o backend rodar em mais de uma instância — ou aceitar `local` só para single-instance/MVP, migrando para S3 antes de escalar horizontalmente |
| **Local**: path traversal (`storageKey` malicioso tipo `../../etc/passwd`) | `storageKey` é sempre montado no backend (`aegis/pms/{tenantId}/{productId}/{category}/{filename}`), nunca aceito pronto do cliente; `filename` (nome original do arquivo, mantido por decisão de produto — ver D.1) passa por sanitização obrigatória antes de virar parte do caminho: remover `..`, `/`, `\`, caracteres de controle, normalizar acentuação/espaços (`slugify`) — rejeitar o upload (400) se, depois de sanitizado, o nome ficar vazio |
| **S3**: custo cresce com volume, e credenciais mal configuradas vazam acesso ao bucket | least-privilege IAM (a credencial do backend só pode `PutObject`/`GetObject`/`DeleteObject` no prefixo do bucket usado pelo Aegis, nunca acesso de bucket inteiro a outros recursos) |
| **S3**: URL pré-assinada vazada continua válida até expirar | TTL curto (Seção D.3) e nunca logar a URL completa (só o `assetId`) nos logs de `logApiCall`/auditoria |
| **Qualquer provider**: troca de estratégia no meio do caminho gera assets "órfãos" de proveniência (alguns local, outros S3, sem ninguém migrar) | regra explícita da Seção D.4 — aceitar a convivência dos dois providers por produto em vez de fingir que não acontece |
| **UUID universal**: um `assetId` válido de outro tenant sendo testado por tentativa e erro | checagem de membership obrigatória em `resolve()` antes de gerar qualquer URL — 404 genérico, nunca 403 (mesma regra de não revelar existência) |

**Module-gating** (`00_padrao_qualidade_e_arquitetura.md`, Seção 9.2): `AssetController` anotado com `@RequireModule(ModuleKey.ASSETS)` — produto com módulo `ASSETS` desabilitado retorna 403 `MODULE_DISABLED` em qualquer endpoint de upload/listagem/edição (não se aplica a `GET /assets/{assetId}/resolve`, que é resolvido por `assetId` global, sem `productId` no path — a checagem de módulo nesse endpoint específico é dispensada, só a checagem de tenant/membership da tabela acima vale).

#### D.6 Melhorias futuras (registrar, não implementar agora)

- CDN na frente do `S3StorageProvider` (CloudFront/Cloudflare) para reduzir latência e custo de egress — natural quando o volume justificar.
- Geração de thumbnails/variantes (ex.: imagem em 3 tamanhos) no upload, guardando `storageKey` por variante — hoje só o arquivo original é guardado.
- Ferramenta de migração entre providers (mencionada na Seção D.4) como comando administrativo, não automática.

### E. Padrão de qualidade e entrega (obrigatório)

> Resumo — detalhe completo em `00_padrao_qualidade_e_arquitetura.md`.

- **Java 25** / **Spring Boot 4.1.x**. Javadoc obrigatório na interface e em todo método de `AssetRepository`, `AssetTagRepository`, `AssetTagAssignmentRepository`, `AssetUsageRepository`. Mapper via MapStruct (`AssetMapper`). 100% de cobertura nas classes funcionais — incluindo `LocalStorageProvider`, `S3StorageProvider` e `AssetStorageProvisioningService` (Seção D), que são exatamente o tipo de classe com lógica que mais precisa de teste (e mais fácil de esquecer, por não serem `Service`/`Controller` típicos).
- Entregar em rodadas:
  1. `Asset`, `AssetTag`, `AssetTagAssignment`, `AssetUsage` (entities) + seus repositories + testes `@DataJpaTest`.
  2. `AssetMapper` (MapStruct) + testes de mapper.
  3. `LocalStorageProvider`, `S3StorageProvider`, `AssetStorageProvisioningService`, `AssetService` (upload, metadados, tags, merge, `resolve()`) + testes com mocks — cada risco da tabela D.5 com teste correspondente (ex.: filename sanitizado rejeita `../`, colisão de nome gera sufixo, `resolve()` de outro tenant retorna 404).
  4. `AssetController` (endpoints da Seção B) + testes `@WebMvcTest` + validação via `curl`.

## Critérios de aceite

- [ ] Upload de asset funciona e retorna o `AssetSummary` criado.
- [ ] Upload de vídeo (`category: "video"`) até 250MB funciona; `spring.servlet.multipart.max-file-size`/`max-request-size` configurados acima desse limite (não o default de 1MB do Spring).
- [ ] Upload com mime type fora da lista da categoria, ou acima do limite de tamanho, é rejeitado (400/413).
- [ ] Metadados podem ser editados.
- [ ] Tags podem ser criadas, usadas em assets, e removidas.
- [ ] Excluir asset em uso sem `force` é rejeitado; com `force: true` funciona.
- [ ] `usage` reflete corretamente onde o asset está referenciado.
- [ ] Criar produto com `assetStorageStrategy: "local"` provisiona a estrutura de pastas por categoria (`image/pdf/audio/video/document`) automaticamente.
- [ ] Upload de asset grava `storageProvider` igual ao `assetStorageStrategy` do produto no momento do upload.
- [ ] `GET /assets/{assetId}/resolve` retorna `url` funcional tanto para `local` quanto para `s3` (testar os dois providers), e nunca expõe caminho de disco ou credencial.
- [ ] `resolve()` de asset de outro tenant retorna 404, não 403.
- [ ] Trocar `assetStorageStrategy` do produto não altera o `storageProvider` de assets já existentes.
- [ ] Produto com módulo `ASSETS` desabilitado retorna 403 `MODULE_DISABLED` nos endpoints de upload/listagem/edição.
- [ ] `mvn clean verify` confirma 100% de cobertura nas classes elegíveis desta etapa, incluindo os `StorageProvider`s (JaCoCo).
- [ ] Todos os repositories desta etapa têm Javadoc na interface e em todo método.

## Validação

> **Entrega via collection Postman, não só curl** (ver `00_padrao_qualidade_e_arquitetura.md`, Seção 11). Os `curl` abaixo são a especificação exata de cada request — adicione-os à pasta desta etapa em `aegis-postman-collection.json` (collection cumulativa, autenticação via `{{token}}` herdado da pasta "Auth") e devolva o JSON completo atualizado para download.

```bash
curl -X POST http://localhost:8080/api/v1/products/<productId>/assets \
  -H "Authorization: Bearer $TOKEN" -F "file=@./teste.png" -F "friendlyName=Teste"

curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/products/<productId>/asset-tags

curl -X POST http://localhost:8080/api/v1/products/<productId>/asset-tags \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '"institucional"'

curl -X POST http://localhost:8080/api/v1/products/<productId>/assets \
  -H "Authorization: Bearer $TOKEN" -F "file=@./curriculo.pdf" -F "friendlyName=Currículo PT-BR"
# esperado: asset criado com category "pdf", storageProvider igual ao assetStorageStrategy do produto
# se strategy "local", o arquivo deve existir em:
# ${AEGIS_STORAGE_LOCAL_PATH}/aegis/pms/<tenantId>/<productId>/pdf/curriculo.pdf

curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/assets/<assetId>/resolve
# esperado: { "id": "...", "url": "...", "contentType": "application/pdf" }
```

## Commit sugerido

```bash
git add backend/
git commit -m "feat(backend): dominio asset com upload, metadados, tags e strategy pattern de storage (local/s3)"
```
