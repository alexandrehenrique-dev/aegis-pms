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

**Product**: `id`, `tenantId`, `key` (único por tenant), `name`, `type`, `status`, `defaultLocale`, `assetStorageStrategy` (`"local"|"s3"`, default `"local"`), `createdAt`, `updatedAt`.

> `assetStorageStrategy` foi adicionado pela Sprint 13 do frontend — escolhido no wizard de criação de produto (passo novo, ver Sprint 13). Ao criar o produto com `assetStorageStrategy: "local"`, o backend deve provisionar a estrutura de pastas do produto no storage local **como parte da transação de criação** (ou logo após, de forma assíncrona com retry — decisão do GPT, documentar a escolha), delegando para o domínio `asset` (etapa 11, Seção D) — `ProductService` chama `AssetStorageProvisioningService.provisionFor(productId, strategy)`, não o contrário. Detalhe completo da estratégia de storage (Strategy pattern, S3, resolução por UUID, riscos) está na etapa 11 — esta etapa só registra o campo e o ponto de integração.

**ProductModule**: `id`, `productId`, `moduleKey`, `enabled`, `settingsJson`, `createdAt`, `updatedAt`.

### B. Catálogo de módulos (enum no backend para o MVP)

```txt
CORE, CONTENT, PAGES, BLOCKS, ASSETS, FORMS, SUBMISSIONS, SEO, ANALYTICS,
INTEGRATIONS, JOBS, PORTFOLIO, LIBRARY, KNOWLEDGE_BASE, MUSIC, BOOKS,
COMMENTS, CONTRIBUTORS, KNOWLEDGE_GRAPH, ECOMMERCE
```

`ECOMMERCE` foi adicionado pela Sprint 13 do frontend — **registrado, não implementado**. Nenhuma etapa desta sprint cria carrinho/checkout/pagamento; o módulo existe no enum só para já ter um lugar reservado quando essa funcionalidade for desenhada (sprint futura dedicada). Deve poder ser listado e (no máximo) habilitado/desabilitado como qualquer outro módulo, sem nenhuma regra de negócio própria por enquanto.

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

### E. Module-gating — `@RequireModule` e `ModuleAccessAspect` (ADR-0015)

Habilitar/desabilitar módulo (Seção C) não pode ficar só decorativo — as etapas de domínio seguintes (07, 10, 11, 12, 13, 17, 21) dependem de um mecanismo, implementado **aqui**, que bloqueia de fato o acesso quando o módulo está desabilitado. Implementar nesta etapa:

- Anotação `@RequireModule(ModuleKey value)` (`@Target(ElementType.METHOD)`, `@Retention(RUNTIME)`), aplicável a métodos de controller.
- `ModuleAccessAspect` (Spring AOP, `@Aspect` + `@Around`): intercepta métodos anotados, extrai `productId` dos argumentos do método (via `@PathVariable` correspondente — documentar a convenção exata escolhida, ex. exigir que todo método anotado tenha um parâmetro `UUID productId`), consulta `ProductModuleRepository.existsByProductIdAndModuleKeyAndEnabledTrue(productId, moduleKey)`.
- Se `false`: lançar `ModuleDisabledException` (`RuntimeException` própria), mapeada por `@ExceptionHandler`/`@ControllerAdvice` para `403` com corpo `{"error": "MODULE_DISABLED", "moduleKey": "<X>"}`.
- Se `true`: segue normalmente para o método do controller.

Este mecanismo vale até para `SUPER_ADMIN` — módulo desabilitado bloqueia todo papel, sem exceção (para usar a funcionalidade, primeiro habilita o módulo via `enable`).

As etapas 07/17 (`KNOWLEDGE_GRAPH`), 10 (`CONTENT`), 11 (`ASSETS`), 12 (`FORMS`), 13 (`ANALYTICS`) e 21 (`PAGES`) anotam seus próprios controllers com `@RequireModule(ModuleKey.X)` reaproveitando esta implementação — não recriam o mecanismo. Domínios de fundação (09 tenants/ProductAssignment, 14 users, 15 audit, 16 settings/dashboard, 23 notification) **não** usam `@RequireModule` — não são módulos que um tenant liga/desliga.

### F. Padrão de qualidade e entrega (obrigatório)

> Resumo — detalhe completo (exemplos de código, regra de exclusão de cobertura, JaCoCo) em `00_padrao_qualidade_e_arquitetura.md`.

- **Java 25**. Javadoc obrigatório na interface e em todo método de `TenantRepository`, `TenantMembershipRepository`, `ProductRepository`, `ProductModuleRepository`. Mappers via MapStruct. 100% de cobertura nas classes funcionais (DTOs de transporte — `CreateTenantRequest`, `ProductSummary`, etc. — ficam fora da régua). `mvn clean verify` deve falhar se a cobertura cair.
- Entregar em rodadas, nesta ordem:
  1. `Tenant`, `TenantMembership`, `Product`, `ProductModule` (entities) + `TenantRepository`, `TenantMembershipRepository`, `ProductRepository`, `ProductModuleRepository` + testes `@DataJpaTest` de cada repository.
  2. `TenantMapper`, `ProductMapper`, `ProductModuleMapper` (MapStruct) + testes de mapper.
  3. `TenantService`, `ProductService`, `ProductModuleService` (regras da Seção D) + testes com mocks de repository/mapper.
  4. `TenantController`, `ProductController` (endpoints da Seção C) + testes `@WebMvcTest` + validação manual via `curl` (Seção "Validação" abaixo).
  5. `@RequireModule` + `ModuleAccessAspect` + `ModuleDisabledException`/`@ControllerAdvice` (Seção E) + teste de integração (`@SpringBootTest`, um controller de exemplo anotado) confirmando 403 com módulo desabilitado e passagem normal com módulo habilitado.

## Critérios de aceite

- [ ] Criar tenant via API funciona e cria a membership de `TENANT_ADMIN` automaticamente.
- [ ] Criar produto vinculado a um tenant funciona.
- [ ] Listagem de produtos respeita membership do usuário autenticado.
- [ ] Habilitar/desabilitar módulo em produto funciona e rejeita `moduleKey` desconhecido.
- [ ] Tudo persiste corretamente (sobrevive a restart do backend).
- [ ] Criar produto com `assetStorageStrategy: "local"` (ou omitido, usando o default) provisiona a estrutura de pastas do produto (ver etapa 11, Seção D) antes de retornar 201.
- [ ] Criar produto com `assetStorageStrategy: "s3"` não tenta criar pasta local nenhuma.
- [ ] `@RequireModule`/`ModuleAccessAspect` existem e bloqueiam com 403 `MODULE_DISABLED` quando o módulo do path está desabilitado no produto, mesmo para `SUPER_ADMIN` (ADR-0015).

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
