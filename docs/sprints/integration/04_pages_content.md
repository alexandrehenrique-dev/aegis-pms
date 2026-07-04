# Sprint de Integração 04 — Pages, Content editorial e workflow

> **Pré-requisito:** Sprint 03 concluída (produto ativo no contexto com UUID real).
>
> **Foco:** ligar o editor de páginas e o catálogo de conteúdo ao backend real. Estas são as telas mais usadas pelo `EDITOR` — qualquer falha de integração aqui impacta o fluxo principal de produção de conteúdo.
>
> **Branch:** `integration/04-pages-content`

---

## A. `pagesService.ts` — slug → UUID nos paths

A auditoria confirmou que `pagesService` usa `productSlug` (o campo `key`) nos paths. Após Sprint 02 e 03, o `effectiveProduct.id` é um UUID. Substituir `productSlug` por `productId` (UUID) em todos os paths de API:

```ts
// ANTES:
async listPages(productSlug: string) { return apiClient.get(`/products/${productSlug}/pages`); }

// DEPOIS:
async listPages(productId: string) { return apiClient.get(`/products/${productId}/pages`); }
```

Aplicar em todos os métodos: `listPages`, `getPage`, `getPageBySlug`, `createPage`, `deletePage`, `updatePage`, `createSection`, `updateSection`, `deleteSection`, `reorderSections`.

**Atenção:** o argumento `productSlug` no mock também serve como chave de lookup no `pagesStore` (`p.productSlug === productSlug`). Em modo mock, a chave continua sendo o slug — o mock não precisa de UUID. A solução é manter o `pagesStore` indexado por slug e usar slug no mock:

```ts
async listPages(productId: string): Promise<ListPagesResponse> {
  if (IS_API_MODE) return apiClient.get<ListPagesResponse>(`/products/${productId}/pages`);
  const { effectiveProduct } = useAuthStore.getState();  // ou receber slug como argumento separado
  return pagesStore.filter(p => p.productSlug === effectiveProduct?.key).map(clonePage);
}
```

Se o mock precisa do slug para filtrar e a assinatura agora só recebe UUID, uma das alternativas:
1. Receber ambos: `listPages(productId: string, productSlug?: string)` — slug usado apenas no mock.
2. Resolver o slug a partir do UUID internamente no mock (lookup no `productsStore`).
3. Manter slug como argumento primário e usar `product.key` em todo chamador — mais simples mas inconsistente com a Sprint 02.

**Decisão recomendada:** opção 2 — o mock resolve `productsStore.find(p => p.id === productId)?.key` para filtrar o `pagesStore`. Assim a assinatura pública usa UUID em ambos os modos.

---

## B. `contentService.ts` — corrigir paths

### B.1 — `listContent()` — adicionar prefixo de produto

`GET /content` não existe no backend. Substituir por:

```ts
async listContent(productId: string): Promise<ListContentResponse> {
  if (IS_API_MODE) return apiClient.get<ListContentResponse>(`/products/${productId}/content`);
  return contentStore;
}
```

Se a tela de conteúdo hoje chama `listContent()` sem argumento, atualizar os chamadores para passar `effectiveProduct.id`.

### B.2 — `getContent(id)` — adicionar prefixo de produto

```ts
// ANTES:
async getContent(id: string) { return apiClient.get(`/content/${id}`); }

// DEPOIS:
async getContent(productId: string, contentId: string) { return apiClient.get(`/products/${productId}/content/${contentId}`); }
```

### B.3 — `updateContent(id, patch)` — corrigir path

```ts
// ANTES (literal {productId}):
apiClient.put(`/content/${id}`, patch)

// DEPOIS:
apiClient.put(`/products/${productId}/content/${contentId}`, patch)
```

`updateContent` precisa receber `productId` como parâmetro. Atualizar todos os chamadores.

### B.4 — `createContent(payload)` — adicionar prefixo de produto

```ts
// ANTES (literal {productId}):
apiClient.post("/content", payload)

// DEPOIS:
apiClient.post(`/products/${productId}/content`, payload)
```

### B.5 — `restoreVersion` e `submitForReview` — placeholders literais

