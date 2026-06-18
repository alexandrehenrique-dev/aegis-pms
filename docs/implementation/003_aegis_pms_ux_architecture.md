# AEGIS PMS
# UX INFORMATION ARCHITECTURE
## Versão 1.0

---

# 1. VISÃO

O Aegis PMS (Product Management System) não é um CMS tradicional.

Seu objetivo não é administrar páginas.

Seu objetivo é administrar produtos digitais.

Todo o restante — conteúdo, assets, formulários, analytics e conhecimento — existe para servir aos produtos.

Por esse motivo, toda a arquitetura de navegação do sistema é centrada em:

Tenant → Produto → Módulo → Recurso

---

# 2. PRINCÍPIOS DE NAVEGAÇÃO

## 2.1 Contexto sempre visível

O usuário nunca deve se perguntar:

- em qual tenant está
- em qual produto está
- em qual módulo está

Toda tela deve exibir:

Tenant Atual
→ Produto Atual
→ Módulo Atual

---

## 2.2 Navegação Progressiva

O sistema deve revelar complexidade gradualmente.

Usuários iniciantes:

- enxergam apenas o necessário

Usuários avançados:

- possuem acesso à profundidade operacional

---

## 2.3 Produto é o Centro

Não existem menus globais de conteúdo.

Não existem menus globais de assets.

Tudo pertence a um produto.

Exemplo:

Conteúdo
→ Site Maestro Beton

Assets
→ Site Maestro Beton

Analytics
→ Site Maestro Beton

Knowledge Graph
→ Site Maestro Beton

---

# 3. HIERARQUIA GLOBAL

## Nível 1

Plataforma

## Nível 2

Tenant

Exemplos:

- BYOP
- Cliente A
- Cliente B

## Nível 3

Produto

Exemplos:

- Maestro Beton
- Eirene UI
- WikiDev
- Genesis
- Aion

## Nível 4

Módulos

Exemplos:

- Dashboard
- Conteúdo
- Assets
- Forms
- Analytics
- Knowledge Graph
- Configurações

## Nível 5

Recursos

Exemplos:

- Artigos
- Categorias
- Mídias
- Formulários
- Eventos
- Relatórios

---

# 4. SITEMAP MASTER

Aegis PMS

├── Login
├── Recuperação de Senha
├── Convites
├── Seleção de Tenant
│
├── Dashboard Global
│
├── Tenants
│   ├── Lista
│   ├── Criar
│   ├── Editar
│   ├── Usuários
│   ├── Permissões
│   └── Auditoria
│
├── Produtos
│   ├── Catálogo
│   ├── Criar Produto
│   ├── Configurações
│   ├── Equipe
│   ├── Deploy
│   └── Histórico
│
└── Produto

    ├── Dashboard

    ├── Conteúdo
    │
    ├── Assets
    │
    ├── Forms
    │
    ├── Analytics
    │
    ├── Knowledge Graph
    │
    └── Configurações

---

# 5. NAVEGAÇÃO PRINCIPAL

Sidebar Permanente

Dashboard

Produtos

Conteúdo

Assets

Forms

Analytics

Knowledge Graph

Configurações

---

# 6. BREADCRUMBS

Exemplos

BYOP
→ Maestro Beton
→ Conteúdo
→ Artigos
→ Editar Artigo

---

BYOP
→ Genesis
→ Assets
→ Biblioteca de Imagens

---

BYOP
→ Eirene UI
→ Analytics
→ Componentes
→ Button

---

# 7. ARQUITETURA DE PERMISSÕES

## Super Admin

Acesso total

Pode:

- criar tenants
- remover tenants
- acessar todos os produtos
- administrar usuários

---

## Tenant Admin

Acesso completo ao tenant

Pode:

- criar produtos
- gerenciar equipes
- definir permissões

Não pode:

- administrar outros tenants

---

## Product Manager

Responsável operacional

Pode:

- administrar módulos
- aprovar conteúdo
- visualizar analytics

---

## Editor

Pode:

- criar conteúdo
- editar conteúdo
- gerenciar assets

Não pode:

- alterar permissões

---

## Viewer

Somente leitura

Pode:

- visualizar

Não pode:

- modificar

---

# 8. ARQUITETURA DE MENUS

Menu Global

- Dashboard
- Produtos
- Perfil
- Notificações

Menu do Produto

- Dashboard
- Conteúdo
- Assets
- Forms
- Analytics
- Knowledge Graph
- Configurações

Menu Contextual

Exemplo:

Conteúdo

- Artigos
- Páginas
- Categorias
- Tags
- Workflow

---

# 9. REGRAS DE UX

Toda ação crítica deve possuir:

- confirmação
- auditoria
- feedback visual

---

Nenhuma tela pode ficar vazia.

Estados obrigatórios:

- Empty
- Loading
- Error
- Success

---

Toda listagem deve possuir:

- busca
- filtro
- ordenação
- paginação

---

# 10. VISÃO FUTURA

A arquitetura foi desenhada para suportar:

- CMS
- Sites
- Landing Pages
- Aplicações Web
- Produtos SaaS
- Knowledge Graph
- IA
- Automações
- Multi-tenant Enterprise

Sem necessidade de mudança estrutural da navegação principal.