# 007 — AEGIS PMS DESIGN SYSTEM MAPPING
## Versão 1.0

# VISÃO

O Aegis PMS não utilizará o Eirene UI como dependência.

O Aegis possuirá seu próprio Design System.

Motivo:

O sistema não é um site.
O sistema não é uma landing page.
O sistema não é um CMS convencional.

O Aegis é um Sistema Operacional para Produtos Digitais.

Portanto seus componentes devem nascer orientados a:

- operação
- produtividade
- gestão
- governança
- auditoria
- conhecimento
- escala

---

# FILOSOFIA VISUAL

Palavras-chave:

- Clareza
- Controle
- Contexto
- Hierarquia
- Produtividade
- Confiabilidade

O usuário nunca deve se sentir perdido.

Toda tela deve transmitir:

"Eu sei onde estou.
Eu sei o que estou administrando.
Eu sei o que acabou de acontecer.
Eu sei qual é o próximo passo."

---

# CAMADAS DO DESIGN SYSTEM

## Foundations

- Colors
- Typography
- Spacing
- Radius
- Elevation
- Motion
- Grid
- Iconography

## Layout

- AppShell
- Container
- Grid
- SplitView
- WorkspaceLayout
- PageHeader
- SectionBlock

## Navigation

- Sidebar
- SidebarGroup
- SidebarItem
- ProductSwitcher
- TenantSwitcher
- Breadcrumb
- CommandPalette
- SearchBar
- MobileNavigation

## Data Entry

- TextField
- TextArea
- RichTextEditor
- NumberField
- CurrencyField
- DatePicker
- DateRangePicker
- Select
- MultiSelect
- Checkbox
- Radio
- Toggle
- FileUpload
- AssetPicker
- UserPicker
- TagInput

## Data Display

- Table
- DataGrid
- Card
- StatsCard
- MetricCard
- Badge
- Avatar
- Timeline
- ActivityFeed
- AuditTrail
- KnowledgeNodeCard

## Feedback

- Toast
- Alert
- Banner
- ConfirmationDialog
- Modal
- Drawer
- LoadingOverlay
- Skeleton

## Workflow

- Stepper
- StatusFlow
- ApprovalFlow
- ReviewPanel
- PublishPanel

## Analytics

- ChartContainer
- KPIBlock
- TrendIndicator
- ReportWidget

## Knowledge Graph

- GraphCanvas
- NodeInspector
- RelationshipPanel
- KnowledgeExplorer

---

# COMPONENTES FUNDACIONAIS

## AppShell

Componente mais importante.

Responsável por:

- sidebar
- header
- área principal
- contexto do produto

Toda tela nasce dentro dele.

---

## TenantSwitcher

Permite alternar entre tenants.

Sempre visível.

Nunca mais de um clique para trocar tenant.

---

## ProductSwitcher

Permite alternar produtos.

Componente central da plataforma.

Exemplos:

- Maestro Beton
- Genesis
- WikiDev
- Aion
- Conecta Talentos

---

# COMPONENTES DE OPERAÇÃO

## DataGrid

Não é uma tabela comum.

Deve suportar:

- ordenação
- filtros
- busca
- paginação
- seleção múltipla
- exportação
- ações em lote
- colunas configuráveis

Será um dos componentes mais usados.

---

## AuditTrail

Exibe:

- quem
- quando
- onde
- o que mudou

Indispensável para governança.

---

## ActivityFeed

Linha temporal operacional.

Exemplo:

Editor publicou artigo.
Product Manager aprovou.
Sistema gerou relatório.

---

# COMPONENTES DE CONTEÚDO

## RichTextEditor

Editor principal do Aegis.

Características:

- markdown
- html
- preview
- imagens
- vídeos
- embeds
- tabelas

Modo especial:

Matrix Mode

Características:

- cursor verde
- tipografia técnica
- foco extremo
- sensação de terminal editorial

---

## AssetPicker

Selecionador universal de assets.

Permite:

- imagens
- vídeos
- documentos
- áudios

Com preview instantâneo.

---

# COMPONENTES DE WORKFLOW

## ApprovalFlow

Representa:

Draft
→ Review
→ Approved
→ Published
→ Archived

Visualmente:

- extremamente claro
- altamente rastreável

---

## PublishPanel

Painel de publicação.

Permite:

- publicar agora
- agendar
- revisar
- cancelar

---

# COMPONENTES DE ANALYTICS

## KPIBlock

Exibe:

- valor
- comparação
- tendência

Exemplo:

+15%
↑

---

## TrendIndicator

Representa:

- crescimento
- queda
- estabilidade

Utilizado em toda plataforma.

---

# COMPONENTES DE KNOWLEDGE GRAPH

## GraphCanvas

Canvas principal.

Permite navegar conhecimento.

---

## NodeInspector

Painel lateral.

Exibe:

- propriedades
- relações
- metadados

---

## RelationshipPanel

Mostra conexões.

Exemplo:

Produto
→ Conteúdo
→ Categoria
→ Autor

---

# COMPONENTES DE SEGURANÇA

## PermissionMatrix

Componente obrigatório.

Representa:

Perfis
×
Permissões

Visualização matricial.

---

## AccessPreview

Mostra exatamente o que determinado perfil consegue visualizar.

---

# COMPONENTES DE COLABORAÇÃO

## CommentThread

Comentários contextuais.

---

## MentionField

Suporta:

@usuário

---

## NotificationCenter

Central de notificações.

Agrupa:

- sistema
- workflow
- analytics
- colaboração

---

# COMPONENTES DE BUSCA

## GlobalSearch

Busca universal.

Procura:

- produtos
- conteúdos
- usuários
- assets
- formulários
- conhecimento

Atalho:

CTRL + K

CMD + K

---

# COMPONENTES DE ESTADO

## EmptyState

Sempre possui:

- ícone
- mensagem
- ação

---

## ErrorState

Sempre possui:

- erro
- explicação
- recuperação

---

## SuccessState

Sempre possui:

- confirmação
- contexto
- próximo passo

---

# MATRIZ DE PRIORIDADE

## MVP

AppShell
Sidebar
TenantSwitcher
ProductSwitcher
Breadcrumb
PageHeader
DataGrid
Card
StatsCard
Table
TextField
TextArea
RichTextEditor
Select
MultiSelect
FileUpload
Toast
Modal
Drawer
ConfirmationDialog
AuditTrail
ActivityFeed
ApprovalFlow
PublishPanel
GlobalSearch

---

## V2

GraphCanvas
NodeInspector
RelationshipPanel
KnowledgeExplorer
PermissionMatrix
AccessPreview
NotificationCenter
CommentThread
MentionField

---

## V3

Automações
IA
Assistentes
Insights Inteligentes
Knowledge Graph Avançado

---

# CONCLUSÃO

O Design System do Aegis não é orientado a componentes bonitos.

É orientado a componentes operacionais.

Cada componente deve responder uma pergunta:

"Isso ajuda alguém a administrar melhor um produto digital?"

Se a resposta for não,
o componente não pertence ao Aegis PMS.
