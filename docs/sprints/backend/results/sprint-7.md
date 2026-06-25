# Aegis PMS --- Relatório Final da Sprint 07

## Identificação

-   **Sprint:** 07 --- Modelo Core: Tenant, Membership, Product e
    Catálogo de Módulos
-   **Status:** ✅ Concluída
-   **Resultado da validação:** BUILD SUCCESS

## Resumo Executivo

Esta sprint implementou o núcleo do modelo Product First do Aegis PMS,
estabelecendo a base para os próximos domínios (Content, Pages, Assets,
Forms, Analytics e Knowledge Graph). Foram entregues o modelo
persistente, regras de negócio, APIs REST, controle de acesso por
produto, catálogo de módulos, bloqueio por módulos via AOP e cobertura
de testes integral.

## Entregas

### Passo 1

-   Entidades: Tenant, TenantMembership, Product, ProductModule e
    ProductAssignment.
-   Repositories com Javadoc.
-   Migration Flyway `V2__core_tenant_product_modules.sql`.
-   Testes `@DataJpaTest`.

### Passo 2

-   DTOs:
    -   TenantSummary
    -   ProductSummary
    -   ProductDetail
    -   ProductModuleSummary
-   Mappers MapStruct.
-   Testes de mapper.

### Passo 3

-   Services:
    -   TenantService
    -   ProductService
    -   ProductModuleService
    -   ProductAccessResolver
-   Exceções de domínio.
-   Regras:
    -   criação automática de TenantMembership;
    -   ProductAssignment automático;
    -   filtro de produtos por papel;
    -   dependência `KNOWLEDGE_GRAPH -> CONTENT`.

### Passo 4

-   Controllers REST:
    -   GET/POST `/api/v1/tenants`
    -   GET/POST `/api/v1/products`
    -   GET `/api/v1/products/{productId}`
    -   POST enable/disable de módulos
-   Requests públicos.
-   CoreExceptionHandler.
-   Testes `@WebMvcTest`.

### Passo 5

-   `@RequireModule`
-   `ModuleAccessAspect`
-   `ModuleDisabledException`
-   `ModuleProductIdMissingException`
-   Bloqueio de módulos inclusive para `SUPER_ADMIN`.
-   Testes de integração do Aspect.

## Qualidade

-   Todos os testes passando.
-   Cobertura JaCoCo aprovada.
-   SonarQube for IDE limpo após remoção dos alertas, incluindo
    externalização do path `/admin/realms/` para configuração.

## Validações

-   `mvn clean verify`
-   **BUILD SUCCESS**
-   **179 testes**
-   **0 failures**
-   **0 errors**
-   **0 skipped**
-   **JaCoCo:** All coverage checks have been met.

## Validação manual

-   Collection Postman da Sprint 07 executada com sucesso.
-   Fluxos de Tenant, Product, Module e autenticação validados.

## Observações importantes

-   Base package oficial mantido: `br.com.byop.aegis`.
-   Nenhum uso de `br.com.aegis.pms`.
-   Nenhuma alteração indevida em contratos, migrations ou arquitetura.
-   O backend permanece aderente aos princípios Product First, Contract
    First, REST First e aos ADRs do projeto.

## Pendências para próximas sprints

-   Evoluir os domínios consumidores do `ProductAccessResolver`.
-   Aplicar `@RequireModule` nos módulos futuros (Content, Pages,
    Assets, Forms, Analytics e Knowledge Graph).
-   Atualizar o `SPRINT-RESULTADO.md` cumulativo antes do encerramento
    definitivo da sprint.
