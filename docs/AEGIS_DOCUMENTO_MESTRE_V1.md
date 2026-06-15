# AEGIS CMS — Documento Mestre de Produto (V1)

## Visão
O Aegis é uma plataforma multi-tenant orientada a contratos para gerenciar produtos digitais do ecossistema BYOP.

## Produtos Identificados
- CMSS
- Maestro Beton
- Alexandre Dev
- Conecta Talentos
- Loki
- WikiDev

## Princípios
- Conteúdo separado da apresentação
- Multi-tenant
- Multi-idioma
- Workflow editorial
- Auditoria
- Segurança centralizada

## Núcleo
- Tenant
- User
- Role
- Permission
- Asset
- Page
- Section
- Block
- SEO
- Analytics
- Translation
- Revision

## Catálogo Inicial de Blocos
- HeroBlock
- RichTextBlock
- MarkdownBlock
- GalleryBlock
- TimelineBlock
- CTASectionBlock
- FAQBlock
- ContactBlock
- EventListBlock
- CardListBlock
- TwoColumnBlock

## Workflow
Draft -> Review -> Approved -> Published -> Archived

## Keycloak
- Realm
- Groups
- Roles
- Policies

## Módulos
### CMS
### Blog
### RH
### Biblioteca
### Wiki

## Domínios
### RH
JobPosting
CandidateSubmission
Lead

### Portfólio
Projects
Articles
Experiences
Downloads

### Música
Serviços
Agenda
Repertório
Depoimentos
Cursos

### Institucional
Eventos
Timeline
Galeria
Contato

## Diretrizes Eirene
- Dark First
- Dashboard moderno
- Menus dinâmicos
- Componentização por feature

## Diretrizes Aegis
- Spring Boot
- PostgreSQL
- Keycloak
- Docker
- OpenAPI

## Conclusão
O Aegis gerencia produtos, não páginas.

# AEGIS CMS — Documento Mestre de Produto

## AMPS — Aegis Master Product Specification

**Versão:** 1.0  
**Status:** Fonte de verdade inicial para arquitetura, UI/UX e desenvolvimento  
**Ecossistema:** BYOP — Build Your Own Path  
**Responsável estratégica:** Athena  
**Consumidores deste documento:** Aegis, Eirene, Zeus, Daedalus, Loki e futuros agentes do Panteão BYOP

---

# 1. Declaração Fundamental

O Aegis não deve ser tratado como um CMS tradicional.

O Aegis é uma **plataforma multi-produto, multi-tenant, modular e orientada a contratos**, criada para administrar produtos digitais do ecossistema BYOP e de futuros clientes.

A definição central é:

> O Aegis não gerencia páginas.  
> O Aegis gerencia produtos digitais, suas capacidades, seus conteúdos, seus contratos, seus usuários, seus assets, seus fluxos editoriais, suas permissões, suas integrações e seus dados de operação.

Páginas são apenas uma forma de materializar um produto.

Frontends não devem possuir conteúdo fixo.

Frontends devem atuar como renderizadores de contratos.

O Aegis deve ser a fonte de verdade.

---

# 2. Problema que o Aegis Resolve

O ecossistema BYOP possui e continuará possuindo diversos produtos digitais com necessidades diferentes:

- sites institucionais;
- sites comerciais;
- portfólios profissionais;
- bibliotecas autorais;
- wikis técnicas;
- portais de recrutamento;
- blogs;
- landing pages;
- lojas futuras;
- sistemas educacionais futuros;
- comunidades futuras;
- produtos SaaS futuros.

Sem o Aegis, cada novo produto tende a nascer como um conjunto isolado de arquivos JSON, componentes específicos, regras hardcoded e contratos improvisados.

Isso cria risco de:

- duplicação de regras;
- acoplamento entre frontend e conteúdo;
- contratos frágeis;
- dificuldade de manutenção;
- dificuldade de evolução;
- falta de governança;
- ausência de controle editorial;
- ausência de versionamento;
- ausência de permissões por produto;
- ausência de histórico;
- dificuldade de escalar para múltiplos clientes;
- risco de virar um Frankenstein arquitetural.

O Aegis nasce para impedir isso.

---

# 3. Visão do Produto

O Aegis será a plataforma administrativa central do ecossistema BYOP.

Ele permitirá que um administrador cadastre produtos digitais, associe donos, habilite funcionalidades e permita que cada dono gerencie seus próprios conteúdos sem precisar alterar código.

A experiência desejada é:

```txt
Administrador BYOP
↓
Cadastra Produto
↓
Define Donos
↓
Seleciona Categoria
↓
Seleciona Funcionalidades
↓
Aegis habilita menus e módulos
↓
Dono acessa painel
↓
Dono edita conteúdos
↓
Frontend externo consome contrato JSON
↓
Produto público é atualizado
```

O Aegis deve suportar tanto uso interno quanto possibilidade futura de comercialização como plataforma.

---

# 4. Princípios Arquiteturais

## 4.1 Product-First

A entidade raiz do sistema é o **Produto**.

Não é site.

Não é página.

Não é cliente.

Produto é qualquer manifestação digital administrada pelo Aegis.

Exemplos:

- CMSS;
- Maestro Beton;
- Conecta Talentos;
- Alexandre Dev;
- Loki;
- WikiDev;
- futuros sites;
- futuras lojas;
- futuros portais;
- futuros produtos SaaS.

## 4.2 Contract-First

Toda entrega pública do Aegis deve ser baseada em contratos.

O contrato é a fronteira entre:

```txt
Aegis
↓
API
↓
Frontend externo
```

O frontend nunca deve depender diretamente da estrutura interna do banco.

O frontend consome contratos estáveis.

## 4.3 CMS-First

Todo conteúdo público deve ser editável pelo Aegis.

Não deve haver texto final hardcoded no frontend.

Pode haver componentes hardcoded no frontend, mas os dados renderizados devem vir do Aegis.

## 4.4 Modularidade

O Aegis deve ser composto por núcleo + módulos.

O núcleo existe para todos os produtos.

Os módulos são habilitados conforme a categoria e as funcionalidades do produto.

## 4.5 Multi-Tenant

O sistema deve suportar múltiplos produtos, múltiplos donos e múltiplos usuários, garantindo isolamento lógico dos dados.

Um dono de produto não pode acessar, consultar, alterar ou inferir dados de outro produto que não esteja associado a ele.

## 4.6 Segurança por Padrão

Toda ação deve ser protegida por autenticação, autorização e auditoria.

Permissões devem considerar:

- usuário;
- role;
- produto;
- funcionalidade;
- recurso;
- ação;
- escopo.

## 4.7 Evolução Sem Frankenstein

Funcionalidades comerciais futuras, como planos, billing, assinaturas, loja virtual e cobrança recorrente, devem ser previstas no modelo conceitual, mas não precisam ser totalmente implementadas no MVP.

O importante é deixar o espaço arquitetural reservado.

---

# 5. Produtos Fonte da Modelagem

Este documento nasce da consolidação de seis produtos reais ou planejados.

## 5.1 CMSS — Corporação Musical São Sebastião

Tipo: site institucional.

Revelou:

- páginas institucionais;
- navegação global;
- hero;
- seções;
- galeria;
- eventos;
- timeline histórica;
- contato;
- apoio/Pix;
- necessidade de `sections[]`;
- necessidade de `blocks[]`;
- necessidade de SEO por página;
- necessidade de versionamento de contratos;
- necessidade de validação runtime;
- necessidade de separar layout de conteúdo.

Aprendizado central:

> O Aegis precisa permitir que um site hoje JSON-driven evolua para um modelo real CMS-first baseado em páginas, seções, blocos, assets, SEO, status editorial, locale e validação.

## 5.2 Maestro Beton

Tipo: site pessoal/profissional/comercial para músico.

Revelou:

- serviços;
- agenda;
- repertório;
- vídeos;
- galeria;
- depoimentos;
- formulário de orçamento;
- WhatsApp flutuante;
- cursos futuros;
- produtos futuros;
- loja futura;
- área de membros futura;
- necessidade de captar leads comerciais.

Aprendizado central:

> O Aegis precisa suportar sites comerciais com foco em conversão, orçamento, prova social, mídia, agenda e expansão futura para cursos e produtos.

## 5.3 Conecta Talentos

Tipo: portal institucional + RH + vagas.

Revelou:

- empresa;
- serviços de RH;
- vagas;
- banco de talentos;
- candidatura;
- upload de currículo;
- leads corporativos;
- blog;
- possível CRM futuro;
- pipeline de candidatos futuro.

Aprendizado central:

> O Aegis precisa suportar domínios transacionais além de páginas, incluindo entidades como JobPosting, CandidateSubmission, Lead e TalentPool.

## 5.4 Alexandre Dev

Tipo: portfólio profissional.

Revelou:

- projetos;
- experiências;
- skills;
- artigos;
- downloads;
- currículo;
- cases;
- SEO profissional;
- analytics de projetos;
- identidade visual pessoal;
- contato com recrutadores.

Aprendizado central:

> O Aegis precisa suportar conteúdo de portfólio, cases, experiências profissionais, skills e downloads versionados.

## 5.5 Loki

Tipo: biblioteca filosófica/autoral.

Revelou:

- manifestos;
- poemas;
- reflexões;
- trechos;
- livros;
- playlists;
- referências musicais;
- download de obras;
- links de compra;
- anonimato autoral;
- identidade visual monocromática;
- experiência narrativa;
- analytics literário.

Aprendizado central:

> O Aegis precisa suportar obras autorais e bibliotecas digitais, não apenas posts e páginas.

## 5.6 WikiDev

Tipo: wiki técnica / plataforma de conhecimento.

Revelou:

- categorias hierárquicas;
- tópicos;
- artigos;
- busca;
- conteúdo relacionado;
- link preview;
- comentários;
- sugestões;
- report de bugs;
- candidatura para contribuidor;
- Telegram;
- Knowledge Graph futuro;
- fórum futuro;
- gamificação futura.

Aprendizado central:

> O Aegis precisa suportar conhecimento conectado, taxonomias, relacionamentos internos, previews contextuais e colaboração.

---

---

# Expansões Arquiteturais do Documento Mestre

As seções a seguir executam os passos do prompt mestre e complementam o Documento Mestre sem remover conteúdo existente. Elas devem ser tratadas como especificação oficial para arquitetura, backend, frontend, infraestrutura, segurança e manutenção futura.

# Modelo Completo de Tenants

## Objetivo

O modelo de Tenants transforma o Aegis em uma plataforma multi-tenant real, capaz de hospedar produtos de clientes, unidades internas do ecossistema BYOP e operações futuras sem vazamento de dados, permissões ou contexto. Tenant é a unidade administrativa isolada. Produto é uma manifestação digital dentro desse espaço. Usuário é uma pessoa autenticada. Organização é uma entidade comercial ou jurídica que pode coincidir com um tenant, mas não é sinônimo obrigatório dele.

O Aegis precisa ser multi-tenant porque CMSS, Maestro Beton, Conecta Talentos, Alexandre Dev, Loki, WikiDev e futuros clientes não podem compartilhar conteúdo, assets, analytics, formulários, SEO ou integrações sem fronteiras explícitas.

## Estratégia Multi-Tenant

A estratégia inicial é **Shared Database**, **Shared Schema** e **tenantId obrigatório** em todas as entidades sensíveis. O Aegis não deve adotar banco por tenant no MVP porque isso aumentaria custo operacional, backup, migração, deploy e observabilidade. Também não deve adotar schema por tenant inicialmente porque schema proliferation torna migrations, índices, versionamento e suporte mais caros. O isolamento será lógico, contratual e validado no backend.

Todo acesso administrativo deve validar: `tenantId`, `productId`, membership e role. O frontend informa contexto; o backend decide autorização. Todo conteúdo, asset, evento, formulário, submissão, SEO, integração, configuração, analytics e audit log deve pertencer direta ou indiretamente a um tenant.

```txt
Tenant BYOP
├── Aegis CMS
├── Loki
├── Alexandre Dev
└── WikiDev

Tenant Conecta Talentos
├── Site institucional
└── Portal de vagas
```

## Relação Tenant, Produto e Usuário

Um tenant pode possuir vários produtos. Um produto pertence a um único tenant e não pode existir sem tenant. Um usuário pode participar de múltiplos tenants e possuir papéis diferentes em cada um. Alexandre pode ser `SUPER_ADMIN` global, `TENANT_OWNER` do BYOP e `VIEWER` em um tenant de cliente. Liliane pode ser `TENANT_OWNER` do Conecta Talentos e não possuir acesso ao BYOP.

```txt
Tenant
├── Products
│   ├── Pages
│   ├── Assets
│   ├── Forms
│   └── Analytics
├── Users
├── Memberships
├── Features
├── Integrations
├── Settings
└── Billing futuro
```

## Ownership e Membership

O dono do tenant responde pela administração máxima do espaço. Administradores gerenciam configurações e usuários. Gestores operam produtos e fluxos. Usuários comuns consomem ou editam apenas aquilo que sua role permitir. A entidade `TenantMembership` conecta usuário e tenant.

```json
{
  "id": "membership_001",
  "tenantId": "tenant_byop",
  "userId": "user_alexandre",
  "role": "TENANT_OWNER",
  "status": "ACTIVE",
  "invitedBy": "user_admin",
  "invitedAt": "2026-01-01T10:00:00Z",
  "acceptedAt": "2026-01-01T12:00:00Z",
  "createdAt": "2026-01-01T10:00:00Z",
  "updatedAt": "2026-01-01T12:00:00Z"
}
```

