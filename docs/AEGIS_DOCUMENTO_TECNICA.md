# AEGIS — Documento Mestre de Produto (V1)

## AMPS — Aegis Master Product Specification

**Versão:** 1.0 (consolidada)
**Status:** Fonte de verdade oficial para arquitetura, UI/UX e desenvolvimento — constituição do projeto Aegis.
**Ecossistema:** BYOP — Build Your Own Path
**Responsável estratégica:** Athena
**Consumidores deste documento:** Aegis (backend), Eirene (UX/UI), Daedalus (infraestrutura), Zeus (visão sistêmica), Loki (execução) e futuros agentes do Panteão BYOP.

> **Definição central:** O Aegis **não gerencia páginas**. O Aegis gerencia **produtos digitais** — suas capacidades, conteúdos, contratos, usuários, assets, fluxos editoriais, permissões, integrações e dados de operação. Páginas são apenas uma forma de materializar um produto.
>
> O Aegis **administra conteúdo**; ele **não renderiza páginas públicas**. Frontends externos atuam como **renderizadores de contratos**. O Aegis é a **fonte de verdade**.

### Como ler este documento

O documento está dividido em **oito Partes**. A Parte I é a base conceitual; as Partes II–VI detalham produto, conteúdo, operação, plataforma e arquitetura; a Parte VII trata de governança; a Parte VIII traz o roadmap. Ao final, há quatro seções transversais obrigatórias (Frontend, Lacunas, Próximos Passos, Resumo Executivo).

Documentação complementar vive em `docs/adr/`, `docs/contracts/`, `docs/api/`, `docs/database/`, `docs/deploy/`, `docs/modules/`, `docs/operations/`, `docs/security/`, `docs/ux/` e `docs/governance/` (ver `docs/README.md`).

---

# Parte I — Visão e Fundamentos

## 1. Declaração Fundamental

O Aegis não deve ser tratado como um CMS tradicional. É uma **plataforma multi-produto, multi-tenant, modular e orientada a contratos**, criada para administrar produtos digitais do ecossistema BYOP e de futuros clientes.

Princípios fundadores:

- Frontends não possuem conteúdo final fixo; consomem contratos.
- O Aegis é a fonte de verdade do conteúdo e das capacidades de cada produto.
- Páginas são uma materialização de produto, não a entidade central.

## 2. Problema que o Aegis Resolve

O BYOP possui e continuará possuindo diversos produtos digitais com necessidades diferentes: sites institucionais, comerciais, portfólios, bibliotecas autorais, wikis técnicas, portais de recrutamento, blogs, landing pages, e (futuro) lojas, sistemas educacionais, comunidades e produtos SaaS.

Sem o Aegis, cada novo produto tende a nascer como um conjunto isolado de JSON, componentes específicos, regras hardcoded e contratos improvisados, gerando: duplicação de regras; acoplamento entre frontend e conteúdo; contratos frágeis; dificuldade de manutenção e evolução; falta de governança, controle editorial, versionamento, permissões por produto e histórico; dificuldade de escalar para múltiplos clientes; e risco de virar um **Frankenstein arquitetural**.

O Aegis nasce para impedir isso.

## 3. Visão do Produto

O Aegis é a plataforma administrativa central do ecossistema BYOP. Permite que um administrador cadastre produtos digitais, associe donos, habilite funcionalidades e permita que cada dono gerencie seus próprios conteúdos sem alterar código.

Experiência desejada:

```txt
Administrador BYOP
↓ cadastra Produto
↓ define Donos
↓ seleciona Categoria
↓ seleciona Funcionalidades (Features)
↓ Aegis habilita menus e módulos
↓ Dono acessa painel
↓ Dono edita conteúdos
↓ Frontend externo consome contrato JSON
↓ Produto público é atualizado
```

O Aegis deve suportar uso interno e possibilidade futura de comercialização como plataforma (SaaS Ready).

## 4. Filosofia

- **CMS First** — conteúdo público é editável pelo Aegis, não hardcoded no frontend.
- **Product First** — a entidade raiz é o Produto.
- **Contract First** — toda entrega pública é baseada em contratos estáveis.
- **Multi-Tenant** — isolamento lógico forte desde o início.
- **Modular** — núcleo estável + módulos habilitáveis por produto.
- **SaaS Ready, não SaaS Enabled** — espaço arquitetural reservado para billing, sem implementá-lo no MVP.
- **REST First, GraphQL Future** — REST é a API oficial do MVP.
- **Evolução sem Frankenstein** — crescer sem perder coerência.

## 5. Princípios Arquiteturais

| Princípio | Enunciado |
|---|---|
| Product-First | A raiz operacional é o Produto (ADR-0001). Não site, não página, não cliente. |
| Contract-First | O frontend nunca depende da estrutura interna do banco; consome contratos (ADR-0002). |
| CMS-First | Dados renderizados vêm do Aegis; componentes podem ser hardcoded, textos finais não (ADR-0003). |
| Modularidade | Núcleo para todos os produtos; módulos habilitados por categoria/feature. |
| Multi-Tenant | Isolamento lógico de dados, permissões e operação por tenant. |
| Segurança por Padrão | Toda ação protegida por autenticação, autorização e auditoria. |
| Evolução sem Frankenstein | Funcionalidades comerciais futuras previstas no modelo, não implementadas no MVP. |

Avaliação de permissão considera: usuário, role, tenant, produto, feature, recurso, ação e escopo/ownership.

## 6. Produtos Fonte da Modelagem

O Aegis nasce da consolidação de seis produtos reais ou planejados. Cada um revelou um domínio que moldou a arquitetura.

| Produto | Tipo | Domínio revelado |
|---|---|---|
| **CMSS** (Corporação Musical São Sebastião) | Site institucional | Páginas, navegação, hero, seções, galeria, eventos, timeline, contato, SEO por página, versionamento, separar layout de conteúdo. |
| **Maestro Beton** | Site comercial (músico) | Serviços, agenda, repertório, vídeos, galeria, depoimentos, orçamento, WhatsApp, captação de leads, cursos/loja futuros. |
| **Conecta Talentos** | Portal RH + vagas | Empresa, serviços RH, vagas, banco de talentos, candidaturas, upload de currículo, leads corporativos, blog, pipeline futuro. |
| **Alexandre Dev** | Portfólio | Projetos, experiências, skills, artigos, downloads, currículo, cases, SEO profissional, analytics. |
| **Loki** | Biblioteca autoral | Manifestos, poemas, reflexões, trechos, livros, playlists, referências musicais, downloads, anonimato autoral, analytics literário. |
| **WikiDev** | Wiki técnica | Categorias hierárquicas, tópicos, artigos, busca, relacionados, link preview, comentários, sugestões, bug reports, contribuidores, Telegram. |

