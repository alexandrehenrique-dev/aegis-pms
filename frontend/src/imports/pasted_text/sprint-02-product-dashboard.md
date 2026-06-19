AEGIS PMS — SPRINT 02

PRODUCT CONTEXT + DASHBOARD GLOBAL + DASHBOARD DE PRODUTO

Continue a partir da Sprint 01.

Preserve exatamente a fundação criada:

* src/app/App.tsx
* src/styles/fonts.css
* src/styles/theme.css

Não reinvente o App Shell.
Não mude a identidade visual.
Não altere a hierarquia principal.
Não crie um estilo novo.

Nesta sprint, expanda a UI com o primeiro fluxo operacional real:

Plataforma → Tenant → Produto → Dashboard

⸻

ARQUIVOS DE REFERÊNCIA

Leia os arquivos enviados nesta ordem:

1. 002_aegis_pms_ux_master_roadmap.md
2. 003_aegis_pms_ux_architecture.md
3. 004_aegis_pms_screen_inventory.md
4. 006_aegis_pms_ux_rules_and_interaction_patterns.md
5. 007_aegis_pms_design_system_mapping.md
6. 008_aegis_pms_ux_master_blueprint.md
7. 009_aegis_pms_figma_master_specification.md
8. 010_aegis_pms_visual_language_specification.md

Use esses documentos apenas para orientar UX, UI, navegação, componentes e estados.

⸻

OBJETIVO DA SPRINT

Criar as telas e componentes do contexto operacional inicial do Aegis PMS:

1. Dashboard Global
2. Lista de Produtos
3. Criar Produto
4. Dashboard de Produto
5. Detalhe do Produto
6. Estado de Produto sem Módulos
7. Catálogo de Módulos

Essas telas devem validar a premissa central:

O usuário não entra no Aegis para editar páginas.
Ele entra para administrar produtos digitais.

⸻

REGRA CENTRAL

Toda tela deve responder claramente:

* Onde estou?
* Qual tenant está ativo?
* Qual produto está ativo?
* Qual módulo está ativo?
* O que aconteceu recentemente?
* O que exige atenção?
* Qual é o próximo passo?

⸻

TELA 01 — DASHBOARD GLOBAL

Criar uma visão de centro operacional da plataforma ou tenant.

Contexto:

Plataforma → Tenant → Dashboard

Elementos obrigatórios:

* Page Header com título “Dashboard Global”
* descrição curta: “Visão operacional dos produtos digitais deste tenant.”
* badge do tenant ativo
* seletor de período
* CTA principal: “Criar produto”

Widgets obrigatórios:

* Produtos ativos
* Conteúdos pendentes
* Aprovações em aberto
* Formulários recebidos
* Assets recentes
* Usuários ativos
* Atividade recente
* Alertas operacionais

A área de atividade recente deve parecer uma timeline operacional.

Exemplos de eventos:

* “Maestro Beton recebeu 3 novas respostas de formulário.”
* “WikiDev publicou uma nova página.”
* “Aion Logbook teve conteúdo enviado para revisão.”
* “Eirene UI atualizou assets do produto.”

Estados obrigatórios:

* carregando com skeleton
* vazio sem produtos
* erro parcial em widgets
* sem permissão para widgets específicos

⸻

TELA 02 — LISTA DE PRODUTOS

Contexto:

Plataforma → Tenant → Produtos

Criar uma tela de catálogo operacional de produtos.

Elementos obrigatórios:

* Page Header com título “Produtos”
* descrição: “Administre os produtos digitais deste tenant.”
* CTA principal: “Novo produto”
* busca
* filtros por tipo
* filtros por status
* alternância grid/lista

Produtos mockados:

* Maestro Beton
* Aion Logbook
* Eirene UI
* Genesis
* WikiDev
* Conecta Talentos

Cada card de produto deve mostrar:

* nome
* tipo
* status
* módulos ativos
* última atividade
* indicadores rápidos
* CTA “Abrir produto”
* menu contextual

Tipos sugeridos:

* Site Institucional
* Produto SaaS
* Design System
* Knowledge Base
* Jogo / Experimento
* Portal

Estados:

* grid preenchido
* tabela/lista
* nenhum produto
* busca sem resultado
* produto arquivado
* produto sem módulos
* loading

⸻

TELA 03 — CRIAR PRODUTO

Contexto:

Plataforma → Tenant → Produtos → Novo Produto

Criar fluxo em formato de tela ou drawer grande.

Campos obrigatórios:

* Nome do produto
* Slug
* Tipo
* Idioma padrão
* Descrição
* Template inicial
* Módulos iniciais

Tipos de produto:

* Site Institucional
* Landing Page
* CMS
* Knowledge Base
* Produto SaaS
* Design System
* App Interno
* Experimento / Lab

Módulos iniciais selecionáveis:

* Conteúdo
* Assets
* Forms
* Analytics
* SEO
* Knowledge Graph
* Workflow

UX obrigatória:

