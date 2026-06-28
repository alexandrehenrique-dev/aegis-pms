# Sprint 20 — Mocks de alta fidelidade, toggle mock↔API e preparação para Spring Boot

> **Branch:** `sprint/20-mocks-fidelidade-e-toggle-api`
>
> **Motivação:** o frontend tem 17 services todos 100% mock. Dois problemas críticos:
> 1. **Bug visível:** produtos não mostram módulos habilitados — nem após edição, nem em produtos recém-criados via `ModuleCatalog`. O `moduleCatalogStore` global nunca atualiza o `modulesList` de cada produto.
> 2. **Arquitetura travada:** `VITE_API_MODE` existe no `.env` mas NENHUM service o lê — trocar de mock para API real exige reescrever todos os services ao mesmo tempo.
>
> Esta sprint resolve os dois de forma cirúrgica, **sem tocar em nenhuma lógica de permissão, autenticação ou AuthContext**. O comportamento em modo mock deve permanecer 100% idêntico ao atual — apenas os bugs são corrigidos. O toggle para `api` é aditivo.
>
> **⚠️ REGRA DE OURO desta sprint:** em modo `mock` (`VITE_API_MODE=mock` ou variável ausente), o comportamento deve ser EXATAMENTE o atual — incluindo todos os mocks existentes, AuthContext, roles, navConfig e permissões. Nenhuma lógica de permissão/autenticação é tocada.

---

## A. Infraestrutura: `src/infra/apiMode.ts`

Criar este arquivo. É a ÚNICA fonte de verdade para o toggle.

```ts
// src/infra/apiMode.ts

/**
 * Verdadeiro quando `VITE_API_MODE=api` — todos os services devem
 * verificar esta constante antes de usar mocks ou `apiClient`.
 *
 * Em modo mock (default), o comportamento é idêntico ao anterior a esta
 * sprint — nenhum mock é removido, apenas bugs são corrigidos.
 *
 * Em modo api, cada service usa `apiClient` (src/shared/services/apiClient.ts)
 * com o `baseURL` apontando para `VITE_API_BASE_URL`.
 */
export const IS_API_MODE = import.meta.env.VITE_API_MODE === 'api';
```

**Atenção:** `keycloakConfig.ts` tem uma função `getApiMode()` que compara com `"real"` — deixar como está. Não remover e não alterar esse arquivo. O `IS_API_MODE` de `apiMode.ts` é o toggle dos services; `getApiMode()` é exclusivo para o fluxo de login do Keycloak e são contextos distintos.

---

## B. Arquivo `.env` e `.env.example`

**`.env`** — criar em `frontend/` se não existir (checar com `ls frontend/.env`):

```env
# Modo dos services: "mock" (padrão) ou "api" (usa VITE_API_BASE_URL)
VITE_API_MODE=mock
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_KEYCLOAK_URL=http://localhost:8282
VITE_KEYCLOAK_REALM=aegis
VITE_KEYCLOAK_CLIENT_ID=aegis-web
```

**`.env.example`** — criar / atualizar com as mesmas variáveis (sem valores sensíveis).

---

## C. Bug crítico: módulos não aparecem em produtos

### Diagnóstico

`products.mocks.ts` define produtos com `modules: 6` (contagem) mas sem `modulesList: string[]`.
`productsService.enableModule(name)` atualiza apenas o `moduleCatalogStore` global — nunca o `modulesList` do produto no `productsStore`.

`resolveEnabledModules(product)` já tem o fallback correto: se `modulesList` for `undefined`, usa os defaults do tipo de produto (`PRODUCT_TYPE_MODULE_DEFAULTS`). Mas após habilitar um módulo via `ModuleCatalog`, essa mudança nunca é refletida.

### C.1 — Adicionar `modulesList` nos mocks de produtos

Arquivo: `src/domains/products/mocks/products.mocks.ts`

Substituir o array `products` por:

```ts
import type { ProductStatus } from "../../../shared/types";

export const products = [
  {
    name: "Maestro Beton", type: "Site Institucional", status: "Ativo" as ProductStatus,
    modules: 6, last: "Formulário recebeu 3 respostas", score: "94%",
    modulesList: ["Páginas", "Conteúdo", "Assets", "Forms", "SEO", "Analytics"],
  },
  {
    name: "Aion Logbook", type: "Jogo / Experimento", status: "Pendente" as ProductStatus,
    modules: 4, last: "Conteúdo enviado para revisão", score: "71%",
    modulesList: ["Conteúdo", "Assets", "SEO", "Analytics"],
  },
  {
    name: "Eirene UI", type: "Design System", status: "Ativo" as ProductStatus,
    modules: 5, last: "Assets do produto atualizados", score: "88%",
    modulesList: ["Páginas", "Conteúdo", "Assets", "SEO", "Analytics"],
  },
  {
    name: "Genesis", type: "Produto SaaS", status: "Ativo" as ProductStatus,
    modules: 7, last: "SEO global revisado", score: "91%",
    modulesList: ["Conteúdo", "Assets", "Forms", "Analytics", "SEO", "Workflow", "Knowledge Graph"],
  },
  {
    name: "WikiDev", type: "Knowledge Base", status: "Arquivado" as ProductStatus,
    modules: 3, last: "Nova página publicada", score: "62%",
    modulesList: ["Conteúdo", "Knowledge Graph", "SEO"],
  },
  {
    name: "Conecta Talentos", type: "Portal", status: "Sem módulos" as ProductStatus,
    modules: 0, last: "Produto criado há 1 dia", score: "—",
    modulesList: [],
  },
  {
    name: "CMSS", type: "Site Institucional", status: "Ativo" as ProductStatus,
    modules: 5, last: "Página Quem Somos atualizada", score: "85%",
    modulesList: ["Páginas", "Conteúdo", "Assets", "SEO", "Analytics"],
  },
  {
    name: "Alexandre Dev", type: "Portfolio", status: "Ativo" as ProductStatus,
    modules: 4, last: "Novo projeto publicado (Eirene UI)", score: "90%",
    modulesList: ["Portfolio", "Páginas", "SEO", "Analytics"],
  },
  {
    name: "Loki", type: "Biblioteca Filosófica", status: "Ativo" as ProductStatus,
    modules: 4, last: "Novo manifesto publicado", score: "77%",
    modulesList: ["Conteúdo", "Knowledge Graph", "SEO", "Analytics"],
  },
];
```

> **Por que "Jogo / Experimento" e "Biblioteca Filosófica" não têm tipo em `PRODUCT_TYPE_MODULE_DEFAULTS`?** Esses tipos não existem no enum canônico. O fallback de `resolveEnabledModules` já lida com isso: se o tipo não está no mapa, retorna array vazio — e agora o `modulesList` explícito nos mocks resolve o problema sem depender do fallback.

