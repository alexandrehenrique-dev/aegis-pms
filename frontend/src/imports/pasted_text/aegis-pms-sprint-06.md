# AEGIS PMS — SPRINT 06
## ANALYTICS MODULE

Continue a partir das Sprints 01, 02, 03, 04 e 05.

Preserve exatamente:

- Foundation
- App Shell
- Dashboards
- Product Context
- Content Module
- Assets Module
- Forms Module
- identidade visual white-first
- componentes existentes
- hierarquia Plataforma → Tenant → Produto → Módulo → Recurso

Não recriar nada.

Não alterar estilo visual.

Não gerar Knowledge Graph.

Não gerar Users, Settings ou Audit.

---

# OBJETIVO

Criar o módulo Analytics do Aegis PMS.

Analytics no Aegis não é dashboard decorativo.

Analytics é leitura operacional do produto.

O usuário deve conseguir entender:

- como o produto está performando;
- quais conteúdos geram resultado;
- quais formulários convertem;
- quais canais trazem tráfego;
- quais tendências merecem atenção;
- quais ações devem ser tomadas.

---

# CONTEXTO

Tenant:

BYOP

Produto:

Maestro Beton

Módulo:

Analytics

Breadcrumb:

Plataforma → BYOP → Maestro Beton → Analytics

---

# REGRA CENTRAL

Toda tela de Analytics deve responder:

- Onde estou?
- Qual produto estou medindo?
- Qual período está ativo?
- O que melhorou?
- O que piorou?
- O que exige atenção?
- Qual ação devo tomar?

---

# TELA 01
## ANALYTICS OVERVIEW

Criar visão principal do módulo.

Page Header:

Título:

Analytics

Descrição:

Acompanhe performance, conversões, conteúdo e sinais operacionais do produto.

Elementos obrigatórios:

- seletor de período
- comparação com período anterior
- filtro por canal
- filtro por conteúdo
- CTA "Gerar relatório"
- CTA secundário "Exportar dados"

KPIs principais:

- Visitas
- Conversões
- Taxa de conversão
- Formulários recebidos
- Conteúdos publicados
- Páginas mais acessadas
- Leads qualificados
- Tempo médio na página

Cada KPI deve mostrar:

- valor
- variação
- tendência
- microcopy explicativa
- estado positivo/neutro/atenção

Exemplos:

"Visitas aumentaram 18% em relação ao período anterior."

"Conversões caíram 6%. Verifique origem de tráfego."

---

# TELA 02
## PRODUCT HEALTH

Criar painel de saúde do produto.

Seções:

- Saúde geral
- Conteúdo
- Forms
- Assets
- SEO
- Performance

Visual:

Não usar velocímetros exagerados.
Não usar gráficos decorativos.
Usar scorecards operacionais e indicadores claros.

Indicadores:

- Produto saudável
- Requer atenção
- Dados insuficientes
- Módulo sem atividade
- Pendências críticas

Ações recomendadas:

- Revisar SEO da Home
- Atualizar página Galeria
- Ver leads não qualificados
- Adicionar alt text em imagens
- Revisar formulário de orçamento

---

# TELA 03
## CONTENT ANALYTICS

Medir conteúdo.

Elementos:

- top páginas
- páginas com queda
- páginas sem atualização
- conteúdos com SEO incompleto
- conteúdos por status
- idiomas com pendência

Gráficos:

- visitas por página
- evolução de publicações
- status editorial
- conteúdos revisados vs publicados

Tabela:

Conteúdo
Tipo
Idioma
Views
Conversões
Status
Última atualização
Ação sugerida

---

# TELA 04
## FORM ANALYTICS

Medir formulários.

Elementos:

- respostas por formulário
- taxa de conversão
- abandono
- origem dos leads
- leads qualificados
- tempo médio de resposta

Gráficos:

- submissions por período
- conversão por formulário
- status dos leads
- origem UTM

Tabela:

Formulário
Visualizações
Envios
Conversão
Leads qualificados
Última resposta
Ação sugerida

---

