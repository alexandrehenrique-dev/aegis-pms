# Resultado da Sprint 16 — Domínio `audit`

> Evidências detalhadas. O resumo consolidado para colar em conversas futuras está em `docs/sprints/backend/SPRINT-RESULTADO.md`.

**Data de fechamento:** 2026-06-27.

## Objetivo

Implementar o domínio `audit` conforme `docs/sprints/backend/16_dominio_audit.md`: toda ação relevante do sistema (criar/editar/excluir tenant, atribuir produto, convidar usuário, publicar conteúdo, excluir asset etc.) deve gravar um evento de auditoria consultável, através de um serviço central único — substituindo qualquer gravação ad-hoc espalhada pelos domínios — e exposto por endpoints de leitura tenant-scoped, sem nenhum endpoint de escrita direta.

## Resultado

Objetivo atingido. `AuditService.recordEvent(...)` (ver Decisões, item 5, sobre o nome) é o único ponto de gravação em todo o backend; as duas gravações ad-hoc pré-existentes (`TenantService`, `TenantUserService`) foram removidas e substituídas; mais três domínios que nunca auditavam nada (`ProductAssignment`, `Content`, `Asset`) passaram a auditar. Os dois endpoints de leitura (`GET .../audit-events`, `GET .../audit-events/{eventId}`) estão implementados, testados e validados manualmente via Bruno. `mvn clean verify` fecha com `BUILD SUCCESS`, JaCoCo 100% e Spring Modulith aprovado.

## Classes criadas

**Domínio (`br.com.byop.aegis.audit`):**
- `audit/domain/AuditEvent.java` (entity, com record interno `AuditEvent.Creation` para evitar excesso de parâmetros) e `audit/domain/AuditRisk.java` (enum `BAIXO/MEDIO/ALTO`, persistido via `@Enumerated(EnumType.STRING)`, com `contractValue()`/`fromContractValue()`).
- `audit/repository/AuditEventRepository.java` — Javadoc na interface e em todo método declarado; `findAllByFilters` (filtros combináveis `actorSubject`/`productId`/`module`/`risk`, via `@Query` JPQL justificado pela combinatória de filtros opcionais) e `findByIdAndTenantId`.
- `audit/mapper/AuditEventMapper.java` (MapStruct) — `toSummary`/`toDetail`, recebendo `actor`/`tenant`/`target` já resolvidos pelo service (mesmo padrão de `ContentMapper`).
- `audit/dto/AuditEventSummary.java`, `audit/dto/AuditEventDetail.java`.
- `audit/service/AuditRiskCatalog.java` (tabela literal ação→risco) e `audit/service/AuditEventQueryService.java` (listagem/detalhe com isolamento por tenant).
- `audit/controller/AuditEventController.java` — só `GET`.
- `audit/exception/AuditEventNotFoundException.java`, `audit/exception/InvalidAuditRiskFilterException.java`, `audit/exception/AuditEventExceptionHandler.java`.
- `audit/api/AuditService.java` (`@NamedInterface("audit-api")`, classe central exigida pelo artefato — método **`recordEvent`**, não `record`, ver Decisões), `audit/api/AuditRecordCommand.java`, `audit/api/TenantVisibilityPort.java`.

**Migration:** `V8__audit_trail_normalization.sql` — renomeia/retipa colunas da tabela `audit_events` criada na V1 (`actor_user_id UUID → actor_subject VARCHAR(160)`, `entity_type → target_type`, `entity_id UUID → target_id VARCHAR(160)`, `event_type → action`, `payload → diff_json`, `occurred_at → created_at`), adiciona `target_label`, `module`, `risk`, `trace_id`, `ip`, `user_agent`, remove as FKs `tenant_id`/`product_id` e adiciona índices. **V1 nunca foi tocada.**

## Classes alteradas (refactor para `AuditService.recordEvent`)

