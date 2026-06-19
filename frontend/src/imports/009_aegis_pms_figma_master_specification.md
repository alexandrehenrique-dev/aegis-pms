# 009 — AEGIS PMS FIGMA MASTER SPECIFICATION
## Versão 1.0
## Documento de Entrada para Figma Make

# OBJETIVO

Este documento serve como especificação única para geração da interface completa do Aegis PMS.

O resultado esperado não é um conjunto de telas isoladas.

O resultado esperado é uma plataforma coesa capaz de administrar produtos digitais.

---

# VISÃO DO PRODUTO

Aegis PMS é um Product Management System.

Ele administra:

- Produtos
- Conteúdo
- Assets
- Formulários
- Analytics
- Conhecimento
- Equipes
- Governança

O usuário nunca administra páginas.

O usuário administra produtos.

---

# DIREÇÃO VISUAL

## Palavras-chave

- Clareza
- Confiança
- Operação
- Inteligência
- Organização
- Contexto

## Referências

- Linear
- Notion
- Stripe Dashboard
- Atlassian
- Vercel Dashboard

Mistura:

50% operação
30% produtividade
20% inteligência

---

# ESTRATÉGIA VISUAL

## Light First

Tema principal:

Claro

Background:
#F8F9FB

Surface:
#FFFFFF

---

## Dark Theme

Disponível como alternância.

Mesmo layout.

Mesmo comportamento.

---

# RESPONSIVIDADE

## Mobile

0–767

## Tablet

768–1279

## Desktop

1280+

Todas as telas devem existir nos 3 formatos.

---

# APP SHELL

Estrutura obrigatória

Header Global

Sidebar

Workspace

Painel Contextual

---

# HEADER

Elementos obrigatórios

- Tenant Switcher
- Product Switcher
- Busca Global
- Notificações
- Perfil

---

# SIDEBAR

Dashboard

Conteúdo

Assets

Forms

Analytics

Knowledge Graph

Configurações

---

# TELA 01 — LOGIN

Objetivo:

Acesso ao sistema.

Estados:

- default
- loading
- erro
- sucesso

Elementos:

- logo
- email
- senha
- recuperar senha
- entrar

---

# TELA 02 — SELEÇÃO DE TENANT

Cards de tenant.

Busca.

Favoritos.

---

# TELA 03 — DASHBOARD GLOBAL

Widgets:

- tenants
- produtos
- usuários
- conteúdos
- alertas
- auditoria

Estados:

- vazio
- carregando
- preenchido

---

# TELA 04 — CATÁLOGO DE PRODUTOS

Visualização:

Grid

Tabela

Busca

Filtro

Ordenação

---

# TELA 05 — DASHBOARD DE PRODUTO

KPIs

Atividade

Workflow

Conversão

Publicações

---

# TELA 06 — CONTEÚDO

Lista:

- artigos
- páginas
- categorias
- tags

Ações:

- criar
- editar
- excluir
- publicar

---

# TELA 07 — EDITOR DE CONTEÚDO

Layout:

Editor
Preview
Metadados

Workflow lateral

Publish Panel

---

# TELA 08 — ASSETS

Visualizações:

Grid

Lista

Preview

Upload

Tags

Busca

---

# TELA 09 — FORMS

Builder

Respostas

Analytics

Exportação

---

# TELA 10 — ANALYTICS

KPIs

Charts

Conversões

Tendências

Comparativos

---

# TELA 11 — KNOWLEDGE GRAPH

Graph Canvas

Node Inspector

Relationship Explorer

---

# TELA 12 — CONFIGURAÇÕES

Produto

Equipe

Permissões

Integrações

Domínios

SEO

---

# TELA 13 — USUÁRIOS

Tabela

Perfis

Permissões

Convites

---

# TELA 14 — AUDITORIA

Timeline

Eventos

Filtros

Exportação

---

# TELA 15 — BUSCA GLOBAL

Modal universal

CTRL+K

CMD+K

Busca:

- produto
- conteúdo
- asset
- usuário
- formulário
- conhecimento

---

# COMPONENTES OBRIGATÓRIOS

## Navegação

Sidebar

Breadcrumb

Header

TenantSwitcher

ProductSwitcher

GlobalSearch

---

## Dados

Card

StatsCard

Table

DataGrid

Badge

Avatar

Timeline

AuditTrail

---

## Entrada

TextField

TextArea

RichTextEditor

Select

MultiSelect

Checkbox

Radio

Toggle

DatePicker

FileUpload

AssetPicker

---

## Feedback

Toast

Alert

Banner

Modal

Drawer

ConfirmationDialog

Skeleton

LoadingOverlay

---

# ESTADOS OBRIGATÓRIOS

Todo componente deve possuir:

- default
- hover
- active
- focus
- disabled

Quando aplicável:

- loading
- success
- error
- empty

---

# EMPTY STATES

Estrutura:

Ícone

Título

Descrição

CTA

---

# ERROS

Estrutura:

Problema

Consequência

Ação recomendada

---

# TOASTS

Posições:

- top-right
- top-left
- bottom-right
- bottom-left

---

# MODAIS

Tamanhos:

- sm
- md
- lg
- xl

---

# TABELAS

Devem possuir:

Busca

Filtro

Ordenação

Paginação

Seleção

Exportação

---

# MOTION

150ms–250ms

Função:

Comunicar mudança

Nunca decorar

---

# ACESSIBILIDADE

WCAG AA

Teclado

ARIA

Focus states

Contraste adequado

---

# FIGMA PAGES

01 Cover

02 Foundations

03 Navigation

04 Components

05 Dashboard

06 Content

07 Assets

08 Forms

09 Analytics

10 Knowledge Graph

11 Settings

12 Responsive

13 Empty States

14 Error States

15 Interaction States

16 User Flows

---

# EXPECTATIVA FINAL

O resultado deve parecer:

- plataforma SaaS real
- pronta para produção
- altamente escalável
- operacional
- confiável

Não deve parecer:

- template genérico
- CMS comum
- painel administrativo simples

O Aegis deve transmitir a sensação de um centro de comando para produtos digitais.
