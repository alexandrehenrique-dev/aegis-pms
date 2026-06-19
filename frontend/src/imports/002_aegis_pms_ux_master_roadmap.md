
# 🛡️ AEGIS PMS — UX ROADMAP MASTER
## Artefato 002 — Guia de UX, Telas e Fluxos para Eirene

Propósito: servir como fonte de verdade para a construção de todas as experiências do Aegis PMS.

O objetivo não é desenhar telas isoladas.

O objetivo é desenhar uma plataforma administrativa capaz de operar múltiplos produtos digitais do ecossistema BYOP.

---

# VISÃO DE EXPERIÊNCIA

O usuário nunca entra no Aegis para editar uma página.

Ele entra para administrar um produto.

Hierarquia mental:

Login
→ Tenant
→ Produto
→ Módulos
→ Conteúdo
→ Operação

---

# PERSONAS

## Aegis Super Admin

Controla toda a plataforma.

Pode:

- criar tenants
- criar produtos
- gerenciar usuários
- habilitar módulos
- visualizar auditoria
- administrar integrações

## Tenant Admin

Controla sua organização.

Pode:

- convidar usuários
- criar produtos
- habilitar módulos
- aprovar conteúdo
- administrar permissões

## Product Manager

Responsável pelo produto.

Pode:

- administrar roadmap
- conteúdo
- formulários
- assets
- analytics

## Editor

Produz conteúdo.

Pode:

- criar
- editar
- enviar para aprovação

## Viewer

Somente leitura.

---

# MAPA DE NAVEGAÇÃO GLOBAL

## Área Pública

- Login
- Recuperar senha
- Primeiro acesso

## Área Autenticada

### Dashboard

### Tenants

### Produtos

### Conteúdos

### Assets

### Formulários

### Analytics

### Knowledge Graph

### Usuários

### Configurações

---

# FLUXO 01 — AUTENTICAÇÃO

## Tela Login

Componentes:

- logo Aegis
- usuário/email
- senha
- entrar
- esqueci senha

Estados:

- carregando
- credenciais inválidas
- bloqueado
- sucesso

## Primeiro acesso

- definir senha
- aceitar termos
- selecionar tenant padrão

## Recuperação de senha

- solicitar email
- token
- redefinição

---

# FLUXO 02 — ONBOARDING

Primeiro login.

Passos:

### Passo 1

Bem-vindo ao Aegis.

### Passo 2

Criar tenant.

Campos:

- nome
- slug
- descrição

### Passo 3

Criar primeiro produto.

### Passo 4

Escolher módulos.

### Passo 5

Finalização.

---

# FLUXO 03 — DASHBOARD GLOBAL

Objetivo:

Centro operacional.

Widgets:

- produtos ativos
- conteúdos pendentes
- aprovações
- formulários recebidos
- assets recentes
- atividades recentes
- usuários ativos

---

# FLUXO 04 — GESTÃO DE TENANTS

## Lista

Tabela:

- nome
- slug
- usuários
- produtos
- status

Ações:

- criar
- editar
- arquivar

## Detalhe

Abas:

- geral
- usuários
- produtos
- auditoria

---

# FLUXO 05 — GESTÃO DE USUÁRIOS

## Listagem

Filtros:

- tenant
- papel
- status

## Cadastro

Campos:

- nome
- email
- papel
- tenant

## Convite

Fluxo completo de convite.

---

# FLUXO 06 — PRODUTOS

## Lista de produtos

Cards ou tabela.

Exemplos:

- Maestro Beton
- Conecta Talentos
- Alexandre Dev
- Loki
- WikiDev
- CMSS

## Criar produto

Campos:

- nome
- slug
- tipo
- idioma padrão

## Detalhe do produto

Abas:

- visão geral
- módulos
- conteúdo
- assets
- forms
- analytics
- graph
- configurações

---

# FLUXO 07 — CATÁLOGO DE MÓDULOS

Tela semelhante a marketplace interno.

Cada módulo possui:

- descrição
- status
- dependências
- habilitar/desabilitar

---

# FLUXO 08 — CMS / CONTENT

