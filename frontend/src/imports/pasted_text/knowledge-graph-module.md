# AEGIS PMS — SPRINT 07
## KNOWLEDGE GRAPH MODULE

Continue a partir das Sprints 01, 02, 03, 04, 05 e 06.

Preserve exatamente:

- Foundation
- App Shell
- Product Context
- Content Module
- Assets Module
- Forms Module
- Analytics Module
- identidade white-first
- componentes existentes
- hierarquia Plataforma → Tenant → Produto → Módulo → Recurso

Não recriar nada.

Não alterar identidade visual.

Não gerar Settings.

Não gerar Audit.

---

# OBJETIVO

Criar o módulo Knowledge Graph.

O Knowledge Graph não é um grafo visual decorativo.

Ele é o mapa de conhecimento operacional do produto.

Seu papel é responder:

- Como tudo se conecta?
- Onde um conteúdo é usado?
- Quais assets dependem deste conteúdo?
- Quais formulários geram dados para este produto?
- Quais entidades possuem relacionamento?
- O que será impactado por uma alteração?

---

# CONTEXTO

Tenant:

BYOP

Produto:

Maestro Beton

Módulo:

Knowledge Graph

Breadcrumb:

Plataforma → BYOP → Maestro Beton → Knowledge Graph

---

# REGRA CENTRAL

Toda tela deve responder:

- Onde estou?
- O que estou analisando?
- Quais relações existem?
- O que depende deste elemento?
- O que será impactado se eu alterar algo?

---

# MODELO MENTAL

Entidades possíveis:

Produto

Conteúdo

Asset

Formulário

Página

Seção

Tag

Categoria

SEO

Usuário

Lead

Domínio

Workflow

Integração

---

# TELA 01
## KNOWLEDGE GRAPH OVERVIEW

Criar dashboard inicial do módulo.

Objetivo:

Mostrar o estado geral do conhecimento do produto.

Widgets:

- Entidades totais
- Relações totais
- Entidades órfãs
- Conteúdos sem vínculo
- Assets sem uso
- Formulários desconectados
- Relações recentes
- Mudanças recentes

Criar timeline:

- Asset conectado à página Home
- Formulário vinculado à Landing
- SEO associado ao conteúdo Sobre
- Nova categoria criada

Estados:

- preenchido
- vazio
- loading
- erro
- sem permissão

---

# TELA 02
## GRAPH CANVAS

Tela principal da sprint.

Layout desktop:

Canvas central.

Painel lateral esquerdo.

Painel lateral direito.

---

# CANVAS

Mostrar grafo visual.

Entidades mockadas:

Produto Maestro Beton

↓

Página Home

↓

Hero

↓

Imagem Hero

↓

SEO Home

↓

Formulário Orçamento

↓

Lead

Mostrar conexões reais.

Não criar rede caótica.

Layout limpo.

Espaçamento amplo.

Linhas suaves.

Fundo branco.

Visual elegante.

Inspirações:

Linear
Notion
FigJam
Miro

Mas mais operacional.

---

# INTERAÇÕES

Zoom

Pan

Selecionar nó

Centralizar

Expandir relações

Ocultar relações

Filtrar tipos

Pesquisar entidade

Destacar caminho

---

# PAINEL ESQUERDO
## GRAPH EXPLORER

Árvore navegável.

Exemplo:

Produto
 ├── Conteúdos
 ├── Assets
 ├── Forms
 ├── SEO
 ├── Categorias
 └── Integrações

Pesquisa.

Filtros.

Agrupamentos.

Estados:

expandido
colapsado
buscando
sem resultado

---

# PAINEL DIREITO
## NODE INSPECTOR

Quando um nó for selecionado.

Mostrar:

Nome

Tipo

Descrição

Data criação

Última alteração

Status

Tags

Relacionamentos

Dependências

Impactos

Ações

---

# AÇÕES

Abrir recurso

Editar recurso

Ver histórico

Ver dependências

Ver uso

Abrir módulo origem

---

# TELA 03
## RELATIONSHIP EXPLORER

Criar visão textual das relações.

Tabela:

Origem

Destino

Tipo

Módulo

Data

Status

Exemplos:

Home → Hero

Hero → Hero Image

Hero Image → Asset

Home → SEO

Home → Formulário

Filtros:

tipo

origem

destino

módulo

status

---

# TELA 04
## ENTITY DETAILS

Criar página detalhada de entidade.

Exemplo:

Página Home

Mostrar:

dados gerais

relacionamentos

dependências

uso

impactos

histórico

responsáveis

---

# SEÇÃO IMPORTANTE
## IMPACT ANALYSIS

Elemento diferencial.

Mostrar:

"Se esta entidade for alterada, os seguintes elementos serão impactados."

Exemplo:

Página Home

Impacta:

- SEO Home
- Hero
- Formulário Orçamento
- Landing Principal

Visual:

cards de impacto.

nível de severidade.

baixo
médio
alto

---

# TELA 05
## ENTITY SEARCH

Criar busca global do conhecimento.

Pesquisar:

conteúdos

assets

forms

usuários

tags

categorias

SEO

Mostrar:

resultado

tipo

módulo

relacionamentos

ações rápidas

---

# TELA 06
## ORPHAN ENTITIES

Criar visão especializada.

Mostrar:

conteúdos sem vínculo

assets não utilizados

formulários desconectados

SEO sem página

Categorias sem uso

Objetivo:

limpeza operacional.

---

# TELA 07
## KNOWLEDGE INSIGHTS

Criar painel de inteligência.

Exemplos:

"5 assets não estão sendo utilizados."

"Página Home possui 12 dependências."

"SEO está conectado a apenas 40% das páginas."

"Existem 3 formulários sem relacionamento."

Cada insight deve possuir:

descrição

gravidade

ação sugerida

link

---

# COMPONENTES NOVOS

GraphCanvas

GraphNode

GraphEdge

GraphExplorer

NodeInspector

RelationshipExplorer

EntityCard

ImpactCard

ImpactPanel

EntitySearch

OrphanEntityTable

KnowledgeInsightCard

GraphToolbar

GraphMiniMap

EntityBadge

RelationshipBadge

---

# REGRAS DE UX

O grafo deve ser compreensível.

Nunca criar visual poluído.

Nunca mostrar centenas de nós.

Priorizar:

contexto

clareza

impacto

dependência

navegação

---

# RESPONSIVIDADE

Desktop 1440

Canvas completo.

Explorer esquerda.

Inspector direita.

---

Tablet 768

Canvas principal.

Explorer e Inspector viram drawers.

---

Mobile 375

Não mostrar grafo gigante.

Usar:

listas

relacionamentos

árvores

impactos

cards

Canvas apenas simplificado.

---

# QUALIDADE ESPERADA

O usuário deve sentir:

"Eu entendo como o produto inteiro está conectado."

Não:

"Estou olhando um monte de bolinhas ligadas."

---

# IMPORTANTE

Não gerar ainda:

Users
Permissions
Settings
Audit

Esses módulos virão na Sprint 08.

Nesta sprint entregue apenas Knowledge Graph.