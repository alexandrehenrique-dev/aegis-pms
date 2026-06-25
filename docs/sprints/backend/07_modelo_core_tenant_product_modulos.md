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

> **`type` é catálogo fechado, não string livre** (ver ADR-0017): `"Site Institucional" | "Portal" | "Knowledge Base" | "Portfolio" | "Library/Books/Music" | "Produto SaaS" | "Custom"` — os mesmos 7 valores de `ProductTypeKey` (frontend, `core/products/moduleDefaults.ts`). `type` desconhecido na criação é rejeitado com 400, mesmo princípio já usado para `moduleKey`/`BlockType`/`nodeType`. É este campo que a etapa 24 usa para decidir o esqueleto de páginas e os módulos recomendados a pré-habilitar — `"Custom"` é o único valor que não dispara nenhum dos dois.

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

> **Dependência entre módulos (adicionado nesta revisão — auditoria de cobertura)**: o frontend (`core/products/moduleDefaults.ts`, `KNOWLEDGE_GRAPH_DEPENDENCY = "Conteúdo"`) já define que `KNOWLEDGE_GRAPH` exige `CONTENT` habilitado, mas nenhuma etapa validava isso no backend — só a UI sugeria a dependência, nunca bloqueava de fato (mesmo tipo de gap que motivou a ADR-0015). `POST .../modules/{moduleKey}/enable` passa a validar: se o `moduleKey` tiver uma dependência registrada e ela não estiver habilitada no produto, rejeitar com 400 (`{"error": "MODULE_DEPENDENCY_MISSING", "moduleKey": "KNOWLEDGE_GRAPH", "requires": "CONTENT"}`). O mapa de dependências (hoje só `KNOWLEDGE_GRAPH → CONTENT`) fica num `Map<ModuleKey, ModuleKey>`/equivalente no `ProductModuleService`, não hardcoded num `if` solto — outras dependências futuras (frontend já lista mais, ex. catálogo de módulos da Sprint 05) só precisam de uma entrada nova nesse mapa. `disable` do módulo dependência (`CONTENT`) com `KNOWLEDGE_GRAPH` ainda habilitado: também rejeitar com 400 pelo mesmo motivo (nunca deixar o produto num estado inconsistente em nenhuma direção).

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

- Usuário que cria um tenant vira `TENANT_ADMIN` (cria a `TenantMembership` automaticamente).
- Produto pertence a exatamente um tenant.
- **Produto criado gera `ProductAssignment` automático para o criador** (ADR-0018): ao persistir um produto, criar imediatamente um `ProductAssignment` com `role: "product_manager"` e `status: "atribuido"` para o `userSubject` do token. Isso vale para qualquer papel que crie o produto (SUPER_ADMIN, TENANT_ADMIN). Sem esse registro, o criador não teria acesso ao conteúdo do produto via `ProductAccessResolver`.
- Produto de outro tenant retorna 404 (nunca 403 — não revelar existência).
- `product.key` único por tenant; `tenant.key` único globalmente.

**Regras de visibilidade de `GET /api/v1/products` por papel (ADR-0019)** — a listagem não usa o mesmo filtro para todos os papéis:

| Papel | Filtro aplicado no `ProductService` |
|---|---|
| `SUPER_ADMIN` | Todos os produtos de todos os tenants (sem filtro) |
| `TENANT_ADMIN` | `findAllByTenantId(caller.tenantId)` via `TenantMembership` |
| `PRODUCT_MANAGER`, `EDITOR`, `VIEWER` | `findAllByUserSubject(caller.subject)` via `ProductAssignment` |

O critério de filtro é determinado internamente pelo `ProductService` com base no papel do `AuthenticatedUser` — nunca recebido como parâmetro do cliente.

**`ProductAccessResolver`** — helper implementado **nesta etapa**, reutilizado por todas as etapas de domínio de produto (08-22 para content, pages, assets, forms, analytics, knowledge graph):

```java
/**
 * Resolve se o usuário autenticado tem acesso ao conteúdo de um produto.
 * Implementa as regras de ADR-0018 e ADR-0019.
 * Deve ser chamado por todos os Services de domínio antes de qualquer operação de conteúdo.
 */
public class ProductAccessResolver {
  /**
   * Valida acesso de conteúdo ao produto.
   *
   * @param caller  usuário autenticado (papel + subject)
   * @param productId produto que se quer acessar
   * @throws ProductContentAccessDeniedException (403) se SUPER_ADMIN sem ProductAssignment
   * @throws ProductNotFoundException (404) se produto não existe ou não pertence ao escopo do caller
   */
  public void assertContentAccess(AuthenticatedUser caller, UUID productId) { ... }
}
```