Status oficiais: `INVITED`, `ACTIVE`, `SUSPENDED`, `REMOVED`. Convites possuem email, expiração, aceite, revogação e auditoria. O fluxo obrigatório é login, listagem de tenants acessíveis, seleção do tenant ativo, criação do tenant context e exibição dos produtos visíveis.

## Segurança Multi-Tenant

Riscos obrigatórios: tenant spoofing, horizontal privilege escalation, acesso cruzado, consulta indevida, upload indevido, analytics indevido, alteração de `tenantId` no payload e uso de `productId` de outro tenant. Mitigações: nunca confiar em `tenantId` isolado vindo do frontend; validar membership do usuário autenticado; validar que o produto pertence ao tenant; filtrar toda query sensível por tenant; registrar audit log; não retornar dados de outro tenant nem em mensagens de erro; separar cache por tenant; limpar estado ao trocar tenant.

## Keycloak, Banco e Billing

Para o MVP recomenda-se realm único no Keycloak, autenticação e roles globais no Keycloak, memberships no banco do Aegis. Tabelas iniciais: `tenants`, `tenant_memberships`, `tenant_settings`, `tenant_invitations`, `tenant_audit_logs`. Índices: `idx_tenants_slug`, `idx_memberships_tenant_user`, `idx_memberships_user_status`, `idx_products_tenant_slug`.

O tenant será a unidade natural de billing futuro: plano mensal, plano anual, limites de usuários, storage, produtos, features e integrações. O MVP deve preparar metadados de plano sem implementar cobrança.

## Contratos JSON

```json
{
  "tenant": {"id": "tenant_byop", "slug": "byop", "name": "BYOP", "status": "ACTIVE"},
  "settings": {"defaultLocale": "pt-BR", "supportedLocales": ["pt-BR", "en-US"]},
  "currentTenant": {"tenantId": "tenant_byop", "role": "TENANT_OWNER", "products": ["product_aegis"]}
}
```

## Leis dos Tenants

1. Todo produto pertence a um tenant.
2. Nenhuma entidade sensível existe fora de um tenant.
3. Nenhuma consulta administrativa ignora `tenantId`.
4. Nenhum usuário acessa tenant sem membership.
5. Nenhum `productId` é válido sem validar `tenantId`.
6. Todo asset pertence a um tenant.
7. Toda submissão pertence a um tenant.
8. Todo analytics event pertence a um tenant.
9. Tenant ativo no frontend é contexto, não autorização.
10. Autorização real acontece no backend.
11. Todo convite gera auditoria.
12. Billing futuro será calculado por tenant.
13. Exclusão de tenant exige política especial.
14. Cache, SEO, assets e integrações respeitam tenant.
15. Nenhum erro deve revelar dados de outro tenant.

# Modelo Completo de Memberships

## Objetivo

Membership é a cola entre identidade, tenant, produto, role e permissão. O Aegis não deve conceder permissões diretamente em `User -> Role`, porque esse modelo não carrega contexto, não escala para múltiplos tenants, dificulta auditoria e impede papéis diferentes por produto. O modelo correto é:

```txt
User
↓
Membership
↓
Tenant
↓
Product
↓
Permissions
```

## Hierarquia

Existem três níveis. Global Membership controla autoridade de plataforma, como `SUPER_ADMIN` e `PLATFORM_ADMIN`. Tenant Membership controla acesso ao espaço isolado, como `TENANT_OWNER`, `TENANT_ADMIN`, `TENANT_MANAGER` e `TENANT_VIEWER`. Product Membership controla atuação granular em um produto, como `PRODUCT_OWNER`, `PRODUCT_MANAGER`, `EDITOR`, `REVIEWER`, `AUTHOR` e `VIEWER`.

```txt
User
├── Global Membership
├── Tenant Membership
│   ├── Product Membership
│   └── Product Membership
└── Tenant Membership
    └── Product Membership
```

Tenant Membership define o limite superior. Product Membership define granularidade. Um `TENANT_VIEWER` não pode ser `PRODUCT_OWNER`. Um `TENANT_OWNER` pode administrar produtos do tenant. Um `TENANT_ADMIN` pode ser `PRODUCT_MANAGER` quando autorizado.

## Ciclo de Vida e Entidades

O ciclo é `INVITED -> PENDING -> ACTIVE -> SUSPENDED -> REMOVED`. Convites podem expirar, ser reenviados, cancelados ou revogados.

```json
{
  "tenantMembership": {
    "id": "tm_001",
    "tenantId": "tenant_byop",
    "userId": "user_alexandre",
    "role": "TENANT_OWNER",
    "status": "ACTIVE"
  },
  "productMembership": {
    "id": "pm_001",
    "tenantMembershipId": "tm_001",
    "productId": "product_wikidev",
    "role": "EDITOR",
    "status": "ACTIVE"
  }
}
```

## Membership Resolution Engine

O backend resolve permissões por cadeia: token, usuário, tenant membership, product membership, avaliação de permissão e decisão. Keycloak não deve armazenar toda a estrutura de memberships; ele armazena identidade, autenticação e roles globais. Memberships permanecem no banco do Aegis porque são domínio do produto e precisam de tenant, produto, status, auditoria e histórico.

## Matriz Essencial

| Ação | SUPER_ADMIN | PLATFORM_ADMIN | TENANT_OWNER | TENANT_ADMIN | TENANT_MANAGER | PRODUCT_OWNER | EDITOR | VIEWER |
|---|---|---|---|---|---|---|---|---|
| criar produto | sim | sim | sim | sim | não | não | não | não |
| convidar usuários | sim | sim | sim | sim | limitado | não | não | não |
| atribuir roles | sim | sim | sim | limitado | não | não | não | não |
| editar conteúdo | sim | sim | sim | sim | sim | sim | sim | não |
| publicar conteúdo | sim | sim | sim | sim | limitado | sim | não | não |
| ver analytics | sim | sim | sim | sim | sim | sim | não | leitura |
| billing futuro | sim | sim | sim | leitura | não | não | não | não |

## Auditoria, Segurança e Banco

Eventos obrigatórios: `membership_created`, `membership_updated`, `membership_suspended`, `membership_removed`, `role_changed`, `invitation_sent`, `invitation_accepted`. Mitigações incluem impedir role spoofing, troca de tenant, troca de productId, payload forjado e privilege escalation. Tabelas: `tenant_memberships`, `product_memberships`, `membership_invitations`, `membership_audit_logs`, `membership_permissions_cache` futuro.

## Leis dos Memberships

1. Nenhum usuário recebe permissões sensíveis diretamente.
2. Toda permissão nasce de um membership.
3. Todo tenant membership pertence a um tenant.
4. Todo product membership depende de tenant membership.
5. Nenhuma role de produto excede a autoridade do tenant.
6. Membership removido perde acesso imediatamente.
7. Membership suspenso não executa ações.
8. Todo convite gera auditoria.
9. Toda mudança de role gera auditoria.
10. O backend resolve permissões a cada operação sensível.
11. O frontend exibe permissões, mas não decide autorização.
12. Keycloak autentica; Aegis autoriza no domínio.
13. Membership é a fonte de verdade da autorização contextual.
14. Um usuário pode ter papéis diferentes em tenants diferentes.
15. A ausência de membership equivale a ausência de acesso.

# Catálogo Global de Content Types

## Objetivo

O Catálogo Global de Content Types é o registry oficial das entidades editoriais do Aegis. Produto representa a unidade digital; Page representa estrutura; Section organiza regiões; Block renderiza composição; Content Type representa uma entidade de negócio administrável, versionável, traduzível e publicável.

```txt
Product: WikiDev
↓
Content Type: KnowledgeArticle
↓
Page: /artigos/spring-security
↓
Section: Hero
↓
Block: RichTextBlock
```

Pages são estrutura. Content Types são domínio. Manifesto, poema, livro, projeto, vaga, evento, serviço e artigo não devem ser modelados como páginas genéricas quando possuem campos, workflow, permissões, SEO, relações e analytics próprios.

## Estrutura Base

```txt
Content Type
├── Schema
├── Fields
├── Workflow
├── SEO
├── Revision
├── Translation
├── Permissions
├── Relations
└── Analytics
```

```json
{
  "id": "content_001",
  "tenantId": "tenant_byop",
  "productId": "product_alexandre_dev",
  "type": "Project",
  "slug": "aegis-cms",
  "title": "Aegis CMS",
  "status": "PUBLISHED",
  "locale": "pt-BR",
  "seo": {},
  "metadata": {},
  "content": {},
  "createdAt": "2026-01-01T10:00:00Z",
  "updatedAt": "2026-01-01T10:00:00Z"
}
```

## Catálogo Oficial

`Page` representa páginas institucionais e landing pages. `Event` atende CMSS e Maestro Beton com título, descrição, datas, local, imagem e visibilidade. `Gallery` agrupa mídia reutilizável. `Service` descreve serviços do Maestro Beton e Conecta. `Testimonial` registra depoimentos. `QuoteRequest` modela orçamento. `Project`, `CaseStudy`, `ProfessionalExperience`, `Skill`, `Download` e `Article` atendem Alexandre Dev e blogs futuros. `Manifesto`, `Poem`, `Reflection`, `Excerpt`, `Book`, `Playlist` e `MusicReference` sustentam Loki. `JobPosting`, `CandidateSubmission`, `Lead` e `CompanyProfile` sustentam Conecta Talentos, com atenção especial à LGPD. `KnowledgeCategory`, `KnowledgeTopic`, `KnowledgeArticle`, `ContentSuggestion`, `ContributorApplication` e `BugReport` sustentam WikiDev. `FormDefinition`, `Navigation`, `SEO Metadata` e `Asset` são tipos globais.

## Relacionamentos

```txt
KnowledgeCategory
└── KnowledgeTopic
    └── KnowledgeArticle
        ├── ContentSuggestion
        └── BugReport

Book
├── Manifesto
├── Poem
└── MusicReference

CompanyProfile
├── JobPosting
└── CandidateSubmission
```

## Workflow, SEO, Traduções, Analytics e Busca

Todo Content Type deve suportar o fluxo `Draft -> Review -> Approved -> Published -> Archived`, SEO por locale, revisões imutáveis, permissões por membership, analytics e indexação quando publicado. A API pública entrega somente versões publicadas. APIs futuras GraphQL devem derivar do registry, não de tabelas improvisadas.

## Contratos Canônicos

Project inclui stack, links, repository, screenshots, timeline, challenges e solution. Manifesto inclui body, themes, references e relatedPoems. KnowledgeArticle inclui categoryId, topicId, body, tags, difficulty e relatedArticles. JobPosting inclui company, location, mode, salaryRange, requirements e applicationFormId. Event inclui date range, location, visibility e media.

## Leis dos Content Types

1. Todo Content Type possui `tenantId`, `productId`, slug e status.
2. Todo Content Type suporta revisão.
3. Todo Content Type pode possuir SEO.
4. Todo Content Type pode possuir tradução.
5. Todo Content Type publicado não perde histórico.
6. Content Type não é Page.
7. Page organiza apresentação; Content Type organiza domínio.
8. Todo Content Type sensível define política LGPD.
9. Todo Content Type indexável possui search document.
10. Todo Content Type possui contrato público estável.
11. Relações entre Content Types devem ser explícitas.
12. Workflow editorial é obrigatório para publicação.
13. Frontends renderizam contratos, não entidades internas.
14. GraphQL futuro deve respeitar o catálogo.
15. Nenhum novo domínio nasce fora do registry.

# Catálogo Global de Form Types

## Objetivo

Formulários no Aegis são entidades arquiteturais, não apenas HTML. `Form Type` é o modelo reutilizável; `Form` é a instância configurada; `Form Submission` é a resposta enviada; `Form Workflow` é o processamento posterior. Cada formulário possui schema, validações, workflow, integrações, analytics, LGPD, notificações, auditoria e permissões.

```txt
Product
↓
Form Type
↓
Form Definition
↓
Submission
↓
Notifications / Integrations / Analytics
```

## Estrutura e Contrato

```txt
FormType
├── Schema
├── Fields
├── Validation
├── Workflow
├── Notifications
├── Integrations
├── Analytics
├── LGPD
└── Permissions
```

```json
{
  "id": "form_quote_request",
  "tenantId": "tenant_maestro_beton",
  "productId": "product_maestro_beton",
  "type": "QUOTE_REQUEST",
  "name": "Solicitação de Orçamento",
  "status": "ACTIVE",
  "fields": [],
  "workflow": {},
  "notifications": {},
  "integrations": {}
}
```

## Catálogo de Form Types

`Contact Form` atende CMSS, Maestro Beton, Alexandre Dev, Loki e WikiDev com nome, email, telefone opcional e mensagem. `Quote Request Form` atende Maestro Beton com nome, email, telefone, tipo de evento, cidade, data e observações, integrando Telegram e email. `Corporate Lead Form`, `Job Application Form` e `Talent Pool Form` atendem Conecta Talentos e exigem consentimento, upload protegido e retenção. `Contributor Application Form`, `Bug Report Form` e `Content Suggestion Form` atendem WikiDev com workflow e Telegram. `Newsletter Subscription`, `Donation Form`, `Book Interest Form` e `Recruiter Contact Form` preparam usos futuros.

## Campos, Validação e Workflow

Tipos oficiais: `TEXT`, `TEXTAREA`, `EMAIL`, `PHONE`, `NUMBER`, `DATE`, `TIME`, `CHECKBOX`, `RADIO`, `SELECT`, `MULTISELECT`, `FILE`, `URL`, `HIDDEN`, `MARKDOWN`. Validações incluem obrigatório, tamanho mínimo, tamanho máximo, regex, tipo, mime type e tamanho de upload.