### C.2 — Corrigir `enableModule` / `disableModule` em `productsService.ts`

O `ModuleCatalog` é sempre renderizado dentro de um contexto de produto (via `ProductDetail`, `ProductDashboard` ou `ModulesPage`, todas com `effectiveProduct` no `AuthContext`). Para propagar a mudança ao produto correto, `enableModule` e `disableModule` devem aceitar um `productId` opcional.

**Regra de compatibilidade:** o parâmetro é opcional — chamadas existentes sem `productId` continuam funcionando (o `moduleCatalogStore` global é atualizado igualmente; o `productsStore` só é atualizado quando `productId` é fornecido).

Substituir as duas funções em `src/domains/products/services/productsService.ts`:

```ts
async enableModule(moduleName: string, productId?: string): Promise<void> {
  // Atualiza estado no catálogo global (comportamento original — mantido)
  const m = moduleCatalogStore.find((x) => x.name === moduleName);
  if (m) {
    logApiCall("POST", `/api/v1/products/${productId ?? "{productId}"}/modules/${moduleName}/enable`);
    m.state = "habilitado";
  }
  // Propaga para o modulesList do produto no store (bug fix)
  if (productId) {
    const p = productsStore.find((x) => x.id === productId || x.name === productId);
    if (p) {
      if (!p.modulesList) p.modulesList = [];
      if (!p.modulesList.includes(moduleName)) p.modulesList.push(moduleName);
      p.modules = p.modulesList.length;
    }
  }
},

async disableModule(moduleName: string, productId?: string): Promise<void> {
  // Atualiza estado no catálogo global (comportamento original — mantido)
  const m = moduleCatalogStore.find((x) => x.name === moduleName);
  if (m) {
    logApiCall("POST", `/api/v1/products/${productId ?? "{productId}"}/modules/${moduleName}/disable`);
    m.state = "desabilitado";
  }
  // Propaga para o modulesList do produto no store (bug fix)
  if (productId) {
    const p = productsStore.find((x) => x.id === productId || x.name === productId);
    if (p && p.modulesList) {
      p.modulesList = p.modulesList.filter((n) => n !== moduleName);
      p.modules = p.modulesList.length;
    }
  }
},
```

### C.3 — Passar `productId` do `ModuleCatalog`

O `ModuleCatalog` precisa receber o id do produto para passar ao `enableModule`. Ele já é chamado em contextos que têm `useAuth()`:

Arquivo: `src/domains/products/components/ModuleCatalog.tsx`

1. Adicionar `productId` como prop opcional:
```ts
export function ModuleCatalog({ compact = false, productId }: { compact?: boolean; productId?: string }) {
```

2. Nas chamadas `productsService.enableModule(m.name)` e `productsService.enableModule(name)` dentro de `handleAction` e `handleEnableSelected`, passar `productId`:
```ts
await productsService.enableModule(m.name, productId);
// ...
await Promise.all(Array.from(selected).map((name) => productsService.enableModule(name, productId)));
```

3. Nos arquivos que renderizam `<ModuleCatalog />`, passar o id do produto efetivo via `useAuth()`:

**`ProductDetail.tsx`** e **`ProductDashboard.tsx`**:
```tsx
const { effectiveProduct } = useAuth();
// ...
<ModuleCatalog compact productId={effectiveProduct?.id} />
```

**`ModulesPage.tsx`**:
```tsx
const { effectiveProduct } = useAuth();
return <ModuleCatalog productId={effectiveProduct?.id} />;
```

> ⚠️ `effectiveProduct.id` pode ser `undefined` para produtos de mock antigos que não tinham `id` — verificar o tipo. Se for undefined, `enableModule` simplesmente ignora a propagação (comportamento seguro).

---

## D. Fidelidade dos mocks — CRUD reagindo como sistemas reais

Nesta seção, corrigir mocks que aceitavam mutações mas não as persistiam ou não atualizavam o estado visível.

### D.1 — `formsService.ts`

**`publish()`** — publicar um formulário deve mudar seu `status` no store:
```ts
async publish(formId?: string): Promise<void> {
  logApiCall("POST", `/api/v1/products/{productId}/forms/${formId ?? "{formId}"}/publish`);
  if (formId) {
    const f = formsStore.find((x) => x.id === formId);
    if (f) f.status = "published";
  }
},

async saveDraft(formId?: string): Promise<void> {
  logApiCall("PUT", `/api/v1/products/{productId}/forms/${formId ?? "{formId}"}`);
  if (formId) {
    const f = formsStore.find((x) => x.id === formId);
    if (f) f.status = "draft";
  }
},
```

**`createForm()`** — adicionar método para criar novo formulário no store:
```ts
async createForm(productSlug: string, payload: { name: string; type: string }): Promise<FormSummary> {
  logApiCall("POST", `/api/v1/products/${productSlug}/forms`, payload);
  const id = `form-${slugify(payload.name)}-${Date.now().toString(36)}`;
  const created: FormSummary = {
    id, productSlug, name: payload.name, type: payload.type,
    status: "draft", responses: 0, conversion: "—", lastActivity: "agora", publication: "—",
  };
  formsStore.push(created);
  fieldsByFormId[id] = [...DEFAULT_CONTATO_FIELDS];
  return created;
},
```

Importar `slugify` no topo do arquivo:
```ts
import { slugify } from "../../../shared/utils/slugify";
```

### D.2 — `contentService.ts`

**`publish()`** e **`archive()`** devem atualizar status no store:
```ts
async publish(id?: string): Promise<void> {
  logApiCall("POST", `/api/v1/products/{productId}/content/${id ?? "{contentId}"}/transition`, { from: "In Review", to: "Published" });
  if (id) {
    const row = contentStore.find((c) => c.id === id);
    if (row) row.status = "Published";
  }
},

async archive(id?: string): Promise<void> {
  logApiCall("POST", `/api/v1/products/{productId}/content/${id ?? "{contentId}"}/transition`, { from: "Published", to: "Archived" });
  if (id) {
    const row = contentStore.find((c) => c.id === id);
    if (row) row.status = "Archived";
  }
},

async deleteContent(id: string): Promise<void> {
  logApiCall("DELETE", `/api/v1/products/{productId}/content/${id}`);
  const index = contentStore.findIndex((c) => c.id === id);
  if (index >= 0) contentStore.splice(index, 1);
},
```

### D.3 — `assetsService.ts`

**`upload()`** já funciona corretamente. **`archiveAsset()`** já funciona. **`uploadFiles()`** (upload em batch) deve ser um noop intencional com aviso — a variante real é `upload(file)`:
```ts
async uploadFiles(): Promise<void> {
  // Noop intencional: usar upload(file: File) para upload unitário com
  // persistência no store. uploadFiles() é o endpoint batch que o backend
  // implementará na etapa 11.
  logApiCall("POST", "/api/v1/products/{productId}/assets/upload (batch — sem persistência no mock)");
},
```

