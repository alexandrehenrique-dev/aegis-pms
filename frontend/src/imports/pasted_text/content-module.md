AEGIS PMS — SPRINT 03

CONTENT MODULE

Continue a partir das Sprints 01 e 02.

Preserve exatamente:

* App Shell
* Header Global
* Sidebar
* Tenant Switcher
* Product Switcher
* Breadcrumb
* Global Search
* Notification Center
* Identidade visual white-first
* Hierarquia Plataforma → Tenant → Produto → Módulo → Recurso
* Componentes criados na Sprint 02

Não recrie a fundação.
Não recrie dashboards.
Não mude o estilo visual.
Não gere Assets, Forms, Analytics, Knowledge Graph ou Settings nesta sprint.

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

Use esses documentos apenas para orientar UX, UI, navegação, componentes, estados e responsividade.

⸻

OBJETIVO DA SPRINT

Gerar o módulo de Conteúdo do Aegis PMS.

Esta sprint deve criar:

1. Dashboard Editorial
2. Lista de Conteúdos
3. Workflow Editorial
4. Editor de Conteúdo
5. Preview de Conteúdo
6. Publish Panel
7. Histórico de Versões
8. Comparação de Versões
9. Estados de conteúdo

O módulo deve provar que o Aegis não é um CMS genérico.

Ele é um centro operacional de conteúdo dentro de um produto digital.

⸻

CONTEXTO BASE

Tenant ativo:

BYOP

Produto ativo:

Maestro Beton

Módulo ativo:

Conteúdo

Breadcrumb base:

Plataforma → BYOP → Maestro Beton → Conteúdo

⸻

REGRA CENTRAL

Toda tela de conteúdo deve responder claramente:

* Onde estou?
* Qual tenant está ativo?
* Qual produto está ativo?
* Qual módulo está ativo?
* Qual conteúdo estou gerenciando?
* Qual é o status editorial?
* O que aconteceu recentemente?
* Qual é o próximo passo?

⸻

MODELO EDITORIAL

Estados oficiais:

Draft
In Review
Published
Archived

Também usar visualmente:

* alterações não salvas
* salvando
* salvo
* erro
* conflito de edição
* sem permissão
* tradução pendente
* versão antiga

⸻

TELA 01 — DASHBOARD EDITORIAL

Contexto:

Plataforma → BYOP → Maestro Beton → Conteúdo → Dashboard Editorial

Criar uma visão operacional do conteúdo do produto.

Elementos obrigatórios:

* Page Header com título “Conteúdo”
* descrição: “Gerencie páginas, artigos, traduções, revisões e publicações deste produto.”
* badge do produto ativo
* badge do módulo “Conteúdo”
* CTA principal: “Novo conteúdo”
* CTA secundário: “Ver workflow”

Widgets obrigatórios:

* Rascunhos
* Em revisão
* Publicados
* Arquivados
* Traduções pendentes
* Conteúdos atualizados recentemente
* Aprovações pendentes
* Atividade editorial recente

Criar seção “Atenção editorial”:

Itens mockados:

* “Página Home possui SEO incompleto.”
* “Sobre o Maestro está aguardando revisão.”
* “Galeria possui imagens sem texto alternativo.”
* “Contato foi publicado há 12 dias.”

Criar timeline editorial recente:

* “Editor criou rascunho da página Agenda.”
* “Product Manager aprovou página Home.”
* “Sistema gerou nova versão do conteúdo Sobre.”
* “Editor enviou Galeria para revisão.”

Estados:

* dashboard preenchido
* sem conteúdo
* loading skeleton
* erro parcial
* sem permissão

⸻

TELA 02 — LISTA DE CONTEÚDOS

Contexto:

Plataforma → BYOP → Maestro Beton → Conteúdo → Lista

Criar DataGrid operacional para conteúdos.

Colunas obrigatórias:

* Título
* Tipo
* Idioma
* Autor
* Status
* Última atualização
* Publicação
* Versão
* Ações

Conteúdos mockados:

* Home
* Sobre o Maestro
* Apresentações
* Galeria
* Depoimentos
* Disponibilidade
* Contato
* Agenda
* Orçamento