```txt
Submission
↓
Validation
↓
Persist
↓
Notification
↓
Analytics
↓
Success
```

Integrações não podem bloquear persistência. Se Telegram falhar, a submissão continua gravada e a falha gera retry e auditoria.

## Permissões, Banco e Contratos

Quem pode criar, editar, publicar, visualizar submissões e exportar dados depende de memberships. Submissões sensíveis exigem mascaramento e autorização granular. Tabelas: `form_types`, `forms`, `form_fields`, `form_validations`, `form_submissions`, `form_submission_files`, `form_integrations`, `form_notifications`.

## Leis dos Formulários

1. Todo formulário possui schema.
2. Toda submissão pertence a tenant e produto.
3. Toda submissão sensível possui política LGPD.
4. Todo upload deve ser validado.
5. Integrações não bloqueiam persistência.
6. Todo formulário pode possuir analytics.
7. Todo formulário possui auditoria.
8. Form Type é reutilizável.
9. Form Submission não altera o schema.
10. Mudanças de schema devem ser versionadas.
11. Exportação de submissões é operação sensível.
12. Consentimento deve ser explícito quando houver dado pessoal.
13. Frontend renderiza o contrato do formulário.
14. Backend valida independentemente do frontend.
15. Formulários pertencem ao conteúdo, não ao layout hardcoded.

# Catálogo Global de Assets

## Objetivo

Asset não é arquivo. Arquivo é binário armazenado. Asset é recurso digital gerenciado com identidade, metadados, permissões, ownership, SEO, analytics, auditoria, ciclo de vida, storage e versões. O Asset Manager permite reutilizar mídia em páginas, blocks, content types, formulários, SEO e downloads sem duplicação física.

## Lifecycle e Estrutura

```txt
Upload -> Processing -> Available -> Published -> Archived -> Deleted
```

```txt
Asset
├── Metadata
├── Storage
├── Ownership
├── Permissions
├── Relations
├── Analytics
├── SEO
├── Audit
└── Versions
```

```json
{
  "id": "asset_001",
  "tenantId": "tenant_byop",
  "productId": "product_loki",
  "type": "IMAGE",
  "status": "AVAILABLE",
  "filename": "cover-book.png",
  "mimeType": "image/png",
  "size": 1048576,
  "metadata": {"alt": "Capa do livro", "caption": "Edição inicial"},
  "storage": {"provider": "S3", "bucket": "aegis-assets", "path": "/loki/books/cover-book.png"},
  "createdAt": "2026-01-01T10:00:00Z",
  "updatedAt": "2026-01-01T10:00:00Z"
}
```

## Catálogo

`Image Asset` exige width, height, alt, caption, credit e focalPoint. `Video Asset` inclui duration, resolution, thumbnail e provider. `Audio Asset` atende Loki e Maestro Beton. `Document Asset`, `PDF Asset`, `Download Asset`, `Resume Asset`, `Book Asset`, `Cover Asset`, `Gallery Asset` e `Attachment Asset` possuem regras próprias. Currículos e documentos pessoais são sensíveis, privados por padrão, com retenção e exclusão controladas.

## Storage, Metadados e Relações

Storage Provider deve ser abstrato: local, S3, MinIO, Cloudflare R2, Azure Blob futuro. Metadados comuns convivem com metadados específicos por tipo. SEO de assets inclui alt, title, caption, OpenGraph e social previews.

```txt
Asset
├── HeroBlock
├── Project
├── Book
├── KnowledgeArticle
└── FormSubmission
```

## Analytics, Auditoria e Banco

Eventos: `asset_view`, `asset_download`, `asset_open`, `asset_share`. Auditoria: upload, update, publish, archive, delete, restore. Tabelas: `assets`, `asset_versions`, `asset_relations`, `asset_metadata`, `asset_downloads`, `asset_audit_logs`.

## Casos BYOP

CMSS usa imagens, galerias e PDFs. Maestro Beton usa fotos, vídeos e material promocional. Alexandre Dev usa screenshots, currículo e downloads. Loki usa capas, ebooks, imagens e áudios. WikiDev usa anexos e imagens técnicas. Conecta usa currículos e documentos sensíveis.

## Leis dos Assets

1. Todo asset pertence a tenant e produto.
2. Asset não é sinônimo de arquivo.
3. Upload não significa publicação.
4. Nenhum asset sensível é público por padrão.
5. Todo image asset publicado possui alt.
6. Todo asset possui ownership.
7. Todo asset pode se relacionar a múltiplos recursos.
8. Exclusão física não ocorre por padrão.
9. Storage provider é detalhe de infraestrutura.
10. Metadados são parte do domínio.
11. Download sensível é auditado.
12. Versionamento de asset não apaga histórico.
13. Asset público pode ter cache; asset privado exige autorização.
14. Assets respeitam tenant, produto e role.
15. Assets podem ter analytics próprio.

# Sistema Global de Traduções e Internacionalização (i18n)

## Especificação

Tradução no Aegis separa idioma da interface administrativa do idioma do conteúdo publicado. O sistema é content language agnostic: nenhum código assume português, inglês ou qualquer idioma. Conceitos: locale, defaultLocale, supportedLocales, TranslationSet e TranslationEntry. A estratégia recomendada é recurso único com traduções associadas, nunca entidades duplicadas como PagePT/PageEN. Produtos definem defaultLocale e supportedLocales. Páginas, content types, blocks, navegação, formulários, SEO e assets possuem traduções independentes, com fallback configurável, URLs recomendadas por locale como /pt-br/sobre e /en-us/about, hreflang, canonical e sitemap multilíngue. Workflow e revisões são independentes por locale. Tabelas: translations, translation_sets, translation_entries, translation_audit_logs. GraphQL futuro recebe locale como argumento. Leis: todo produto possui locale padrão; tradução não substitui recurso original; SEO, busca, analytics e workflow respeitam locale; traduções possuem auditoria; publicação pode ocorrer por idioma.

## Contrato e Modelo Operacional

```json
{
  "id": "example_001",
  "tenantId": "tenant_byop",
  "productId": "product_aegis",
  "type": "SISTEMA_GLOBAL_DE_TRADUÇÕES_E_INTERNACIO",
  "status": "ACTIVE",
  "metadata": {},
  "createdAt": "2026-01-01T10:00:00Z",
  "updatedAt": "2026-01-01T10:00:00Z"
}
```

## Diagrama Conceitual

```txt
Tenant
└── Product
    └── Sistema Global de Traduções e Internacionalização (i18n)
        ├── Contract
        ├── Permissions
        ├── Audit
        └── Lifecycle
```

## Regras Obrigatórias

1. Deve respeitar tenant, produto, membership e role.
2. Deve possuir contrato JSON canônico antes de ser consumido por frontend.
3. Deve gerar auditoria em operações administrativas relevantes.
4. Deve respeitar LGPD quando manipular dados pessoais.
5. Deve preservar compatibilidade com REST do MVP e GraphQL futuro.
6. Deve separar domínio de provider, storage, canal ou tecnologia específica.
7. Deve ser cacheável somente quando segurança e contexto permitirem.
8. Deve ser observável por correlationId, logs e métricas.
9. Deve possuir estratégia de evolução sem quebra de contrato.
10. Deve ser documentado por ADR quando alterar decisão arquitetural.

# Sistema Global de Revisões, Versionamento e Histórico

## Especificação

Revisão registra o estado de um recurso em um momento. Publicação aponta para uma revisão; auditoria registra quem executou ações. A fonte de verdade editorial é o histórico e o conteúdo atual é projeção da última revisão válida. O MVP usa snapshots completos em JSON, com diff futuro opcional. Entidades versionáveis: pages, sections, blocks, articles, manifestos, poems, reflections, books, job postings, projects, services, forms, navigation e SEO metadata. Analytics events e logs temporários não precisam de revisão. Contrato: revisionId, resourceId, resourceType, version, status, snapshot, createdBy, createdAt. Fluxo: Draft, Review, Approved, Published, Archived. Rollback nunca apaga histórico; cria ou aponta uma publicação anterior. APIs admin: /revisions, /revisions/{id}/publish, /rollback, /compare. Leis: nenhum conteúdo publicado é perdido; revisão publicada é imutável; API pública nunca expõe drafts; edição e publicação são conceitos distintos.

## Contrato e Modelo Operacional

```json
{
  "id": "example_001",
  "tenantId": "tenant_byop",
  "productId": "product_aegis",
  "type": "SISTEMA_GLOBAL_DE_REVISÕES",
  "status": "ACTIVE",
  "metadata": {},
  "createdAt": "2026-01-01T10:00:00Z",
  "updatedAt": "2026-01-01T10:00:00Z"
}
```

## Diagrama Conceitual

```txt
Tenant
└── Product
    └── Sistema Global de Revisões, Versionamento e Histórico
        ├── Contract
        ├── Permissions
        ├── Audit
        └── Lifecycle
```

## Regras Obrigatórias

1. Deve respeitar tenant, produto, membership e role.
2. Deve possuir contrato JSON canônico antes de ser consumido por frontend.
3. Deve gerar auditoria em operações administrativas relevantes.
4. Deve respeitar LGPD quando manipular dados pessoais.
5. Deve preservar compatibilidade com REST do MVP e GraphQL futuro.
6. Deve separar domínio de provider, storage, canal ou tecnologia específica.
7. Deve ser cacheável somente quando segurança e contexto permitirem.
8. Deve ser observável por correlationId, logs e métricas.
9. Deve possuir estratégia de evolução sem quebra de contrato.
10. Deve ser documentado por ADR quando alterar decisão arquitetural.

# Sistema Global de Auditoria, Rastreabilidade e Governança

## Especificação

Auditoria responde quem fez, quando fez, de onde fez, em qual contexto e qual recurso foi afetado. Revisão responde o que mudou. Audit logs não são restauráveis; são trilha de governança, segurança, LGPD, suporte e investigação. Conceitos: AuditEvent, Actor, Resource, Action, Context, Metadata, CorrelationId, TraceId, Session e Origin. Deve auditar login, falhas, tenants, produtos, features, convites, memberships, roles, páginas, blocks, forms, assets, SEO, traduções, revisões, publicações, rollbacks, integrações e configurações. Contrato: eventId, timestamp, actorId, tenantId, productId, resourceType, resourceId, action, result, metadata. Actions: CREATE, UPDATE, DELETE, ARCHIVE, RESTORE, PUBLISH, LOGIN, INVITE, ASSIGN_ROLE, UPLOAD, DOWNLOAD, EXPORT. Tabelas: audit_logs, audit_metadata, audit_exports, audit_retention_policies. Leis: nenhuma ação relevante acontece sem auditoria; logs persistidos não são alterados; toda exportação e mudança de role é auditada.

## Contrato e Modelo Operacional

```json
{
  "id": "example_001",
  "tenantId": "tenant_byop",
  "productId": "product_aegis",
  "type": "SISTEMA_GLOBAL_DE_AUDITORIA",
  "status": "ACTIVE",
  "metadata": {},
  "createdAt": "2026-01-01T10:00:00Z",
  "updatedAt": "2026-01-01T10:00:00Z"
}
```

## Diagrama Conceitual

```txt
Tenant
└── Product
    └── Sistema Global de Auditoria, Rastreabilidade e Governança
        ├── Contract
        ├── Permissions
        ├── Audit
        └── Lifecycle
```

## Regras Obrigatórias

1. Deve respeitar tenant, produto, membership e role.
2. Deve possuir contrato JSON canônico antes de ser consumido por frontend.
3. Deve gerar auditoria em operações administrativas relevantes.
4. Deve respeitar LGPD quando manipular dados pessoais.
5. Deve preservar compatibilidade com REST do MVP e GraphQL futuro.
6. Deve separar domínio de provider, storage, canal ou tecnologia específica.
7. Deve ser cacheável somente quando segurança e contexto permitirem.
8. Deve ser observável por correlationId, logs e métricas.
9. Deve possuir estratégia de evolução sem quebra de contrato.
10. Deve ser documentado por ADR quando alterar decisão arquitetural.

# Sistema Global de Comentários, Discussões e Colaboração

## Especificação

Comentário é reação a conteúdo existente; resposta é comentário subordinado; thread agrupa conversa; discussão amplia o tema; fórum é comunidade estruturada. O MVP nasce com comentários e um nível de resposta, preparado para evoluir para threads, discussões, comunidades e fóruns. Comentários são desacoplados do conteúdo por Resource Reference. Recursos comentáveis: Article, KnowledgeArticle, Manifesto, Reflection, Poem, Project e JobPosting opcional. Status: PENDING, APPROVED, REJECTED, PUBLISHED, ARCHIVED, DELETED. Contratos incluem Comment, Reply, CommentReport e CommentModeration. Moderação pode ser pré, pós ou inexistente por produto. Comentários autenticados integram Keycloak, memberships e tenants; anônimos exigem mitigação de spam. Analytics: comment_view, comment_create, comment_reply, comment_report. Tabelas: comments, comment_replies, comment_reports, comment_moderation, comment_audit_logs. Leis: comentário pertence a recurso, tenant e produto; denúncias não removem automaticamente; comentários não substituem workflow editorial.

## Contrato e Modelo Operacional

```json
{
  "id": "example_001",
  "tenantId": "tenant_byop",
  "productId": "product_aegis",
  "type": "SISTEMA_GLOBAL_DE_COMENTÁRIOS",
  "status": "ACTIVE",
  "metadata": {},
  "createdAt": "2026-01-01T10:00:00Z",
  "updatedAt": "2026-01-01T10:00:00Z"
}
```

## Diagrama Conceitual