```ts
// ANTES (literais não substituídos):
apiClient.post(`/content/{contentId}/versions/${version}/restore`)
apiClient.post(`/content/${id}/transition`, ...)

// DEPOIS:
apiClient.post(`/products/${productId}/content/${contentId}/versions/${version}/restore`)
apiClient.post(`/products/${productId}/content/${contentId}/transition`, ...)
```

### B.6 — `listEditEvents()` e `listWorkflowItems()`

Verificar se os endpoints `/content/edit-events` e `/content/workflow` existem no backend. Se não existem, manter mock temporariamente e registrar como retrofit pendente.

---

## C. Globals e Events — `globalsService.ts` e `eventsService.ts`

Verificar os paths — ambos provavelmente usam `productSlug` onde deveriam usar `productId`. Aplicar a mesma correção slug→UUID da seção A.

```ts
// globalsService.ts
async listGlobals(productId: string) { return apiClient.get(`/products/${productId}/globals`); }
async updateGlobal(productId: string, key: string, value: string) { return apiClient.put(`/products/${productId}/globals/${key}`, { value }); }

// eventsService.ts
async listEvents(productId: string) { return apiClient.get(`/products/${productId}/events`); }
async createEvent(productId: string, req: CreateEventRequest) { return apiClient.post(`/products/${productId}/events`, req); }
```

---

## D. Mapeamento de tipos de resposta

### D.1 — `Page` e `Section`

Backend retorna `Page` com shape definido em `br.com.byop.aegis.page.dto.PageResponse`:
```json
{ "id": "UUID", "productId": "UUID", "slug": "home", "title": "Home", "locale": "pt-BR", "status": "DRAFT", "version": 1, "seo": {}, "sections": [...] }
```

Se o status vem como `"DRAFT"` e o frontend espera `"draft"`, criar mapper:

```ts
export function mapPage(dto: PageDto): Page {
  return {
    ...dto,
    productSlug: dto.productKey ?? '',  // o backend não manda productSlug — usar key se disponível
    status: dto.status.toLowerCase() as Page['status'],
  };
}
```

### D.2 — `ContentRow`

Verificar shape de `GET /products/{id}/content`. Se backend devolve campos com nomes diferentes (e.g., `updatedAt` vs `updatedAt`), criar `mapContentRow(dto)`.

---

## E. Editor de seções — `pagesService.createSection` / `updateSection`

Estes métodos já usam `productSlug` no path mas passam por section mutations. Garantir que todos os métodos de seção estão alinhados:

```ts
async createSection(productId: string, pageId: string, req: CreateSectionRequest): Promise<Section> {
  if (IS_API_MODE) return apiClient.post<Section>(`/products/${productId}/pages/${pageId}/sections`, req);
  // ...mock
}
async updateSection(productId: string, pageId: string, sectionId: string, req: UpdateSectionRequest): Promise<Section> {
  if (IS_API_MODE) return apiClient.put<Section>(`/products/${productId}/pages/${pageId}/sections/${sectionId}`, req);
  // ...mock
}
async deleteSection(productId: string, pageId: string, sectionId: string): Promise<void> {
  if (IS_API_MODE) return apiClient.delete(`/products/${productId}/pages/${pageId}/sections/${sectionId}`);
  // ...mock
}
```

---

## F. Critérios de aceite

- [ ] `GET /api/v1/products/{UUID}/pages` retorna as páginas reais do produto selecionado.
- [ ] `POST /api/v1/products/{UUID}/pages` cria uma nova página — aparece na lista sem reload.
- [ ] `GET /api/v1/products/{UUID}/content` retorna lista de conteúdo do produto.
- [ ] `PUT /api/v1/products/{UUID}/content/{contentId}` salva edição de conteúdo.
- [ ] `POST /api/v1/products/{UUID}/content/{contentId}/transition` muda o status de workflow.
- [ ] Publicação de página (`PUT .../pages/{id}` com `status: "published"`) funciona.
- [ ] Modo mock não regrediu.
- [ ] `npm run typecheck` — zero erros.

---

## G. Commit sugerido

```bash
git add frontend/src/domains/pages/ frontend/src/domains/content/
git commit -m "feat(integration): pages e content editorial integrados ao backend real"
```
