# Sprint de Integração 03 — Núcleo de navegação: Products, Tenants, Dashboard, Notifications

> **Pré-requisito:** Sprint 02 concluída (slug→UUID, placeholders, multipart).
>
> **Foco:** ligar as telas centrais de navegação ao backend real. Sem isto, nenhuma tela de domínio tem contexto válido (tenant ativo, produto ativo) para chamar os seus próprios endpoints.
>
> **Branch:** `integration/03-nucleo-navegacao`

---

## A. Products — `productsService.ts`

### A.1 — Listagem e mapeamento de resposta

`GET /api/v1/products` do backend retorna `List<ProductSummary>` com shape:
```json
{ "id": "UUID", "tenantId": "UUID", "key": "maestro-beton", "type": "Site Institucional", "status": "ACTIVE", "defaultLocale": "pt-BR", "createdAt": "...", "updatedAt": "..." }
```

O frontend espera:
```ts
{ id, name, type, status: "Ativo"|"Pendente"|"Arquivado"|"Sem módulos", modules: number, last, score }
```

O backend **não** retorna `name` em `ProductSummary`. Verificar `br.com.byop.aegis.product.dto.ProductSummary` — se `name` não existe, adicionar o campo ao DTO Java OU construir `name` no frontend a partir de `key` (desideal). Decisão recomendada: **adicionar `name` ao DTO backend** na próxima sessão de backend, e no frontend mapear apenas os campos disponíveis por enquanto.

Criar `mapProductSummary(dto)` em `frontend/src/domains/products/mappers/productMapper.ts`:

```ts
export function mapProductSummary(dto: ProductSummaryDto): ProductSummary {
  return {
    id: dto.id,
    key: dto.key,
    name: dto.name ?? dto.key,         // fallback até o DTO ter name
    type: dto.type,
    status: mapProductStatus(dto.status),
    modules: 0,                        // buscar de /products/{id}/modules se necessário
    last: dto.updatedAt ?? '—',
    score: '—',
    tenantId: dto.tenantId,
  };
}

function mapProductStatus(s: string): ProductSummary['status'] {
  const map: Record<string, ProductSummary['status']> = {
    ACTIVE: 'Ativo', PENDING: 'Pendente', ARCHIVED: 'Arquivado', INACTIVE: 'Sem módulos',
  };
  return map[s] ?? 'Pendente';
}
```

Aplicar o mapper na resposta de `productsService.listProducts()`:

```ts
async listProducts(): Promise<ListProductsResponse> {
  if (IS_API_MODE) {
    const dtos = await apiClient.get<ProductSummaryDto[]>('/products');
    return dtos.map(mapProductSummary);
  }
  return productsStore;
},
```

### A.2 — `checkSlugAvailable` — remover ou adaptar

Endpoint `GET /products/check-slug` **não existe** no backend. Duas opções:

**Opção A (recomendada):** Remover a chamada. A validação de slug duplicado é retornada pelo `POST /products` com status 400. Adaptar a UI para mostrar o erro de validação do servidor na submissão do formulário ao invés de validar em tempo real.

**Opção B:** Se a validação em tempo real for requisito de UX, adicionar `GET /api/v1/products/check-slug?slug=...` ao backend (retrofit simples, registrar para próxima sessão de backend).

Em qualquer caso, não deixar a chamada apontar para um endpoint inexistente.

### A.3 — `archiveProduct` — substituir por `update` com status

Endpoint `POST /products/{id}/archive` **não existe**. Substituir por:

```ts
async archiveProduct(id: string): Promise<void> {
  if (IS_API_MODE) {
    await apiClient.put(`/products/${id}`, { status: 'ARCHIVED' });
    return;
  }
  const p = productsStore.find(x => x.id === id);
  if (p) p.status = 'Arquivado';
},
```

### A.4 — `AuthContext` — `effectiveProduct` alimentado pelo backend

Após Sprint 01 (D.2), `availableTenants` vem do `/me`. O endpoint `/me` já retorna os tenants do usuário. Para produtos: após selecionar o tenant ativo, chamar `GET /api/v1/products` com header `X-Tenant-Id: {tenantId}` (ou filtro por `tenantId` na query — verificar como o backend filtra por tenant logado).

Se o backend usa `Authorization` Bearer + tenant do JWT: nenhum header adicional. Se usa `X-Tenant-Id`: adicionar ao `apiClient` como default header após seleção de tenant.

---

## B. Tenants — `tenantsService.ts`

`tenantsService` já tem `IS_API_MODE` em todos os métodos com paths corretos (`/tenants`, `/tenants/{id}`). Verificar apenas o mapeamento de resposta:

