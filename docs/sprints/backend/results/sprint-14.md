# Sprint 14 — Domínio analytics

Data de fechamento: 2026-06-27

## Resultado

Sprint concluída com o domínio `analytics` entregue como camada read-only de agregação por produto.

Endpoints implementados:

- `GET /api/v1/products/{productId}/analytics/kpis`
- `GET /api/v1/products/{productId}/analytics/health`
- `GET /api/v1/products/{productId}/analytics/channels`
- `GET /api/v1/products/{productId}/analytics/trends`
- `GET /api/v1/products/{productId}/analytics/reports`

Todos os endpoints respeitam `@RequireModule(ModuleKey.ANALYTICS)` e isolamento por produto via `ProductAccessPort.assertAccessible`.

## Arquitetura

O módulo `analytics` não possui entidade, migration ou repository próprios nesta sprint. Ele consome somente APIs públicas dos módulos vizinhos:

- `content.api.ContentAnalyticsService`
- `asset.api.AssetAnalyticsService`
- `submission.api.SubmissionAnalyticsService`

As fronteiras públicas foram expostas por `NamedInterface` quando necessário, preservando a auditoria do Spring Modulith.

## Contratos e comportamento

KPIs, health, trends e reports são calculados a partir de dados já existentes de conteúdo, assets, forms e submissions.

O endpoint de channels retorna tráfego simulado e zerado, identificado no payload como `simulado`, porque ainda não existe domínio real de tracking/aquisição.

O resumo consolidado para dashboard/settings não foi implementado nesta sprint e permanece para a etapa posterior dedicada a dashboard/settings.

## Bruno

A collection Bruno recebeu a pasta `14-analytics` com fluxo sequencial:

- habilita `ANALYTICS` no produto criado pela collection;
- valida KPIs;
- valida health;
- valida channels;
- valida trends;
- valida reports;
- desabilita `ANALYTICS` e valida 403 `MODULE_DISABLED`;
- reabilita `ANALYTICS` para manter a collection estável para etapas futuras.

`docs/api-testing/README.md` foi atualizado para registrar a pasta `14-analytics` e a dependência de `productId` na execução completa da collection.

## Documentação adicional

`docs/trace/00_endpoints_esperados.md` foi atualizado para formalizar os contratos de `TrendCard` e `AnalyticsReport`, antes descritos apenas como necessidade derivada do frontend.

`docs/sprints/backend/21_seed_inicial_e_grafo.md` foi preservado no fechamento com ajustes de seed já presentes no worktree: tenant principal `CLIENTES BETA` e módulo `PAGES` no produto `loki`.

## SonarQube for IDE

Apontamentos conhecidos corrigidos sem alterar contratos, payloads, endpoints, migrations, Bruno funcional, arquitetura ou regra de negócio:

- `java:S3358`: ternários aninhados extraídos em `AnalyticsService`.
- `java:S1192`: literais duplicados movidos para constantes privadas semânticas em `AnalyticsService`.
- `java:S5853`: assertions de `AnalyticsServiceTest` consolidadas em chain AssertJ.
- `java:S6862`: `AnalyticsConfigTest` deixou de comparar contra `Clock.systemUTC()` e passou a validar diretamente `ZoneOffset.UTC`.

Não foi utilizado `@SuppressWarnings` e nenhum finding foi marcado como falso positivo.

## Validações automatizadas

Executado:

```bash
mvn -Dtest='AnalyticsServiceTest,AnalyticsControllerTest' test
mvn -Dtest=AnalyticsConfigTest test
mvn clean verify
```

Resultados:

- `mvn -Dtest='AnalyticsServiceTest,AnalyticsControllerTest' test`: `BUILD SUCCESS`, 18 testes, 0 falhas, 0 erros.
- `mvn -Dtest=AnalyticsConfigTest test`: `BUILD SUCCESS`, 1 teste, 0 falhas, 0 erros.
- `mvn clean verify`: `BUILD SUCCESS`, 746 testes, 0 falhas, 0 erros, JaCoCo aprovado e Spring Modulith aprovado.

Validação Bruno final obrigatória:

```bash
cd bruno
npx @usebruno/cli run --env local
```

Resultado:

- `PASS`
- 111 requests executados
- 111 requests aprovados
- 212/212 testes aprovados