```txt
Tenant
└── Product
    └── Sistema Global de Comentários, Discussões e Colaboração
        ├── Contract
        ├── Permissions
        ├── Audit
        └── Lifecycle
```

## Regras Obrigatórias

1. Deve respeitar tenant, produto, membership e role.
2. Deve possuir contrato JSON canônico antes de ser consumido por frontend.
3. Deve gerar auditoria em operações administrativas relevantes.
4. Deve respeitar LGPD quando manipular dados pessoais.
5. Deve preservar compatibilidade com REST do MVP e GraphQL futuro.
6. Deve separar domínio de provider, storage, canal ou tecnologia específica.
7. Deve ser cacheável somente quando segurança e contexto permitirem.
8. Deve ser observável por correlationId, logs e métricas.
9. Deve possuir estratégia de evolução sem quebra de contrato.
10. Deve ser documentado por ADR quando alterar decisão arquitetural.

# Sistema Global de Sugestões, Ideias e Evolução Colaborativa

## Especificação

Sugestão é proposta de melhoria ou criação; comentário é reação; bug report relata defeito; tarefa é item operacional; feature request é sugestão de capacidade. Sugestões capturam ideias antes que se percam e podem apontar para artigo, projeto, página, vaga, manifesto, produto ou existir sem referência. Categorias: CONTENT, FEATURE, CORRECTION, UX, SEO, PERFORMANCE, DOCUMENTATION, INTEGRATION, OTHER. Status: NEW, TRIAGED, ACCEPTED, PLANNED, IN_PROGRESS, REJECTED, DONE, ARCHIVED. Entidade: id, tenantId, productId, title, description, category, status, authorId, resourceRef, votes, review, resolution. WikiDev usa para novos artigos e correções; Loki para temas; Alexandre Dev para conteúdos técnicos; Conecta para melhorias operacionais; Aegis para evolução do CMS. Integra comentários, notificações, auditoria, analytics e jobs. Leis: toda sugestão possui status; sugestão aceita não vira conteúdo automaticamente; resolução deve ser registrada; votos não substituem decisão editorial.

## Contrato e Modelo Operacional

```json
{
  "id": "example_001",
  "tenantId": "tenant_byop",
  "productId": "product_aegis",
  "type": "SISTEMA_GLOBAL_DE_SUGESTÕES",
  "status": "ACTIVE",
  "metadata": {},
  "createdAt": "2026-01-01T10:00:00Z",
  "updatedAt": "2026-01-01T10:00:00Z"
}
```

## Diagrama Conceitual

```txt
Tenant
└── Product
    └── Sistema Global de Sugestões, Ideias e Evolução Colaborativa
        ├── Contract
        ├── Permissions
        ├── Audit
        └── Lifecycle
```

## Regras Obrigatórias

1. Deve respeitar tenant, produto, membership e role.
2. Deve possuir contrato JSON canônico antes de ser consumido por frontend.
3. Deve gerar auditoria em operações administrativas relevantes.
4. Deve respeitar LGPD quando manipular dados pessoais.
5. Deve preservar compatibilidade com REST do MVP e GraphQL futuro.
6. Deve separar domínio de provider, storage, canal ou tecnologia específica.
7. Deve ser cacheável somente quando segurança e contexto permitirem.
8. Deve ser observável por correlationId, logs e métricas.
9. Deve possuir estratégia de evolução sem quebra de contrato.
10. Deve ser documentado por ADR quando alterar decisão arquitetural.

# Sistema Global de Notificações, Alertas e Comunicação

## Especificação

Notificação é comunicação controlada por evento de domínio. Telegram, email, webhook, push futuro e in-app são canais, não o domínio. Eventos incluem convites, submissões, bug reports, sugestões, comentários, publicações, falhas de integração, exportações e alertas de segurança. Modelo: Event -> Notification Rule -> Channel -> Delivery -> Retry -> Audit. Contrato: notificationId, tenantId, productId, eventType, channel, recipient, payload, status, attempts, createdAt. Telegram atende orçamento, bug report, candidatura, contato e sugestão, mas não deve ser acoplado ao Form Type. Tabelas: notification_rules, notification_deliveries, notification_templates, notification_channels, notification_audit_logs. Leis: notificação não é autorização; falha de notificação não apaga evento; delivery possui retry; payload sensível deve ser minimizado; todo canal é substituível.

## Contrato e Modelo Operacional

```json
{
  "id": "example_001",
  "tenantId": "tenant_byop",
  "productId": "product_aegis",
  "type": "SISTEMA_GLOBAL_DE_NOTIFICAÇÕES",
  "status": "ACTIVE",
  "metadata": {},
  "createdAt": "2026-01-01T10:00:00Z",
  "updatedAt": "2026-01-01T10:00:00Z"
}
```

## Diagrama Conceitual

```txt
Tenant
└── Product
    └── Sistema Global de Notificações, Alertas e Comunicação
        ├── Contract
        ├── Permissions
        ├── Audit
        └── Lifecycle
```

## Regras Obrigatórias

1. Deve respeitar tenant, produto, membership e role.
2. Deve possuir contrato JSON canônico antes de ser consumido por frontend.
3. Deve gerar auditoria em operações administrativas relevantes.
4. Deve respeitar LGPD quando manipular dados pessoais.
5. Deve preservar compatibilidade com REST do MVP e GraphQL futuro.
6. Deve separar domínio de provider, storage, canal ou tecnologia específica.
7. Deve ser cacheável somente quando segurança e contexto permitirem.
8. Deve ser observável por correlationId, logs e métricas.
9. Deve possuir estratégia de evolução sem quebra de contrato.
10. Deve ser documentado por ADR quando alterar decisão arquitetural.

# Sistema Global de Busca, Descoberta e Recuperação de Informação

## Especificação

Busca não é SQL LIKE. Filtro restringe conjuntos conhecidos; busca recupera informação por relevância; navegação orienta caminhos; descoberta sugere relações; recomendação decide por sinais. O Aegis nasce com Search Service e Search Provider. MVP: PostgreSQL Full Text Search. Futuro: Meilisearch, OpenSearch, Elasticsearch e busca semântica sem quebrar domínio. Indexáveis: Page, Article, KnowledgeArticle, Manifesto, Reflection, Poem, Book, Project, JobPosting, Service, Event, Suggestion, Comment opcional e Asset Metadata. SearchDocument contém resourceType, resourceId, title, content, locale, tags, status, tenantId, productId. Busca respeita tenant, produto, locale, role e status. Analytics: search_query, search_click, search_result_open, search_empty_result. Tabelas: search_indexes, search_documents, search_queries, search_clicks, search_statistics. Leis: busca é desacoplada do provider; resultados não expõem conteúdo privado; busca administrativa é auditável.

## Contrato e Modelo Operacional

```json
{
  "id": "example_001",
  "tenantId": "tenant_byop",
  "productId": "product_aegis",
  "type": "SISTEMA_GLOBAL_DE_BUSCA",
  "status": "ACTIVE",
  "metadata": {},
  "createdAt": "2026-01-01T10:00:00Z",
  "updatedAt": "2026-01-01T10:00:00Z"
}
```

## Diagrama Conceitual

```txt
Tenant
└── Product
    └── Sistema Global de Busca, Descoberta e Recuperação de Informação
        ├── Contract
        ├── Permissions
        ├── Audit
        └── Lifecycle
```

## Regras Obrigatórias

1. Deve respeitar tenant, produto, membership e role.
2. Deve possuir contrato JSON canônico antes de ser consumido por frontend.
3. Deve gerar auditoria em operações administrativas relevantes.
4. Deve respeitar LGPD quando manipular dados pessoais.
5. Deve preservar compatibilidade com REST do MVP e GraphQL futuro.
6. Deve separar domínio de provider, storage, canal ou tecnologia específica.
7. Deve ser cacheável somente quando segurança e contexto permitirem.
8. Deve ser observável por correlationId, logs e métricas.
9. Deve possuir estratégia de evolução sem quebra de contrato.
10. Deve ser documentado por ADR quando alterar decisão arquitetural.

# Sistema Global de Navegação, Estrutura da Informação e Descoberta

## Especificação

Navegação é conteúdo. Frontends não possuem menus hardcoded; consomem contratos do Aegis. Navegação difere de menu, link, estrutura da informação, descoberta e busca. Tipos: HEADER, SIDEBAR, FOOTER, MOBILE, CONTEXTUAL, RELATED. Cada produto possui navigation tree própria, isolada por tenant e traduzível por locale. Contratos: Navigation com id, productId, type e items; NavigationItem com id, label, slug, target e children. Menus podem ser estáticos ou gerados por páginas, categorias, artigos e content types. Breadcrumbs devem ser geráveis: Home -> Wiki -> Backend -> Spring Security. Eventos: navigation_click, menu_open, menu_close, breadcrumb_click, related_content_click. Tabelas: navigations, navigation_items, navigation_translations, navigation_versions, navigation_audit_logs. Leis: navegação pertence ao produto; menus possuem revisão e auditoria; breadcrumbs são parte do contrato; navegação melhora SEO e descoberta.

## Contrato e Modelo Operacional

```json
{
  "id": "example_001",
  "tenantId": "tenant_byop",
  "productId": "product_aegis",
  "type": "SISTEMA_GLOBAL_DE_NAVEGAÇÃO",
  "status": "ACTIVE",
  "metadata": {},
  "createdAt": "2026-01-01T10:00:00Z",
  "updatedAt": "2026-01-01T10:00:00Z"
}
```

## Diagrama Conceitual

```txt
Tenant
└── Product
    └── Sistema Global de Navegação, Estrutura da Informação e Descoberta
        ├── Contract
        ├── Permissions
        ├── Audit
        └── Lifecycle
```

## Regras Obrigatórias

1. Deve respeitar tenant, produto, membership e role.
2. Deve possuir contrato JSON canônico antes de ser consumido por frontend.
3. Deve gerar auditoria em operações administrativas relevantes.
4. Deve respeitar LGPD quando manipular dados pessoais.
5. Deve preservar compatibilidade com REST do MVP e GraphQL futuro.
6. Deve separar domínio de provider, storage, canal ou tecnologia específica.
7. Deve ser cacheável somente quando segurança e contexto permitirem.
8. Deve ser observável por correlationId, logs e métricas.
9. Deve possuir estratégia de evolução sem quebra de contrato.
10. Deve ser documentado por ADR quando alterar decisão arquitetural.

# Sistema Global de Contratos JSON Canônicos e Interoperabilidade

## Especificação

Contrato é o acordo estável entre domínio e consumidores. Entidade é persistência; DTO é transporte interno; payload é instância; schema descreve forma; contrato define semântica, versionamento, compatibilidade e obrigações. Filosofia: Domínio -> Contrato -> Frontend, nunca Banco -> Frontend. Todo contrato possui id, type, version, status, metadata, timestamps e não expõe detalhes internos. Metadata inclui locale, tenantId e productId. Devem existir contratos canônicos para Page, Section, Block, Navigation, Content Types, Form, Asset, Comment, Suggestion, Notification, Search Result, Translation, Revision, Audit e Analytics. Erros, paginação e coleções possuem padrões únicos. APIs públicas incluem /public/products/{slug}/contract, /pages/{slug}, /navigation e /content. Compatibilidade retroativa é obrigatória: adicionar campos opcionais é permitido; remover ou renomear exige major version. Leis: frontend consome contrato; contrato é versionado; schemas vivem em registry; evolução nunca quebra consumidores sem migração.

## Contrato e Modelo Operacional

```json
{
  "id": "example_001",
  "tenantId": "tenant_byop",
  "productId": "product_aegis",
  "type": "SISTEMA_GLOBAL_DE_CONTRATOS_JSON_CANÔNIC",
  "status": "ACTIVE",
  "metadata": {},
  "createdAt": "2026-01-01T10:00:00Z",
  "updatedAt": "2026-01-01T10:00:00Z"
}
```

## Diagrama Conceitual

```txt
Tenant
└── Product
    └── Sistema Global de Contratos JSON Canônicos e Interoperabilidade
        ├── Contract
        ├── Permissions
        ├── Audit
        └── Lifecycle
```

## Regras Obrigatórias

1. Deve respeitar tenant, produto, membership e role.
2. Deve possuir contrato JSON canônico antes de ser consumido por frontend.
3. Deve gerar auditoria em operações administrativas relevantes.
4. Deve respeitar LGPD quando manipular dados pessoais.
5. Deve preservar compatibilidade com REST do MVP e GraphQL futuro.
6. Deve separar domínio de provider, storage, canal ou tecnologia específica.
7. Deve ser cacheável somente quando segurança e contexto permitirem.
8. Deve ser observável por correlationId, logs e métricas.
9. Deve possuir estratégia de evolução sem quebra de contrato.
10. Deve ser documentado por ADR quando alterar decisão arquitetural.

# Estratégia GraphQL, Query Federation e APIs Futuras

## Especificação

GraphQL não é objetivo do MVP, mas a arquitetura deve nascer compatível. REST atende operações oficiais iniciais; GraphQL futuro atende composição, seleção de campos, queries agregadas e experiências ricas. A estratégia exige schema derivado dos contratos canônicos, resolvers com validação de tenant, produto, membership e role, e proibição de expor entidades JPA diretamente. Queries futuras: product(slug), page(slug, locale), content(type, filters), search(query), navigation(type). Mutations administrativas devem respeitar workflow, auditoria e revisões. Federation futura pode conectar Aegis, analytics, billing e integrações. Leis: GraphQL não substitui autorização; resolvers são backend; N+1 deve ser controlado; schema evolui por versionamento; REST e GraphQL compartilham domínio.

## Contrato e Modelo Operacional

