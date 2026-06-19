# AEGIS PMS — SPRINT 08
## SETTINGS + USERS + PERMISSIONS + AUDIT

Continue a partir das Sprints 01, 02, 03, 04, 05, 06 e 07.

Preserve exatamente:

- Foundation
- App Shell
- Product Context
- Dashboards
- Content Module
- Assets Module
- Forms Module
- Analytics Module
- Knowledge Graph Module
- identidade white-first
- componentes existentes
- hierarquia Plataforma → Tenant → Produto → Módulo → Recurso

Não recriar nada.

Não alterar identidade visual.

Não gerar novos módulos de negócio.

Esta sprint fecha o primeiro ciclo da UI com governança, permissões, usuários, configurações e auditoria.

---

# OBJETIVO

Criar a camada administrativa e de governança do Aegis PMS.

Esta sprint deve criar:

1. Settings Overview
2. Product Settings
3. Tenant Settings
4. Users Management
5. Invite User
6. User Detail
7. Permission Matrix
8. Role Management
9. Access Preview
10. Audit Timeline
11. Audit Event Detail
12. Security & Integrations Settings

O foco não é configuração genérica.

O foco é controle, rastreabilidade e segurança operacional.

---

# CONTEXTO

Tenant:

BYOP

Produto:

Maestro Beton

Módulos ativos:

Settings

Users

Permissions

Audit

Breadcrumb base:

Plataforma → BYOP → Maestro Beton → Configurações

---

# REGRA CENTRAL

Toda tela deve responder:

- Onde estou?
- Estou configurando tenant, produto ou sistema?
- Quem tem acesso?
- O que pode ser alterado?
- Quais permissões afetam esta ação?
- O que foi alterado recentemente?
- Esta ação gera auditoria?
- Qual o risco operacional?

---

# TELA 01
## SETTINGS OVERVIEW

Contexto:

Plataforma → BYOP → Maestro Beton → Configurações

Criar visão geral das configurações.

Cards de configuração:

- Produto
- Tenant
- Equipe
- Permissões
- Integrações
- Segurança
- Auditoria
- SEO
- Domínios futuros
- Feature Flags futuras

Cada card deve mostrar:

- nome
- descrição
- status
- última atualização
- responsável
- risco operacional
- CTA "Abrir"

Criar seção "Atenção administrativa":

- "2 usuários com convite pendente."
- "1 integração sem configuração."
- "Permissões de Editor foram alteradas há 2 dias."
- "Auditoria possui eventos críticos recentes."

Estados:

- preenchido
- sem permissão
- loading
- erro parcial

---

# TELA 02
## PRODUCT SETTINGS

Contexto:

Plataforma → BYOP → Maestro Beton → Configurações → Produto

Configurações do produto.

Seções:

Dados gerais:

- nome
- slug
- tipo
- idioma padrão
- descrição
- status

Branding:

- logo
- cor principal
- favicon
- assets de marca

SEO:

- title padrão
- description padrão
- OG image
- locale

Publicação:

- ambiente
- preview público
- domínio futuro
- status de publicação

Módulos:

- módulos habilitados
- dependências
- status

Estados:

- alterações não salvas
- salvando
- salvo
- erro
- sem permissão

---

# TELA 03
## TENANT SETTINGS

Contexto:

Plataforma → BYOP → Configurações → Tenant

Configurações do tenant.

Seções:

Dados gerais:

- nome
- slug
- descrição
- status
- idioma padrão

Branding do tenant:

- logo
- cor institucional
- assinatura visual

Governança:

- política de convites
- regras de acesso
- retenção futura
- auditoria obrigatória

Limites futuros:

- produtos
- usuários
- storage
- formulários

Estados:

- alterações não salvas
- salvando
- salvo
- sem permissão

---

# TELA 04
## USERS MANAGEMENT

Contexto:

Plataforma → BYOP → Usuários

Criar tela operacional de usuários.

Tabela:

- Nome
- Email
- Papel
- Produtos
- Status
- Último acesso
- Convite
- Ações

Filtros:

- papel
- status
- produto
- último acesso
- convite pendente

Status:

- ativo
- convidado
- bloqueado
- removido
- sem acesso a produto

Ações:

- convidar
- editar permissões
- abrir usuário
- revogar acesso
- reenviar convite

Estados:

- lista preenchida
- sem usuários
- busca sem resultado
- convite pendente
- sem permissão
- loading

Mobile:

Tabela vira cards de usuário.

---

# TELA 05
## INVITE USER

Criar drawer ou tela de convite.

Campos:

- nome
- email
- papel
- produtos permitidos
- módulos permitidos
- mensagem opcional

Papéis:

- Super Admin
- Tenant Admin
- Product Manager
- Editor
- Viewer

UX obrigatória:

- resumo de permissões concedidas
- aviso de risco para papéis administrativos
- CTA "Enviar convite"
- CTA secundário "Cancelar"

Estados:

- email inválido
- usuário já existe
- convite enviado
- convite pendente
- sem permissão

---

# TELA 06
## USER DETAIL

Contexto:

Plataforma → BYOP → Usuários → Ana Martins

Criar detalhe do usuário.

Seções:

- Perfil
- Papéis
- Produtos permitidos
- Permissões efetivas
- Atividade recente
- Segurança
- Convites

Mostrar:

- avatar
- nome
- email
- status
- último acesso
- tenant
- produtos vinculados
- eventos recentes

Ações:

- editar permissões
- reenviar convite
- bloquear
- revogar acesso

---

# TELA 07
## PERMISSION MATRIX

Tela crítica.

Criar matriz visual:

Linhas:

- Conteúdo
- Assets
- Forms
- Analytics
- Knowledge Graph
- Users
- Settings
- Audit

Colunas:

- Visualizar
- Criar
- Editar
- Publicar
- Arquivar
- Excluir
- Exportar
- Administrar

Papéis:

- Tenant Admin
- Product Manager
- Editor
- Viewer

UX:

- checkboxes claros
- estados herdados
- estados bloqueados
- tooltip explicativo
- resumo de impacto
- botão salvar
- botão restaurar padrão

Estados:

- alterações não salvas
- conflito de permissão
- sem permissão para editar
- salvando
- salvo

---

# TELA 08
## ROLE MANAGEMENT

Criar tela de papéis.

Mostrar:

- lista de roles
- descrição
- número de usuários
- permissões principais
- risco
- última alteração

Roles:

- Super Admin
- Tenant Admin
- Product Manager
- Editor
- Viewer

Ações:

- duplicar role
- editar role
- restaurar padrão
- visualizar impacto

---

# TELA 09
## ACCESS PREVIEW

Diferencial UX.

Criar tela que mostra exatamente o que um papel ou usuário consegue ver.

Entrada:

- selecionar usuário ou role
- selecionar produto
- selecionar módulo

Resultado:

- menu visível
- ações disponíveis
- ações ocultas
- recursos bloqueados
- explicação

Exemplo:

Editor em Maestro Beton:

Pode:

- criar conteúdo
- editar conteúdo
- enviar revisão
- usar assets

Não pode:

- publicar
- alterar permissões
- acessar auditoria completa
- editar configurações do tenant

---

# TELA 10
## AUDIT TIMELINE

Contexto:

Plataforma → BYOP → Maestro Beton → Auditoria

Criar timeline operacional de auditoria.

Filtros:

- usuário
- produto
- módulo
- evento
- período
- severidade

Eventos mockados:

- conteúdo publicado
- permissão alterada
- usuário convidado
- asset substituído
- formulário exportado
- módulo habilitado
- configuração alterada

Cada evento deve mostrar:

- ator
- ação
- recurso
- produto
- módulo
- data/hora
- severidade
- CTA "Ver detalhe"

Estados:

- timeline preenchida
- sem eventos
- loading
- erro
- sem permissão

---

# TELA 11
## AUDIT EVENT DETAIL

Criar detalhe do evento de auditoria.

Mostrar:

- ID do evento
- ator
- papel do ator
- tenant
- produto
- módulo
- recurso
- ação
- data/hora
- antes/depois
- payload resumido
- IP futuro
- user agent futuro
- trace id futuro

Ações:

- copiar ID
- exportar evento
- abrir recurso relacionado
- ver usuário
- ver produto

---

# TELA 12
## SECURITY & INTEGRATIONS SETTINGS

Criar tela administrativa simples.

Seções:

Segurança:

- sessões ativas
- tokens futuros
- política de senha
- rate limit futuro
- 2FA futuro

Integrações:

- Webhooks
- Analytics Provider
- Storage Provider
- Email Provider
- WhatsApp futuro
- Telegram futuro

Cada integração:

- status
- última sincronização
- ambiente
- CTA configurar

Estados:

- conectado
- desconectado
- erro
- requer atenção

---

# COMPONENTES NOVOS

SettingsCard

SettingsSection

UserTable

UserCardMobile

InviteUserDrawer

UserDetailPanel

PermissionMatrix

PermissionCell

RoleCard

AccessPreviewPanel

AuditTimeline

AuditEventCard

AuditEventDetail

SecuritySettingsPanel

IntegrationCard

RiskBadge

PermissionImpactSummary

UnsavedPermissionChanges

---

# REGRAS DE UX

Aplicar:

- contexto sempre visível
- breadcrumbs obrigatórios
- ações críticas com confirmação
- permissões ocultas quando não disponíveis
- auditoria para toda alteração relevante
- feedback visual após salvar
- estados de erro recuperáveis
- alterações não salvas bem visíveis

---

# RESPONSIVIDADE

Desktop 1440

- tabelas completas
- matriz de permissões ampla
- audit timeline com filtros laterais
- settings em cards organizados

Tablet 768

- tabelas reduzidas
- matriz com scroll controlado interno
- filtros em drawer
- cards em 2 colunas

Mobile 375

- usuários viram cards
- permission matrix vira agrupamento por módulo
- audit timeline vira lista vertical
- settings em cards empilhados
- drawers full-screen

Nunca permitir scroll horizontal na página.

---

# QUALIDADE ESPERADA

O usuário deve sentir:

"Eu tenho controle sobre quem acessa o quê e consigo rastrear tudo que aconteceu."

Não:

"Estou preenchendo uma tela genérica de configurações."

---

# IMPORTANTE

Esta sprint fecha o primeiro ciclo do protótipo.

Não gerar novos módulos de negócio.

Não gerar automações.

Não gerar IA.

Não gerar marketplace.

Apenas Settings, Users, Permissions e Audit.