### D.4 — `auditService.ts`

Adicionar mutação para criação de evento de auditoria (útil para testar o log em operações):
```ts
async recordEvent(event: Omit<AuditEvent, "time">): Promise<void> {
  logApiCall("POST", "/api/v1/audit/events", event);
  auditStore.unshift({ ...event, time: new Date().toLocaleTimeString("pt-BR") });
},
```

### D.5 — `notificationsService.ts` — nenhuma mudança necessária

Já é o mock mais completo e fiel: fan-out, stores separados por userId, markShown/markRead. Não tocar.

### D.6 — `tenantsService.ts`, `pagesService.ts`, `eventsService.ts`, `globalsService.ts`, `knowledgeService.ts`, `feedbackService.ts`, `productAssignmentsService.ts`, `dashboardService.ts`, `analyticsService.ts`

Todos já têm CRUD que persiste corretamente. Não tocar nas implementações mock.

---

## E. Toggle mock↔API em cada service (esqueleto da integração real)

O padrão para cada service é:

```ts
import { IS_API_MODE } from '@/infra/apiMode';
import { apiClient } from '@/shared/services/apiClient';

// ... stores de mock permanecem idênticos ...

export const meuService = {
  async listar(): Promise<Tipo[]> {
    if (IS_API_MODE) return apiClient.get<Tipo[]>('/endpoint');
    return store; // mock — sem mudança
  },
};
```

Aplicar este padrão nos services abaixo. **Em modo `mock`, o código é idêntico ao atual** — nenhum comportamento muda. Em modo `api`, a chamada vai para o `apiClient`. Os paths seguem o padrão canônico do backend (`/api/v1/...` sem prefixo `/admin/`).

> ⚠️ O `apiClient` já configura `baseURL = VITE_API_BASE_URL` — os paths passados devem ser relativos (ex.: `/products`, não `/api/v1/products`), pois `VITE_API_BASE_URL` já inclui `/api/v1`.

### E.1 — `productsService.ts`

```ts
import { IS_API_MODE } from '@/infra/apiMode';
import { apiClient } from '@/shared/services/apiClient';
// ... imports existentes ...

export const productsService = {
  async listProducts(): Promise<ListProductsResponse> {
    if (IS_API_MODE) return apiClient.get<ListProductsResponse>('/products');
    return productsStore;
  },

  async listModuleCatalog(): Promise<ListModulesResponse> {
    // Catálogo de módulos: em modo api, vem do backend por produto
    // Path: /products/{productId}/modules — implementar quando houver contexto de produto
    return moduleCatalogStore; // mock para ambos os modos por enquanto
  },

  async enableModule(moduleName: string, productId?: string): Promise<void> {
    if (IS_API_MODE && productId) {
      return apiClient.post(`/products/${productId}/modules/${moduleName}/enable`);
    }
    // mock — ver Tarefa C.2
    // ... (implementação do mock da Tarefa C.2 aqui)
  },

  async disableModule(moduleName: string, productId?: string): Promise<void> {
    if (IS_API_MODE && productId) {
      return apiClient.post(`/products/${productId}/modules/${moduleName}/disable`);
    }
    // mock — ver Tarefa C.2
  },

  async archiveProduct(idOrName: string): Promise<void> {
    if (IS_API_MODE) return apiClient.post(`/products/${idOrName}/archive`);
    // ... mock existente ...
  },

  async update(idOrName: string, req: UpdateProductRequest): Promise<ProductSummary> {
    if (IS_API_MODE) return apiClient.put<ProductSummary>(`/products/${idOrName}`, req);
    // ... mock existente ...
  },

  async remove(idOrName: string, req: DeleteProductRequest): Promise<void> {
    if (IS_API_MODE) return apiClient.delete(`/products/${idOrName}`);
    // ... mock existente ...
  },

  async create(req: CreateProductRequest): Promise<ProductSummary> {
    if (IS_API_MODE) return apiClient.post<ProductSummary>('/products', req);
    // ... mock existente (com skeleton de páginas) ...
  },

  async saveSettings(): Promise<void> {
    if (IS_API_MODE) return apiClient.put('/products/{productId}/settings');
    logApiCall("PATCH", "/api/v1/admin/products/{productId} (settings)");
  },
};
```

### E.2 — `contentService.ts`

```ts
import { IS_API_MODE } from '@/infra/apiMode';
import { apiClient } from '@/shared/services/apiClient';

export const contentService = {
  async listContent(): Promise<ListContentResponse> {
    if (IS_API_MODE) return apiClient.get<ListContentResponse>('/content');
    return contentStore;
  },
  async listContentByProduct(productSlug: string): Promise<ListContentResponse> {
    if (IS_API_MODE) return apiClient.get<ListContentResponse>(`/products/${productSlug}/content`);
    return contentByProduct[productSlug] ?? [];
  },
  async getContent(id: string): Promise<ContentRow | undefined> {
    if (IS_API_MODE) return apiClient.get<ContentRow>(`/content/${id}`);
    return contentStore.find((c) => c.id === id);
  },
  async updateContent(id: string, patch: Partial<ContentRow>, product: ContentProductContext = null): Promise<ContentRow | undefined> {
    if (IS_API_MODE) return apiClient.put<ContentRow>(`/content/${id}`, patch);
    // ... mock existente (com syncKnowledgeGraphRefs) ...
  },
  async createContent(payload: { title: string; type: string; lang: string; author: string; body?: string; metadata?: Record<string, unknown> }, product: ContentProductContext = null): Promise<ContentRow> {
    if (IS_API_MODE) return apiClient.post<ContentRow>('/content', payload);
    // ... mock existente ...
  },
  // publish, archive, deleteContent: ver D.2 para assinatura atualizada
  async publish(id?: string): Promise<void> {
    if (IS_API_MODE && id) return apiClient.post(`/content/${id}/transition`, { from: "In Review", to: "Published" });
    // ... mock da Tarefa D.2 ...
  },
  async archive(id?: string): Promise<void> {
    if (IS_API_MODE && id) return apiClient.post(`/content/${id}/transition`, { from: "Published", to: "Archived" });
    // ... mock da Tarefa D.2 ...
  },
  async submitForReview(id?: string): Promise<void> {
    if (IS_API_MODE && id) return apiClient.post(`/content/${id}/transition`, { from: "Draft", to: "In Review" });
    // ... mock existente ...
  },
  async restoreVersion(version: string): Promise<void> {
    if (IS_API_MODE) return apiClient.post(`/content/{contentId}/versions/${version}/restore`);
    logApiCall("POST", `/api/v1/products/{productId}/content/{contentId}/versions/${version}/restore`, { version });
  },
  async deleteContent(id: string): Promise<void> {
    if (IS_API_MODE) return apiClient.delete(`/content/${id}`);
    // ... mock da Tarefa D.2 ...
  },
  async saveDraft(): Promise<void> {
    if (IS_API_MODE) return apiClient.put('/content/{contentId}');
    logApiCall("PUT", "/api/v1/products/{productId}/content/{contentId}");
  },
  async schedulePublish(): Promise<void> {
    if (IS_API_MODE) return apiClient.post('/content/{contentId}/transition', { from: "In Review", to: "Published", scheduled: true });
    logApiCall("POST", "/api/v1/products/{productId}/content/{contentId}/transition", { from: "In Review", to: "Published", scheduled: true });
  },
  async listEditEvents(): Promise<ListEditEventsResponse> {
    if (IS_API_MODE) return apiClient.get<ListEditEventsResponse>('/content/edit-events');
    return editEvents;
  },
  async listWorkflowItems(): Promise<ListWorkflowItemsResponse> {
    if (IS_API_MODE) return apiClient.get<ListWorkflowItemsResponse>('/content/workflow');
    return wfInitialItems;
  },
};
```