- `tenant/service/TenantService.java` — removida a gravação ad-hoc via `JdbcTemplate` (`recordTenantDeletionAudit`); adicionados `TENANT_CREATED`/`TENANT_UPDATED`; `updateTenant` ganhou parâmetro `AuthenticatedUser caller`.
- `tenant/controller/TenantController.java` — `updateTenant` passou a extrair e propagar o caller.
- `tenant/api/TenantAccessService.java` — passou a implementar `TenantVisibilityPort` (ver Decisões) e ganhou `findTenantName`.
- `product/user/service/TenantUserService.java` — removida a gravação ad-hoc via `JdbcTemplate` (retrofit pendente desde a Sprint 15); adicionados `USER_INVITED_TO_TENANT` (não auditado antes) e `USER_BLOCKED` (não auditado antes), além de refatorar `USER_REMOVED_FROM_TENANT`/`USER_RESTORED_TO_TENANT`.
- `product/service/ProductAssignmentService.java` — `assignUser`/`removeAssignment` ganharam parâmetro `AuthenticatedUser caller`; adicionados `PRODUCT_ASSIGNMENT_CREATED`/`PRODUCT_ASSIGNMENT_REMOVED`.
- `product/controller/ProductAssignmentController.java` — passou a injetar `AuthenticatedUserProvider` e propagar o caller.
- `content/service/ContentService.java` — adicionados `CONTENT_CREATED` (em `createContent`) e `CONTENT_PUBLISHED` (em `doTransition`, quando `to == PUBLISHED`).
- `asset/service/AssetService.java` — `deleteAsset` ganhou parâmetro `AuthenticatedUser caller`; adicionado `ASSET_DELETED`.
- `asset/controller/AssetController.java` — `deleteAsset` passou a propagar o caller já resolvido por `assertProductAccess`.

## Endpoints confirmados

- `GET /api/v1/tenants/{tenantId}/audit-events` — filtros opcionais `actorSubject`, `productId`, `module`, `risk` via query string.
- `GET /api/v1/tenants/{tenantId}/audit-events/{eventId}`.
- **Confirmado via teste automatizado e via Bruno**: não existe nenhum `POST`/`PUT`/`DELETE` nesses dois paths.

## Decisões de implementação registradas