```json
{
  "id": "example_001",
  "tenantId": "tenant_byop",
  "productId": "product_aegis",
  "type": "ESTRATÉGIA_GRAPHQL",
  "status": "ACTIVE",
  "metadata": {},
  "createdAt": "2026-01-01T10:00:00Z",
  "updatedAt": "2026-01-01T10:00:00Z"
}
```

## Diagrama Conceitual

```txt
Tenant
└── Product
    └── Estratégia GraphQL, Query Federation e APIs Futuras
        ├── Contract
        ├── Permissions
        ├── Audit
        └── Lifecycle
```

## Regras Obrigatórias

1. Deve respeitar tenant, produto, membership e role.
2. Deve possuir contrato JSON canônico antes de ser consumido por frontend.
3. Deve gerar auditoria em operações administrativas relevantes.
4. Deve respeitar LGPD quando manipular dados pessoais.
5. Deve preservar compatibilidade com REST do MVP e GraphQL futuro.
6. Deve separar domínio de provider, storage, canal ou tecnologia específica.
7. Deve ser cacheável somente quando segurança e contexto permitirem.
8. Deve ser observável por correlationId, logs e métricas.
9. Deve possuir estratégia de evolução sem quebra de contrato.
10. Deve ser documentado por ADR quando alterar decisão arquitetural.

# Estrutura REST Oficial do MVP e Estratégia de APIs

## Especificação

REST é a superfície oficial do MVP. APIs públicas entregam contratos publicados, cacheáveis e sem drafts. APIs administrativas operam tenants, produtos, content types, assets, forms, revisões, auditoria, memberships e configurações. Padrões: /api/admin/{resource}, /api/public/products/{slug}/{resource}, versionamento por /v1, erros canônicos, paginação, filtros, ordenação e correlationId. Métodos HTTP devem ser semânticos. Toda rota administrativa valida token, tenant context, membership, role e product ownership. Endpoints sensíveis geram audit log. Leis: API pública não expõe interno; API admin não confia no frontend; todo erro segue contrato; toda resposta sensível respeita tenant.

## Contrato e Modelo Operacional

```json
{
  "id": "example_001",
  "tenantId": "tenant_byop",
  "productId": "product_aegis",
  "type": "ESTRUTURA_REST_OFICIAL_DO_MVP_E_ESTRATÉG",
  "status": "ACTIVE",
  "metadata": {},
  "createdAt": "2026-01-01T10:00:00Z",
  "updatedAt": "2026-01-01T10:00:00Z"
}
```

## Diagrama Conceitual

```txt
Tenant
└── Product
    └── Estrutura REST Oficial do MVP e Estratégia de APIs
        ├── Contract
        ├── Permissions
        ├── Audit
        └── Lifecycle
```

## Regras Obrigatórias

1. Deve respeitar tenant, produto, membership e role.
2. Deve possuir contrato JSON canônico antes de ser consumido por frontend.
3. Deve gerar auditoria em operações administrativas relevantes.
4. Deve respeitar LGPD quando manipular dados pessoais.
5. Deve preservar compatibilidade com REST do MVP e GraphQL futuro.
6. Deve separar domínio de provider, storage, canal ou tecnologia específica.
7. Deve ser cacheável somente quando segurança e contexto permitirem.
8. Deve ser observável por correlationId, logs e métricas.
9. Deve possuir estratégia de evolução sem quebra de contrato.
10. Deve ser documentado por ADR quando alterar decisão arquitetural.

# Estratégia Global de Migração, Evolução de Dados e Compatibilidade Histórica

## Especificação

Migração é governança da evolução do banco e dos contratos. O Aegis deve usar migrations versionadas, revisáveis e reprodutíveis, preferencialmente com Flyway ou ferramenta equivalente no ecossistema Spring. Mudanças destrutivas exigem plano, backup, janela e compatibilidade. Dados históricos, revisões e auditoria não podem ser quebrados por refactors. Estratégias: expand-and-contract, backfill, feature flags, dual read/write quando necessário, migração por tenant em operações futuras. Contratos evoluem separadamente de tabelas. Leis: nenhuma migration manual fora do histórico; rollback de schema exige plano; migrations são testadas; dados sensíveis respeitam LGPD durante backfill.

## Contrato e Modelo Operacional

```json
{
  "id": "example_001",
  "tenantId": "tenant_byop",
  "productId": "product_aegis",
  "type": "ESTRATÉGIA_GLOBAL_DE_MIGRAÇÃO",
  "status": "ACTIVE",
  "metadata": {},
  "createdAt": "2026-01-01T10:00:00Z",
  "updatedAt": "2026-01-01T10:00:00Z"
}
```

## Diagrama Conceitual

```txt
Tenant
└── Product
    └── Estratégia Global de Migração, Evolução de Dados e Compatibilidade Histórica
        ├── Contract
        ├── Permissions
        ├── Audit
        └── Lifecycle
```

## Regras Obrigatórias

1. Deve respeitar tenant, produto, membership e role.
2. Deve possuir contrato JSON canônico antes de ser consumido por frontend.
3. Deve gerar auditoria em operações administrativas relevantes.
4. Deve respeitar LGPD quando manipular dados pessoais.
5. Deve preservar compatibilidade com REST do MVP e GraphQL futuro.
6. Deve separar domínio de provider, storage, canal ou tecnologia específica.
7. Deve ser cacheável somente quando segurança e contexto permitirem.
8. Deve ser observável por correlationId, logs e métricas.
9. Deve possuir estratégia de evolução sem quebra de contrato.
10. Deve ser documentado por ADR quando alterar decisão arquitetural.

# Estratégia Global de Deploy, Ambientes e Entrega Contínua

## Especificação

Deploy deve suportar desenvolvimento local, staging, produção e ambientes futuros por cliente sem duplicar arquitetura. O pipeline executa build, testes, análise, migrations, publicação de imagem Docker e deploy controlado. Ambientes possuem variáveis próprias, secrets fora do repositório, Keycloak configurado e PostgreSQL versionado. Estratégias: blue/green ou rolling futuro, health checks, readiness, liveness, rollback e changelog. Frontend Eirene e backend Aegis devem ter contratos sincronizados. Leis: produção não usa secrets locais; deploy sem health check não é deploy completo; migration e aplicação são coordenadas; staging valida antes da produção.

## Contrato e Modelo Operacional

```json
{
  "id": "example_001",
  "tenantId": "tenant_byop",
  "productId": "product_aegis",
  "type": "ESTRATÉGIA_GLOBAL_DE_DEPLOY",
  "status": "ACTIVE",
  "metadata": {},
  "createdAt": "2026-01-01T10:00:00Z",
  "updatedAt": "2026-01-01T10:00:00Z"
}
```

## Diagrama Conceitual

```txt
Tenant
└── Product
    └── Estratégia Global de Deploy, Ambientes e Entrega Contínua
        ├── Contract
        ├── Permissions
        ├── Audit
        └── Lifecycle
```

## Regras Obrigatórias

1. Deve respeitar tenant, produto, membership e role.
2. Deve possuir contrato JSON canônico antes de ser consumido por frontend.
3. Deve gerar auditoria em operações administrativas relevantes.
4. Deve respeitar LGPD quando manipular dados pessoais.
5. Deve preservar compatibilidade com REST do MVP e GraphQL futuro.
6. Deve separar domínio de provider, storage, canal ou tecnologia específica.
7. Deve ser cacheável somente quando segurança e contexto permitirem.
8. Deve ser observável por correlationId, logs e métricas.
9. Deve possuir estratégia de evolução sem quebra de contrato.
10. Deve ser documentado por ADR quando alterar decisão arquitetural.

# Estratégia SaaS, Monetização e Escalabilidade Comercial

## Especificação

SaaS futuro usa tenant como unidade comercial. Planos podem definir usuários, produtos, storage, bandwidth, features, formulários, submissions, analytics, integrações e suporte. Billing deve nascer preparado por metadados, limites e usage tracking, sem cobrança ativa no MVP. Mensal, anual, trial, suspensão, upgrade, downgrade e inadimplência futura devem ser modeláveis. Feature gating ocorre por tenant e produto; autorização continua por membership. Leis: billing não substitui permissão; plano limita capacidade, role define ação; limites devem ser auditáveis; suspensão comercial não apaga dados; exportação e retenção seguem LGPD.

## Contrato e Modelo Operacional

```json
{
  "id": "example_001",
  "tenantId": "tenant_byop",
  "productId": "product_aegis",
  "type": "ESTRATÉGIA_SAAS",
  "status": "ACTIVE",
  "metadata": {},
  "createdAt": "2026-01-01T10:00:00Z",
  "updatedAt": "2026-01-01T10:00:00Z"
}
```

## Diagrama Conceitual

```txt
Tenant
└── Product
    └── Estratégia SaaS, Monetização e Escalabilidade Comercial
        ├── Contract
        ├── Permissions
        ├── Audit
        └── Lifecycle
```

## Regras Obrigatórias

1. Deve respeitar tenant, produto, membership e role.
2. Deve possuir contrato JSON canônico antes de ser consumido por frontend.
3. Deve gerar auditoria em operações administrativas relevantes.
4. Deve respeitar LGPD quando manipular dados pessoais.
5. Deve preservar compatibilidade com REST do MVP e GraphQL futuro.
6. Deve separar domínio de provider, storage, canal ou tecnologia específica.
7. Deve ser cacheável somente quando segurança e contexto permitirem.
8. Deve ser observável por correlationId, logs e métricas.
9. Deve possuir estratégia de evolução sem quebra de contrato.
10. Deve ser documentado por ADR quando alterar decisão arquitetural.

# Estratégia LGPD, Privacidade, Governança de Dados e Conformidade

## Especificação

LGPD é requisito de arquitetura. Dados pessoais devem ser minimizados, classificados, protegidos, retidos por política e apagados ou anonimizados quando aplicável. Candidaturas, currículos, leads, contatos, comentários e auditoria exigem tratamento especial. Conceitos: titular, controlador, operador, consentimento, base legal, retenção, anonimização, exportação e direito de esquecimento. Auditoria pode reter metadados necessários com mascaramento. Tabelas sensíveis devem carregar tenantId, purpose, retentionUntil e consentRef quando cabível. Leis: dado pessoal tem finalidade; consentimento é auditável; exclusão lógica precede física; currículos não são públicos; exportações são sensíveis; privacidade é padrão.

## Contrato e Modelo Operacional

```json
{
  "id": "example_001",
  "tenantId": "tenant_byop",
  "productId": "product_aegis",
  "type": "ESTRATÉGIA_LGPD",
  "status": "ACTIVE",
  "metadata": {},
  "createdAt": "2026-01-01T10:00:00Z",
  "updatedAt": "2026-01-01T10:00:00Z"
}
```

## Diagrama Conceitual

```txt
Tenant
└── Product
    └── Estratégia LGPD, Privacidade, Governança de Dados e Conformidade
        ├── Contract
        ├── Permissions
        ├── Audit
        └── Lifecycle
```

## Regras Obrigatórias

1. Deve respeitar tenant, produto, membership e role.
2. Deve possuir contrato JSON canônico antes de ser consumido por frontend.
3. Deve gerar auditoria em operações administrativas relevantes.
4. Deve respeitar LGPD quando manipular dados pessoais.
5. Deve preservar compatibilidade com REST do MVP e GraphQL futuro.
6. Deve separar domínio de provider, storage, canal ou tecnologia específica.
7. Deve ser cacheável somente quando segurança e contexto permitirem.
8. Deve ser observável por correlationId, logs e métricas.
9. Deve possuir estratégia de evolução sem quebra de contrato.
10. Deve ser documentado por ADR quando alterar decisão arquitetural.

# Estratégia Global de Backup, Retenção e Recuperação de Dados

## Especificação

Backup protege continuidade, LGPD e confiança. Deve cobrir PostgreSQL, assets, configurações, contracts, Keycloak exportável, audit logs e metadados de storage. Estratégia: backups automáticos, retenção por ambiente, criptografia, testes de restore, RPO e RTO definidos. Assets exigem política de versão e retenção separada do banco. Retenção diferencia conteúdo, auditoria, analytics, submissões e dados sensíveis. Leis: backup não testado é hipótese; restore deve ser ensaiado; backups são criptografados; retenção respeita LGPD; exclusão de dados deve considerar cópias retidas.

## Contrato e Modelo Operacional

```json
{
  "id": "example_001",
  "tenantId": "tenant_byop",
  "productId": "product_aegis",
  "type": "ESTRATÉGIA_GLOBAL_DE_BACKUP",
  "status": "ACTIVE",
  "metadata": {},
  "createdAt": "2026-01-01T10:00:00Z",
  "updatedAt": "2026-01-01T10:00:00Z"
}
```

## Diagrama Conceitual

```txt
Tenant
└── Product
    └── Estratégia Global de Backup, Retenção e Recuperação de Dados
        ├── Contract
        ├── Permissions
        ├── Audit
        └── Lifecycle
```

## Regras Obrigatórias

1. Deve respeitar tenant, produto, membership e role.
2. Deve possuir contrato JSON canônico antes de ser consumido por frontend.
3. Deve gerar auditoria em operações administrativas relevantes.
4. Deve respeitar LGPD quando manipular dados pessoais.
5. Deve preservar compatibilidade com REST do MVP e GraphQL futuro.
6. Deve separar domínio de provider, storage, canal ou tecnologia específica.
7. Deve ser cacheável somente quando segurança e contexto permitirem.
8. Deve ser observável por correlationId, logs e métricas.
9. Deve possuir estratégia de evolução sem quebra de contrato.
10. Deve ser documentado por ADR quando alterar decisão arquitetural.