### E.3 — `pagesService.ts`

```ts
import { IS_API_MODE } from '@/infra/apiMode';
import { apiClient } from '@/shared/services/apiClient';

export const pagesService = {
  async listPages(productSlug: string): Promise<ListPagesResponse> {
    if (IS_API_MODE) return apiClient.get<ListPagesResponse>(`/products/${productSlug}/pages`);
    return pagesStore.filter((p) => p.productSlug === productSlug).map(clonePage);
  },
  async getPage(productSlug: string, pageId: string): Promise<Page | undefined> {
    if (IS_API_MODE) return apiClient.get<Page>(`/products/${productSlug}/pages/${pageId}`);
    const page = findStorePage(productSlug, pageId);
    return page ? clonePage(page) : undefined;
  },
  async getPageBySlug(productSlug: string, slug: string): Promise<Page | undefined> {
    if (IS_API_MODE) return apiClient.get<Page>(`/products/${productSlug}/pages/by-slug/${slug}`);
    const page = pagesStore.find((p) => p.productSlug === productSlug && p.slug === slug);
    return page ? clonePage(page) : undefined;
  },
  async createPage(productSlug: string, req: CreatePageRequest): Promise<Page> {
    if (IS_API_MODE) return apiClient.post<Page>(`/products/${productSlug}/pages`, req);
    // ... mock existente ...
  },
  async deletePage(productSlug: string, pageId: string): Promise<void> {
    if (IS_API_MODE) return apiClient.delete(`/products/${productSlug}/pages/${pageId}`);
    // ... mock existente ...
  },
  async updatePage(productSlug: string, pageId: string, patch: Partial<Pick<Page, "title" | "status" | "seo">>): Promise<Page> {
    if (IS_API_MODE) return apiClient.put<Page>(`/products/${productSlug}/pages/${pageId}`, patch);
    // ... mock existente ...
  },
  async createSection(productSlug: string, pageId: string, req: CreateSectionRequest): Promise<Section> {
    if (IS_API_MODE) return apiClient.post<Section>(`/products/${productSlug}/pages/${pageId}/sections`, req);
    // ... mock existente ...
  },
  async updateSection(productSlug: string, pageId: string, sectionId: string, req: UpdateSectionRequest): Promise<Section> {
    if (IS_API_MODE) return apiClient.put<Section>(`/products/${productSlug}/pages/${pageId}/sections/${sectionId}`, req);
    // ... mock existente ...
  },
  async deleteSection(productSlug: string, pageId: string, sectionId: string): Promise<void> {
    if (IS_API_MODE) return apiClient.delete(`/products/${productSlug}/pages/${pageId}/sections/${sectionId}`);
    // ... mock existente ...
  },
  async reorderSections(productSlug: string, pageId: string, req: ReorderSectionsRequest): Promise<Page> {
    if (IS_API_MODE) return apiClient.put<Page>(`/products/${productSlug}/pages/${pageId}/sections/reorder`, req);
    // ... mock existente ...
  },
  async listBlockTypes(): Promise<readonly BlockType[]> {
    // Catálogo estático por enquanto em ambos os modos
    return BLOCK_TYPES;
  },
};
```

### E.4 — `formsService.ts`

```ts
import { IS_API_MODE } from '@/infra/apiMode';
import { apiClient } from '@/shared/services/apiClient';

export const formsService = {
  async listForms(productSlug?: string): Promise<ListFormsResponse> {
    if (IS_API_MODE) {
      const path = productSlug ? `/products/${productSlug}/forms` : '/forms';
      return apiClient.get<ListFormsResponse>(path);
    }
    return productSlug ? formsStore.filter((f) => f.productSlug === productSlug) : formsStore;
  },
  async getForm(id: string): Promise<FormSummary | undefined> {
    if (IS_API_MODE) return apiClient.get<FormSummary>(`/forms/${id}`);
    return formsStore.find((f) => f.id === id);
  },
  async createForm(productSlug: string, payload: { name: string; type: string }): Promise<FormSummary> {
    if (IS_API_MODE) return apiClient.post<FormSummary>(`/products/${productSlug}/forms`, payload);
    // ... mock da Tarefa D.1 ...
  },
  async getFormFields(id: string): Promise<FormField[]> {
    if (IS_API_MODE) return apiClient.get<FormField[]>(`/forms/${id}/fields`);
    return fieldsByFormId[id] ?? [];
  },
  async saveFormFields(id: string, fields: FormField[]): Promise<void> {
    if (IS_API_MODE) return apiClient.put(`/forms/${id}/fields`, { fields });
    // ... mock existente ...
  },
  async listSubmissions(formId?: string): Promise<ListSubmissionsResponse> {
    if (IS_API_MODE) {
      const path = formId ? `/forms/${formId}/submissions` : '/submissions';
      return apiClient.get<ListSubmissionsResponse>(path);
    }
    return submissionsStore;
  },
  async listFieldTypes(): Promise<ListFieldTypesResponse> {
    // Catálogo estático — mesmo em modo api, retorna local por ora
    return fieldTypes;
  },
  async markQualified(email: string): Promise<void> {
    if (IS_API_MODE) return apiClient.post(`/submissions/${email}/qualify`);
    // ... mock existente ...
  },
  async assignSubmissions(emails: string[], owner: string): Promise<void> {
    if (IS_API_MODE) return apiClient.post('/submissions/assign', { emails, owner });
    // ... mock existente ...
  },
  async removeForm(id: string, productSlug: string): Promise<void> {
    if (IS_API_MODE) return apiClient.delete(`/products/${productSlug}/forms/${id}`);
    // ... mock existente (com verificação de uso em páginas) ...
  },
  async getDelivery(formId: string): Promise<FormDelivery> {
    if (IS_API_MODE) return apiClient.get<FormDelivery>(`/forms/${formId}/delivery`);
    // ... mock existente ...
  },
  async saveDelivery(formId: string, delivery: FormDelivery): Promise<void> {
    if (IS_API_MODE) return apiClient.put(`/forms/${formId}/delivery`, delivery);
    // ... mock existente ...
  },
  async publish(formId?: string): Promise<void> {
    if (IS_API_MODE && formId) return apiClient.post(`/forms/${formId}/publish`);
    // ... mock da Tarefa D.1 ...
  },
  async saveDraft(formId?: string): Promise<void> {
    if (IS_API_MODE && formId) return apiClient.put(`/forms/${formId}`);
    // ... mock da Tarefa D.1 ...
  },
  async submitTest(): Promise<void> {
    if (IS_API_MODE) return apiClient.post('/forms/{formId}/test-submit');
    logApiCall("POST", "/api/v1/products/{productId}/forms/{formId}/test-submit");
  },
  makeField, // função pura — nunca usa api
};
```