## Dashboard editorial

Widgets:

- rascunhos
- em revisão
- publicados
- arquivados

## Conteúdos

Tabela:

- título
- tipo
- autor
- idioma
- status

## Editor

Layout principal.

Sidebar esquerda:

estrutura.

Centro:

editor.

Direita:

propriedades.

---

# FLUXO 09 — PÁGINAS

## Listagem

- Home
- Sobre
- Agenda
- Contato

## Edição

Tabs:

- PT
- EN
- ES

Blocos:

- hero
- sections
- seo

Publicação por bloco.

---

# FLUXO 10 — WORKFLOW EDITORIAL

Estados:

Draft
→ In Review
→ Published
→ Archived

## Editor

Envia revisão.

## Admin

Aprova.

## Histórico

Timeline completa.

---

# FLUXO 11 — VERSIONAMENTO

Tela semelhante Git.

Lista:

- versão
- autor
- data

Comparação:

lado a lado.

Ação:

restaurar versão.

---

# FLUXO 12 — ASSETS

## Biblioteca

Grid.

Filtros:

- imagem
- vídeo
- pdf
- áudio

Busca:

- nome
- tag

## Upload

Drag and drop.

## Detalhe

Preview.

Metadados.

Uso no sistema.

---

# FLUXO 13 — FORMULÁRIOS

## Lista

- contato
- orçamento
- candidatura

## Builder MVP

Campos fixos inicialmente.

## Respostas

Tabela filtrável.

---

# FLUXO 14 — SUBMISSIONS

Visualização operacional.

Filtros:

- produto
- formulário
- período

Ações:

- exportar
- arquivar

---

# FLUXO 15 — ANALYTICS

Dashboard.

Widgets:

- visitas
- conversão
- páginas populares
- formulários

---

# FLUXO 16 — SEO

## Configuração

- title
- description
- keywords
- og image

## Preview

Google.

Social.

---

# FLUXO 17 — KNOWLEDGE GRAPH

Uma das áreas mais importantes.

## Visão tabela

Nodes.

Edges.

## Visão grafo

Canvas interativo.

## Node

Detalhe:

- tipo
- relações
- metadados

## Edge

Origem.
Destino.
Tipo.

Casos:

Projeto → Tecnologia

Artigo → Tópico

Poema → Música

Vaga → Candidato

---

# FLUXO 18 — AUDITORIA

Timeline.

Eventos:

- login
- criação
- edição
- publicação
- exclusão lógica

Filtros avançados.

---

# FLUXO 19 — CONFIGURAÇÕES

## Produto

- idioma padrão
- branding
- domínios

## Tenant

- dados gerais
- segurança

## Sistema

Somente super admin.

---

# FLUXO 20 — NOTIFICAÇÕES FUTURAS

Centro de notificações.

Tipos:

- aprovação pendente
- erro integração
- novo formulário
- publicação

---

# TELAS OBRIGATÓRIAS MVP

1. Login
2. Recuperar senha
3. Dashboard
4. Lista tenants
5. Detalhe tenant
6. Lista usuários
7. Convite usuário
8. Lista produtos
9. Criar produto
10. Detalhe produto
11. Catálogo módulos
12. Dashboard conteúdo
13. Lista conteúdos
14. Editor conteúdo
15. Workflow aprovação
16. Histórico versões
17. Biblioteca assets
18. Upload asset
19. Lista formulários
20. Lista submissions
21. Dashboard analytics
22. Configuração SEO
23. Lista nodes
24. Lista edges
25. Visualização graph
26. Auditoria
27. Configurações

---

# DIRETRIZES PARA EIRENE

Não desenhar páginas.

Desenhar sistemas.

Toda tela deve responder:

- Onde estou?
- Qual tenant?
- Qual produto?
- Qual módulo?
- O que posso fazer?
- O que aconteceu?

A experiência deve ser:

- rápida
- previsível
- modular
- escalável

O usuário nunca deve se perder.

O Aegis deve parecer um centro de comando.

Não um CMS.
Não um ERP.
Não um CRUD.

Mas uma plataforma para administrar produtos digitais.
