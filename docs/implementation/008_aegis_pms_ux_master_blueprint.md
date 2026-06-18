# 008 — AEGIS PMS UX MASTER BLUEPRINT
## Versão 1.0
## Documento Mestre para UX, UI e Figma

---

# VISÃO

Aegis PMS não é um CMS.

Aegis PMS é um Sistema Operacional para Produtos Digitais.

Sua missão é permitir que pessoas, equipes e organizações criem, operem, evoluam e governem produtos digitais a partir de uma única plataforma.

O usuário não administra páginas.

O usuário administra produtos.

---

# NORTH STAR

Ao abrir o sistema, qualquer usuário deve responder em menos de 3 segundos:

- Onde estou?
- O que estou administrando?
- O que aconteceu recentemente?
- Qual é o próximo passo?

---

# ARQUITETURA MENTAL

Plataforma
→ Tenant
→ Produto
→ Módulo
→ Recurso

Toda navegação nasce desta estrutura.

---

# IDENTIDADE VISUAL

## Personalidade

- Profissional
- Estratégica
- Operacional
- Confiável
- Inteligente
- Moderna

Nunca:

- Gamer
- Infantil
- Excessivamente corporativa
- Excessivamente futurista

---

## Sensação

Mistura de:

- Notion
- Linear
- Atlassian
- Stripe Dashboard
- Vercel Dashboard

Mas com identidade própria.

---

# PALETA CONCEITUAL

## Light First

O sistema deve nascer claro.

Razões:

- Longas jornadas de trabalho
- Menor fadiga visual
- Melhor leitura de dados

---

## Cores

Background Primário

#F8F9FB

Surface

#FFFFFF

Surface Secondary

#F2F4F7

Texto Primário

#111827

Texto Secundário

#6B7280

Border

#E5E7EB

Accent

#2563EB

Success

#16A34A

Warning

#D97706

Danger

#DC2626

---

# TIPOGRAFIA

Títulos

Inter

Peso 600

Corpo

Inter

Peso 400

Dados

JetBrains Mono

---

# GRID

Mobile First

12 colunas desktop

8 colunas tablet

4 colunas mobile

---

# ESTRATÉGIA RESPONSIVA

## Mobile

Prioridade máxima.

Usuário deve conseguir:

- aprovar conteúdo
- revisar métricas
- consultar produto

sem precisar de desktop.

---

## Tablet

Modo operacional simplificado.

---

## Desktop

Experiência completa.

---

# APP SHELL

Estrutura universal.

┌ Header ─────────────┐
├ Sidebar ────────────┤
├ Workspace ──────────┤
└ Footer opcional ────┘

---

# HEADER GLOBAL

Sempre visível.

Contém:

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

# DASHBOARD GLOBAL

Objetivo:

Mostrar saúde do tenant.

Widgets:

- Produtos
- Usuários
- Conteúdos
- Conversões
- Alertas
- Atividade recente

---

# DASHBOARD DE PRODUTO

Objetivo:

Mostrar saúde operacional.

Widgets:

- KPI
- Workflow
- Publicações
- Conversões
- Assets recentes
- Atividade

---

# MÓDULO DE CONTEÚDO

## Estrutura

Conteúdo

├ Artigos
├ Páginas
├ Categorias
├ Tags
├ Workflow

---

## Tela de Edição

Layout:

Editor
Preview
Metadados

---

## Workflow

Draft
Review
Approved
Published
Archived

---

# MÓDULO DE ASSETS

Biblioteca central.

Tipos:

- imagem
- vídeo
- áudio
- documento

---

## Funcionalidades

Upload

Busca

Filtros

Tags

Pastas lógicas

Preview

---

# MÓDULO DE FORMS

Objetivo:

Capturar informação.

---

## Recursos

Builder

Respostas

Relatórios

Exportação

Integrações

---

# MÓDULO ANALYTICS

Objetivo:

Tomada de decisão.

---

## Widgets

KPI

Gráficos

Tendências

Comparativos

Conversões

---

# KNOWLEDGE GRAPH

Objetivo:

Transformar conteúdo em conhecimento navegável.

---

## Estrutura

Nós

Relações

Metadados

Dependências

---

## Telas

Graph Canvas

Node Inspector

Relationship Explorer

---

# CONFIGURAÇÕES

## Produto

Nome

Slug

Domínio

SEO

Integrações

---

## Equipe

Usuários

Perfis

Permissões

---

## Segurança

Auditoria

Tokens

Chaves

Logs

---

# COMPONENTES CRÍTICOS

## DataGrid

Com:

- busca
- filtro
- paginação
- exportação
- seleção múltipla

---

## AuditTrail

Quem

Quando

Onde

O que mudou

---

## ActivityFeed

Linha temporal do sistema.

---

## GlobalSearch

CTRL+K

CMD+K

Pesquisa:

- produtos
- usuários
- conteúdo
- assets
- conhecimento

---

# FEEDBACK

## Loading

Skeleton

---

## Success

Toast

---

## Warning

Banner

---

## Error

Recovery Action

---

# EMPTY STATES

Toda tela deve possuir:

- ícone
- explicação
- CTA

---

# ACESSIBILIDADE

WCAG AA

Navegação por teclado

Focus states

ARIA labels

---

# MOTION

Animações discretas.

Duração:

150ms a 250ms

Objetivo:

Comunicar estado.

Nunca decorar.

---

# DESIGN PRINCIPLES

## Clareza acima da beleza

## Informação acima do efeito

## Fluxo acima da estética

## Contexto acima da velocidade

## Confiança acima da inovação

---

# TELAS PRIORITÁRIAS MVP

1. Login
2. Dashboard Global
3. Dashboard Produto
4. Lista de Produtos
5. Produto
6. Lista de Conteúdo
7. Editor de Conteúdo
8. Assets
9. Forms
10. Analytics
11. Configurações
12. Usuários
13. Auditoria
14. Busca Global

---

# ENTREGA PARA FIGMA

O Figma deve produzir:

- Mobile
- Tablet
- Desktop

Com:

- estados vazios
- loading
- erro
- sucesso
- permissões

Todas as telas devem compartilhar:

- mesmo AppShell
- mesma navegação
- mesma linguagem visual
- mesmos componentes

---

# CONCLUSÃO

Aegis PMS deve parecer menos um CMS.

E mais um centro de comando.

Uma plataforma construída para administrar produtos digitais com clareza, contexto, rastreabilidade e confiança.