**Aprendizado consolidado:** o Aegis precisa suportar páginas/seções/blocos **e** domínios transacionais (RH), obras autorais, conhecimento conectado, conversão comercial e colaboração — tudo sob contratos versionados, multi-tenant e CMS-first.

## 7. Leis do Aegis

1. Produto é a entidade raiz.
2. Nenhum conteúdo público depende de hardcode no frontend.
3. Todo conteúdo publicado possui versão.
4. Todo recurso editável possui auditoria.
5. Todo produto tem donos e pertence a um tenant.
6. Nenhum usuário atribui permissões acima das próprias.
7. Todo acesso é validado por tenant, produto, feature, role e ação.
8. Markdown é conteúdo, não layout.
9. Blocks definem estrutura/composição.
10. Assets possuem metadados obrigatórios.
11. SEO é parte do conteúdo.
12. Analytics respeita privacidade (LGPD).
13. Módulos evoluem; o core permanece estável.
14. O frontend interpreta contratos; não governa conteúdo.
15. Tenant isolation é inegociável.
16. REST First no MVP; GraphQL Future até ADR contrário.
17. O Aegis deve nascer pronto para crescer sem virar Frankenstein.

---

# Parte II — Produto

## 8. Entidade Central: Produto

Produto é qualquer unidade digital administrada pelo Aegis: site institucional, comercial, blog, wiki, e-commerce, portal de RH, portfólio, biblioteca, plataforma educacional ou combinação de módulos.

**Regras:**
- Podem existir diversos produtos; um produto pode ter diversos donos; um dono pode ter diversos produtos.
- Um produto possui uma categoria principal e features habilitadas.
- Features podem ser alteradas conforme necessidade.
- Um produto pode ser desativado, arquivado ou deletado logicamente; nunca apagado fisicamente sem política de retenção explícita.
- Todo produto pertence a **exatamente um tenant**.

**Categorias iniciais:** Site Institucional, Landing Page, Blog, Portfólio, Portal de RH, Wiki, Biblioteca Digital, Site Comercial, e (futuro) E-commerce, LMS, Comunidade.

**Estrutura conceitual:**

```txt
Product
├── Identity (id, slug, name, category, status)
├── Owners / Users (via memberships)
├── Enabled Features
├── Pages / Content Types
├── Assets / Forms
├── SEO / Analytics
├── Integrations / Audit Logs
└── Billing Metadata (futuro)
```

## 9. Tenants

Tenant é a **fronteira primária** de pertencimento, autorização, configuração, auditoria, conteúdo, assets, analytics e billing futuro. Multi-tenant não é só decisão de banco: é decisão de produto, segurança, operação e governança.

### 9.1 Conceitos

```txt
Tenant      = espaço isolado de administração (empresa, marca, grupo de produtos, operação interna).
Produto     = item digital gerenciado dentro de um tenant.
Usuário     = identidade global que pode participar de um ou mais tenants.
Organização = conceito comercial/jurídico que pode ou não coincidir com um tenant.
```

Regra de cardinalidade: **Tenant 1 ── N Product**; um produto nunca pertence a mais de um tenant.

Exemplos de tenants: `byop`, `cmss`, `maestro-beton`, `conecta-talentos`, `alexandre-dev`, `loki`, `wikidev`. O tenant não precisa aparecer ao visitante público, mas o painel, a API administrativa, jobs, logs, assets e integrações são sempre tenant-aware.

> Login prova quem o usuário é. Membership prova onde ele pode atuar. Role prova o que ele pode fazer naquele tenant.

### 9.2 Estratégia Multi-Tenant (ADR-0004)

Decisão: **Shared Database + Shared Schema + `tenantId` obrigatório**. Isolamento lógico forte por regras de domínio, autorização, índices, constraints, middleware, testes e auditoria.

- Banco por tenant e schema por tenant foram **rejeitados no MVP** (custo, migrations multiplicadas, complexidade operacional, risco de drift), reservados para enterprise futuro.
- Toda entidade tenant-owned carrega `tenantId`, direta ou indiretamente, validável por constraint/join/serviço.

**Entidades tenant-owned:** products, tenant_memberships, pages, sections, blocks, content_entries, assets, forms, form_submissions, seo_metadata, analytics_events, integrations, audit_logs, e entidades futuras de billing.

Isolamento aplicado a: permissões, conteúdo, assets (com chave de storage prefixada por tenant), analytics, SEO, formulários e integrações (segredos tenant-scoped, criptografados e auditados).

> Shared Database e Shared Schema só são seguros se `tenantId` for tratado como campo **obrigatório de segurança**, não metadado opcional.

### 9.3 Estrutura, Ownership e Roles de Tenant

Estrutura do tenant: Products, Users, Memberships, Features, Assets, Forms, Analytics, Integrations, Settings, Billing futuro.

Campos do Tenant: id, name, slug, status (`active`, `suspended`, `archived`, `pending_setup`), type, ownerUserId, metadata, createdAt, updatedAt, archivedAt.

**Três níveis de autorização:**

- **Roles globais** (plataforma): `SUPER_ADMIN` (raro, auditado).
- **Roles de tenant**: `TENANT_OWNER`, `TENANT_ADMIN`, `TENANT_MANAGER`, `TENANT_VIEWER`.
- **Roles de produto**: `PRODUCT_OWNER`, `PRODUCT_MANAGER`, `PRODUCT_EDITOR`, `PRODUCT_VIEWER`.

Regras: role global não substitui membership de tenant para fluxos comuns; role de tenant não autoriza automaticamente todo detalhe de produto; **role de produto nunca excede a autoridade da role de tenant**.

Ownership: todo tenant ativo tem ao menos um `TENANT_OWNER`; o último owner ativo não pode ser removido/rebaixado sem transferência; toda mudança de owner/role gera audit log; `SUPER_ADMIN` pode operar tenants para suporte, mas com motivo registrado e auditado.

## 10. Memberships

`TenantMembership` é o **contrato de acesso contextual** do usuário ao tenant — não uma simples tabela de associação.

Campos: id, tenantId (imutável), userId, role, status, invitedBy, invitedAt, acceptedAt, createdAt, updatedAt.

**Hierarquia:** Global Membership (plataforma) → Tenant Membership (limite superior de atuação no espaço) → Product Membership (granularidade por produto, nunca excedendo o tenant).

**Ciclo de vida / status:**

```txt
INVITED → ACTIVE → (SUSPENDED ↔ ACTIVE) → REMOVED
```