### E.5 — `usersService.ts`

> ⚠️ Os paths do mock usavam `/api/v1/admin/users/...` — paths incorretos que nunca foram o padrão canônico do Aegis (ver etapa 15, Seção A, nota de divergência). Corrigir apenas nos **paths dos logApiCall** e nos **branches api**. A lógica de permissão/roles (ADMIN_ROLES, isLastActiveAdmin) **não é tocada**.

```ts
import { IS_API_MODE } from '@/infra/apiMode';
import { apiClient } from '@/shared/services/apiClient';

export const usersService = {
  async listUsers(tenantId?: string): Promise<ListUsersResponse> {
    if (IS_API_MODE && tenantId) return apiClient.get<ListUsersResponse>(`/tenants/${tenantId}/users`);
    return usersStore;
  },
  async isLastActiveAdmin(email: string): Promise<boolean> {
    // Regra de negócio local — mantida em ambos os modos (ADR-0020)
    const activeAdmins = usersStore.filter((u) => ADMIN_ROLES.includes(u.role) && u.status === "ativo");
    return activeAdmins.length === 1 && activeAdmins[0].email === email;
  },
  async invite(req: InviteUserRequest, tenantId?: string): Promise<UserSummary> {
    if (IS_API_MODE && tenantId) return apiClient.post<UserSummary>(`/tenants/${tenantId}/users/invite`, req);
    logApiCall("POST", `/api/v1/tenants/${tenantId ?? "{tenantId}"}/users/invite`, req);
    const created: UserSummary = {
      name: req.name, email: req.email, role: req.role, products: req.allowedProducts,
      status: "convidado", lastAccess: "nunca", inviteStatus: "pendente",
    };
    usersStore.push(created);
    return created;
  },
  async resendInvite(email: string, tenantId?: string): Promise<void> {
    if (IS_API_MODE && tenantId) return apiClient.post(`/tenants/${tenantId}/users/${email}/resend-invite`);
    const u = usersStore.find((x) => x.email === email);
    if (u) {
      logApiCall("POST", `/api/v1/tenants/${tenantId ?? "{tenantId}"}/users/${email}/resend-invite`);
      u.inviteStatus = "pendente";
    }
  },
  async blockUser(email: string, tenantId?: string): Promise<void> {
    if (IS_API_MODE && tenantId) return apiClient.post(`/tenants/${tenantId}/users/${email}/block`);
    const u = usersStore.find((x) => x.email === email);
    if (u) {
      logApiCall("POST", `/api/v1/tenants/${tenantId ?? "{tenantId}"}/users/${email}/block`);
      u.status = "bloqueado";
    }
  },
  async removeUser(email: string, tenantId?: string): Promise<void> {
    if (IS_API_MODE && tenantId) return apiClient.delete(`/tenants/${tenantId}/users/${email}`);
    const u = usersStore.find((x) => x.email === email);
    if (u) {
      logApiCall("DELETE", `/api/v1/tenants/${tenantId ?? "{tenantId}"}/users/${email}`);
      u.status = "removido";
    }
  },
  async restoreUser(email: string, tenantId?: string): Promise<UserSummary> {
    if (IS_API_MODE && tenantId) return apiClient.post<UserSummary>(`/tenants/${tenantId}/users/${email}/restore`);
    const u = usersStore.find((x) => x.email === email);
    if (!u) throw { status: 404, message: `Usuário ${email} não encontrado.` };
    logApiCall("POST", `/api/v1/tenants/${tenantId ?? "{tenantId}"}/users/${email}/restore`);
    u.status = "ativo";
    return u;
  },
};
```

### E.6 — `settingsService.ts`

```ts
import { IS_API_MODE } from '@/infra/apiMode';
import { apiClient } from '@/shared/services/apiClient';

export const settingsService = {
  async listSettingCards(): Promise<ListSettingCardsResponse> {
    if (IS_API_MODE) return apiClient.get<ListSettingCardsResponse>('/settings/cards');
    return settingCardsStore;
  },
  async restoreDefaultRoles(): Promise<void> {
    if (IS_API_MODE) return apiClient.post('/settings/roles/restore-defaults');
    logApiCall("POST", "/api/v1/admin/roles/restore-defaults");
  },
  async createRole(payload: { name: string; description: string }): Promise<void> {
    if (IS_API_MODE) return apiClient.post('/settings/roles', payload);
    logApiCall("POST", "/api/v1/admin/roles", payload);
  },
  async restoreDefaultPermissions(): Promise<void> {
    if (IS_API_MODE) return apiClient.post('/settings/permissions/restore-defaults');
    logApiCall("POST", "/api/v1/admin/permissions/restore-defaults");
  },
  async savePermissions(): Promise<void> {
    if (IS_API_MODE) return apiClient.put('/settings/permissions');
    logApiCall("PUT", "/api/v1/admin/permissions");
  },
  async saveSecurity(productId: string, payload?: unknown): Promise<void> {
    if (IS_API_MODE) return apiClient.put(`/products/${productId}/settings/security`, payload);
    // Path corrigido: /products/{productId}/settings/security (etapa 19, Seção C)
    logApiCall("PUT", `/api/v1/products/${productId}/settings/security`, payload);
  },
  async generateAccessPreview(payload: { subject: string; product: string; module: string }): Promise<void> {
    if (IS_API_MODE) return apiClient.post('/settings/permissions/preview', payload);
    logApiCall("POST", "/api/v1/admin/permissions/preview", payload);
  },
};
```