1. **Migration aditiva (V8), nunca alterar a V1.** A tabela `audit_events` da V1 tinha colunas em inglês/formato diferente do artefato (`actor_user_id UUID`, `event_type`, `payload`, `occurred_at`); a V8 renomeia/retipa via `ALTER TABLE`, mesmo padrão já usado na V4 (etapa 10) para evoluir tabelas de migrations anteriores.
2. **FKs de `tenant_id`/`product_id` removidas deliberadamente** (confirmado com o usuário antes da implementação). A trilha de auditoria precisa sobreviver à exclusão do que ela audita — sem isso, o próprio critério de aceite da sprint ("excluir tenant e depois consultar `audit-events` daquele tenantId") não seria satisfazível. O código ad-hoc anterior (`TenantService`) contornava essa FK gravando `tenant_id = NULL` — um bug real, corrigido nesta sprint.
3. **Isolamento do `AuditEventController` não verifica se o tenant existe na tabela `tenants`** — só verifica `SUPER_ADMIN` ou membership ativa. Decorrência direta da decisão 2: depois que um tenant é excluído, suas `TenantMembership` são cascade-deletadas, então só `SUPER_ADMIN` continua vendo o histórico (acesso forense). Confirmado com o usuário antes da implementação.
4. **Ciclo do Spring Modulith `audit ↔ tenant` resolvido com `TenantVisibilityPort`** (Dependency Inversion). `AuditEventQueryService` precisava de `tenant.api` para isolamento/nome do tenant, e `tenant` precisava de `audit.api` para gravar eventos — dependência cíclica entre os dois slices, rejeitada pelo `ModulithArchitectureTest`. Resolvido definindo `TenantVisibilityPort` (interface) em `audit.api`, implementada por `TenantAccessService` (que já mora em `tenant.api` e já depende de `audit.api` para gravar) — `audit` passou a depender só da própria interface, nunca de `tenant.api` diretamente.
5. **`AuditService.record(...)` renomeado para `AuditService.recordEvent(...)`** — divergência deliberada do nome literal do artefato (`record` é identificador restrito da linguagem desde Java 14+, `java:S6213` do SonarQube). Decisão tomada a pedido explícito do usuário numa rodada de correção de SonarQube for IDE, depois da implementação inicial; todos os call-sites e testes foram atualizados na mesma sessão.
6. **Risco calculado por tabela literal em `AuditRiskCatalog`** (não no chamador) — 12 ações mapeadas (`TENANT_CREATED`→baixo, `TENANT_UPDATED`→medio, `TENANT_DELETED`→alto, `PRODUCT_ASSIGNMENT_CREATED`→baixo, `PRODUCT_ASSIGNMENT_REMOVED`→medio, `USER_INVITED_TO_TENANT`→baixo, `USER_BLOCKED`→medio, `USER_REMOVED_FROM_TENANT`→alto, `USER_RESTORED_TO_TENANT`→medio, `CONTENT_CREATED`→baixo, `CONTENT_PUBLISHED`→medio, `ASSET_DELETED`→alto); ação desconhecida cai no default `MEDIO` (nunca `BAIXO`, por segurança).
7. **`diffJson` é um envelope `{"before":..,"after":..}` montado só dentro de `AuditService`** — os 5 domínios chamadores passam `Map<String,Object> before/after` já estruturados, nunca uma `String` JSON pronta.
8. **`traceId`/`ip`/`userAgent` ficam `null` nesta sprint** — nenhum `Service` de domínio tem acesso a `HttpServletRequest` (proibido pelo padrão de qualidade). Populá-los exigiria um filtro/interceptor dedicado fora do escopo literal desta sprint; ver Retrofits.
9. **`actor`/`tenant`/`target` na resposta são nomes resolvidos, não IDs crus** — `actor` via `IdentityUserDirectory` (fallback pro subject se a busca falhar, mesmo padrão de `ContentService.resolveAuthorName`), `tenant` via `TenantVisibilityPort.findTenantName` (fallback pro `targetLabel` quando o próprio evento audita o tenant excluído, senão pro id bruto), `target` via `targetLabel` com fallback `targetType + " " + targetId`. Decisão registrada porque o artefato (`docs/trace/00_endpoints_esperados.md`, Seção B.6) e o mock do frontend usam strings legíveis, nunca UUID.
10. **`id` foi adicionado ao `AuditEventSummary`**, além dos 7 campos documentados no mock do frontend (`actor`, `action`, `target`, `tenant`, `module`, `time`, `risk`) — necessário para o endpoint de detalhe (`GET .../audit-events/{eventId}`) ser de fato navegável a partir da listagem; o mock atual do frontend é 100% estático e não usa esse campo ainda.

## Retrofits pendentes para etapas futuras

- **`traceId`/`ip`/`userAgent`** continuam `null` em todo evento gravado nesta sprint — se uma sprint futura quiser popular esses campos, precisará de um filtro/interceptor de request (`HandlerInterceptor` ou `Filter`) que capture IP/User-Agent/trace id e os disponibilize via algum contexto request-scoped, sem violar a regra de `Service` nunca acessar `HttpServletRequest`.
- **Bruno**: o cenário "tenant fora do escopo do caller (404)" (`16-audit/tenant-fora-do-escopo-404.bru`) usa o mesmo padrão já estabelecido em `11-content/editor-tentando-publicar-403.bru` (variável `nonMemberToken` sem seed automático — aceita 401 como fallback documentado). Se uma sprint futura criar um fluxo de seed de segundo usuário (sem membership), atualizar ambos os requests para validar o status real (404/403) sempre, não só quando a variável for preenchida manualmente.
- **Module enable/disable** (`ProductModuleService`) não foi auditado nesta sprint — não havia gravação ad-hoc para ele e não está na lista explícita do artefato; permanece fora do escopo até uma sprint que peça explicitamente.