- `INVITED` não concede acesso; `ACTIVE` concede conforme role; `SUSPENDED` preserva vínculo e bloqueia acesso; `REMOVED` encerra sem apagar trilha de auditoria.
- Estados auxiliares (`PENDING`, `EXPIRED`) podem existir no domínio de convites, mas a semântica de autorização da membership deve ser simples.

**Regras de herança:** Product Role ≤ Tenant Role. O backend avalia sempre tenant → produto → feature → ação.

## 11. Convites

Operação sensível. Fluxo: admin/owner informa email e role → Aegis valida permissão → cria `TenantInvitation` + membership pendente → envia email com link seguro → usuário autentica/cria conta no Keycloak → aceita → membership vira `ACTIVE`.

- Token opaco/assinado; não expõe `tenantId`/`role`/email manipuláveis.
- Convites expiram (padrão sugerido: **7 dias**, configurável por tenant no futuro).
- Convite pode ser reenviado ou revogado; revogação invalida token e gera audit log.

`TenantInvitation`: id, tenantId, email, role, status, tokenHash, invitedByUserId, invitedAt, expiresAt, acceptedAt, revokedAt, revokedByUserId, createdAt, updatedAt.

## 12. Contexto Ativo (Current Tenant)

Após login, o Aegis resolve os tenants acessíveis e exige seleção quando houver mais de um. O **Current Tenant** define o contexto operacional do painel (menus, produtos, assets, analytics, role, cache).

- Zero tenants → estado sem acesso; um → seleção automática/confirmação; muitos → seletor.
- Troca de tenant é **explícita**: limpa caches/estado do tenant anterior, recarrega permissões/menus/produtos.
- O frontend não é fonte de verdade: o tenant ativo no cliente é intenção; o backend valida token + tenant + membership + role + productId + feature em toda chamada sensível.

## 13. Features

Feature é uma **capacidade funcional controlável** do Aegis — uma unidade de comportamento, não apenas item de menu. Se uma feature não está habilitada para um produto: não aparece no menu, não é acessível por URL direta, não aceita escrita na API administrativa, não aparece no contrato público e não gera permissões/limites/billing para aquele produto.

### 13.1 Categoria vs. Módulo vs. Feature

```txt
Product Category  → "Que tipo de produto é este?" (orienta template inicial)
Module            → "Em que área do sistema isso vive?" (organiza o código)
Feature           → "O que este produto pode fazer?" (governa capacidade real por produto)
```

Relação: Categoria sugere Módulos → cada Módulo oferece Features → cada Produto habilita só as Features necessárias.

### 13.2 Feature Catalog e product_features

- **Feature Catalog** — catálogo global de capacidades (possibilidade, não habilitação). Cada item: key estável, nome, descrição, módulo, tipo, status global, dependências, permissões, limites, metadados de billing futuro, impacto em menu/contrato, flags experimentais, datas.
- **product_features** — relação produto↔feature (fonte de verdade do runtime administrativo): status no produto, config, limites, permissões efetivas, origem, datas. O Aegis consulta essa relação antes de montar menu, aceitar comandos, expor endpoints, validar permissões, gerar contrato e aplicar limites.

### 13.3 Status, Dependências, Limites e Billing

- **Status global:** `draft`, `active`, `deprecated`, `retired`, `experimental`.
- **Status por produto:** `enabled`, `active`, `disabled`, `suspended`, `archived`, `pending_billing`, `pending_dependency`, `experimental`.
- **Dependências:** obrigatória, opcional, recomendada, incompatível, alternativa. Feature com dependência obrigatória não ativa sem ela; desativação alerta sobre dependentes; cascata é explícita.
- **Limites** (validados no backend, UI só avisa): páginas, eventos, vagas, candidatos, artigos, armazenamento, tamanho de arquivo, usuários, idiomas, chamadas de API, retenção de analytics, formulários, submissões/mês.
- **Billing metadata (futuro):** armazenar metadados opcionais, **sem executar cobrança nem bloquear por inadimplência no MVP**; separar decisão comercial de permissão técnica.

### 13.4 Permissões de Feature

Uma permissão só é efetiva se:

```txt
usuário autenticado
AND possui acesso ao tenant (membership ativa)
AND produto pertence ao tenant
AND feature ativa para o produto
AND role permite a ação
AND recurso pertence ao produto/tenant
```

### 13.5 Modelo de Banco (Features)

Tabelas: `feature_catalog`, `feature_dependencies`, `product_features`, `feature_permissions`, `product_feature_permissions`. `config`/`limits`/`billing_metadata` podem começar como JSONB. Chaves de feature são imutáveis após publicação. Soft delete preferido. Auditar ativação, suspensão, alteração de limites e permissões. Índices por `product_id`, `feature_id`, `key`, `status`.

### 13.6 Serviços e Endpoints

Serviços: `FeatureCatalogService`, `ProductFeatureService`, `FeatureDependencyResolver`, `FeaturePermissionEvaluator`, `FeatureLimitEvaluator`, `FeatureContractAssembler`.

Endpoints administrativos (exemplos):

```txt
GET   /products/{productId}/features
POST  /products/{productId}/features/{featureKey}/enable
POST  /products/{productId}/features/{featureKey}/disable
PATCH /products/{productId}/features/{featureKey}/config
PATCH /products/{productId}/features/{featureKey}/limits
GET   /features/catalog
POST  /features/catalog
PATCH /features/catalog/{featureKey}
```

> Nenhum domínio do Aegis deve assumir que uma feature existe para um produto apenas porque o código do módulo existe.

## 14. IAM e Roles (Keycloak)

Decisão (ADR-0005): **Keycloak autentica; Aegis autoriza por tenant.**

- **Keycloak** (realm único no MVP): autenticação, usuários globais, credenciais, sessões, tokens, roles globais, login social futuro.
- **Aegis** (banco próprio): tenants, memberships, product memberships, convites, roles de tenant/produto, permissões efetivas, audit logs de negócio.
- Tokens do Keycloak carregam identidade e roles globais, **não** a autorização tenant-specific final — o backend valida membership ativa, tenant ativo e permissões efetivas.
- Realm por tenant foi rejeitado no MVP (provisionamento complexo, usuário multi-tenant difícil), reservado para enterprise futuro.

---

# Parte III — Conteúdo

## 15. CMS Core: Pages, Sections e Blocks

Hierarquia de composição de conteúdo:

```txt
Product
└── Page
    └── Section
        └── Block
```

- **Page** — unidade navegável (slug, status editorial, SEO, locale, sections).
- **Section** — agrupamento estrutural dentro da página (layout, spacing).
- **Block** — unidade de composição com tipo e dados (ADR sobre Blocks: estruturam layout/composição).
- **Markdown é conteúdo, não layout** — Markdown carrega texto; Blocks carregam estrutura visual.