# Estratégia Telegram, Comunicação Operacional e Integrações em Tempo Real

## Especificação

Telegram é canal operacional para alertas de orçamento, bug report, candidatura, contato, sugestão e incidentes. Deve ser implementado por Integration Provider desacoplado, com templates, retries, deduplicação e auditoria. Payloads devem minimizar dados pessoais. Falha no Telegram não deve impedir persistência da ação original. Chat IDs, tokens e webhooks são secrets. Integrações futuras seguem o mesmo padrão: email, webhook, Slack, Discord, push. Leis: Telegram não é fila principal; integração não é domínio; toda entrega tem status; dados sensíveis são mascarados; retry possui limite.

## Contrato e Modelo Operacional

```json
{
  "id": "example_001",
  "tenantId": "tenant_byop",
  "productId": "product_aegis",
  "type": "ESTRATÉGIA_TELEGRAM",
  "status": "ACTIVE",
  "metadata": {},
  "createdAt": "2026-01-01T10:00:00Z",
  "updatedAt": "2026-01-01T10:00:00Z"
}
```

## Diagrama Conceitual

```txt
Tenant
└── Product
    └── Estratégia Telegram, Comunicação Operacional e Integrações em Tempo Real
        ├── Contract
        ├── Permissions
        ├── Audit
        └── Lifecycle
```

## Regras Obrigatórias

1. Deve respeitar tenant, produto, membership e role.
2. Deve possuir contrato JSON canônico antes de ser consumido por frontend.
3. Deve gerar auditoria em operações administrativas relevantes.
4. Deve respeitar LGPD quando manipular dados pessoais.
5. Deve preservar compatibilidade com REST do MVP e GraphQL futuro.
6. Deve separar domínio de provider, storage, canal ou tecnologia específica.
7. Deve ser cacheável somente quando segurança e contexto permitirem.
8. Deve ser observável por correlationId, logs e métricas.
9. Deve possuir estratégia de evolução sem quebra de contrato.
10. Deve ser documentado por ADR quando alterar decisão arquitetural.

# Estratégia Global de Cache, Performance e Distribuição de Conteúdo

## Especificação

Cache acelera contratos públicos, navegação, assets, busca e páginas publicadas, mas nunca substitui autorização. Camadas: browser, CDN, API, aplicação e banco. Estratégias: ETag, Cache-Control, stale-while-revalidate, invalidation por publicação, cache key com tenant, product, locale, version e role quando aplicável. Conteúdo privado não entra em cache público. Assets públicos podem usar CDN; assets sensíveis exigem URL assinada ou proxy autorizado. Leis: cache respeita tenant; publicação invalida cache; draft não é cacheado publicamente; performance não viola segurança.

## Contrato e Modelo Operacional

```json
{
  "id": "example_001",
  "tenantId": "tenant_byop",
  "productId": "product_aegis",
  "type": "ESTRATÉGIA_GLOBAL_DE_CACHE",
  "status": "ACTIVE",
  "metadata": {},
  "createdAt": "2026-01-01T10:00:00Z",
  "updatedAt": "2026-01-01T10:00:00Z"
}
```

## Diagrama Conceitual

```txt
Tenant
└── Product
    └── Estratégia Global de Cache, Performance e Distribuição de Conteúdo
        ├── Contract
        ├── Permissions
        ├── Audit
        └── Lifecycle
```

## Regras Obrigatórias

1. Deve respeitar tenant, produto, membership e role.
2. Deve possuir contrato JSON canônico antes de ser consumido por frontend.
3. Deve gerar auditoria em operações administrativas relevantes.
4. Deve respeitar LGPD quando manipular dados pessoais.
5. Deve preservar compatibilidade com REST do MVP e GraphQL futuro.
6. Deve separar domínio de provider, storage, canal ou tecnologia específica.
7. Deve ser cacheável somente quando segurança e contexto permitirem.
8. Deve ser observável por correlationId, logs e métricas.
9. Deve possuir estratégia de evolução sem quebra de contrato.
10. Deve ser documentado por ADR quando alterar decisão arquitetural.

# Estratégia Global de Internacionalização, Localização e Conteúdo Multilíngue

## Especificação

Esta seção consolida i18n em nível de plataforma: tradução de conteúdo, localização de formatos, timezone, moeda futura, pluralização, direção de texto futura e preferências de usuário. Produtos podem ter default locale, locales suportados e estratégia de URL. Admin UI pode estar em idioma diferente do conteúdo. Search, SEO, analytics, navigation, forms e notifications carregam locale. Leis: locale é contexto obrigatório; localização não é apenas tradução; timezone deve ser explícito; fallback é previsível; métricas segmentam idioma.

## Contrato e Modelo Operacional

```json
{
  "id": "example_001",
  "tenantId": "tenant_byop",
  "productId": "product_aegis",
  "type": "ESTRATÉGIA_GLOBAL_DE_INTERNACIONALIZAÇÃO",
  "status": "ACTIVE",
  "metadata": {},
  "createdAt": "2026-01-01T10:00:00Z",
  "updatedAt": "2026-01-01T10:00:00Z"
}
```

## Diagrama Conceitual

```txt
Tenant
└── Product
    └── Estratégia Global de Internacionalização, Localização e Conteúdo Multilíngue
        ├── Contract
        ├── Permissions
        ├── Audit
        └── Lifecycle
```

## Regras Obrigatórias

1. Deve respeitar tenant, produto, membership e role.
2. Deve possuir contrato JSON canônico antes de ser consumido por frontend.
3. Deve gerar auditoria em operações administrativas relevantes.
4. Deve respeitar LGPD quando manipular dados pessoais.
5. Deve preservar compatibilidade com REST do MVP e GraphQL futuro.
6. Deve separar domínio de provider, storage, canal ou tecnologia específica.
7. Deve ser cacheável somente quando segurança e contexto permitirem.
8. Deve ser observável por correlationId, logs e métricas.
9. Deve possuir estratégia de evolução sem quebra de contrato.
10. Deve ser documentado por ADR quando alterar decisão arquitetural.

# Casos de Borda, Cenários Extremos e Estratégias de Resiliência

## Especificação

Casos de borda devem ser tratados como parte do produto: tenant sem produtos, produto arquivado com conteúdo publicado, usuário removido durante sessão ativa, convite expirado, asset órfão, tradução sem default, formulário alterado durante submissão, rollback com cache ativo, busca desatualizada, integração fora do ar, billing suspenso e restore parcial. Estratégias: validação transacional, idempotência, locks otimistas, retries, filas futuras, fallback, auditoria e mensagens de erro seguras. Leis: caso raro não pode vazar dado; erro parcial é auditável; operação sensível deve ser idempotente quando possível.

## Contrato e Modelo Operacional

```json
{
  "id": "example_001",
  "tenantId": "tenant_byop",
  "productId": "product_aegis",
  "type": "CASOS_DE_BORDA",
  "status": "ACTIVE",
  "metadata": {},
  "createdAt": "2026-01-01T10:00:00Z",
  "updatedAt": "2026-01-01T10:00:00Z"
}
```

## Diagrama Conceitual

```txt
Tenant
└── Product
    └── Casos de Borda, Cenários Extremos e Estratégias de Resiliência
        ├── Contract
        ├── Permissions
        ├── Audit
        └── Lifecycle
```

## Regras Obrigatórias

1. Deve respeitar tenant, produto, membership e role.
2. Deve possuir contrato JSON canônico antes de ser consumido por frontend.
3. Deve gerar auditoria em operações administrativas relevantes.
4. Deve respeitar LGPD quando manipular dados pessoais.
5. Deve preservar compatibilidade com REST do MVP e GraphQL futuro.
6. Deve separar domínio de provider, storage, canal ou tecnologia específica.
7. Deve ser cacheável somente quando segurança e contexto permitirem.
8. Deve ser observável por correlationId, logs e métricas.
9. Deve possuir estratégia de evolução sem quebra de contrato.
10. Deve ser documentado por ADR quando alterar decisão arquitetural.

# Modelo de Segurança, Ameaças, Vetores de Ataque e Estratégias de Proteção

## Especificação

Segurança cobre autenticação, autorização, isolamento multi-tenant, uploads, APIs, integrações, secrets, auditoria e resposta a incidentes. Ameaças: tenant spoofing, horizontal privilege escalation, XSS, CSRF quando aplicável, SSRF em integrações, upload malicioso, enumeração de recursos, brute force, token replay, vazamento por cache e exposição de erro. Mitigações: validação server-side, RBAC/ABAC contextual, rate limiting, scanning de arquivos, headers seguros, secrets manager, logs sem dados sensíveis e testes de segurança. Leis: backend é autoridade; frontend não protege sozinho; deny by default; least privilege; nenhum upload é confiável.

## Contrato e Modelo Operacional

```json
{
  "id": "example_001",
  "tenantId": "tenant_byop",
  "productId": "product_aegis",
  "type": "MODELO_DE_SEGURANÇA",
  "status": "ACTIVE",
  "metadata": {},
  "createdAt": "2026-01-01T10:00:00Z",
  "updatedAt": "2026-01-01T10:00:00Z"
}
```

## Diagrama Conceitual

```txt
Tenant
└── Product
    └── Modelo de Segurança, Ameaças, Vetores de Ataque e Estratégias de Proteção
        ├── Contract
        ├── Permissions
        ├── Audit
        └── Lifecycle
```

## Regras Obrigatórias

1. Deve respeitar tenant, produto, membership e role.
2. Deve possuir contrato JSON canônico antes de ser consumido por frontend.
3. Deve gerar auditoria em operações administrativas relevantes.
4. Deve respeitar LGPD quando manipular dados pessoais.
5. Deve preservar compatibilidade com REST do MVP e GraphQL futuro.
6. Deve separar domínio de provider, storage, canal ou tecnologia específica.
7. Deve ser cacheável somente quando segurança e contexto permitirem.
8. Deve ser observável por correlationId, logs e métricas.
9. Deve possuir estratégia de evolução sem quebra de contrato.
10. Deve ser documentado por ADR quando alterar decisão arquitetural.

# Casos de Escalabilidade, Crescimento da Plataforma e Estratégias Evolutivas

## Especificação

Escalabilidade começa com shared database/schema e evolui por índices, particionamento futuro, read replicas, filas, cache, CDN, search provider dedicado, storage externo e isolamento premium por tenant quando houver justificativa comercial. Gargalos previstos: analytics, busca, assets, submissions, audit logs e public API. A arquitetura deve permitir extrair módulos sem reescrever contratos. Leis: escalar não deve quebrar contrato; gargalos devem ser medidos; provider é substituível; tenantId viabiliza particionamento futuro.

## Contrato e Modelo Operacional

```json
{
  "id": "example_001",
  "tenantId": "tenant_byop",
  "productId": "product_aegis",
  "type": "CASOS_DE_ESCALABILIDADE",
  "status": "ACTIVE",
  "metadata": {},
  "createdAt": "2026-01-01T10:00:00Z",
  "updatedAt": "2026-01-01T10:00:00Z"
}
```

## Diagrama Conceitual

```txt
Tenant
└── Product
    └── Casos de Escalabilidade, Crescimento da Plataforma e Estratégias Evolutivas
        ├── Contract
        ├── Permissions
        ├── Audit
        └── Lifecycle
```

## Regras Obrigatórias

1. Deve respeitar tenant, produto, membership e role.
2. Deve possuir contrato JSON canônico antes de ser consumido por frontend.
3. Deve gerar auditoria em operações administrativas relevantes.
4. Deve respeitar LGPD quando manipular dados pessoais.
5. Deve preservar compatibilidade com REST do MVP e GraphQL futuro.
6. Deve separar domínio de provider, storage, canal ou tecnologia específica.
7. Deve ser cacheável somente quando segurança e contexto permitirem.
8. Deve ser observável por correlationId, logs e métricas.
9. Deve possuir estratégia de evolução sem quebra de contrato.
10. Deve ser documentado por ADR quando alterar decisão arquitetural.

# Disaster Recovery, Continuidade Operacional e Sobrevivência da Plataforma

## Especificação

Disaster Recovery define como o Aegis sobrevive a perda de banco, storage, região, secrets, deploy ruim, corrupção de dados, exclusão acidental e falha de integração. Elementos: backups, restore testado, runbooks, RPO/RTO, status page futura, modo read-only, rollback de deploy, comunicação operacional e auditoria de incidente. Continuidade exige priorizar contratos públicos, login admin, assets essenciais e formulários críticos. Leis: incidente tem dono; restore tem procedimento; comunicação é parte da recuperação; DR sem simulado não é confiável.

## Contrato e Modelo Operacional

```json
{
  "id": "example_001",
  "tenantId": "tenant_byop",
  "productId": "product_aegis",
  "type": "DISASTER_RECOVERY",
  "status": "ACTIVE",
  "metadata": {},
  "createdAt": "2026-01-01T10:00:00Z",
  "updatedAt": "2026-01-01T10:00:00Z"
}
```

## Diagrama Conceitual

```txt
Tenant
└── Product
    └── Disaster Recovery, Continuidade Operacional e Sobrevivência da Plataforma
        ├── Contract
        ├── Permissions
        ├── Audit
        └── Lifecycle
```

## Regras Obrigatórias

1. Deve respeitar tenant, produto, membership e role.
2. Deve possuir contrato JSON canônico antes de ser consumido por frontend.
3. Deve gerar auditoria em operações administrativas relevantes.
4. Deve respeitar LGPD quando manipular dados pessoais.
5. Deve preservar compatibilidade com REST do MVP e GraphQL futuro.
6. Deve separar domínio de provider, storage, canal ou tecnologia específica.
7. Deve ser cacheável somente quando segurança e contexto permitirem.
8. Deve ser observável por correlationId, logs e métricas.
9. Deve possuir estratégia de evolução sem quebra de contrato.
10. Deve ser documentado por ADR quando alterar decisão arquitetural.