> **Atenção:** `saveSecurity()` agora recebe `productId` obrigatório — verificar se `SecuritySettingsPanel.tsx` já passa esse argumento. Se não, passar o `effectiveProduct?.id` via `useAuth()`. Não alterar nenhuma lógica de permissão do painel.

### E.7 — `tenantsService.ts`

```ts
import { IS_API_MODE } from '@/infra/apiMode';
import { apiClient } from '@/shared/services/apiClient';

export const tenantsService = {
  async listTenants(): Promise<ListTenantsResponse> {
    if (IS_API_MODE) return apiClient.get<ListTenantsResponse>('/tenants');
    return tenantsStore;
  },
  async getTenant(id: string): Promise<TenantDetailResponse | undefined> {
    if (IS_API_MODE) return apiClient.get<TenantDetailResponse>(`/tenants/${id}`);
    return tenantsStore.find((t) => t.id === id);
  },
  async create(req: CreateTenantRequest): Promise<TenantOption> {
    if (IS_API_MODE) return apiClient.post<TenantOption>('/tenants', req);
    // ... mock existente ...
  },
  async update(id: string, req: UpdateTenantRequest): Promise<TenantOption> {
    if (IS_API_MODE) return apiClient.put<TenantOption>(`/tenants/${id}`, req);
    // ... mock existente (com notificação de suspensão) ...
  },
  async remove(id: string, req: DeleteTenantRequest): Promise<void> {
    if (IS_API_MODE) return apiClient.delete(`/tenants/${id}`);
    // ... mock existente ...
  },
};
```

### E.8 — `notificationsService.ts`

```ts
import { IS_API_MODE } from '@/infra/apiMode';
import { apiClient } from '@/shared/services/apiClient';

export const notificationsService = {
  async listMine(): Promise<NotificationWithStatus[]> {
    if (IS_API_MODE) return apiClient.get<NotificationWithStatus[]>('/notifications/me');
    // ... mock existente ...
  },
  async getPendingModal(): Promise<NotificationWithStatus | null> {
    if (IS_API_MODE) return apiClient.get<NotificationWithStatus | null>('/notifications/me/pending-modal');
    // ... mock existente ...
  },
  async markShown(notificationId: string): Promise<void> {
    if (IS_API_MODE) return apiClient.post(`/notifications/${notificationId}/mark-shown`);
    // ... mock existente ...
  },
  async markRead(notificationId: string): Promise<void> {
    if (IS_API_MODE) return apiClient.post(`/notifications/${notificationId}/mark-read`);
    // ... mock existente ...
  },
  async create(req: CreateNotificationRequest): Promise<Notification> {
    if (IS_API_MODE) return apiClient.post<Notification>('/notifications', req);
    // ... mock existente (com fan-out) ...
  },
  async listAll(): Promise<Notification[]> {
    if (IS_API_MODE) return apiClient.get<Notification[]>('/notifications');
    // ... mock existente ...
  },
};
```

### E.9 — `analyticsService.ts`

```ts
import { IS_API_MODE } from '@/infra/apiMode';
import { apiClient } from '@/shared/services/apiClient';

export const analyticsService = {
  async listKpis(): Promise<ListKpisResponse> {
    if (IS_API_MODE) return apiClient.get<ListKpisResponse>('/products/{productId}/analytics/kpis');
    return kpisStore;
  },
  async listHealth(): Promise<ListHealthResponse> {
    if (IS_API_MODE) return apiClient.get<ListHealthResponse>('/products/{productId}/analytics/health');
    return healthStore;
  },
  async listChannels(): Promise<ListChannelsResponse> {
    if (IS_API_MODE) return apiClient.get<ListChannelsResponse>('/products/{productId}/analytics/channels');
    return channelsStore;
  },
  async generateReport(name: string): Promise<void> {
    if (IS_API_MODE) return apiClient.post('/products/{productId}/analytics/reports', { name });
    logApiCall("POST", "/api/v1/products/{productId}/analytics/reports", { name });
  },
  async markTrendReviewed(label: string): Promise<void> {
    if (IS_API_MODE) return apiClient.post('/products/{productId}/analytics/trends/review', { label });
    logApiCall("POST", "/api/v1/products/{productId}/analytics/trends/review", { label });
  },
  async generateActionPlan(): Promise<void> {
    if (IS_API_MODE) return apiClient.post('/products/{productId}/analytics/action-plan');
    logApiCall("POST", "/api/v1/products/{productId}/analytics/action-plan");
  },
};
```

### E.10 — `assetsService.ts`

```ts
import { IS_API_MODE } from '@/infra/apiMode';
import { apiClient } from '@/shared/services/apiClient';

export const assetsService = {
  async listAssets(productId?: string): Promise<ListAssetsResponse> {
    if (IS_API_MODE) return apiClient.get<ListAssetsResponse>(`/products/${productId ?? '{productId}'}/assets`);
    return assetsStore;
  },
  async listTags(): Promise<ListAssetTagsResponse> {
    if (IS_API_MODE) return apiClient.get<ListAssetTagsResponse>('/products/{productId}/assets/tags');
    return assetTagsStore;
  },
  async upload(file: File): Promise<{ assetId: string }> {
    if (IS_API_MODE) {
      const form = new FormData();
      form.append('file', file);
      // upload multipart: não usa apiClient.post (JSON), usa fetch direto
      const token = (await import('@/shared/services/apiClient')).apiClient;
      // TODO: implementar upload multipart no apiClient quando necessário
      // Por ora, logar intenção
      logApiCall("POST", "/api/v1/products/{productId}/assets (upload multipart — TODO)");
      return { assetId: file.name };
    }
    // ... mock existente ...
  },
  async archiveAsset(name: string): Promise<void> {
    if (IS_API_MODE) return apiClient.post(`/products/{productId}/assets/${name}/archive`);
    // ... mock existente ...
  },
  async createTag(name: string): Promise<void> {
    if (IS_API_MODE) return apiClient.post('/products/{productId}/assets/tags', { name });
    // ... mock existente ...
  },
  async renameTag(oldName: string, newName: string): Promise<void> {
    if (IS_API_MODE) return apiClient.put(`/products/{productId}/assets/tags/${oldName}`, { newName });
    // ... mock existente ...
  },
  async removeTag(name: string): Promise<void> {
    if (IS_API_MODE) return apiClient.delete(`/products/{productId}/assets/tags/${name}`);
    // ... mock existente ...
  },
  async mergeTags(tagsToMerge: string[], into: string): Promise<void> {
    if (IS_API_MODE) return apiClient.post('/products/{productId}/assets/tags/merge', { tagsToMerge, into });
    // ... mock existente ...
  },
  async saveMetadata(): Promise<void> {
    if (IS_API_MODE) return apiClient.put('/products/{productId}/assets/{assetId}/metadata');
    logApiCall("PUT", "/api/v1/products/{productId}/assets/{assetId}/metadata");
  },
  async downloadAsset(name: string): Promise<void> {
    if (IS_API_MODE) return apiClient.get(`/products/{productId}/assets/${name}/download`);
    logApiCall("GET", `/api/v1/products/{productId}/assets/${name}/download`);
  },
  async uploadFiles(): Promise<void> {
    // ver Tarefa D.3
    logApiCall("POST", "/api/v1/products/{productId}/assets/upload (batch — TODO)");
  },
};
```