O editor administrativo (MVP) mostra página, seções, blocos, campos, status e preview textual/estrutural. **Preview visual completo é evolução futura.**

### Catálogo inicial de Blocks

HeroBlock, RichTextBlock, MarkdownBlock, GalleryBlock, TimelineBlock, CTASectionBlock, FAQBlock, ContactBlock, EventListBlock, CardListBlock, TwoColumnBlock.

## 16. Content Types

Um **Content Type** é a definição de um tipo de conteúdo estruturado (além de page/section/block), com schema, fields, workflow, SEO, revision, translation, permissions, relations e analytics.

**Estrutura base de um Content Type:** Schema, Fields, Workflow editorial, SEO, Revision, Translation, Permissions, Relations, Analytics.

### Catálogo Global de Content Types (consolidado dos 6 produtos)

| Domínio | Content Types |
|---|---|
| Núcleo CMS | Page, Section, Block, Navigation, SEO Metadata, Asset, FormDefinition |
| Música/Eventos | Event, Gallery, Service, Testimonial, QuoteRequest |
| Portfólio | Project, CaseStudy, ProfessionalExperience, Skill, Download, Article |
| Biblioteca (Loki) | Manifesto, Poem, Reflection, Excerpt, Book, Playlist, MusicReference |
| RH (Conecta) | JobPosting, CandidateSubmission, Lead, CompanyProfile |
| Wiki (WikiDev) | KnowledgeCategory, KnowledgeTopic, KnowledgeArticle, ContentSuggestion, ContributorApplication, BugReport |

Cada Content Type expõe um **contrato canônico** (ver Parte V e `docs/contracts/`). Há um mapa de relacionamentos entre tipos, suportando conteúdo relacionado, taxonomias e knowledge graph futuro.

## 17. Assets

Asset é qualquer mídia/arquivo gerenciado: imagens, vídeos, áudio, documentos, PDFs, downloads, currículos, capas, anexos.

- **Lifecycle:** upload → processamento (variações de imagem) → metadados → uso → versionamento → arquivamento.
- **Tipos:** Image, Video, Audio, Document, PDF, Download, Resume, Book, Cover, Gallery, Attachment.
- **Storage tenant-scoped:** `tenants/{tenantId}/products/{productId}/assets/{assetId}/{filename}`. O backend nunca confia só no caminho — valida o registro no banco e o `tenantId`.
- Metadados obrigatórios; SEO de assets (alt, title); permissões de upload/delete; assets sensíveis (currículos) seguem LGPD; versionamento e relações; analytics e audit de assets.

## 18. Forms

Formulários são parte do domínio do Aegis (não exceções espalhadas em páginas). Um **Form Type** define schema, fields, validação, workflow, notifications, integrations, analytics, LGPD e permissions.

### Catálogo de Form Types

Contact, Quote Request, Corporate Lead, Job Application, Talent Pool, Contributor Application, Bug Report, Content Suggestion, Newsletter Subscription, Donation, Book Interest, Recruiter Contact.

- **Validation Engine** e **Workflow Engine** próprios; catálogo de campos reutilizável.
- **Notification Engine** dispara e-mail/Telegram/in-app; **integração Telegram** para alertas operacionais (orçamentos, candidaturas, bug reports).
- **LGPD e consentimento** explícitos; submissões carregam `tenantId + productId + formId`; exportação exige permissão forte; retenção configurável.

## 19. Traduções e Internacionalização (i18n)

Tradução no Aegis cobre páginas, content types, blocks, navegação, formulários, SEO e assets.

- Cada produto tem **idioma padrão**; demais idiomas são traduções com **fallback** transparente para o backend e explícito para SEO.
- **Translation Entry** e **Translation Registry** versionáveis; integrados a workflow, revisões, analytics e busca.
- URL strategy por idioma; traduções afetam SEO (hreflang) e busca por locale.
- Casos reais: CMSS (pt-BR), produtos BYOP (pt-BR + en + es).

## 20. SEO

SEO é **parte do conteúdo** e é tenant-owned. Inclui: title, description, canonical, robots, sitemap, Open Graph, schema.org, redirects, slugs públicos, hreflang.

- SEO por Content Type e por Page; sitemap gerado por produto e tenant; redirects pertencem a tenant/produto; alterações geram audit log.
- Slugs únicos dentro do produto/escopo definido (não globalmente sem necessidade).

## 21. Navegação

Navegação é estrutura de informação e descoberta. Entidades: `Navigation` e `Navigation Item`.

- Tipos: menu principal, footer, contextual, relacionada, breadcrumbs, mobile.
- **Menus dinâmicos** montados por tenant ativo, role, produtos e features ativas; multilíngue.
- Navegação integra-se a busca, content types, traduções, SEO, analytics, auditoria e revisões.

---

# Parte IV — Operação

## 22. Revisões, Versionamento e Histórico

Uma **revisão** é um registro versionado do estado de um recurso editorial.

- Estratégia MVP: **snapshots completos** (ADR: revisions por snapshot).
- Entidades com revisão: Pages, Blocks, Content Types, Traduções, SEO, Navegação, Formulários, Assets.
- Entidades sem revisão: logs de auditoria (imutáveis), eventos analíticos, dados efêmeros.
- Fluxo editorial: `Draft → Review → Approved → Published → Archived`. Publicação, rollback e compare engine suportados. Publicação programada é futuro.
- API pública entrega apenas conteúdo **publicado**; drafts e revisões não aparecem.

## 23. Auditoria, Rastreabilidade e Governança de Dados

Auditoria registra quem fez o quê, quando, em qual tenant, com qual resultado. **Audit logs são imutáveis** (ADR: audit logs imutáveis).

- **Audit Event:** actor, action, resource, result, context (correlation IDs), tenantId, timestamp.
- Auditar: segurança, conteúdo, memberships, assets, formulários, configurações, integrações.
- Não auditar: leitura trivial sem sensibilidade, dados efêmeros, segredos em claro.
- LGPD: classificação, finalidade, retenção, exportação e anonimização aplicáveis por tenant.

## 24. Notificações e Comunicação

Notificação é evento comunicado a um destinatário por um canal.

- **Canais:** in-app, e-mail, **Telegram**; preferências por usuário/tenant.
- Notification Event Registry, templates, status, retry strategy, fila.
- Integra-se a memberships, comentários, sugestões, formulários, workflow editorial, auditoria, analytics e jobs.

## 25. Telegram (Comunicação Operacional)

Telegram é **canal/provider operacional, não domínio** (ADR: Telegram como canal). Pode ser substituído por Slack, Discord ou e-mail sem quebrar o domínio.