Lógica interna do `assertContentAccess`:
1. `SUPER_ADMIN` com `ProductAssignment` ativo para este `productId` → **passa** (usa papel do assignment internamente)
2. `SUPER_ADMIN` sem `ProductAssignment` → lança `ProductContentAccessDeniedException` → `403 PRODUCT_CONTENT_ACCESS_DENIED`
3. `TENANT_ADMIN` com `TenantMembership` ativa no tenant do produto → **passa**
4. `PRODUCT_MANAGER | EDITOR | VIEWER` com `ProductAssignment` ativo → **passa**
5. Qualquer outro caso (produto de outro tenant, sem membership) → lança `ProductNotFoundException` → **404**

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
  1. `Tenant`, `TenantMembership`, `Product`, `ProductModule` (entities) + `TenantRepository`, `TenantMembershipRepository`, `ProductRepository`, `ProductModuleRepository`, `ProductAssignmentRepository` (se ainda não existir — pré-criado aqui para o `ProductAccessResolver`) + testes `@DataJpaTest` de cada repository.
  2. `TenantMapper`, `ProductMapper`, `ProductModuleMapper` (MapStruct) + testes de mapper.
  3. `TenantService`, `ProductService` (incluindo `ProductAssignment` automático ao criar produto + filtro de listagem por papel), `ProductModuleService` + `ProductAccessResolver` — testes com mocks cobrindo: SUPER_ADMIN sem assignment → 403; SUPER_ADMIN com assignment → passa; TENANT_ADMIN → passa; EDITOR sem assignment → 404.
  4. `TenantController`, `ProductController` + testes `@WebMvcTest` + validação via `curl`.
  5. `@RequireModule` + `ModuleAccessAspect` + `ModuleDisabledException`/`@ControllerAdvice` (Seção E) + teste de integração confirmando 403 com módulo desabilitado e passagem normal com módulo habilitado.

## Critérios de aceite

- [ ] Criar tenant via API funciona e cria a `TenantMembership` de `TENANT_ADMIN` automaticamente.
- [ ] Criar produto funciona e cria `ProductAssignment` automático para o criador com `role: "product_manager"`.
- [ ] `GET /products` com token de `PRODUCT_MANAGER` retorna apenas os produtos com `ProductAssignment` para seu subject — não todos os do tenant.
- [ ] `GET /products` com token de `TENANT_ADMIN` retorna todos os produtos do tenant.
- [ ] `GET /products` com token de `SUPER_ADMIN` retorna todos os produtos de todos os tenants.
- [ ] `ProductAccessResolver.assertContentAccess`: SUPER_ADMIN sem `ProductAssignment` → 403 `PRODUCT_CONTENT_ACCESS_DENIED`; SUPER_ADMIN com `ProductAssignment` → passa; TENANT_ADMIN → passa; EDITOR sem assignment → 404.
- [ ] Habilitar/desabilitar módulo em produto funciona e rejeita `moduleKey` desconhecido.
- [ ] Habilitar `KNOWLEDGE_GRAPH` sem `CONTENT` habilitado é rejeitado com 400 `MODULE_DEPENDENCY_MISSING`; habilitando `CONTENT` primeiro, `KNOWLEDGE_GRAPH` passa a ser aceito.
- [ ] Desabilitar `CONTENT` com `KNOWLEDGE_GRAPH` ainda habilitado é rejeitado com 400.
- [ ] Tudo persiste corretamente (sobrevive a restart do backend).
- [ ] Criar produto com `assetStorageStrategy: "local"` (ou omitido, usando o default) provisiona a estrutura de pastas do produto (ver etapa 11, Seção D) antes de retornar 201.
- [ ] Criar produto com `assetStorageStrategy: "s3"` não tenta criar pasta local nenhuma.
- [ ] `@RequireModule`/`ModuleAccessAspect` existem e bloqueiam com 403 `MODULE_DISABLED` quando o módulo do path está desabilitado no produto, mesmo para `SUPER_ADMIN` (ADR-0015).

## Validação

> **Entrega via collection Postman, não só curl** (ver `00_padrao_qualidade_e_arquitetura.md`, Seção 11). Os `curl` abaixo são a especificação exata de cada request — adicione-os à pasta desta etapa em `aegis-postman-collection.json` (collection cumulativa, autenticação via `{{token}}` herdado da pasta "Auth") e devolva o JSON completo atualizado para download.

```bash
curl -X POST http://localhost:8080/api/v1/tenants \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"key":"byop","name":"BYOP"}'

curl -X POST http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"tenantId":"<tenant-id>","key":"maestro-beton","name":"Maestro Beton","type":"Site Institucional","defaultLocale":"pt-BR"}'

curl http://localhost:8080/api/v1/products/<productId> -H "Authorization: Bearer $TOKEN"
# esperado: inclui "modules": [{"moduleKey": "...", "enabled": ...}]
```

## Artefato de continuidade — `SPRINT-RESULTADO.md`

> Ver `00_padrao_qualidade_e_arquitetura.md`, Seção 12. Antes do commit, gere/atualize `docs/sprints/sprint-02-fundacao-backend-gpt/SPRINT-RESULTADO.md` (arquivo inteiro, nunca um diff) com a entrada desta etapa (template fixo da Seção 12.2): classes criadas, endpoints confirmados, qualquer decisão que esta etapa deixou a seu critério (registre a escolha real), e retrofits pendentes para etapas futuras. É o que a próxima conversa do GPT vai receber em vez da memória que ela não tem.

## Commit sugerido

```bash
git add backend/
git commit -m "feat(backend): modelo core tenant/membership/product e catalogo de modulos"
```