### E.11 — `auditService.ts`

```ts
import { IS_API_MODE } from '@/infra/apiMode';
import { apiClient } from '@/shared/services/apiClient';

export const auditService = {
  async listEvents(): Promise<ListAuditEventsResponse> {
    if (IS_API_MODE) return apiClient.get<ListAuditEventsResponse>('/audit/events');
    return auditStore;
  },
  async recordEvent(event: Omit<AuditEvent, "time">): Promise<void> {
    if (IS_API_MODE) return apiClient.post('/audit/events', event);
    // ... mock da Tarefa D.4 ...
  },
};
```

### E.12 — `knowledgeService.ts`

```ts
import { IS_API_MODE } from '@/infra/apiMode';
import { apiClient } from '@/shared/services/apiClient';

export const knowledgeService = {
  async listNodes(): Promise<ListNodesResponse> {
    if (IS_API_MODE) return apiClient.get<ListNodesResponse>('/products/{productId}/graph/nodes');
    return allNodes;
  },
  async listEdges(): Promise<ListEdgesResponse> {
    if (IS_API_MODE) return apiClient.get<ListEdgesResponse>('/products/{productId}/graph/edges');
    return allEdges;
  },
  async searchNodes(query: string, productSlug?: string): Promise<KGNode[]> {
    if (IS_API_MODE) {
      const path = productSlug ? `/products/${productSlug}/graph/nodes/search?q=${query}` : `/graph/nodes/search?q=${query}`;
      return apiClient.get<KGNode[]>(path);
    }
    // ... mock existente ...
  },
  async getNodePreview(nodeId: string): Promise<GraphNodePreview | undefined> {
    if (IS_API_MODE) return apiClient.get<GraphNodePreview>(`/graph/nodes/${nodeId}/preview`);
    // ... mock existente ...
  },
  async listRelated(nodeId: string): Promise<RelatedNode[]> {
    if (IS_API_MODE) return apiClient.get<RelatedNode[]>(`/graph/nodes/${nodeId}/related`);
    // ... mock existente ...
  },
  async markInsightReviewed(text: string): Promise<void> {
    if (IS_API_MODE) return apiClient.post('/products/{productId}/graph/insights/review', { text });
    logApiCall("POST", "/api/v1/products/{productId}/graph/insights/review", { text });
  },
  async resolveOrphan(id: string, action: string): Promise<void> {
    if (IS_API_MODE) return apiClient.post(`/products/{productId}/graph/orphans/${id}/resolve`, { action });
    logApiCall("POST", `/api/v1/products/{productId}/graph/orphans/${id}/resolve`, { action });
  },
  async resolveOrphans(ids: string[]): Promise<void> {
    if (IS_API_MODE) return apiClient.post('/products/{productId}/graph/orphans/resolve', { ids });
    logApiCall("POST", "/api/v1/products/{productId}/graph/orphans/resolve", { ids });
  },
  async ensureNodeForContent(nodeId: string, label: string, productSlug: string, type: KGEntityType = "Página"): Promise<void> {
    if (IS_API_MODE) return apiClient.post(`/products/${productSlug}/graph/nodes`, { id: nodeId, label, type });
    // ... mock existente ...
  },
  async createEdge(from: string, to: string, productSlug: string, edgeType: EdgeType = "RELATED_TO"): Promise<void> {
    if (IS_API_MODE) return apiClient.post(`/products/${productSlug}/graph/edges`, { from, to, edgeType });
    // ... mock existente (com validações) ...
  },
};
```

### E.13 — `dashboardService.ts`

```ts
import { IS_API_MODE } from '@/infra/apiMode';
import { apiClient } from '@/shared/services/apiClient';

export const dashboardService = {
  async getSummary(): Promise<DashboardSummaryResponse> {
    if (IS_API_MODE) return apiClient.get<DashboardSummaryResponse>('/dashboard/summary');
    return dashboardSummary;
  },
};
```

### E.14 — `eventsService.ts` e `globalsService.ts`

```ts
// eventsService.ts
async listEvents(productSlug: string): Promise<PageEvent[]> {
  if (IS_API_MODE) return apiClient.get<PageEvent[]>(`/products/${productSlug}/events`);
  return eventsStore.filter((e) => e.productSlug === productSlug);
},
// createEvent, updateEvent, deleteEvent: idem, com IS_API_MODE + apiClient

// globalsService.ts
async getGlobals(productSlug: string): Promise<ProductGlobals> {
  if (IS_API_MODE) return apiClient.get<ProductGlobals>(`/products/${productSlug}/globals`);
  // ... mock existente ...
},
async updateGlobals(productSlug: string, req: UpdateGlobalsRequest): Promise<ProductGlobals> {
  if (IS_API_MODE) return apiClient.put<ProductGlobals>(`/products/${productSlug}/globals`, req);
  // ... mock existente ...
},
```

### E.15 — `feedbackService.ts`

```ts
import { IS_API_MODE } from '@/infra/apiMode';
import { apiClient } from '@/shared/services/apiClient';

export const feedbackService = {
  async create(req: CreateFeedbackRequest): Promise<{ id: string }> {
    if (IS_API_MODE) return apiClient.post<{ id: string }>('/feedback', req);
    // ... mock existente ...
  },
  async listAll(): Promise<FeedbackSummary[]> {
    if (IS_API_MODE) return apiClient.get<FeedbackSummary[]>('/feedback');
    return [...feedbackStore];
  },
};
```

### E.16 — `productAssignmentsService.ts`

```ts
import { IS_API_MODE } from '@/infra/apiMode';
import { apiClient } from '@/shared/services/apiClient';

export const productAssignmentsService = {
  async listForProduct(productId: string): Promise<ProductAssignmentSummary[]> {
    if (IS_API_MODE) return apiClient.get<ProductAssignmentSummary[]>(`/products/${productId}/assignments`);
    return assignmentsStore.filter((a) => a.productId === productId);
  },
  async assign(req: AssignProductUserRequest): Promise<ProductAssignmentSummary> {
    if (IS_API_MODE) return apiClient.post<ProductAssignmentSummary>(`/products/${req.productId}/assignments`, req);
    // ... mock existente (com invite + user lookup) ...
  },
};
```

