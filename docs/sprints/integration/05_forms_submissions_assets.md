# Sprint de Integração 05 — Forms, Submissions e Assets

> **Pré-requisito:** Sprint 03 concluída (produto ativo com UUID real). Sprint 02 (multipart no `apiClient`) deve estar aplicada.
>
> **Foco:** formulários, submissões e upload de assets — o único domínio com upload binário real.
>
> **Branch:** `integration/05-forms-submissions-assets`

---

## A. `formsService.ts` — corrigir paths com prefixo de produto

### A.1 — `getForm(id)` — adicionar prefixo de produto

```ts
// ANTES:
async getForm(id: string) { return apiClient.get(`/forms/${id}`); }

// DEPOIS:
async getForm(productId: string, formId: string): Promise<FormSummary | undefined> {
  if (IS_API_MODE) return apiClient.get<FormSummary>(`/products/${productId}/forms/${formId}`);
  return formsStore.find(f => f.id === formId);
}
```

Atualizar todos os chamadores que passam apenas `id`.

### A.2 — `getFormFields(id)` — adicionar prefixo de produto

```ts
// ANTES:
async getFormFields(id: string) { return apiClient.get(`/forms/${id}/fields`); }

// DEPOIS:
async getFormFields(productId: string, formId: string): Promise<FormField[]> {
  if (IS_API_MODE) return apiClient.get<FormField[]>(`/products/${productId}/forms/${formId}/fields`);
  return fieldsByFormId[formId] ?? [];
}
```

### A.3 — `listForms` — garantir path com produto

`listForms(productSlug?)` já tem `IS_API_MODE` e usa `/products/${productSlug}/forms` quando há slug. Substituir `productSlug` por `productId` (UUID) — o argumento sem slug não deve acontecer em modo API:

```ts
async listForms(productId: string): Promise<ListFormsResponse> {
  if (IS_API_MODE) return apiClient.get<ListFormsResponse>(`/products/${productId}/forms`);
  const { effectiveProduct } = authStore.getState();
  return formsStore.filter(f => f.productSlug === effectiveProduct?.key);
}
```

### A.4 — `saveFormFields(formId, fields)` — corrigir path

Verificar se existe `saveFormFields` — se chama `PUT /forms/{id}/fields`, corrigir para `PUT /products/{productId}/forms/{formId}/fields`.

### A.5 — `saveDelivery(formId, delivery)` — corrigir path

Se chama `PUT /forms/{id}/delivery`, corrigir para `PUT /products/{productId}/forms/{formId}/delivery`.

### A.6 — `listSubmissions(formId)` — corrigir path

```ts
// ANTES:
async listSubmissions(formId: string) { return apiClient.get(`/forms/${formId}/submissions`); }

// DEPOIS:
async listSubmissions(productId: string, formId: string): Promise<ListSubmissionsResponse> {
  if (IS_API_MODE) return apiClient.get<ListSubmissionsResponse>(`/products/${productId}/forms/${formId}/submissions`);
  return submissionsStore;
}
```

### A.7 — `testSubmit(formId)` — placeholder literal

```ts
// ANTES (literal {formId}):
async testSubmit(formId: string) { return apiClient.post('/forms/{formId}/test-submit'); }

// DEPOIS:
async testSubmit(productId: string, formId: string): Promise<void> {
  if (IS_API_MODE) return apiClient.post(`/products/${productId}/forms/${formId}/test-submit`);
  logApiCall('POST', `/api/v1/products/${productId}/forms/${formId}/test-submit`);
}
```

---

## B. `assetsService.ts` — substituir todos os placeholders `{productId}`

Todos os métodos de assets usam o literal `{productId}` (nunca substituído). Cada método precisa receber `productId: string` como argumento:

### B.1 — `listAssets(productId)`

Já tem argumento opcional `productId`. Tornar obrigatório:

```ts
async listAssets(productId: string): Promise<ListAssetsResponse> {
  if (IS_API_MODE) return apiClient.get<ListAssetsResponse>(`/products/${productId}/assets`);
  return assetsStore;
}
```

### B.2 — `listTags(productId)`, `createTag(productId, name)`, etc.

```ts
async listTags(productId: string): Promise<ListAssetTagsResponse> {
  if (IS_API_MODE) return apiClient.get<ListAssetTagsResponse>(`/products/${productId}/assets/tags`);
  return assetTagsStore;
}
async createTag(productId: string, name: string): Promise<void> {
  if (IS_API_MODE) return apiClient.post(`/products/${productId}/assets/tags`, { name });
  // ...mock
}
async renameTag(productId: string, oldName: string, newName: string): Promise<void> {
  if (IS_API_MODE) return apiClient.put(`/products/${productId}/assets/tags/${encodeURIComponent(oldName)}`, { newName });
  // ...mock
}
async removeTag(productId: string, name: string): Promise<void> {
  if (IS_API_MODE) return apiClient.delete(`/products/${productId}/assets/tags/${encodeURIComponent(name)}`);
  // ...mock
}
async mergeTags(productId: string, tagsToMerge: string[], into: string): Promise<void> {
  if (IS_API_MODE) return apiClient.post(`/products/${productId}/assets/tags/merge`, { tagsToMerge, into });
  // ...mock
}
```