- Usos: novo orçamento, nova candidatura, novo bug report, alertas operacionais.
- Configuração por tenant; segredos tenant-scoped, criptografados e auditados.

## 26. Comentários, Sugestões e Colaboração

Sistemas colaborativos do ecossistema (especialmente WikiDev e Loki):

- **Comentários/Discussões:** recursos comentáveis, threads, moderação (pré/pós), anônimos vs. autenticados, denúncias, curtidas/reações/menções (futuro). Comentário ≠ Fórum (fórum é evolução para comunidades).
- **Sugestões/Ideias:** backlog colaborativo rastreável (categorias, status, workflow, votação, priorização), relacionado a conteúdo e bug reports.

## 27. Busca, Descoberta e Recuperação

- **MVP:** Full Text Search nativo do **PostgreSQL**.
- **Futuro:** motor de busca dedicado e busca semântica.
- **Search Document** indexa recursos por tenant, produto, content type, categoria, tags, locale e status. Busca administrativa vs. pública; busca respeita permissões; autocomplete; conteúdo relacionado; relevância.

## 28. Analytics

Analytics é tenant-owned e por produto.

- Eventos carregam `tenantId` para dashboards/retenção/exportação/cobrança futura por tenant.
- Respeita privacidade (LGPD); agregações agrupadas por tenant; cache de dashboard tenant-scoped.
- Nenhum usuário soma/compara/exporta analytics de tenants aos quais não pertence (salvo roles globais de operação interna auditadas).

---

# Parte V — Plataforma

## 29. Estrutura REST Oficial do MVP (ADR-0006)

REST é a API oficial do MVP, versionada (`/api/v1`), documentada via **OpenAPI**.

### 29.1 Convenções gerais

- Recursos no plural; verbos HTTP semânticos; status codes padronizados.
- Respostas e **erros padronizados** (Error Contract); paginação canônica (Pagination Contract).
- Política de breaking changes; testes de contrato.

### 29.2 API Pública vs. Administrativa

**API Pública** (frontends externos, somente conteúdo publicado):

```txt
GET  /public/products/{productSlug}/contract
GET  /public/products/{productSlug}/pages/{slug}
GET  /public/products/{productSlug}/content-types/{type}
GET  /public/products/{productSlug}/assets/{assetId}
POST /public/products/{productSlug}/analytics/events
POST /public/products/{productSlug}/forms/{formId}/submit
```

A API pública resolve tenant por domínio/slug/produto/chave pública antes de buscar dados — permanece tenant-aware mesmo para visitantes anônimos. Drafts e revisões nunca aparecem.

**API Administrativa** (sempre tenant-scoped):

```txt
/products, /products/{id}, /products/{id}/features, /products/{id}/users,
/products/{id}/pages, /products/{id}/assets, /products/{id}/forms,
/products/{id}/seo, /products/{id}/analytics, /products/{id}/content-types
```

Toda rota administrativa valida: usuário autenticado, **tenant**, **membership**, **role**, **produto**, **feature**, **ação** e status do recurso. Consultas por id incluem `tenantId`; recursos fora do tenant retornam `404`.

### 29.3 Regras obrigatórias de backend (segurança multi-tenant)

1. Validar `tenantId` (nunca confiar no enviado isolado pelo cliente; validar contra membership).
2. Validar que `productId` pertence ao `tenantId`.
3. Validar membership ativa.
4. Validar role/permissão para a ação + feature ativa.

> Cliente pode informar intenção de contexto. O backend decide se o contexto é válido.

## 30. GraphQL Futuro (ADR-0007)

GraphQL é **camada futura**, não MVP. REST permanece oficial. Quando adotado: conviver com REST, respeitar tenant isolation e permissões, usar persisted queries e limites de complexidade, sem duplicar a fonte de verdade dos contratos. Federation e API Gateway são evoluções posteriores.

## 31. Contratos JSON Canônicos (ADR-0002, ADR-0008)

O contrato é a fronteira estável frontend/backend. **Frontend consome contratos, não entidades internas.**

- **Envelopes:** Metadata, Versioning, Error, Pagination, Collection.
- **Contratos por recurso:** Product, Page, Section, Block, Navigation, ContentType, Form, Asset, Comment, Suggestion, Notification, SearchResult, Translation, Revision, Audit, Analytics.
- Validação runtime (JSON Schema); versionamento explícito; backward compatibility; schema evolution; contract registry.
- Contrato público não expõe entidade interna, tem owner, versão e exemplo. Frontend interpreta features, não infere por categoria.

Detalhes e schemas vivem em `docs/contracts/` e `docs/api/`.

## 32. Cache, Performance e Distribuição

- Estratégia: **Caffeine** (in-memory) no início; **Redis** quando necessário (ADR: cache evolutivo).
- Toda chave de cache tenant-owned inclui `tenantId` (client-side e server-side).
- Cache de contratos públicos; invalidação ao alterar conteúdo/feature/limite/permissão.

## 33. Jobs e Processamento Assíncrono

- Jobs para processamento de imagens, notificações, agregações de analytics, indexação de busca, retenção/anonimização.
- Todo job tenant-owned recebe `tenantId` explícito ou itera por tenants com isolamento por lote; idempotência; observabilidade.

## 34. Observabilidade

Logs, métricas, traces (correlation IDs), alertas, dashboards, retenção e cardinalidade. Logs incluem tenant quando há contexto e nunca vazam dados sensíveis. Observabilidade tem owner e padrões (`docs/operations/`).

---

# Parte VI — Arquitetura

## 35. Banco de Dados (ADR-0010)

**PostgreSQL** é o banco inicial. Modelo relacional profundo + **JSONB com uso controlado** (config, limits, metadata, payloads — nunca substituto de domínio).

- Migrations com **Flyway** (ADR: Flyway); migration aplicada não é editada; mudanças destrutivas exigem plano.
- Toda tabela tenant-owned com `tenantId`; índices compostos com `tenantId`; índices seguem consultas reais; soft delete para relações históricas.
- Tabelas-núcleo: `tenants`, `tenant_memberships`, `tenant_settings`, `tenant_invitations`, `tenant_audit_logs`, `products`, `feature_catalog`, `product_features`, e tabelas de conteúdo/assets/forms/seo/analytics.

## 36. Segurança e Modelo de Ameaças

Vazamento cross-tenant é **incidente severo**. Vetores tratados: tenant spoofing, horizontal privilege escalation, acesso cruzado, consulta indevida, upload indevido, analytics indevido.

Mitigações obrigatórias: `tenantId` em todas as tabelas tenant-owned; índices/constraints compostos; resolução centralizada de tenant; services/repositories que exigem `tenantId`; validação de membership; testes de isolamento (fixtures com dois tenants); cache e storage tenant-scoped; erros que não revelam recursos de outro tenant (`404`); secrets criptografados e rotacionáveis; rate limiting; headers de segurança.