# Governança Arquitetural, ADRs e Fonte da Verdade do Ecossistema Aegis

## Especificação

Governança impede o Aegis de virar um conjunto de exceções. Decisões arquiteturais relevantes devem virar ADRs. O Documento Mestre define visão e domínio; ADRs registram decisões; contratos JSON registram interoperabilidade; OpenAPI registra REST; migrations registram banco; Code Connect e design system registram UI quando aplicável. Mudanças que afetam tenants, memberships, contratos, segurança, LGPD, billing, deploy ou storage exigem revisão. Leis: decisão importante sem ADR é dívida; documento mestre é fonte de verdade estratégica; contrato é fonte de verdade de integração; implementação deve obedecer governança.

## Contrato e Modelo Operacional

```json
{
  "id": "example_001",
  "tenantId": "tenant_byop",
  "productId": "product_aegis",
  "type": "GOVERNANÇA_ARQUITETURAL",
  "status": "ACTIVE",
  "metadata": {},
  "createdAt": "2026-01-01T10:00:00Z",
  "updatedAt": "2026-01-01T10:00:00Z"
}
```

## Diagrama Conceitual

```txt
Tenant
└── Product
    └── Governança Arquitetural, ADRs e Fonte da Verdade do Ecossistema Aegis
        ├── Contract
        ├── Permissions
        ├── Audit
        └── Lifecycle
```

## Regras Obrigatórias

1. Deve respeitar tenant, produto, membership e role.
2. Deve possuir contrato JSON canônico antes de ser consumido por frontend.
3. Deve gerar auditoria em operações administrativas relevantes.
4. Deve respeitar LGPD quando manipular dados pessoais.
5. Deve preservar compatibilidade com REST do MVP e GraphQL futuro.
6. Deve separar domínio de provider, storage, canal ou tecnologia específica.
7. Deve ser cacheável somente quando segurança e contexto permitirem.
8. Deve ser observável por correlationId, logs e métricas.
9. Deve possuir estratégia de evolução sem quebra de contrato.
10. Deve ser documentado por ADR quando alterar decisão arquitetural.

---

# 6. Entidade Central: Produto

## 6.1 Definição

Produto é qualquer unidade digital administrada pelo Aegis.

Um produto pode ser:

- site institucional;
- site comercial;
- blog;
- wiki;
- e-commerce;
- portal de RH;
- portfólio;
- biblioteca;
- plataforma educacional;
- combinação de vários módulos.

## 6.2 Regras

- Podem existir diversos produtos.
- Um produto pode ter diversos donos.
- Um dono pode possuir diversos produtos.
- Um produto possui uma categoria principal.
- Um produto possui funcionalidades habilitadas.
- Um produto pode ter funcionalidades alteradas conforme necessidade.
- Um produto pode ser desativado, arquivado ou deletado logicamente.
- Um produto não deve ser apagado fisicamente sem política explícita de retenção.

## 6.3 Categorias Iniciais

- Site Institucional
- Landing Page
- Blog
- Portfólio
- Portal de RH
- Wiki
- Biblioteca Digital
- Site Comercial
- E-commerce futuro
- LMS futuro
- Comunidade futura

## 6.4 Estrutura Conceitual

```txt
Product
├── Identity
├── Owners
├── Users
├── Category
├── Enabled Features
├── Pages
├── Content Types
├── Assets
├── Forms
├── SEO
├── Analytics
├── Integrations
├── Audit Logs
└── Billing Metadata futuro
```

---

# 7. Fluxos Fundamentais

## 7.1 Fluxo A — Administrador cadastra produto

Ator: Administrador da plataforma.

Objetivo: cadastrar um novo produto e seus donos.

Fluxo:

```txt
Admin acessa Aegis
↓
Abre módulo Produtos
↓
Clica em Novo Produto
↓
Preenche identidade do produto
↓
Seleciona categoria
↓
Seleciona funcionalidades
↓
Cadastra ou associa donos
↓
Define plano comercial futuro, se aplicável
↓
Salva produto
↓
Aegis provisiona estrutura inicial
↓
Menus são gerados conforme funcionalidades habilitadas
```

Dados mínimos:

- nome;
- slug;
- categoria;
- descrição;
- donos;
- funcionalidades;
- status;
- idioma padrão;
- domínio público futuro;
- plano futuro;
- configurações iniciais.

Regras:

- Produto deve ter pelo menos um dono.
- Produto deve ter categoria.
- Produto deve ter slug único.
- Produto deve iniciar como draft ou active conforme decisão do admin.
- Apenas administradores da plataforma podem criar produtos manualmente no MVP.
- No futuro, compra de produto poderá criar produto automaticamente com funcionalidades pré-selecionadas.

## 7.2 Fluxo B — Roles e permissões

Ator: Administrador, dono de produto, Keycloak.

Objetivo: controlar acesso com segurança.

Regras:

- A autenticação será feita via Keycloak.
- Usuários podem ter roles globais e roles por produto.
- Donos de produto podem convidar usuários para colaborar.
- Donos não podem conceder roles acima das suas próprias permissões.
- Um editor de um produto não pode acessar outro produto.
- Usuário master da plataforma não deve ser criado por donos.
- Roles administrativas globais só podem ser concedidas por Super Admin.

Roles iniciais:

- `SUPER_ADMIN`
- `PLATFORM_ADMIN`
- `PRODUCT_OWNER`
- `PRODUCT_MANAGER`
- `EDITOR`
- `REVIEWER`
- `AUTHOR`
- `VIEWER`

Ações protegidas:

- criar produto;
- editar produto;
- deletar produto;
- convidar usuário;
- atribuir role;
- editar conteúdo;
- publicar conteúdo;
- arquivar conteúdo;
- gerenciar assets;
- gerenciar SEO;
- visualizar analytics;
- gerenciar integrações;
- gerenciar billing futuro.

## 7.3 Fluxo C — Usuário acessa produto já cadastrado

Ator: dono/editor do produto.

Objetivo: gerenciar conteúdo do produto.

Fluxo:

```txt
Usuário acessa Aegis
↓
Autentica via Keycloak
↓
Sistema lista produtos acessíveis
↓
Usuário seleciona produto
↓
Aegis carrega funcionalidades habilitadas
↓
Menu lateral é montado dinamicamente
↓
Usuário acessa módulo desejado
↓
Usuário edita conteúdo
↓
Conteúdo passa por workflow
↓
API pública passa a entregar contrato atualizado quando publicado
```

Regras:

- O menu do usuário depende do produto selecionado.
- Funcionalidades não habilitadas não aparecem.
- Funcionalidades não habilitadas não podem ser acessadas por URL direta.
- Toda consulta deve validar `productId` e permissão.

## 7.4 Fluxo D — Usuário publica primeiro conteúdo

Ator: editor, dono ou autor.

Objetivo: editar e publicar conteúdo.

Fluxo:

```txt
Usuário seleciona produto
↓
Acessa Conteúdo
↓
Seleciona página ou tipo de conteúdo
↓
Visualiza lista de seções/blocos
↓
Seleciona componente/bloco
↓
Edita campos permitidos
↓
Salva como rascunho
↓
Envia para revisão ou publica, conforme permissão
↓
Aegis cria nova revisão
↓
Contrato público é atualizado se status = Published
```

Regras:

- O frontend público não é renderizado dentro do Aegis no MVP.
- O Aegis mostra uma representação administrativa dos componentes.
- Preview visual completo pode existir no futuro.
- O conteúdo publicado deve gerar contrato JSON estável.
- Conteúdo em draft não deve aparecer na API pública.

---

# 8. Modelo de Navegação do Admin

O painel administrativo deve ser dinâmico.

Menu base:

```txt
Dashboard
Produtos
Conteúdo
Mídia
SEO
Formulários
Analytics
Usuários
Configurações
```

Menu por produto institucional:

```txt
Dashboard
Páginas
Seções
Mídia
Eventos
Galeria
Formulários
SEO
Analytics
Configurações
```

Menu por produto RH:

```txt
Dashboard
Páginas
Vagas
Candidatos
Currículos
Leads
Blog
Mídia
SEO
Analytics
Configurações
```

Menu por produto Biblioteca:

```txt
Dashboard
Manifestos
Poemas
Reflexões
Trechos
Livros
Playlists
Mídia
SEO
Analytics
Configurações
```

Menu por produto Wiki:

```txt
Dashboard
Categorias
Tópicos
Artigos
Relacionamentos
Comentários
Sugestões
Contribuidores
Downloads
SEO
Analytics
Configurações
```

Menu por produto Portfólio:

```txt
Dashboard
Projetos
Experiências
Skills
Artigos
Downloads
FAQ
Mídia
SEO
Analytics
Configurações
```

---

# 9. Núcleo do Aegis

## 9.1 Core Platform

Responsável por:

- produtos;
- usuários;
- permissões;
- autenticação;
- tenants;
- configurações globais;
- módulos habilitados;
- billing metadata futuro.

## 9.2 Content Core

Responsável por:

- páginas;
- seções;
- blocos;
- tipos de conteúdo;
- revisões;
- workflow;
- tradução;
- publicação.

## 9.3 Asset Core

Responsável por:

- imagens;
- documentos;
- vídeos;
- áudios;
- capas;
- downloads;
- metadados;
- alt text;
- focal point;
- créditos.

## 9.4 SEO Core

Responsável por:

- metadados globais;
- metadados por página;
- OpenGraph;
- Twitter Cards;
- canonical;
- sitemap;
- robots;
- schema.org futuro.

## 9.5 Analytics Core

Responsável por:

- page views;
- visitantes únicos;
- eventos;
- cliques;
- downloads;
- envios de formulário;
- buscas;
- conversões.

## 9.6 Integration Core

Responsável por:

- Telegram;
- email;
- webhooks;
- WhatsApp links;
- YouTube futuro;
- Spotify futuro;
- pagamentos futuros;
- APIs externas futuras.

---

# 10. Estratégia de Conteúdo: Pages, Sections e Blocks

## 10.1 Page

Página é uma composição de seções.

Campos mínimos:

```json
{
  "id": "page_home",
  "productId": "product_cmss",
  "slug": "home",
  "title": "Home",
  "locale": "pt-BR",
  "status": "PUBLISHED",
  "seo": {},
  "sections": []
}
```

## 10.2 Section

Seção é uma unidade editorial ordenável.

```json
{
  "id": "section_hero_home",
  "type": "hero",
  "variant": "default",
  "order": 0,
  "status": "PUBLISHED",
  "blocks": []
}
```

## 10.3 Block

Bloco é a menor unidade renderizável relevante.

```json
{
  "id": "block_home_hero",
  "type": "HeroBlock",
  "variant": "artist-premium",
  "content": {},
  "settings": {},
  "rules": {}
}
```

## 10.4 Regra Fundamental

Layout estrutural não deve ficar dentro do Markdown.

Markdown é para conteúdo editorial.

Blocks são para estrutura.

---

# 11. Estratégia Markdown

O Aegis deve suportar Markdown, mas de forma controlada.

## 11.1 Markdown deve ser usado para

- textos longos;
- descrições;
- artigos;
- manifestos;
- reflexões;
- poemas;
- FAQ;
- conteúdo editorial.

## 11.2 Markdown não deve ser usado para

- criar layout de página;
- definir grid;
- criar hero;
- criar galeria;
- definir navegação;
- definir formulário;
- criar duas colunas estruturais;
- embutir HTML livre.

## 11.3 Duas colunas

O caso do site institucional CMSS revelou a necessidade de um bloco de duas colunas.

A decisão recomendada:

```json
{
  "type": "TwoColumnBlock",
  "variant": "text-media",
  "content": {
    "left": [
      {
        "type": "MarkdownBlock",
        "content": {
          "markdown": "## Título\nTexto editorial."
        }
      }
    ],
    "right": [
      {
        "type": "ImageBlock",
        "content": {
          "assetId": "asset_001"
        }
      }
    ]
  },
  "settings": {
    "desktopRatio": "1:1",
    "mobileOrder": "left-first"
  }
}
```

O Markdown pode existir dentro da coluna, mas não deve criar a coluna.

---

# 12. Catálogo Inicial de Blocks

## 12.1 HeroBlock

Usado em:

- CMSS;
- Maestro Beton;
- Conecta Talentos;
- Alexandre Dev;
- Loki;
- WikiDev.

Campos:

- title;
- subtitle;
- eyebrow;
- description;
- image;
- cta;
- secondaryCta;
- variant;
- overlay;
- alignment.

## 12.2 RichTextBlock

Conteúdo editorial estruturado.

## 12.3 MarkdownBlock

Conteúdo textual em Markdown sanitizado.

## 12.4 ImageBlock

Imagem com metadados.

## 12.5 GalleryBlock

Galeria de imagens/vídeos.

## 12.6 TimelineBlock

Linha do tempo institucional, histórica ou narrativa.

## 12.7 CTASectionBlock

Chamada para ação.

## 12.8 FAQBlock

Perguntas frequentes.

## 12.9 ContactBlock

Informações de contato + formulário.

## 12.10 EventListBlock

Lista de eventos baseada em content type.

## 12.11 CardListBlock

Cards genéricos.

## 12.12 FeatureGridBlock

Grade de recursos, serviços, valores ou diferenciais.

## 12.13 TwoColumnBlock

Layout controlado de duas colunas.

## 12.14 ImageTextBlock

Especialização de duas colunas para texto + imagem.