---

## F. `vite.config.ts` — Build para Spring Boot + proxy de dev

Substituir o `vite.config.ts` atual por:

```ts
import { defineConfig } from 'vite'
import path from 'path'
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  assetsInclude: ['**/*.svg', '**/*.csv'],

  build: {
    /**
     * Spring Boot serve arquivos estáticos de `src/main/resources/static`.
     * `npm run build` dentro de `frontend/` deposita os artefatos lá
     * diretamente — sem copiar arquivos manualmente.
     *
     * Para produção com imagem Docker, o Dockerfile do frontend pode usar
     * este outDir ou copiar para o jar na etapa de build multi-stage.
     */
    outDir: '../backend/src/main/resources/static',
    emptyOutDir: true,
  },

  server: {
    /**
     * Proxy de desenvolvimento: redireciona chamadas /api/* e /auth/* para
     * o Spring Boot local, evitando CORS durante o desenvolvimento com
     * `VITE_API_MODE=api`.
     *
     * Em modo mock (VITE_API_MODE=mock), o proxy nunca é atingido — nenhuma
     * chamada HTTP real é feita.
     */
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/auth': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
    port: 5173,
  },
})
```

---

## G. Spring Boot — catch-all para SPA routing

Para que o React Router funcione quando o usuário acessa rotas diretamente (ex.: `/products/maestro-beton`) e o Spring Boot serve o frontend, é necessário um controller catch-all.

Criar `backend/src/main/java/com/aegis/web/SpaFallbackController.java`:

```java
package com.aegis.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Catch-all para o React SPA: qualquer rota não mapeada pelo backend
 * retorna o index.html do build do Vite, deixando o React Router resolver.
 *
 * Excluídos: /api/**, /auth/**, /actuator/** (servidos pelo backend).
 * Referência: frontend/vite.config.ts → build.outDir aponta para
 * src/main/resources/static, onde o Spring Boot serve arquivos estáticos.
 */
@Controller
public class SpaFallbackController {

    @RequestMapping(value = {
        "/",
        "/login",
        "/invite",
        "/reset-password",
        "/forgot-password",
        "/forgot-password/sent",
        "/products/**",
        "/tenants/**",
        "/users/**",
        "/settings/**",
        "/notifications/**",
        "/audit/**",
        "/dashboard/**",
        "/modules/**",
    })
    public String index() {
        return "forward:/index.html";
    }
}
```

> **Atenção:** a anotação `@RequestMapping` com padrões `/**` NÃO deve incluir `/api/**` nem `/auth/**` — esses prefixos já são capturados pelos controllers de API antes de chegar aqui.

---

## H. Verificações de segurança (o que NÃO deve mudar)

Antes de considerar a sprint concluída, verificar que os itens abaixo estão intocados:

| Item | Arquivo(s) | Verificação |
|---|---|---|
| Lógica de roles e permissões | `core/permissions/roles.ts`, `navConfig.ts` | Nenhuma linha alterada |
| AuthContext | `core/auth/AuthContext.tsx` | Nenhuma linha alterada |
| `resolveEnabledModules` | `core/products/moduleDefaults.ts` | Nenhuma linha alterada |
| Fallback de módulos por tipo | `core/products/moduleDefaults.ts` → `PRODUCT_TYPE_MODULE_DEFAULTS` | Nenhuma linha alterada |
| `getApiMode()` em keycloakConfig | `core/config/keycloakConfig.ts` | Nenhuma linha alterada |
| `setAuthTokenProvider` | `shared/services/apiClient.ts` | Nenhuma linha alterada |
| `setNotificationsCurrentUser` | `notificationsService.ts` | Chamada no AuthContext preservada |

---

## I. Critérios de aceite

### Modo mock (VITE_API_MODE=mock ou variável ausente)

- [ ] Todos os produtos no mock inicial exibem módulos corretos (via `modulesList` explícito). `Maestro Beton` mostra: Páginas, Conteúdo, Assets, Forms, SEO, Analytics.
- [ ] `Conecta Talentos` (0 módulos) não mostra módulos na sidebar.
- [ ] Habilitar um módulo via `ModuleCatalog` em um produto → o `modulesList` do produto é atualizado → a sidebar/nav daquele produto reflete o módulo imediatamente na próxima navegação.
- [ ] Criar um produto novo → módulos aparecem na tela de módulos do produto recém-criado.
- [ ] Editar um produto (mudar módulos) → lista de módulos do produto atualiza imediatamente.
- [ ] Publicar um formulário → `status` muda para `"published"` no `formsStore` → o badge na lista de formulários reflete.
- [ ] `npm run dev` sem erros. `npm run build` sem erros de tipo. TypeScript em modo strict.
- [ ] Nenhuma lógica de permissão/role/auth alterada — `PRODUCT_MANAGER` ainda não vê produtos de outros tenants, `EDITOR` ainda não vê a aba de Settings, etc.

### Modo api (VITE_API_MODE=api)

- [ ] Com o backend Spring Boot rodando em `localhost:8080`, mudar para `VITE_API_MODE=api` → `npm run dev` → login → dashboard carrega dados reais.
- [ ] Sem backend, `VITE_API_MODE=api` mostra erros de rede (esperado — não trava a aplicação com tela branca; os erros devem ser capturados pelos hooks `useAsyncData` existentes e exibir `PartialErrorWidget`).
- [ ] Proxy de dev funciona: chamadas `/api/*` redirecionadas para `localhost:8080`.

### Build Spring Boot

- [ ] `npm run build` dentro de `frontend/` deposita arquivos em `backend/src/main/resources/static/`.
- [ ] `mvn spring-boot:run` no `backend/` → acessar `http://localhost:8080/` → React app carrega.
- [ ] Acessar `http://localhost:8080/products` diretamente (sem navegar pelo app) → `SpaFallbackController` serve o `index.html` e o React Router assume.

---

## J. Commit sugerido

```bash
git add frontend/src/infra/ \
        frontend/src/domains/products/mocks/products.mocks.ts \
        frontend/src/domains/products/services/productsService.ts \
        frontend/src/domains/products/components/ModuleCatalog.tsx \
        frontend/src/domains/products/pages/ \
        frontend/src/domains/content/services/contentService.ts \
        frontend/src/domains/forms/services/formsService.ts \
        frontend/src/domains/*/services/*.ts \
        frontend/src/core/*/services/*.ts \
        frontend/vite.config.ts \
        frontend/.env \
        frontend/.env.example \
        backend/src/main/java/com/aegis/web/SpaFallbackController.java

git commit -m "feat(frontend): mocks alta fidelidade, toggle mock<->api e build para Spring Boot"
```