Decisões de segurança que alteram autenticação, autorização, secrets, uploads, rate limit, tenant isolation, deploy pipeline, backup ou logging sensível **exigem ADR**.

## 37. Escalabilidade

- Crescimento progressivo dentro de Shared Schema; cache evolutivo (Caffeine→Redis); jobs assíncronos; busca evoluível (FTS→motor dedicado).
- Banco/schema por tenant e GraphQL/federation são evoluções via ADR conforme demanda real.

## 38. LGPD e Governança de Dados

- Classificação, finalidade, retenção, consentimento, exportação e anonimização por tenant.
- Dados sensíveis: submissões de formulário (currículos, contatos), assets sensíveis, comentários, sugestões.
- Retenção configurável (`submissionRetentionDays`, `analytics.retentionDays`); anonimização de usuários removidos; atendimento a solicitações de exportação.
- LGPD pode **bloquear feature**; mudanças que envolvem dados pessoais exigem governança.

## 39. Deploy, Ambientes e Entrega Contínua

> Deploy é processo controlado de entrega, validação, ativação e recuperação — não cópia de arquivos.

- Ambientes: `local → development → staging → production` (promotion).
- Distinções: Build (código→artefato), Release (versão planejada), Deploy (ativa em ambiente), Rollback, Migration.
- **Docker Compose** como deploy inicial (ADR: Docker Compose inicial); **Genesis Lab** como ambiente inicial.
- Deploy repetível, auditável, observável, seguro, reversível e documentado; health checks e migrations no deploy.
- **Entrega do frontend:** o Spring Boot serve a SPA na mesma origem e no mesmo deploy (ver Seção transversal "Estratégia Oficial de Frontend" e ADR-0009).

## 40. Backup e Disaster Recovery

- Política de backup com retenção; restore testado; RPO/RTO definidos.
- Disaster Recovery: continuidade operacional e sobrevivência da plataforma.
- Backup considera tenant isolation e dados sensíveis (`docs/operations/`).

## 41. Stack e Arquitetura do Backend

Stack recomendada: Java, Spring Boot, Spring Security, Keycloak, PostgreSQL, Flyway, OpenAPI, Docker, JUnit, Mockito, Bean Validation, Testcontainers (futuro).

Arquitetura modular por domínio:

```txt
core | identity | products | content | assets | forms | seo | analytics
modules/rh | modules/wiki | modules/library | modules/portfolio | modules/music
```

Camadas internas: controller, application/service, domain, repository, integration, config, security, dto, mapper, exception.

---

# Parte VII — Governança

## 42. Governança Arquitetural

> Arquitetura sem governança vira opinião. Governança transforma decisões em patrimônio técnico.

- Decisões importantes devem sobreviver à memória de quem as tomou.
- Toda decisão arquitetural relevante possui contexto, motivação e consequência.

Objetivos: preservar decisões, evitar retrabalho/contradições, orientar humanos e agentes de IA, manter coerência, reduzir dívida técnica, impedir arquitetura paralela, tornar a evolução rastreável.

## 43. ADRs

ADR (Architecture Decision Record) é o registro formal de uma decisão arquitetural relevante. Responde: contexto, decisão, motivação, alternativas, consequências e ponto de revisão.

**Template oficial:** Status, Contexto, Decisão, Consequências, Alternativas Consideradas, Impactos, Links Relacionados.

**Status:** `PROPOSED`, `ACCEPTED`, `SUPERSEDED`, `REJECTED`, `DEPRECATED`.

**Numeração:** `ADR-NNNN`, nunca reutilizada; ADR removido vira `REJECTED`/`DEPRECATED`/`SUPERSEDED`, não desaparece. Localização: `docs/adr/ADR-NNNN-slug.md`.

**Quando criar:** mudança de banco, autenticação, arquitetura, contrato, provider, adoção/remoção de tecnologia, breaking change, modelo multi-tenant, deploy, segurança, versionamento, fronteira entre módulos.

**Quando não criar:** ajuste visual, typo, refactor local, bugfix sem decisão, rename irrelevante, alteração temporária.

### ADRs V1 (catálogo em `docs/adr/`)

| ADR | Decisão | Status |
|---|---|---|
| ADR-0001 | Product First | ACCEPTED |
| ADR-0002 | Contract First | ACCEPTED |
| ADR-0003 | CMS First | ACCEPTED |
| ADR-0004 | Shared Database, Shared Schema, TenantId obrigatório | ACCEPTED |
| ADR-0005 | Keycloak como IAM | ACCEPTED |
| ADR-0006 | REST First | ACCEPTED |
| ADR-0007 | GraphQL Future | ACCEPTED |
| ADR-0008 | JSON Contracts | ACCEPTED |
| ADR-0009 | SPA servida pelo Spring Boot | ACCEPTED |
| ADR-0010 | PostgreSQL como banco inicial | ACCEPTED |

Decisões adicionais registradas conceitualmente (a formalizar em ADRs futuros): JSONB com uso controlado, Markdown não é layout, Blocks como estrutura, Revisions por snapshot, Audit logs imutáveis, Telegram como canal, SaaS Ready não SaaS Enabled, Caffeine/Redis evolutivo, Flyway, Docker Compose inicial, Genesis Lab.

## 44. Governança de Contratos, APIs, Módulos e Features

- **Contratos:** owner, versão, exemplo, validação; breaking change exige versão e revisão de compatibilidade; não expõem entidade interna.
- **APIs:** OpenAPI para REST; versionamento `/api/v1`; depreciação documentada; respostas/erros padronizados; testes de contrato.
- **Módulos:** criar quando há domínio/owner/contratos/dados próprios; promover a core quando vários produtos dependem; core não cresce por conveniência.
- **Features:** existem no Feature Catalog com key, descrição, owner, dependências, status, impacto em UI/API/billing e estratégia de desativação.

## 45. Governança de IA

Agentes de IA atuam **dentro** da arquitetura, não criam arquitetura paralela:

- Consultar o Documento Mestre primeiro.
- Não alterar contratos sem registrar decisão; não adotar/remover tecnologia sem justificativa registrada.
- Preservar product-first, tenant isolation, contract-first e REST-first (MVP); manter GraphQL como futuro até ADR contrário.
- Sempre proteger LGPD e segurança; evitar refatoração ampla sem motivação.

## 46. Fonte da Verdade e Estrutura Documental