## 12.15 NavbarBlock

Navegação global.

## 12.16 FooterBlock

Rodapé global.

---

# 13. Domínio Institucional

Produtos relacionados:

- CMSS;
- Maestro Beton parcialmente;
- Conecta Talentos parcialmente.

Entidades:

- Page;
- Section;
- Block;
- Event;
- HistoricalMilestone;
- Gallery;
- SupportOption;
- ContactChannel;
- FormDefinition;
- SocialLink.

Requisitos:

- gerenciar páginas institucionais;
- criar timeline;
- listar eventos;
- gerenciar galerias;
- configurar contato;
- configurar apoio/doação;
- SEO por página;
- contrato público JSON.

---

# 14. Domínio Música / Eventos

Produto principal:

- Maestro Beton.

Entidades:

- MusicService;
- RepertoireCategory;
- Song;
- PerformanceEvent;
- Testimonial;
- QuoteRequest;
- Course;
- Product;
- Video;
- Availability;
- ServiceFormat.

Requisitos:

- listar serviços;
- gerenciar repertório;
- gerenciar depoimentos;
- gerenciar agenda;
- receber orçamento;
- preservar privacidade de eventos privados;
- preparar cursos futuros;
- preparar loja futura;
- configurar WhatsApp.

Regras:

- Evento privado não deve expor dados sensíveis.
- Formulário de orçamento deve capturar contexto.
- CTA de orçamento deve estar sempre acessível.

---

# 15. Domínio RH

Produto principal:

- Conecta Talentos.

Entidades:

- CompanyProfile;
- Service;
- JobPosting;
- CandidateSubmission;
- Candidate;
- ResumeAsset;
- CorporateLead;
- Article;
- TalentPool.

Requisitos:

- publicar vagas;
- filtrar vagas;
- receber currículos;
- vincular candidatura a vaga;
- gerar lead corporativo;
- notificar via Telegram/email;
- preparar CRM futuro.

Regras:

- Currículos são dados sensíveis.
- Uploads devem ser validados.
- Acesso a candidatos deve ser restrito ao produto.
- LGPD deve ser considerada desde o MVP.

---

# 16. Domínio Portfólio

Produto principal:

- Alexandre Dev.

Entidades:

- Project;
- CaseStudy;
- ProfessionalExperience;
- Skill;
- SkillCategory;
- Article;
- Download;
- Resume;
- RecruiterLead;
- SocialLink.

Requisitos:

- publicar projetos;
- publicar experiências;
- publicar skills;
- gerenciar currículo;
- gerenciar downloads;
- publicar artigos;
- capturar contatos profissionais;
- medir acessos a projetos.

---

# 17. Domínio Biblioteca Autoral

Produto principal:

- Loki.

Entidades:

- AuthorIdentity;
- AuthorWork;
- Manifesto;
- Poem;
- Reflection;
- Excerpt;
- Book;
- BookEdition;
- BookDownload;
- BookPurchaseLink;
- Playlist;
- MusicReference.

Requisitos:

- manter anonimato;
- publicar obras;
- relacionar obras com músicas;
- disponibilizar downloads;
- permitir links de compra;
- analytics literário;
- SEO por obra;
- identidade visual monocromática no frontend.

Regras:

- A obra é mais importante que o autor.
- Música pode ser referência de poema, reflexão ou manifesto.
- Livro possui ciclo próprio.

---

# 18. Domínio Wiki / Knowledge Base

Produto principal:

- WikiDev.

Entidades:

- Category;
- Topic;
- KnowledgeArticle;
- KnowledgeRelation;
- ArticlePreview;
- Comment;
- ContentSuggestion;
- BugReport;
- ContributorApplication;
- DownloadAsset;
- SearchEvent.

Requisitos:

- busca central;
- categorias hierárquicas;
- tópicos;
- artigos;
- links internos;
- popup de preview;
- conteúdo relacionado;
- comentários;
- sugestões;
- report de bugs;
- candidatura para contribuidor;
- fórum futuro;
- gamificação futura.

Regras:

- Usuário nunca deve chegar a beco sem saída.
- Todo conteúdo deve sugerir próximos caminhos.
- Links internos devem ser resolvidos por referência, não por string frágil.

---

# 19. Form Builder

O Aegis precisa de um sistema de formulários configuráveis.

Entidades:

- FormDefinition;
- FormField;
- FormSubmission;
- FormIntegration;

Tipos de campo:

- text;
- textarea;
- email;
- phone;
- select;
- multiselect;
- checkbox;
- radio;
- date;
- file;
- hidden.

Integrações:

- email;
- Telegram;
- webhook;
- banco de dados;
- API futura.

Segurança:

- rate limit;
- captcha futuro;
- honeypot;
- validação server-side;
- consentimento LGPD;
- sanitização.

---

# 20. Asset Manager

O Aegis deve possuir biblioteca de mídia centralizada por produto.

Tipos:

- image;
- video;
- audio;
- pdf;
- document;
- archive.

Campos:

- id;
- productId;
- filename;
- mimeType;
- size;
- url;
- alt;
- caption;
- credit;
- focalPoint;
- tags;
- status;
- createdBy;
- createdAt.

Regras:

- Imagens públicas devem ter alt obrigatório.
- Uploads devem validar MIME e tamanho.
- Arquivos sensíveis devem ter acesso protegido.
- Currículos não devem ser públicos por padrão.

---

# 21. SEO Engine

O Aegis deve gerenciar SEO global e por recurso.

Aplicável a:

- produto;
- página;
- artigo;
- vaga;
- projeto;
- livro;
- manifesto;
- poema;
- tópico;
- serviço.

Campos:

- title;
- description;
- keywords;
- canonical;
- ogImage;
- noIndex;
- structuredData;
- locale;

---

# 22. Analytics

O Aegis deve expor API de analytics para produtos externos.

Eventos iniciais:

- page_view;
- content_view;
- form_submit;
- file_download;
- cta_click;
- search;
- job_apply;
- resume_upload;
- book_download;
- project_click.

Regras:

- Analytics deve respeitar privacidade.
- IP deve ser armazenado com hash ou estratégia segura.
- Dados sensíveis não devem ser expostos no dashboard sem permissão.

---

# 23. Keycloak e IAM

## 23.1 Estratégia

O Keycloak será responsável por autenticação e emissão de tokens.

O Aegis será responsável por aplicar regras de negócio de autorização por produto.

## 23.2 Roles globais

- SUPER_ADMIN
- PLATFORM_ADMIN

## 23.3 Roles por produto

- PRODUCT_OWNER
- PRODUCT_MANAGER
- EDITOR
- REVIEWER
- AUTHOR
- VIEWER

## 23.4 Regra crítica

Um dono de produto não pode conceder uma role superior à sua própria.

## 23.5 Claims desejadas

Token deve permitir identificar:

- userId;
- email;
- global roles;
- product memberships;
- product roles;
- realm;
- tenant.

Caso o Keycloak não carregue tudo no token, o Aegis deve consultar sua própria base de memberships.

---

# 24. Modelo Conceitual Inicial de Banco

Tabelas centrais sugeridas:

- products
- product_owners
- product_users
- product_features
- feature_catalog
- users_shadow
- pages
- sections
- blocks
- content_types
- content_entries
- assets
- forms
- form_fields
- form_submissions
- seo_metadata
- analytics_events
- revisions
- audit_logs
- translations
- integrations
- navigation_items

Tabelas de módulos:

- job_postings
- candidate_submissions
- leads
- projects
- professional_experiences
- skills
- music_services
- repertoire_items
- testimonials
- books
- poems
- manifestos
- reflections
- playlists
- music_references
- wiki_categories
- wiki_topics
- wiki_articles
- wiki_relations
- comments
- contributor_applications

---

# 25. API Pública de Contratos

O frontend externo deve consumir endpoints como:

```txt
GET /public/products/{productSlug}/contract
GET /public/products/{productSlug}/pages/{slug}
GET /public/products/{productSlug}/content-types/{type}
GET /public/products/{productSlug}/assets/{assetId}
POST /public/products/{productSlug}/analytics/events
POST /public/products/{productSlug}/forms/{formId}/submit
```

A API pública deve entregar somente conteúdo publicado.

Drafts e revisões não devem aparecer.

---

# 26. API Administrativa

Endpoints administrativos devem exigir autenticação.

```txt
/products
/products/{id}
/products/{id}/features
/products/{id}/users
/products/{id}/pages
/products/{id}/assets
/products/{id}/forms
/products/{id}/seo
/products/{id}/analytics
/products/{id}/content-types
```

Toda rota deve validar:

- usuário autenticado;
- membership;
- role;
- produto;
- ação;
- status do recurso.

---

# 27. Billing Futuro

O Aegis deve reservar espaço para monetização futura.

Entidades futuras:

- Plan;
- Subscription;
- BillingCycle;
- Invoice;
- PaymentProvider;
- ProductPurchase;

Regras futuras:

- um usuário poderá comprar um produto pré-configurado;
- produto pode nascer com features selecionadas;
- planos podem limitar features;
- planos podem limitar armazenamento;
- planos podem limitar usuários;
- planos podem limitar tráfego/API.

No MVP:

- criar campos e TODOs;
- não executar cobrança;
- não bloquear uso por billing;
- manter estrutura preparada.

---

# 28. Diretrizes para Eirene

Eirene deve desenhar o Aegis como produto SaaS moderno.

Não deve parecer painel administrativo genérico.

Referências de sensação:

- Linear;
- Vercel;
- Stripe Dashboard;
- Notion;
- Raycast;
- GitHub Projects.

## 28.1 Princípios UI/UX

- Dark-first.
- Leve.
- Modular.
- Hierárquico.
- Sem poluição visual.
- Menus dinâmicos por produto.
- Foco em edição clara.
- Separar conteúdo de configuração.
- Mostrar status editorial com clareza.
- Indicar produto ativo sempre.

## 28.2 Telas mínimas

- Login integrado ao Keycloak.
- Seleção de produto.
- Dashboard global.
- Dashboard do produto.
- Lista de produtos.
- Cadastro de produto.
- Gestão de usuários.
- Gestão de features.
- Editor de páginas.
- Editor de blocos.
- Asset manager.
- Form builder.
- SEO manager.
- Analytics.
- Audit log.
- Configurações.

## 28.3 Editor de Conteúdo

O editor não deve tentar renderizar o site final no MVP.

Ele deve mostrar:

- página;
- seções;
- blocos;
- campos;
- status;
- preview textual/estrutural;
- botão salvar;
- botão publicar se permitido.

Preview visual completo é evolução futura.

---

# 29. Diretrizes para Aegis Backend

Stack recomendada:

- Java;
- Spring Boot;
- Spring Security;
- Keycloak;
- PostgreSQL;
- Flyway;
- OpenAPI;
- Docker;
- Testcontainers futuro;
- JUnit;
- Mockito;
- Bean Validation.

Arquitetura recomendada:

```txt
controller
application/service
domain
repository
integration
config
security
dto
mapper
exception
```

Preferir arquitetura modular por domínio:

```txt
core
identity
products
content
assets
forms
seo
analytics
modules/rh
modules/wiki
modules/library
modules/portfolio
modules/music
```

---

# 30. Roadmap

## Fase 0 — Fundação

- setup Spring Boot;
- Keycloak;
- PostgreSQL;
- Docker Compose;
- autenticação;
- produtos;
- usuários;
- roles;
- features;
- audit básico.

## Fase 1 — CMS Core

- páginas;
- seções;
- blocos;
- assets;
- SEO;
- workflow;
- contrato público.

## Fase 2 — Formulários e Analytics

- form builder;
- submissions;
- Telegram;
- eventos analytics;
- dashboard inicial.

## Fase 3 — Módulos Reais

- institucional/CMSS;
- Maestro Beton;
- Alexandre Dev;
- Conecta Talentos.

## Fase 4 — Biblioteca e Wiki

- Loki;
- WikiDev;
- relações;
- comentários;
- sugestões;
- conhecimento conectado.

## Fase 5 — SaaS Futuro

- planos;
- assinaturas;
- loja;
- LMS;
- CRM;
- comunidade.

---

# 31. Leis do Aegis

1. Produto é a entidade raiz.
2. Nenhum conteúdo público deve depender de hardcode no frontend.
3. Todo conteúdo publicado deve possuir versão.
4. Todo recurso editável deve possuir auditoria.
5. Todo produto deve ter donos.
6. Nenhum usuário pode atribuir permissões acima das próprias.
7. Todo acesso deve ser validado por produto.
8. Markdown é conteúdo, não layout.
9. Blocks definem estrutura.
10. Assets possuem metadados obrigatórios.
11. SEO é parte do conteúdo.
12. Analytics respeita privacidade.
13. Módulos podem evoluir; o core deve permanecer estável.
14. O frontend interpreta contratos; não governa conteúdo.
15. O Aegis deve nascer pronto para crescer sem virar Frankenstein.

---

# 32. Conclusão

O Aegis é a fundação administrativa do ecossistema BYOP.

Ele nasce dos casos reais CMSS, Maestro Beton, Conecta Talentos, Alexandre Dev, Loki e WikiDev.

Cada produto revelou um domínio.

Cada domínio revelou entidades.

Cada entidade revelou capacidades.

Cada capacidade revelou a arquitetura necessária.

O Aegis deve ser construído como plataforma modular, segura, auditável, multi-produto e orientada a contratos.

Este documento é a fonte de verdade inicial para:

- Aegis arquitetar o backend;
- Eirene prototipar a UI/UX;
- Daedalus preparar infraestrutura;
- Zeus validar a visão sistêmica;
- Loki executar o desenvolvimento.