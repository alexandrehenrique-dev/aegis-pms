# Etapa 11 — Domínio `asset` (biblioteca de mídia, upload, metadados, tags)

> Cole este arquivo inteiro numa conversa nova do GPT. Pré-requisito: etapa 06 concluída. Usa o package `asset` já previsto na etapa 04.

## Contexto fixo

Telas `AssetLibrary`, `AssetUploadScreen`, `AssetDetail`, `AssetMetadataForm`, `AssetTagManager`, `AssetUsageScreen`, `AssetPicker` — hoje 100% mock. Payloads conforme `docs/trace/00_endpoints_esperados.md` (Seção B.2), espelhando `frontend/src/domains/assets/contracts/responses.ts`.

## Objetivo

Upload e gestão de assets (arquivos), com metadados editáveis, tags do produto, e rastreamento de onde cada asset é usado.

## Tarefas

### A. Entidades

**Asset**: `id`, `tenantId`, `productId`, `name`, `friendlyName`, `altText`, `caption`, `credit`, `type` (mime/categoria), `sizeBytes`, `status`, `storageKey` (referência ao arquivo físico — local em disco ou bucket, decisão do GPT, documentar a escolha), `uploadedBySubject`, `createdAt`, `updatedAt`.

**AssetTag**: `id`, `productId`, `name` (único por produto).

**AssetTagAssignment**: tabela de junção `assetId` ↔ `assetTagId`.

**AssetUsage**: `id`, `assetId`, `usedInType` (ex.: `CONTENT`, `SEO`), `usedInRefId`, `usedInLabel` — para responder "onde este asset está sendo usado".

### B. Endpoints

```txt
GET    /api/v1/products/{productId}/assets
POST   /api/v1/products/{productId}/assets                  (multipart/form-data)
GET    /api/v1/products/{productId}/assets/{assetId}
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

- Upload valida tipo/tamanho (definir limite razoável, ex. 25MB para imagem/documento, configurável).
- Excluir um asset com `usage` não vazio exige confirmação explícita no payload (`{ force: true }`) — replicando o cuidado já aplicado em exclusão de tenant na etapa 09.
- Tag duplicada (mesmo nome, mesmo produto) é rejeitada com 409; "mesclar tags" (ação do frontend `AssetTagManager.tsx`) é implementável como: criar a tag destino se não existir, mover todas as `AssetTagAssignment` da tag origem para a destino, excluir a tag origem.

## Critérios de aceite

- [ ] Upload de asset funciona e retorna o `AssetSummary` criado.
- [ ] Metadados podem ser editados.
- [ ] Tags podem ser criadas, usadas em assets, e removidas.
- [ ] Excluir asset em uso sem `force` é rejeitado; com `force: true` funciona.
- [ ] `usage` reflete corretamente onde o asset está referenciado.

## Validação

```bash
curl -X POST http://localhost:8080/api/v1/products/<productId>/assets \
  -H "Authorization: Bearer $TOKEN" -F "file=@./teste.png" -F "friendlyName=Teste"

curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/products/<productId>/asset-tags

curl -X POST http://localhost:8080/api/v1/products/<productId>/asset-tags \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '"institucional"'
```

## Commit sugerido

```bash
git add backend/
git commit -m "feat(backend): dominio asset com upload, metadados e tags"
```
