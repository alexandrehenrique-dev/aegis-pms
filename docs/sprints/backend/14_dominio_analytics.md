# Etapa 13 — Domínio `analytics` (KPIs, saúde de produto, canais, tendências)

> Cole este arquivo inteiro numa conversa nova do GPT. Pré-requisito: etapas 06, 10 (content) e 12 (forms) concluídas — analytics agrega dados desses domínios.

## Contexto fixo

Telas `AnalyticsOverview`, `ProductHealthPanel`, `ContentAnalytics`, `FormAnalyticsModule`, `ChannelBreakdown`, `ReportGrid`, `TrendCards`, `AnalyticsStates` — hoje 100% mock, a maioria sem nem contrato TypeScript formal ainda (`ReportGrid`/`TrendCards` são estáticas). Payloads conforme `docs/trace/00_endpoints_esperados.md` (Seção B.4) — os três primeiros endpoints abaixo têm contrato real e devem ser implementados literalmente; os dois últimos (`trends`, `reports`) ainda não têm shape formal no frontend e exigem levantamento próprio antes de fixar a resposta (ver Tarefa C).

## Objetivo

Endpoints agregadores de analytics por produto — a maioria pode ser uma camada de leitura sobre dados que já existem em outros domínios (content, forms, assets), não uma tabela de fatos própria nesta fase.

## Tarefas

### A. Endpoints com contrato definido

```txt
GET /api/v1/products/{productId}/analytics/kpis
GET /api/v1/products/{productId}/analytics/health
GET /api/v1/products/{productId}/analytics/channels
```

```ts
type AnalyticsKpi = { label: string; value: string; comparison: string; note: string; tone: string };
// GET /kpis → AnalyticsKpi[]

type HealthSignal = { label: string; status: string; score: string; tone: string };
// GET /health → HealthSignal[]

type ChannelRow = { name: string; visits: string; conversion: string; trend: string };
// GET /channels → ChannelRow[]
```

`kpis` deve derivar de contagens reais já disponíveis: conteúdo publicado/pendente (domínio `content`), submissions recebidas (domínio `form`), assets recentes (domínio `asset`) — não inventar uma fonte de dados de analytics separada nesta fase.

`health` é um agregado de sinais (ex.: "Conteúdo sem revisão há mais de 30 dias", "Formulário sem submissions há 2 semanas") calculado a partir dos mesmos domínios.

`channels` pode começar como dados estáticos/simulados (não há instrumentação de tráfego real ainda no MVP) — documentar isso explicitamente na resposta da etapa, não tentar implementar tracking de visitas agora.

**Isolamento por produto** (`00_padrao_qualidade_e_arquitetura.md`, Seção 10): qualquer endpoint desta etapa para um produto fora do escopo do usuário retorna 404, nunca 403. **Module-gating** (Seção 9.2 do padrão): `AnalyticsController` anotado com `@RequireModule(ModuleKey.ANALYTICS)` — produto com módulo `ANALYTICS` desabilitado retorna 403 `MODULE_DISABLED`.

### B. Endpoint agregador do dashboard global (ver também etapa 16)

`GET /api/v1/dashboard/summary` é compartilhado com o domínio `dashboard` — implementar aqui ou na etapa 16, à escolha do GPT, mas implementar só uma vez.

### C. `trends` e `reports` — levantamento antes de implementar

`GET /api/v1/products/{productId}/analytics/trends` e `GET /api/v1/products/{productId}/analytics/reports` (usados por `TrendCards.tsx`/`ReportGrid.tsx`) **não têm contrato formal no frontend ainda** — antes de implementar, é necessário abrir essas duas telas no frontend (`frontend/src/domains/analytics/pages/TrendCards.tsx` e `ReportGrid.tsx`) e definir o shape de resposta a partir do que elas efetivamente renderizam hoje (mesmo que estático). Não inventar um shape sem essa checagem — documentar o shape definido em `docs/trace/00_endpoints_esperados.md` (atualizando a Seção B.4) como parte desta etapa.

### D. Padrão de qualidade e entrega (obrigatório)

> Resumo — detalhe completo em `00_padrao_qualidade_e_arquitetura.md`.

- **Java 25** / **Spring Boot 4.1.x**. Esta etapa **não tem entidade própria** (é leitura agregada sobre `content`/`form`/`asset`) — não há rodada 1/2 (entity/repository/mapper). 100% de cobertura nas classes funcionais mesmo assim: `AnalyticsService` é a classe com lógica que precisa de teste completo (cada KPI/sinal de saúde calculado, cada cenário de "sem dados ainda").
- Entregar em rodadas:
  1. `AnalyticsService` (agregação via os repositories já existentes de `content`/`form`/`asset`, etapas 10-12) + testes com mocks desses repositories — cobrir caminho com dados e caminho sem dados (produto novo, zero conteúdo/submissions).
  2. `AnalyticsController` (endpoints da Seção A) + testes `@WebMvcTest` + validação via `curl`.
  3. Repetir o mesmo para `trends`/`reports` depois do levantamento de shape da Seção C.

## Critérios de aceite

- [ ] `kpis`, `health`, `channels` respondem com os shapes acima.
- [ ] `kpis`/`health` refletem dados reais de `content`/`form`/`asset`, não números fixos.
- [ ] `trends`/`reports` têm contrato definido (atualizado no trace) e implementado de acordo.
- [ ] `mvn clean verify` confirma 100% de cobertura em `AnalyticsService`/`AnalyticsController` (JaCoCo).
- [ ] Analytics de produto fora do escopo do usuário retorna 404 (não 403).
- [ ] Produto com módulo `ANALYTICS` desabilitado retorna 403 `MODULE_DISABLED`.

## Validação

> **Entrega via collection Postman, não só curl** (ver `00_padrao_qualidade_e_arquitetura.md`, Seção 11). Os `curl` abaixo são a especificação exata de cada request — adicione-os à pasta desta etapa em `aegis-postman-collection.json` (collection cumulativa, autenticação via `{{token}}` herdado da pasta "Auth") e devolva o JSON completo atualizado para download.

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/products/<productId>/analytics/kpis
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/products/<productId>/analytics/health
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/products/<productId>/analytics/channels
```

## Artefato de continuidade — `SPRINT-RESULTADO.md`

> Ver `00_padrao_qualidade_e_arquitetura.md`, Seção 12. Antes do commit, gere/atualize `docs/sprints/sprint-02-fundacao-backend-gpt/SPRINT-RESULTADO.md` (arquivo inteiro, nunca um diff) com a entrada desta etapa (template fixo da Seção 12.2): classes criadas, endpoints confirmados, qualquer decisão que esta etapa deixou a seu critério (registre a escolha real), e retrofits pendentes para etapas futuras. É o que a próxima conversa do GPT vai receber em vez da memória que ela não tem.

## Commit sugerido

```bash
git add backend/
git commit -m "feat(backend): dominio analytics com kpis, saude do produto e canais"
```