```txt
docs/
├── AEGIS_DOCUMENTO_MESTRE_V1.md   # constituição do produto
├── adr/         # decisões formais
├── contracts/   # contratos JSON canônicos
├── api/         # REST/OpenAPI e GraphQL futuro
├── database/    # schema, migrations, modelagem
├── deploy/      # ambientes, build, entrega
├── modules/     # documentação por módulo
├── operations/  # observabilidade, jobs, backup, DR, runbooks
├── security/    # ameaças, isolamento, LGPD, políticas
├── ux/          # diretrizes de Eirene
└── governance/  # processo de governança
```

O Documento Mestre é a constituição (não README, não backlog, não doc de endpoint). Versiona por V1/V2; ADRs imutáveis; revisão mensal durante desenvolvimento ativo e obrigatória após incidentes críticos/breaking changes.

Matriz de classificação de decisões: `LOW` / `MEDIUM` / `HIGH IMPACT` / `STRATEGIC` / `IRREVERSIBLE` — HIGH, STRATEGIC e IRREVERSIBLE exigem ADR.

---

# Parte VIII — Roadmap

## 47. MVP e Fases

| Fase | Escopo |
|---|---|
| **Fase 0 — Fundação** | Setup Spring Boot, Keycloak, PostgreSQL, Docker Compose; autenticação; produtos; usuários; roles; features; tenants/memberships; audit básico; SPA servida pelo backend. |
| **Fase 1 — CMS Core** | Páginas, seções, blocos, assets, SEO, workflow editorial, contrato público, revisões. |
| **Fase 2 — Formulários e Analytics** | Form builder, submissions, Telegram, eventos de analytics, dashboard inicial. |
| **Fase 3 — Módulos Reais** | Institucional/CMSS, Maestro Beton, Alexandre Dev, Conecta Talentos. |
| **Fase 4 — Biblioteca e Wiki** | Loki, WikiDev, relações, comentários, sugestões, conhecimento conectado, busca. |
| **Fase 5 — SaaS Futuro** | Planos, assinaturas, loja, LMS, CRM, comunidade. |

## 48. Evolução e SaaS

SaaS Ready, não SaaS Enabled: o MVP reserva espaço arquitetural para billing (Plan, Subscription, BillingCycle, Invoice, PaymentProvider, ProductPurchase) sem executar cobrança nem bloquear uso. Billing futuro tem o **tenant como unidade de cobrança**. Evoluções: GraphQL/federation, motor de busca dedicado, CDN para a SPA, banco/schema por tenant para enterprise — todas via ADR.

---

# Seções Transversais Obrigatórias

## A. Estratégia Oficial de Frontend e Entrega da Aplicação

> **Decisão de referência:** ADR-0009 — SPA servida pelo Spring Boot (mesma origem).

### A.1 SPA desacoplada no código

O painel administrativo do Aegis (Eirene) é uma **Single Page Application (SPA)**. Ela é **desacoplada no código**: projeto e base de código próprios, com seu próprio build e ferramental. O acoplamento é apenas de **entrega/deploy**, não de código-fonte.

A SPA consome a **API REST** do Aegis por **contratos JSON** (Parte V). Ela interpreta features, navegação e conteúdo a partir dos contratos — não infere estrutura interna do backend.

Importante: o Aegis **não renderiza páginas públicas**. A SPA é o **painel administrativo**. Os produtos públicos são frontends externos que consomem a API pública de contratos.

### A.2 Build do frontend

```txt
Frontend source → build (toolchain própria) → dist/ (assets estáticos da SPA)
```

O build gera artefatos estáticos (`index.html`, JS, CSS, assets) prontos para distribuição.

### A.3 Deploy: Spring Boot serve o SPA

O artefato `dist/` é **empacotado no artefato do backend (.jar)**. O Spring Boot serve:

- a **API REST** (`/api/v1/...`, `/public/...`);
- os **assets estáticos** da SPA;
- **fallback de rotas não-API** para o `index.html` da SPA (client-side routing).

```txt
Frontend source → dist/ ┐
                        ├─► artefato único (Spring Boot .jar / imagem Docker)
Backend source → .jar ──┘     serve API REST + SPA na MESMA ORIGEM
```

Um único deploy, uma única origem, uma única aplicação.

### A.4 Vantagens

- **Simplicidade de deploy:** um artefato, um container, um release.
- **Sem CORS:** SPA e API na mesma origem.
- **Mesma sessão/cookies:** integração de autenticação Keycloak simplificada; sem complexidade de sessão cross-origin.
- **Menor custo e menor superfície operacional:** ideal para o estágio atual (MVP, Genesis Lab).

### A.5 Limitações

- **Rebuild necessário para atualizar o frontend:** uma mudança na SPA exige rebuild e redeploy do artefato do backend.
- **Ciclo de release compartilhado:** frontend e backend compartilham o ciclo de deploy (acoplamento de entrega, não de código).
- **Escala de entrega estática limitada à aplicação:** sem CDN dedicada no início.

### A.6 Evoluções futuras

Quando houver necessidade de escala de entrega estática ou de deploy independente do frontend, a SPA pode migrar para uma **CDN/host estático separado** (com domínio próprio), mantendo o consumo de contratos por REST e resolvendo CORS e sessão de forma explícita. Essa mudança será registrada em novo ADR (substituindo/estendendo ADR-0009).

## B. Lacunas Arquiteturais Identificadas

Itens que ainda precisam ser **decididos, validados ou implementados** antes/durante o desenvolvimento. Não são contradições do modelo, mas pontos abertos que devem virar ADR/spec própria quando maduros.

1. **Detalhe do fluxo de fallback de rotas SPA** no Spring Boot (qual padrão de matching, exclusão de rotas de API e estáticos) — definir em `docs/deploy/`.
2. **Sincronização de identidade Keycloak ↔ base local** (quando criar `user` local, como tratar exclusão/desativação, claims mínimas no token) — ADR futuro.
3. **Formato concreto dos tokens de convite** (assinatura vs. armazenamento, TTL, invalidação) — spec em `docs/security/`.
4. **Estratégia de validação runtime de contratos** (biblioteca de JSON Schema, ponto de validação, versionamento de schema) — `docs/contracts/`.
5. **Política de invalidação de cache** ao publicar conteúdo/alterar feature (eventos, TTL, chaves) — `docs/operations/` quando Redis entrar.
6. **Modelo de permissões efetivas** consolidado (matriz role×ação×feature) ainda conceitual — formalizar tabela e `PermissionEvaluator`.
7. **Estratégia de processamento de imagens** (síncrono vs. job, variações geradas, storage) — `docs/operations/`.
8. **Migração FTS → motor de busca dedicado**: gatilhos e critérios não definidos — ADR futuro.
9. **Billing futuro**: apenas metadados reservados; modelo comercial real (planos, métricas, provider) não especificado — fora do MVP.
10. **Estratégia de versionamento de contratos públicos** (header vs. path, janela de compatibilidade) — `docs/api/`.
11. **Testes de isolamento multi-tenant**: definidos como obrigatórios, mas o harness/fixtures concretos ainda não existem.
12. **RPO/RTO numéricos** de backup/DR não definidos — `docs/operations/`.
13. **Self-service onboarding de tenant** (criação de conta → tenant + owner + settings + produto em transação): previsto como fluxo futuro, não implementado.
14. **Observabilidade**: stack concreta (logs/métricas/traces) não escolhida — Daedalus + ADR.