Tipos:

* Página
* Seção
* Artigo
* Landing
* Bloco reutilizável

Idiomas:

* PT-BR
* EN-US
* ES-ES

Status:

* Draft
* In Review
* Published
* Archived

Recursos obrigatórios:

* busca
* filtro por status
* filtro por idioma
* filtro por tipo
* filtro por autor
* ordenação
* paginação
* seleção múltipla
* ações em lote

Ações de linha:

* abrir
* editar
* preview
* enviar para revisão
* publicar
* arquivar
* histórico

Estados:

* lista preenchida
* busca sem resultado
* sem conteúdo
* loading
* erro
* sem permissão
* seleção múltipla ativa

Mobile:

Tabela deve virar cards de conteúdo.

⸻

TELA 03 — WORKFLOW EDITORIAL

Contexto:

Plataforma → BYOP → Maestro Beton → Conteúdo → Workflow

Criar tela visual do fluxo editorial.

Mostrar colunas ou lanes:

Draft
In Review
Published
Archived

Cada lane deve conter cards de conteúdo.

Card deve mostrar:

* título
* tipo
* idioma
* autor
* última atualização
* versão
* pendências
* próxima ação

Criar ações:

* mover para revisão
* aprovar
* publicar
* arquivar
* solicitar alteração

Criar painel lateral “Detalhes do item selecionado”:

* status atual
* responsável
* comentários
* histórico resumido
* ações disponíveis

Estados:

* workflow preenchido
* lane vazia
* drag/arrastar visual
* sem permissão para mover
* loading
* erro

⸻

TELA 04 — EDITOR DE CONTEÚDO

Contexto:

Plataforma → BYOP → Maestro Beton → Conteúdo → Home → Editar

Criar a tela mais importante desta sprint.

Layout desktop obrigatório em 3 áreas:

1. Sidebar esquerda — Estrutura
2. Centro — Editor / Canvas
3. Painel direito — Propriedades / SEO / Publicação

⸻

Sidebar esquerda — Estrutura

Mostrar árvore do conteúdo:

Home

* Hero
* Experiências
* Vídeo destaque
* Sobre
* Galeria
* Depoimentos
* CTA final
* SEO

Cada item deve mostrar:

* nome
* tipo de bloco
* status
* idioma
* indicador de erro ou pendência

Ações:

* adicionar bloco
* reorganizar
* duplicar bloco
* ocultar bloco

⸻

Centro — Editor / Canvas

Mostrar edição de uma página por blocos.

Bloco selecionado:

Hero

Campos visíveis:

* Título
* Subtítulo
* Texto do CTA principal
* Texto do CTA secundário
* Imagem hero
* Alinhamento
* Visibilidade

Também criar visual de preview parcial da página.

O editor deve parecer cockpit de conteúdo, não textarea gigante.

⸻

Painel direito — Propriedades

Tabs:

* Propriedades
* SEO
* Workflow
* Histórico

Propriedades:

* slug
* tipo
* idioma
* autor
* status
* versão
* última atualização

SEO:

* title
* description
* keywords
* OG image
* preview Google
* preview Social

Workflow:

* status atual
* responsável
* enviar para revisão
* publicar
* arquivar

Histórico:

* versões recentes
* alterações
* autor

Estados obrigatórios:

* rascunho
* salvando
* salvo
* erro
* conflito de edição
* sem permissão
* conteúdo bloqueado
* alterações não salvas

Mobile:

Editor deve virar fluxo em etapas:

1. Estrutura
2. Conteúdo
3. Propriedades
4. Preview
5. Publicação

Não tentar manter 3 colunas no mobile.

⸻

TELA 05 — PREVIEW DE CONTEÚDO

Contexto:

Plataforma → BYOP → Maestro Beton → Conteúdo → Home → Preview

Criar tela de preview responsivo.

Elementos:

* barra superior com status
* seletor de viewport: mobile, tablet, desktop
* seletor de idioma
* botão voltar ao editor
* botão enviar para revisão/publicar
* área de preview