## Riscos remanescentes

- **Quirk de plataforma 403 vs. 405** (ver Seção "Bruno" abaixo) — não é um risco introduzido por esta sprint (mesma classe de comportamento já observada na Sprint 12), mas continua valendo para qualquer endpoint futuro: um método HTTP não mapeado num path existente retorna 403 `insufficient_scope` na aplicação real, nunca 405, por causa de como o filtro de exceções do Spring Security intercepta `HttpRequestMethodNotSupportedException`. Não corrigido nesta sprint (correção exigiria tocar a configuração global de segurança, fora do escopo de um domínio específico).
- **`traceId`/`ip`/`userAgent` permanecem `null` indefinidamente** até uma sprint dedicada criar a infraestrutura de captura de request (ver Retrofits) — o contrato já expõe os campos, mas eles não carregam informação real ainda.
- **Cobertura de auditoria depende de cada domínio chamar `AuditService.recordEvent(...)` corretamente** — não há nenhum mecanismo automático (ex.: AOP/interceptor) que force isso; um novo domínio futuro que esqueça de chamar o serviço central não vai gerar nenhum erro de compilação ou teste, só a ausência silenciosa do evento de auditoria. Mitigação possível para sprint futura: um teste de arquitetura (ArchUnit/Modulith) que valide que toda gravação relevante passa por `AuditService`.

## Arquivos criados

```
backend/src/main/java/br/com/byop/aegis/audit/api/AuditRecordCommand.java
backend/src/main/java/br/com/byop/aegis/audit/api/AuditService.java
backend/src/main/java/br/com/byop/aegis/audit/api/TenantVisibilityPort.java
backend/src/main/java/br/com/byop/aegis/audit/api/package-info.java
backend/src/main/java/br/com/byop/aegis/audit/controller/AuditEventController.java
backend/src/main/java/br/com/byop/aegis/audit/domain/AuditEvent.java
backend/src/main/java/br/com/byop/aegis/audit/domain/AuditRisk.java
backend/src/main/java/br/com/byop/aegis/audit/dto/AuditEventDetail.java
backend/src/main/java/br/com/byop/aegis/audit/dto/AuditEventSummary.java
backend/src/main/java/br/com/byop/aegis/audit/exception/AuditEventExceptionHandler.java
backend/src/main/java/br/com/byop/aegis/audit/exception/AuditEventNotFoundException.java
backend/src/main/java/br/com/byop/aegis/audit/exception/InvalidAuditRiskFilterException.java
backend/src/main/java/br/com/byop/aegis/audit/mapper/AuditEventMapper.java
backend/src/main/java/br/com/byop/aegis/audit/repository/AuditEventRepository.java
backend/src/main/java/br/com/byop/aegis/audit/service/AuditEventQueryService.java
backend/src/main/java/br/com/byop/aegis/audit/service/AuditRiskCatalog.java
backend/src/main/resources/db/migration/V8__audit_trail_normalization.sql
backend/src/test/java/br/com/byop/aegis/audit/api/AuditServiceTest.java
backend/src/test/java/br/com/byop/aegis/audit/controller/AuditEventControllerTest.java
backend/src/test/java/br/com/byop/aegis/audit/mapper/AuditEventMapperTest.java
backend/src/test/java/br/com/byop/aegis/audit/repository/AuditEventRepositoryTest.java
backend/src/test/java/br/com/byop/aegis/audit/service/AuditEventQueryServiceTest.java
backend/src/test/java/br/com/byop/aegis/audit/service/AuditRiskCatalogTest.java
bruno/16-audit/criar-tenant-descartavel-para-exclusao.bru
bruno/16-audit/detalhar-audit-event-do-tenant-excluido.bru
bruno/16-audit/detalhar-audit-event.bru
bruno/16-audit/evento-inexistente-404.bru
bruno/16-audit/excluir-tenant-descartavel.bru
bruno/16-audit/filtrar-por-modulo.bru
bruno/16-audit/filtrar-por-risco.bru
bruno/16-audit/filtro-risco-invalido-400.bru
bruno/16-audit/folder.bru
bruno/16-audit/listar-audit-events-do-tenant.bru
bruno/16-audit/listar-audit-events-tenant-excluido-risco-alto.bru
bruno/16-audit/sem-endpoint-de-escrita.bru
bruno/16-audit/tenant-fora-do-escopo-404.bru
docs/sprints/backend/results/sprint-16.md
```