## C. Próximos Passos

### C.1 Aegis Backend

**Backlog arquitetural:**
- Formalizar os 10 ADRs (feito) e os ADRs conceituais pendentes (JSONB, Markdown, Blocks, Revisions, Audit, Telegram, Flyway, Docker, cache).
- Definir `PermissionEvaluator` e a matriz de permissões efetivas (tenant→produto→feature→ação).
- Definir contratos públicos canônicos e o contract registry (`docs/contracts/`).
- Definir sincronização Keycloak ↔ base local.

**MVP (Fase 0–2):**
- Setup Spring Boot + Spring Security + Keycloak + PostgreSQL + Flyway + Docker Compose.
- Domínios: tenants, memberships, convites, products, features, audit básico.
- CMS Core: pages/sections/blocks, assets, SEO, workflow, contrato público, revisões por snapshot.
- Forms + submissions + Telegram + analytics inicial.
- Servir a SPA pelo backend (fallback de rotas).
- Testes de isolamento multi-tenant desde o início.

**Fases seguintes:** módulos reais (3), biblioteca/wiki + colaboração + busca (4), SaaS (5).

### C.2 Eirene UX/UI

**Backlog de prototipação:**
- Design system dark-first (tokens, componentes por feature), sensação Linear/Vercel/Stripe/Notion.
- Seletor de **tenant ativo** persistente e indicador de produto em edição.

**Telas obrigatórias (MVP):** login (Keycloak); seleção de tenant/produto; dashboard global; dashboard do produto; lista e cadastro de produtos; gestão de usuários/memberships/convites; gestão de features (com dependências e limites); editor de páginas/blocos; asset manager; form builder; SEO manager; analytics; audit log; configurações.

**Fluxos:** onboarding de produto por preset de categoria; convite/aceite de membro; troca de tenant (limpa estado); ativação/desativação de feature com avisos de dependência; publicação editorial (draft→review→publish) com diferenciação clara de status.

**Design system:** menus dinâmicos por tenant/role/features; estados de bloqueio por permissão vs. recurso inexistente; preview textual/estrutural no editor (sem preview visual completo no MVP).

### C.3 Daedalus Infraestrutura

- **Docker/Docker Compose** para backend + PostgreSQL + Keycloak; empacotamento do `dist/` da SPA no artefato do backend.
- **CI/CD:** pipeline de build (frontend `dist/` → artefato backend → imagem), testes, migrations Flyway no deploy, health checks, rollback.
- **Ambientes:** local → development → staging → production; Genesis Lab como ambiente inicial.
- **Monitoramento:** escolher stack de observabilidade (logs/métricas/traces/alertas); definir retenção e cardinalidade.
- **Backup/DR:** política de backup PostgreSQL + realm Keycloak; restore testado; RPO/RTO.

### C.4 Loki Desenvolvimento

**Implementar primeiro:** fundação (auth, tenants, products, features, audit) → CMS Core → forms/analytics. Tenant isolation e contratos antes de domínios ricos.

**NÃO implementar no MVP:** billing/cobrança; GraphQL; motor de busca dedicado; banco/schema por tenant; realm por tenant; comunidades/fórum; LMS/CRM/loja; preview visual completo do editor; publicação programada; CDN separada para a SPA.

**Evitar overengineering em:** sistema de permissões (começar simples por role, evoluir); cache (Caffeine antes de Redis); busca (FTS nativo antes de Elasticsearch); JSONB (apenas para config/limits/metadata, não para fugir de modelagem); abstrações de provider (Telegram como canal trocável, sem framework de integração genérico prematuro).

---

# Documento Mestre V1 Finalizado — Resumo Executivo

## Visão

O Aegis é a **fundação administrativa do ecossistema BYOP**: uma plataforma multi-tenant, multi-produto, modular e orientada a contratos que **gerencia produtos digitais** — não páginas. Ele **administra conteúdo**; frontends externos **renderizam contratos**. O Aegis é a fonte de verdade.

Nasceu da consolidação de seis produtos reais (CMSS, Maestro Beton, Conecta Talentos, Alexandre Dev, Loki, WikiDev). Cada produto revelou um domínio; cada domínio, entidades; cada entidade, capacidades; cada capacidade, a arquitetura necessária.

## Princípios

CMS First · Product First · Contract First · Multi-Tenant · Modular · SaaS Ready (não Enabled) · REST First (GraphQL Future) · Segurança e Auditoria por padrão · Evolução sem Frankenstein.

## Pilares

- **Produto** como raiz; **Tenant** como fronteira inegociável de isolamento.
- **Features** governam capacidades reais por produto.
- **CMS Core** (pages/sections/blocks/content types) + módulos de domínio.
- **Contratos JSON** como fronteira estável; **REST/OpenAPI** como API oficial.
- **Keycloak** autentica; **Aegis** autoriza por tenant.
- **PostgreSQL + Flyway** (Shared Schema, `tenantId` obrigatório).
- **SPA servida pelo Spring Boot** na mesma origem (deploy único, sem CORS).
- **Governança por ADRs** e Documento Mestre como constituição.

## Roadmap

Fase 0 Fundação → Fase 1 CMS Core → Fase 2 Forms/Analytics → Fase 3 Módulos Reais → Fase 4 Biblioteca/Wiki/Colaboração → Fase 5 SaaS.

## Conclusão

O Aegis deve ser construído como plataforma **modular, segura, auditável, multi-produto e orientada a contratos**, nascendo pronta para crescer sem virar Frankenstein. Este documento é a fonte de verdade inicial para:

- **Aegis** arquitetar o backend;
- **Eirene** prototipar a UI/UX;
- **Daedalus** preparar a infraestrutura;
- **Zeus** validar a visão sistêmica;
- **Athena** orquestrar estratégia e requisitos;
- **Loki** executar o desenvolvimento.

Decisões importantes devem sobreviver à memória de quem as tomou. O futuro do Aegis deve ser decidido por **registros, não por lembranças**.

---

**Fim do Documento Mestre V1 (consolidado).** Documentação complementar e detalhamento por área vivem nos diretórios listados na Parte VII e em `docs/README.md`.