Mostrar visual de página institucional do Maestro Beton como mock simples dentro do preview.

Não precisa ser site completo.

Apenas preview plausível.

Estados:

* preview desktop
* preview tablet
* preview mobile
* idioma sem tradução
* preview carregando
* erro de renderização

⸻

TELA 06 — PUBLISH PANEL

Criar painel lateral ou drawer de publicação.

Elementos:

* status atual
* checklist pré-publicação
* SEO completo/incompleto
* traduções completas/incompletas
* assets com alt text
* formulário vinculado
* data de publicação
* opção publicar agora
* opção agendar publicação
* opção enviar para revisão
* opção arquivar

Estados:

* pronto para publicar
* bloqueado por pendências
* agendado
* publicado
* erro de publicação
* sem permissão

Microcopy importante:

* “Este conteúdo ainda possui pendências antes da publicação.”
* “Publicar agora tornará esta versão visível no produto.”
* “Você pode salvar como rascunho e continuar depois.”

⸻

TELA 07 — HISTÓRICO DE VERSÕES

Contexto:

Plataforma → BYOP → Maestro Beton → Conteúdo → Home → Versões

Criar tela estilo histórico Git simplificado.

Elementos:

* lista de versões
* autor
* data
* comentário
* status
* diferença resumida
* ações

Ações:

* visualizar
* comparar
* restaurar

Estados:

* histórico preenchido
* sem versões
* loading
* erro
* sem permissão para restaurar

⸻

TELA 08 — COMPARAÇÃO DE VERSÕES

Criar tela de diff lado a lado.

Elementos:

* seletor versão A
* seletor versão B
* resumo das diferenças
* diff visual
* diff JSON simplificado
* botão restaurar versão
* botão voltar

Diferenças mockadas:

* título alterado
* CTA alterado
* imagem hero alterada
* SEO description alterada

Estados:

* comparando
* diferenças encontradas
* sem diferenças
* erro
* sem permissão para restaurar

⸻

COMPONENTES NOVOS DA SPRINT

Criar ou refinar:

* EditorialDashboardWidget
* ContentStatusBadge
* ContentDataGrid
* ContentCardMobile
* WorkflowBoard
* WorkflowLane
* WorkflowCard
* ContentStructureTree
* BlockEditorCanvas
* PropertiesPanel
* SEOPanel
* WorkflowPanel
* VersionTimeline
* VersionCompareView
* PublishPanel
* ResponsivePreviewFrame
* EditorialAttentionCard
* UnsavedChangesBanner
* ConflictAlert
* PermissionBlockedState

⸻

REGRAS DE UX

Aplicar:

* contexto sempre visível
* breadcrumbs obrigatórios
* feedback para toda ação
* skeleton para loading
* empty states úteis
* erros com recuperação
* botões ocultos quando usuário não tem permissão
* ações destrutivas sempre com confirmação

⸻

RESPONSIVIDADE

Gerar:

Mobile 375px
Tablet 768px
Desktop 1440px

Desktop:

* editor em 3 colunas
* dashboard em grid
* workflow em lanes
* DataGrid completo

Tablet:

* painéis laterais viram drawers
* workflow horizontal com scroll controlado
* editor em 2 áreas

Mobile:

* tabelas viram cards
* filtros viram drawer
* editor vira etapas
* publish panel vira bottom sheet
* preview ocupa quase tela inteira

Nunca permitir scroll horizontal na página.

⸻

QUALIDADE ESPERADA

O módulo deve parecer uma ferramenta editorial profissional.

Não deve parecer:

* Wordpress
* CMS genérico
* admin template
* formulário gigante
* editor improvisado

Deve transmitir:

“Estou operando conteúdo dentro de um produto digital.”

⸻

IMPORTANTE

Não gerar ainda:

* Asset Library detalhada
* Upload de Asset
* Forms Builder
* Submissions
* Analytics avançado
* Knowledge Graph Canvas
* Users
* Settings
* Audit completa

Esses módulos virão em sprints futuras.

Nesta sprint, entregue apenas o módulo de Conteúdo.