## Arquivos alterados

```
AGENTS.md
backend/src/main/java/br/com/byop/aegis/asset/controller/AssetController.java
backend/src/main/java/br/com/byop/aegis/asset/service/AssetService.java
backend/src/main/java/br/com/byop/aegis/content/service/ContentService.java
backend/src/main/java/br/com/byop/aegis/product/controller/ProductAssignmentController.java
backend/src/main/java/br/com/byop/aegis/product/service/ProductAssignmentService.java
backend/src/main/java/br/com/byop/aegis/product/user/service/TenantUserService.java
backend/src/main/java/br/com/byop/aegis/tenant/api/TenantAccessService.java
backend/src/main/java/br/com/byop/aegis/tenant/controller/TenantController.java
backend/src/main/java/br/com/byop/aegis/tenant/service/TenantService.java
backend/src/test/java/br/com/byop/aegis/asset/controller/AssetControllerTest.java
backend/src/test/java/br/com/byop/aegis/asset/service/AssetServiceTest.java
backend/src/test/java/br/com/byop/aegis/content/service/ContentServiceTest.java
backend/src/test/java/br/com/byop/aegis/product/controller/ProductAssignmentControllerTest.java
backend/src/test/java/br/com/byop/aegis/product/service/ProductAssignmentServiceTest.java
backend/src/test/java/br/com/byop/aegis/product/user/service/TenantUserServiceTest.java
backend/src/test/java/br/com/byop/aegis/tenant/api/TenantAccessServiceTest.java
backend/src/test/java/br/com/byop/aegis/tenant/controller/TenantControllerTest.java
backend/src/test/java/br/com/byop/aegis/tenant/service/TenantServiceTest.java
bruno/collection.bru
bruno/environments/dev.bru
bruno/environments/homolog.bru
bruno/environments/local.bru
bruno/environments/prod.bru
docs/api-testing/README.md
docs/sprints/backend/SPRINT-RESULTADO.md
```

## Arquivos removidos

Nenhum. Esta sprint não removeu nenhum arquivo do repositório.

## Cobertura de testes

`mvn clean verify` final: **BUILD SUCCESS**, **862 testes**, 0 falhas, 0 erros, 0 ignorados. JaCoCo: `All coverage checks have been met` (100% linhas e branches, nível `BUNDLE`, 235 classes analisadas). Spring Modulith: `ModulithArchitectureTest` aprovado, incluindo a nova fronteira `audit-api` e a resolução do ciclo `audit ↔ tenant` via `TenantVisibilityPort`.

Testes novos por camada: `AuditEventRepositoryTest` (`@DataJpaTest`, 9 casos, incluindo isolamento por tenant e sobrevivência à exclusão do tenant), `AuditEventMapperTest` (sem contexto Spring, 13 casos), `AuditRiskCatalogTest` (2 casos), `AuditServiceTest` (Mockito, 4 casos), `AuditEventQueryServiceTest` (Mockito, 14 casos, incluindo todos os ramos de fallback de `resolveTenantName`/`resolveTarget`/`readDiffJson`), `AuditEventControllerTest` (`@WebMvcTest`, 9 casos, incluindo a confirmação explícita de ausência de endpoint de escrita via 405). Testes existentes atualizados: `TenantServiceTest`, `TenantUserServiceTest`, `ProductAssignmentServiceTest`/`ProductAssignmentControllerTest`, `ContentServiceTest`, `AssetServiceTest`/`AssetControllerTest`, `TenantAccessServiceTest`, `TenantControllerTest`.