### B.3 — `saveMetadata(productId, assetId, metadata)`

```ts
// ANTES (dois placeholders literais):
async saveMetadata() { return apiClient.put('/products/{productId}/assets/{assetId}/metadata'); }

// DEPOIS:
async saveMetadata(productId: string, assetId: string, metadata: Record<string, unknown>): Promise<void> {
  if (IS_API_MODE) return apiClient.put(`/products/${productId}/assets/${assetId}/metadata`, metadata);
  logApiCall('PUT', `/api/v1/products/${productId}/assets/${assetId}/metadata`, metadata);
}
```

### B.4 — `archiveAsset(productId, assetId)`

```ts
// ANTES (literal {productId}, usa `name` como id):
async archiveAsset(name: string) { return apiClient.post(`/products/{productId}/assets/${name}/archive`); }

// DEPOIS:
async archiveAsset(productId: string, assetId: string): Promise<void> {
  if (IS_API_MODE) return apiClient.post(`/products/${productId}/assets/${assetId}/archive`);
  const a = assetsStore.find(x => x.name === assetId);
  if (a) a.status = 'arquivado';
}
```

### B.5 — `upload(file, productId)` — usar `apiClient.upload()`

```ts
async upload(file: File, productId: string): Promise<{ assetId: string }> {
  if (IS_API_MODE) {
    const fd = new FormData();
    fd.append('file', file);
    // Sprint 02 (E.1) adicionou apiClient.upload() que NÃO força Content-Type
    return apiClient.upload<{ assetId: string }>(`/products/${productId}/assets`, fd);
  }
  const assetId = `mock-asset-${Date.now()}`;
  assetsStore.push({
    name: file.name, type: inferAssetType(file), size: formatSize(file.size),
    status: 'ativo', tags: [], usage: 0, uploadedAt: new Date().toISOString(),
  });
  return { assetId };
}
```

---

## C. Download de asset — endpoint de download

`GET /api/v1/assets/{assetId}/download` deve retornar um `302 redirect` para a URL do arquivo (ou o próprio arquivo em stream). O frontend exibe como link `href` que abre em nova aba — não usa o `apiClient`, é um link HTML direto:

```tsx
<a href={`${BASE_URL}/assets/${assetId}/download`} target="_blank" rel="noreferrer">
  Baixar
</a>
```

Em modo mock: construir uma URL de object URL (`URL.createObjectURL`) para o arquivo carregado, ou exibir o botão desabilitado com texto "indisponível no mock".

---

## D. FormDelivery — Telegram

`formsService.saveDelivery(formId, delivery)` persiste configuração de canais incluindo Telegram. O backend valida que `chatId` e `botToken` estão presentes e testa o envio (etapa 30 D.4). Se a validação falhar, backend retorna `400` com `{"error": "telegram_invalid_credentials"}`.

Frontend deve capturar o `400` e exibir toast de erro com mensagem amigável.

---

## E. Critérios de aceite

- [ ] `GET /api/v1/products/{UUID}/forms` retorna formulários reais do produto.
- [ ] `POST /api/v1/products/{UUID}/forms` cria formulário — aparece na lista.
- [ ] `PUT /api/v1/products/{UUID}/forms/{formId}/fields` salva campos do FormBuilder.
- [ ] `PUT /api/v1/products/{UUID}/forms/{formId}/delivery` salva configuração de entrega.
- [ ] `GET /api/v1/products/{UUID}/forms/{formId}/submissions` retorna submissões reais.
- [ ] `POST /api/v1/products/{UUID}/assets` (multipart) faz upload — asset aparece na lista com nome e tamanho reais.
- [ ] Link de download de asset aponta para `/api/v1/assets/{id}/download` e abre o arquivo em nova aba.
- [ ] Nenhum placeholder literal `{productId}`, `{formId}`, `{assetId}` restante nos services de forms e assets.
- [ ] Modo mock não regrediu.
- [ ] `npm run typecheck` — zero erros.

---

## F. Commit sugerido

```bash
git add frontend/src/domains/forms/ frontend/src/domains/assets/
git commit -m "feat(integration): forms, submissions e assets integrados; upload multipart funcional"
```