Backend retorna `{ id, key, name, status: "ACTIVE"|"SUSPENDED", plan, createdAt, updatedAt }`.
Frontend espera `{ id, name, plan, productCount, lastAccess, status: "ativo"|"suspenso" }`.

Criar `mapTenantSummary(dto)` em `frontend/src/core/tenants/mappers/tenantMapper.ts`:

```ts
export function mapTenantSummary(dto: TenantSummaryDto): TenantOption {
  return {
    id: dto.id,
    name: dto.name,
    plan: dto.plan,
    productCount: 0,    // não disponível no endpoint de listagem — omitir ou buscar separado
    lastAccess: '—',    // não disponível — omitir
    status: dto.status === 'ACTIVE' ? 'ativo' : 'suspenso',
  };
}
```

Aplicar nos métodos `listTenants()` e `getTenant()`.

---

## C. Dashboard — `dashboardService.ts`

`GET /api/v1/dashboard/summary` — verificar que o endpoint existe no backend (`DashboardController`). Se não existe ainda, registrar como retrofit e manter mock para o dashboard.

A resposta do backend precisa bater com `DashboardSummaryResponse` do frontend. Se houver divergência, criar mapper semelhante ao de produtos.

---

## D. Notifications — `notificationsService.ts`

### D.1 — Corrigir paths `/me` → `/mine`

```ts
// ANTES:
async listMine() { return apiClient.get('/notifications/me'); }
async getPendingModal() { return apiClient.get('/notifications/me/pending-modal'); }

// DEPOIS:
async listMine() { return apiClient.get('/notifications/mine'); }
async getPendingModal() { return apiClient.get('/notifications/mine/pending-modal'); }
```

### D.2 — Remover acoplamento a `mockUsers` / `mockTenantsByUser`

`notificationsService.ts` importa `mockUsers` e `mockTenantsByUser` (linhas 1-2) para implementação mock interna (`allUserIds()`, `userIdsForTenant()`). Em modo API esses imports são irrelevantes mas ainda entram no bundle.

Guardar os imports dentro de bloco `if (!IS_API_MODE)` — como são usados em funções internas ao módulo e não em `import` top-level, a solução prática é mover a lógica mock para um módulo `_mock.ts` importado dinamicamente, ou simplesmente aceitar que em modo API as funções `allUserIds()` e `userIdsForTenant()` não são chamadas (o `IS_API_MODE` na frente de cada método já resolve).

### D.3 — Mapeamento de `NotificationWithStatus`

Verificar shape da resposta de `GET /api/v1/notifications/mine` no backend (`NotificationController`) e criar mapper se os campos divergirem.

---

## E. `ProductSelectScreen` e `TenantSelectScreen`

Após Sprint 01 (F), `TenantSelectScreen` deve receber `availableTenants` do `AuthContext`. Verificar que não importa diretamente `mockTenantsByUser`.

`ProductSelectScreen` deve chamar `productsService.listProducts()` (com tenant ativo no contexto) ao invés de consumir dados do `AuthContext` hardcoded. Se o `AuthContext` já tem a lista de produtos do tenant selecionado via `/me`, usar esses dados diretamente.

---

## F. `ModuleCatalog` — integração opcional

`productsService.listModuleCatalog()` ignora `IS_API_MODE` e sempre retorna mock. O módulo catálogo existe no backend (etapa 19 — `GET /products/{id}/modules`). Para esta sprint: manter mock por enquanto, registrar como retrofit menor.

---

## G. Critérios de aceite

- [ ] `GET /api/v1/products` retorna lista de produtos reais do backend — tela `ProductSelectScreen` exibe os produtos do CLIENTES BETA tenant.
- [ ] `GET /api/v1/tenants` retorna lista de tenants reais — `TenantsPage` exibe os tenants cadastrados.
- [ ] `PUT /api/v1/products/{id}` com `status: ARCHIVED` arquiva o produto corretamente.
- [ ] `GET /api/v1/notifications/mine` retorna notificações do usuário autenticado — bell badge no topo mostra a contagem correta.
- [ ] `POST /api/v1/notifications/{id}/mark-read` marca como lida e o badge atualiza.
- [ ] Modo mock não regrediu — `VITE_API_MODE=mock` funciona como antes.
- [ ] `npm run typecheck` — zero erros.

---

## H. Commit sugerido

```bash
git add frontend/src/domains/products/ frontend/src/core/tenants/ frontend/src/core/notifications/ frontend/src/domains/dashboard/
git commit -m "feat(integration): products, tenants, dashboard e notifications integrados ao backend real"
```