* indicador de slug disponível
* preview do identificador do produto
* card de resumo lateral
* CTA “Criar produto”
* CTA secundário “Cancelar”

Estados:

* formulário vazio
* validação
* slug indisponível
* salvando
* sucesso

⸻

TELA 04 — DASHBOARD DE PRODUTO

Contexto:

Plataforma → Tenant → Produto → Dashboard

Produto ativo sugerido:

Maestro Beton

Criar a tela mais importante desta sprint.

Ela deve parecer o cockpit operacional de um produto digital.

Elementos obrigatórios:

* Page Header com nome do produto
* tipo do produto
* status
* última atualização
* ações: “Editar produto”, “Ver módulos”, “Preview público”
* breadcrumb completo
* resumo operacional

Widgets obrigatórios:

* Status do produto
* Módulos habilitados
* Conteúdos publicados
* Conteúdos em revisão
* Formulários recebidos
* Conversão estimada
* Assets recentes
* Atividade recente
* Pendências

Criar seção “Próximas ações”:

* “Revisar conteúdo em aprovação”
* “Configurar SEO da página Home”
* “Adicionar imagens à galeria”
* “Ver respostas do formulário de orçamento”

Criar seção “Módulos ativos”:

Cards para:

* Conteúdo
* Assets
* Forms
* Analytics
* SEO
* Knowledge Graph

Cada módulo deve mostrar:

* status
* última atualização
* contagem rápida
* CTA para abrir módulo

Estados:

* produto saudável
* produto com pendências
* produto sem módulos
* loading
* erro parcial

⸻

TELA 05 — DETALHE DO PRODUTO

Contexto:

Plataforma → Tenant → Produto → Visão Geral

Criar uma tela mais administrativa do produto.

Abas obrigatórias:

* Visão Geral
* Módulos
* Conteúdo
* Assets
* Forms
* Analytics
* Graph
* Configurações

Conteúdo da aba Visão Geral:

* dados gerais
* status
* tipo
* slug
* idioma padrão
* descrição
* módulos habilitados
* equipe vinculada
* domínios futuros
* auditoria recente

⸻

TELA 06 — PRODUTO SEM MÓDULOS

Contexto:

Plataforma → Tenant → Produto → Dashboard

Criar estado vazio elegante para produto recém-criado sem módulos habilitados.

Mensagem principal:

“Este produto ainda não possui módulos habilitados.”

Descrição:

“Escolha as capacidades iniciais para começar a operar este produto digital.”

CTA principal:

“Habilitar módulos”

CTA secundário:

“Editar dados do produto”

Mostrar sugestões por tipo de produto.

⸻

TELA 07 — CATÁLOGO DE MÓDULOS

Contexto:

Plataforma → Tenant → Produto → Módulos

Criar tela estilo marketplace interno operacional.

Não deve parecer loja.
Deve parecer catálogo de capacidades da plataforma.

Módulos obrigatórios:

* Conteúdo
* Assets
* Forms
* Analytics
* SEO
* Knowledge Graph
* Workflow
* Versionamento
* Auditoria
* Integrações

Cada módulo deve mostrar:

* nome
* descrição
* status: habilitado, desabilitado, dependência faltando, futuro
* dependências
* impacto operacional
* CTA: habilitar, configurar, abrir
* badge de maturidade: MVP, V1, Futuro

Estados:

* módulo habilitado
* módulo desabilitado
* módulo com dependência faltando
* sem permissão
* loading

⸻

COMPONENTES NOVOS DA SPRINT

Criar ou refinar:

* ProductCard
* ProductStatusBadge
* ModuleCard
* OperationalTimeline
* KPIWidget
* QuickActionCard
* ProductSummaryPanel
* CreateProductForm
* ModuleCatalogGrid
* PermissionHint
* PartialErrorWidget

⸻

RESPONSIVIDADE

Gerar variações:

Mobile 375px
Tablet 768px
Desktop 1440px

Mobile:

* sidebar vira drawer
* cards empilhados
* tabelas viram cards
* ações ficam em menu
* filtros viram painel expansível
* dashboard prioriza alertas e próximas ações

Tablet:

* grid em 2 colunas
* sidebar colapsável
* painéis laterais viram drawers

Desktop:

* layout 12 colunas
* sidebar permanente
* dashboard com cards e timeline lateral

⸻

QUALIDADE ESPERADA

O resultado deve parecer uma plataforma SaaS real.

Não deve parecer:

* template admin genérico
* dashboard aleatório
* CRM
* ERP
* CMS comum

Deve transmitir:

“Estou administrando produtos digitais importantes.”

⸻

IMPORTANTE

Não gerar ainda:

* Editor de Conteúdo
* Biblioteca de Assets detalhada
* Forms Builder
* Analytics avançado
* Knowledge Graph canvas
* Usuários e permissões avançadas
* Auditoria completa

Esses módulos virão em sprints futuras.

Nesta sprint, entregue apenas o contexto operacional de Produto.