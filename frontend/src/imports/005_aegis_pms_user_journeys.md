# 005 — AEGIS PMS USER JOURNEYS
## Versão 1.0

# Objetivo

Este documento descreve as jornadas dos principais perfis do Aegis PMS.

O foco não é a tela.

O foco é o comportamento do usuário.

Cada jornada representa um fluxo real de trabalho dentro do ecossistema.

---

# Perfis Oficiais

1. Super Admin
2. Tenant Admin
3. Product Manager
4. Editor
5. Viewer

---

# JOURNEY 01 — SUPER ADMIN

## Missão

Administrar toda a plataforma.

## Objetivos

- Criar tenants
- Gerenciar assinaturas
- Administrar segurança
- Auditar operações
- Acompanhar saúde do ecossistema

## Fluxo Principal

Login
→ Dashboard Global
→ Criar Tenant
→ Configurar Tenant
→ Convidar Tenant Admin
→ Monitorar Ativação
→ Auditoria

## Cenários Críticos

### Tenant Suspenso

Recebe alerta
→ Acessa Tenant
→ Analisa métricas
→ Suspende ou reativa

### Investigação

Dashboard
→ Auditoria
→ Usuário
→ Evento
→ Evidências

## Métricas

- Tenants ativos
- Produtos ativos
- Usuários ativos
- Erros críticos
- Consumo de recursos

---

# JOURNEY 02 — TENANT ADMIN

## Missão

Administrar um tenant.

## Objetivos

- Criar produtos
- Organizar equipes
- Definir permissões
- Acompanhar operação

## Fluxo Principal

Login
→ Selecionar Tenant
→ Dashboard Tenant
→ Criar Produto
→ Configurar Produto
→ Convidar Equipe
→ Liberar Operação

## Cenários Críticos

### Novo Produto

Dashboard
→ Produtos
→ Novo Produto
→ Escolher Template
→ Configurar
→ Publicar

### Gestão de Equipe

Equipe
→ Convidar Usuário
→ Escolher Perfil
→ Confirmar Permissões

## Métricas

- Produtos ativos
- Usuários ativos
- Conteúdos publicados
- Formulários enviados

---

# JOURNEY 03 — PRODUCT MANAGER

## Missão

Garantir que um produto digital funcione.

## Objetivos

- Coordenar conteúdo
- Aprovar publicações
- Acompanhar métricas
- Gerenciar ativos

## Fluxo Principal

Login
→ Produto
→ Dashboard Produto
→ Revisar Pendências
→ Aprovar Conteúdo
→ Validar Analytics
→ Acompanhar Evolução

## Cenário Editorial

Conteúdo
→ Revisão
→ Aprovar
→ Publicar
→ Monitorar Performance

## Cenário Estratégico

Analytics
→ Indicadores
→ Queda de Conversão
→ Investigar
→ Criar Plano de Ação

## Métricas

- Publicações
- Conversões
- Crescimento
- Performance

---

# JOURNEY 04 — EDITOR

## Missão

Produzir conteúdo.

## Objetivos

- Criar páginas
- Criar artigos
- Gerenciar mídia
- Revisar materiais

## Fluxo Principal

Login
→ Produto
→ Conteúdo
→ Novo Conteúdo
→ Salvar Rascunho
→ Revisar
→ Enviar Aprovação

## Cenário de Produção

Novo Artigo
→ Editor
→ Assets
→ Inserir Imagens
→ Preview
→ Workflow

## Cenário de Atualização

Buscar Conteúdo
→ Editar
→ Nova Versão
→ Aprovação

## Métricas

- Conteúdos criados
- Conteúdos publicados
- Conteúdos pendentes

---

# JOURNEY 05 — VIEWER

## Missão

Consumir informação.

## Objetivos

- Consultar dados
- Acompanhar indicadores
- Visualizar conteúdo

## Fluxo Principal

Login
→ Produto
→ Dashboard
→ Relatórios
→ Consulta

## Cenários

### Diretoria

Dashboard
→ Indicadores
→ Exportar Relatório

### Cliente

Conteúdo
→ Aprovação
→ Histórico

## Métricas

- Acessos
- Relatórios visualizados

---

# JORNADAS TRANSVERSAIS

## Notificações

Evento
→ Geração de Notificação
→ Centro de Notificações
→ Ação do Usuário

## Busca Global

Abrir Busca
→ Digitar
→ Encontrar Produto
→ Encontrar Conteúdo
→ Navegar

## Auditoria

Ação
→ Registro
→ Histórico
→ Consulta

---

# EXPERIÊNCIA DE SUCESSO

O usuário deve sentir:

- Clareza
- Controle
- Contexto
- Segurança
- Rastreabilidade

Nunca deve sentir:

- Perda de contexto
- Navegação confusa
- Falta de feedback
- Medo de executar ações

---

# CONCLUSÃO

O Aegis PMS não é orientado a telas.

É orientado a jornadas.

Toda decisão futura de UX, UI, arquitetura e desenvolvimento deve preservar a capacidade do usuário compreender:

Tenant
→ Produto
→ Módulo
→ Recurso

sem perder contexto durante a navegação.