## SonarQube for IDE

Rodada de correção pós-implementação (apontamentos reais encontrados pelo usuário via SonarQube for IDE), nenhum resolvido com `@SuppressWarnings`/`NOSONAR`/desabilitação de regra — todos via reescrita:

- `java:S6213` (`AuditService.record` → identificador restrito) — método renomeado para `recordEvent`, todos os call-sites e testes atualizados.
- `java:S1135` (comentário "TODO") — falso gatilho do scanner na palavra "Todo" (português, pronome) no início de uma frase do Javadoc de `AuditService`; reescrita para "Qualquer dominio" elimina o gatilho sem mudar o conteúdo.
- `java:S1168` (retornar `null` em método de coleção) — `AuditEventQueryService.readDiffJson` passou a retornar `Map.of()` em vez de `null` quando não há diff; `AuditEventDetail.diffJson()` agora é sempre uma coleção, nunca `null`.
- `java:S7467` (`catch (IllegalArgumentException ex)` sem usar `ex`) — trocado por `catch (IllegalArgumentException _)` em `parseRiskFilter`.
- `java:S5778` (lambda de `assertThatThrownBy` com mais de uma chamada que pode lançar) — corrigido em `AuditEventQueryServiceTest` (5 ocorrências, incluindo `event.getId()` dentro da lambda), `AuditServiceTest` (1) e `ProductAssignmentServiceTest` (8): toda chamada auxiliar (`service()`, `caller()`, `event.getId()`) extraída para variável local antes do `assertThatThrownBy`.
- `java:S1192` (literal duplicado) — `"status"` como chave de `Map.of(...)` extraído para `DIFF_KEY_STATUS` em `ContentService` (3 ocorrências) e `TenantUserService` (7 ocorrências).

Os 6 padrões acima foram documentados na Seção 4 (nova) de `AGENTS.md`, a pedido do usuário, para reduzir recorrência em sprints futuras.

## Bruno

Pasta `16-audit` criada com 12 requests, cobrindo: tenant descartável criado e excluído só dentro da pasta (nunca o `{{tenantId}}` compartilhado pelo resto da collection) para validar literalmente o cenário "excluir tenant → evento de risco alto" da Seção "Validação" do artefato; listagem/detalhe usando o `{{tenantId}}` compartilhado (que já acumula eventos reais de `10-product-assignments`, `11-content`, `15-users` por causa do `AuditService` central); filtros por `risk`/`module`; rejeições 400/404; isolamento 404 (mesmo padrão de `nonMemberToken`/fallback 401 já usado em `11-content`); e confirmação de ausência de endpoint de escrita.

**Achado de plataforma durante a validação manual:** `POST` num path que só tem `GET` mapeado retorna **403** (`insufficient_scope`) na aplicação real, não 405 — o header `Allow: GET` da resposta confirma que o Spring MVC reconhece corretamente que só existe `GET`, mas o filtro de exceções do Spring Security intercepta a `HttpRequestMethodNotSupportedException` antes do handler de erro da aplicação e a traduz para 403. Mesma classe de quirk já documentada na Sprint 12 (NullPointerException de credenciais S3 traduzido em 403 confuso). O teste automatizado `@WebMvcTest` (fatia isolada, sem o filtro OAuth2 real) continua corretamente validando 405 nesse cenário — só a aplicação real com o filtro de segurança completo diverge.

Validado via `cd bruno && npx @usebruno/cli run --env local`: **133 requests executados, 133 aprovados, 253/253 testes aprovados** — confirmado em duas execuções completas consecutivas (idempotente).