# TELA 05
## TRAFFIC & CHANNELS

Medir origem.

Canais mockados:

- Direto
- Google
- Instagram
- WhatsApp
- Referral
- Orgânico
- Campanha

Componentes:

- gráfico de canais
- tendência por canal
- tabela de origem
- cards de destaque

Microcopy:

"Instagram trouxe tráfego, mas WhatsApp converteu melhor."

"Google orgânico cresceu após atualização da página Home."

---

# TELA 06
## REPORTS

Criar central de relatórios.

Relatórios mockados:

- Relatório mensal do produto
- Performance de conteúdo
- Conversões de formulários
- Saúde do produto
- Auditoria editorial
- SEO e acessibilidade

Cada relatório deve mostrar:

- nome
- descrição
- período
- última geração
- formato
- status
- CTA gerar
- CTA baixar

Estados:

- gerando
- pronto
- erro
- sem dados suficientes

---

# TELA 07
## TREND CARDS

Criar seção de tendências e insights operacionais.

Cards:

- Crescimento
- Queda
- Estabilidade
- Anomalia
- Oportunidade

Exemplos:

"Página Galeria gerou 42% mais interação esta semana."

"Formulário de orçamento teve queda de conversão."

"Assets sem alt text podem prejudicar SEO."

"Conteúdo Sobre o Maestro não é atualizado há 45 dias."

Cada Trend Card deve possuir:

- tipo
- severidade
- explicação
- métrica relacionada
- ação recomendada

---

# TELA 08
## ANALYTICS EMPTY / LOADING / ERROR

Criar estados globais do módulo.

Empty:

"Este produto ainda não possui dados suficientes."

Descrição:

"Assim que o produto receber visitas, formulários ou publicações, os indicadores aparecerão aqui."

CTA:

"Ver produto"

Loading:

Skeleton para KPIs, gráficos e tabelas.

Error:

"Não foi possível carregar os dados de analytics."

Ação:

"Tentar novamente"

Partial Error:

Alguns widgets carregam, outros mostram erro discreto.

---

# COMPONENTES NOVOS

Criar ou refinar:

AnalyticsOverview
KPIBlock
KPIGrid
TrendIndicator
TrendCard
ChartContainer
ReportCard
ReportGrid
ProductHealthPanel
HealthScoreCard
ContentAnalyticsTable
FormAnalyticsTable
ChannelBreakdown
PeriodSelector
ComparisonBadge
InsightPanel
AnalyticsEmptyState
PartialAnalyticsError

---

# GRÁFICOS

Criar visualmente:

- line chart
- bar chart
- donut chart
- stacked bar
- area chart
- mini sparkline

Importante:

Não precisa ter dados reais.

Mas deve parecer implementável.

Usar estilo limpo, leve, operacional.

Sem cores excessivas.

Sem visual financeiro agressivo.

Sem dashboard executivo genérico.

---

# REGRAS DE UX

Analytics deve sempre gerar ação.

Cada métrica importante deve ter uma interpretação.

Evitar cards que mostram apenas número.

Preferir:

Número
+
Tendência
+
Explicação
+
Ação sugerida

---

# RESPONSIVIDADE

Desktop 1440

- KPIs em grid
- gráficos lado a lado
- tabelas completas
- insights em painel lateral

Tablet 768

- KPIs em 2 colunas
- gráficos empilhados
- insights abaixo

Mobile 375

- KPIs em cards
- gráficos simplificados
- tabelas viram cards
- filtros em drawer
- relatórios em lista

Nunca permitir scroll horizontal na página.

---

# QUALIDADE ESPERADA

O usuário deve sentir:

"Eu entendo a saúde do meu produto e sei o que fazer em seguida."

Não:

"Estou vendo gráficos bonitos."

---

# IMPORTANTE

Não gerar ainda:

- Knowledge Graph Canvas
- Node Inspector
- Relationship Explorer
- Users
- Settings
- Audit completa

Esses módulos virão nas próximas sprints.

Nesta sprint, entregue apenas Analytics.