# Etapa 06 — Modelo core: Tenant, Membership, Product e catálogo de módulos

> Cole este arquivo inteiro numa conversa nova do GPT. Pré-requisito: etapa 05 concluída.

## Contexto fixo

Aegis PMS é Product First: `Tenant → Product → Modules/Features/Contracts/Content/Graph`. Backend é a fonte da verdade sobre autorização, ownership e status — o frontend nunca decide regra crítica.

## Objetivo

Modelar Tenant, TenantMembership, Product e ProductModule, com os endpoints mínimos de criação/listagem e o catálogo fixo de módulos do Aegis.

## Tarefas

### A. Entidades

**Tenant**: `id`, `key` (único globalmente), `name`, `status`, `createdAt`, `updatedAt`.

**TenantMembership**: `id`, `tenantId`, `userSubject` (subject do JWT), `role`, `status`, `createdAt`, `updatedAt`.

**Product**: `id`, `tenantId`, `key` (único por tenant), `name`, `type`, `status`, `defaultLocale`, `createdAt`, `updatedAt`.

**ProductModule**: `id`, `productId`, `moduleKey`, `enabled`, `settingsJson`, `createdAt`, `updatedAt`.

### B. Catálogo de módulos (enum no backend para o MVP)

```txt
CORE, CONTENT, PAGES, BLOCKS, ASSETS, FORMS, SUBMISSIONS, SEO, ANALYTICS,
INTEGRATIONS, JOBS, PORTFOLIO, LIBRARY, KNOWLEDGE_BASE, MUSIC, BOOKS,
COMMENTS, CONTRIBUTORS, KNOWLEDGE_GRAPH
```

Módulo desconhecido deve gerar erro de validação.

### C. Endpoints mínimos

```txt
GET  /api/v1/tenants
POST /api/v1/tenants
GET  /api/v1/products
POST /api/v1/products
GET  /api/v1/products/{productId}
POST /api/v1/products/{productId}/modules/{moduleKey}/enable
POST /api/v1/products/{productId}/modules/{moduleKey}/disable
```

### D. Regras de negócio

- Usuário que cria um tenant vira `TENANT_ADMIN` (cria a membership automaticamente).
- Produto pertence a exatamente um tenant.
- Listagem de produtos retorna só produtos de tenants onde o usuário tem membership ativa.
- Produto de outro tenant retorna 404 (nunca 403 — não revelar existência).
- `product.key` único por tenant; `tenant.key` único globalmente.

> Nota para esta sprint: o front-end (Sprint 03, fora do GPT) vai consumir exatamente esses endpoints para a tela de criação de tenant e de produto — não altere os nomes/formatos sem necessidade.

## Critérios de aceite

- [ ] Criar tenant via API funciona e cria a membership de `TENANT_ADMIN` automaticamente.
- [ ] Criar produto vinculado a um tenant funciona.
- [ ] Listagem de produtos respeita membership do usuário autenticado.
- [ ] Habilitar/desabilitar módulo em produto funciona e rejeita `moduleKey` desconhecido.
- [ ] Tudo persiste corretamente (sobrevive a restart do backend).

## Validação

```bash
curl -X POST http://localhost:8080/api/v1/tenants \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"key":"byop","name":"BYOP"}'

curl -X POST http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"tenantId":"<tenant-id>","key":"maestro-beton","name":"Maestro Beton","type":"MUSICIAN_SITE","defaultLocale":"pt-BR"}'

curl http://localhost:8080/api/v1/products/<productId> -H "Authorization: Bearer $TOKEN"
# esperado: inclui "modules": [{"moduleKey": "...", "enabled": ...}]
```

## Commit sugerido

```bash
git add backend/
git commit -m "feat(backend): modelo core tenant/membership/product e catalogo de modulos"
```
