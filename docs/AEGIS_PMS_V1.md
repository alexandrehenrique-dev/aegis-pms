# AEGIS — Documento Mestre de Produto (V1)

## AMPS — Aegis Master Product Specification

**Versão:** 1.0 (consolidada e expandida)
**Status:** Fonte de verdade oficial para arquitetura, UI/UX e desenvolvimento — constituição do projeto Aegis.
**Ecossistema:** BYOP — Build Your Own Path
**Responsável estratégica:** Athena
**Consumidores deste documento:** Aegis (backend), Eirene (UX/UI), Daedalus (infraestrutura), Zeus (visão sistêmica), Loki (execução) e futuros agentes do Panteão BYOP.

> **Definição central:** O Aegis **não gerencia páginas**. O Aegis gerencia **produtos digitais** — suas capacidades, conteúdos, contratos, usuários, assets, fluxos editoriais, permissões, integrações e dados de operação. Páginas são apenas uma forma de materializar um produto.
>
> O Aegis **administra conteúdo**; ele **não renderiza páginas públicas**. Frontends externos atuam como **renderizadores de contratos**. O Aegis é a **fonte de verdade**.

### Como ler este documento

O documento está dividido em **oito Partes**. A Parte I é a base conceitual; as Partes II–VI detalham produto, conteúdo, operação, plataforma e arquitetura; a Parte VII trata de governança; a Parte VIII traz o roadmap. Ao final, há quatro seções transversais obrigatórias (Frontend, Lacunas, Próximos Passos, Resumo Executivo).

Cada seção numerada (§1–§48) abre com a **definição normativa** (o "o quê" e o "porquê") e prossegue com a **especificação técnica** (o "como"): contratos JSON canônicos, modelos de banco (tabelas e campos), diagramas ASCII de fluxo e arquitetura, leis por domínio, casos de uso reais do ecossistema BYOP e endpoints REST. As leis numeradas de cada domínio são **normativas**: violá-las é um defeito de arquitetura, não uma escolha de estilo.

Documentação complementar vive em `docs/adr/`, `docs/contracts/`, `docs/api/`, `docs/database/`, `docs/deploy/`, `docs/modules/`, `docs/operations/`, `docs/security/`, `docs/ux/` e `docs/governance/` (ver `docs/README.md`). Os ADRs completos são referenciados na tabela de §43 e vivem em `docs/adr/`.

---

# Parte I — Visão e Fundamentos

## 1. Declaração Fundamental

O Aegis não deve ser tratado como um CMS tradicional. É uma **plataforma multi-produto, multi-tenant, modular e orientada a contratos**, criada para administrar produtos digitais do ecossistema BYOP e de futuros clientes.

Princípios fundadores:

- Frontends não possuem conteúdo final fixo; consomem contratos.
- O Aegis é a fonte de verdade do conteúdo e das capacidades de cada produto.
- Páginas são uma materialização de produto, não a entidade central.

### 1.1 O que o Aegis é e o que não é

| O Aegis **é** | O Aegis **não é** |
|---|---|
| Uma plataforma de administração de produtos digitais | Um gerador de sites (site builder) |
| Uma fonte de verdade de conteúdo e capacidades | Um renderizador de páginas públicas |
| Um provedor de contratos JSON versionados | Um framework de frontend |
| Um sistema multi-tenant com isolamento forte | Um banco compartilhado sem fronteiras |
| Um catálogo de features habilitáveis por produto | Um monolito com tudo ligado para todos |
| Um motor de governança editorial e auditoria | Um repositório de arquivos estáticos |

### 1.2 Diagrama de posicionamento

```txt
                        ┌──────────────────────────────────┐
   Administrador BYOP ─►│            AEGIS (core)          │
   Donos de produto  ──►│  produtos · features · conteúdo  │
                        │  contratos · auditoria · IAM     │
                        └───────────────┬──────────────────┘
                                        │ API REST (contratos JSON)
              ┌─────────────────────────┼─────────────────────────┐
              ▼                         ▼                         ▼
       Frontend CMSS            Frontend Loki            Frontend WikiDev
   (renderiza contrato)     (renderiza contrato)     (renderiza contrato)
```

## 2. Problema que o Aegis Resolve

O BYOP possui e continuará possuindo diversos produtos digitais com necessidades diferentes: sites institucionais, comerciais, portfólios, bibliotecas autorais, wikis técnicas, portais de recrutamento, blogs, landing pages, e (futuro) lojas, sistemas educacionais, comunidades e produtos SaaS.

Sem o Aegis, cada novo produto tende a nascer como um conjunto isolado de JSON, componentes específicos, regras hardcoded e contratos improvisados, gerando: duplicação de regras; acoplamento entre frontend e conteúdo; contratos frágeis; dificuldade de manutenção e evolução; falta de governança, controle editorial, versionamento, permissões por produto e histórico; dificuldade de escalar para múltiplos clientes; e risco de virar um **Frankenstein arquitetural**.

O Aegis nasce para impedir isso.

### 2.1 Sintomas do problema (antes do Aegis)

```txt
Produto A ──► JSON hardcoded ──► regras no frontend ──► sem histórico
Produto B ──► outro JSON      ──► outras regras       ──► sem auditoria
Produto C ──► copy/paste de B ──► regras divergentes  ──► drift silencioso
                                                          │
                                                          ▼
                                              FRANKENSTEIN ARQUITETURAL
```

### 2.2 Como o Aegis dissolve cada sintoma

| Sintoma | Solução do Aegis |
|---|---|
| JSON hardcoded por produto | Contratos JSON canônicos versionados (§31) |
| Regras no frontend | Backend é a fonte de verdade; frontend interpreta contrato (§29, §31) |
| Sem histórico | Revisões por snapshot em todo recurso editorial (§22) |
| Sem auditoria | Audit logs imutáveis em toda ação relevante (§23) |
| Drift entre produtos | Núcleo estável + features habilitáveis (§13) |
| Sem isolamento | Multi-tenant com `tenantId` obrigatório (§9, §35, §36) |

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

### 3.1 Jornada ponta a ponta (caso CMSS)

```txt
1. SUPER_ADMIN cria tenant "cmss"                          → tenant ACTIVE
2. SUPER_ADMIN convida owner (regente) como TENANT_OWNER   → membership INVITED→ACTIVE
3. TENANT_OWNER cria produto "site-cmss" categoria=Institucional
4. Preset de categoria habilita features: pages, seo, gallery, events, contact
5. TENANT_OWNER convida editor como PRODUCT_EDITOR
6. Editor cria página "home" → seções → blocos (Hero, Gallery, EventList)
7. Editor envia para Review → Approved → Published
8. Aegis monta contrato público do produto
9. Frontend CMSS consome GET /public/products/site-cmss/contract
10. Site público reflete o conteúdo publicado
```

## 4. Filosofia

- **CMS First** — conteúdo público é editável pelo Aegis, não hardcoded no frontend.
- **Product First** — a entidade raiz é o Produto.
- **Contract First** — toda entrega pública é baseada em contratos estáveis.
- **Multi-Tenant** — isolamento lógico forte desde o início.
- **Modular** — núcleo estável + módulos habilitáveis por produto.
- **SaaS Ready, não SaaS Enabled** — espaço arquitetural reservado para billing, sem implementá-lo no MVP.
- **REST First, GraphQL Future** — REST é a API oficial do MVP.
- **Evolução sem Frankenstein** — crescer sem perder coerência.

### 4.1 Tensões filosóficas e como resolvê-las

| Tensão | Decisão do Aegis |
|---|---|
| Flexibilidade vs. governança | Governança vence no core; flexibilidade vive em features e JSONB controlado |
| Velocidade de MVP vs. preparo para SaaS | SaaS Ready (espaço reservado), não SaaS Enabled (sem cobrança) |
| Simplicidade vs. multi-tenant | Multi-tenant é inegociável; simplicidade vive na ausência de banco-por-tenant |
| REST simples vs. GraphQL poderoso | REST First no MVP; GraphQL Future só via ADR |

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

### 5.1 O eixo de autorização canônico

Toda decisão de acesso atravessa, em ordem, o mesmo eixo. Esse eixo aparece, literal ou implicitamente, em §9, §10, §13, §14, §29 e §36.

```txt
Usuário (autenticado pelo Keycloak)
  └─► Tenant (membership ativa?)
        └─► Produto (pertence ao tenant?)
              └─► Feature (ativa para o produto?)
                    └─► Role (permite a ação?)
                          └─► Recurso (pertence ao produto/tenant?)
                                └─► Escopo/Ownership (se aplicável)
                                      └─► PERMITIDO / NEGADO (404 cross-tenant)
```

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

### 6.1 Matriz produto × feature (origem da modelagem)

| Feature / Produto | CMSS | Maestro | Conecta | Alex Dev | Loki | WikiDev |
|---|:--:|:--:|:--:|:--:|:--:|:--:|
| pages/sections/blocks | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ |
| seo | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ |
| gallery | ✔ | ✔ | ○ | ✔ | ○ | ○ |
| events | ✔ | ✔ | ○ | ○ | ○ | ○ |
| forms (contact/quote) | ✔ | ✔ | ✔ | ✔ | ○ | ○ |
| job postings | ○ | ○ | ✔ | ○ | ○ | ○ |
| candidate submissions | ○ | ○ | ✔ | ○ | ○ | ○ |
| projects/case studies | ○ | ○ | ○ | ✔ | ○ | ○ |
| library (manifesto/poem) | ○ | ○ | ○ | ○ | ✔ | ○ |
| knowledge (wiki) | ○ | ○ | ○ | ○ | ○ | ✔ |
| comments/suggestions | ○ | ○ | ○ | ○ | ○ | ✔ |
| telegram alerts | ○ | ✔ | ✔ | ○ | ○ | ✔ |
| i18n multilíngue | ○ | ○ | ○ | ✔ | ✔ | ✔ |

(✔ = essencial · ○ = opcional/futuro)

## 7. Leis do Aegis

As leis abaixo são a **constituição operacional**. Cada domínio (Partes II–VI) deriva suas próprias leis numeradas destas dezessete leis-raiz.

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

### 7.1 Rastreabilidade lei-raiz → domínio

| Lei-raiz | Domínios que a detalham |
|---|---|
| 1, 5 | §8 Produto, §9 Tenants, §10 Memberships |
| 2, 14 | §29 REST, §31 Contratos JSON |
| 3 | §22 Revisões |
| 4 | §23 Auditoria |
| 6, 7 | §13 Features, §14 IAM, §36 Segurança |
| 8, 9 | §15 CMS Core, §16 Content Types |
| 10 | §17 Assets |
| 11 | §20 SEO |
| 12 | §28 Analytics, §38 LGPD |
| 13, 17 | §37 Escalabilidade, §42 Governança, §48 SaaS |
| 15 | §9 Tenants, §35 Banco, §36 Segurança |
| 16 | §29 REST, §30 GraphQL |

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

### 8.1 Modelo de banco — `products`

```sql
CREATE TABLE products (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    slug            VARCHAR(120) NOT NULL,
    name            VARCHAR(200) NOT NULL,
    category        VARCHAR(40)  NOT NULL,   -- INSTITUTIONAL, LANDING, BLOG, PORTFOLIO, HR, WIKI, LIBRARY, COMMERCIAL...
    status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT', -- DRAFT, ACTIVE, SUSPENDED, ARCHIVED, DELETED
    default_locale  VARCHAR(10)  NOT NULL DEFAULT 'pt-BR',
    public_key      VARCHAR(64),             -- resolução de tenant para API pública (opcional)
    settings        JSONB        NOT NULL DEFAULT '{}'::jsonb,
    metadata        JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_by      UUID         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    archived_at     TIMESTAMPTZ,
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT uq_product_slug_per_tenant UNIQUE (tenant_id, slug)
);

CREATE INDEX idx_products_tenant        ON products (tenant_id);
CREATE INDEX idx_products_tenant_status ON products (tenant_id, status);
CREATE INDEX idx_products_category      ON products (tenant_id, category);
```

### 8.2 Ciclo de vida do produto

```txt
        create
DRAFT ─────────────► ACTIVE ─────────► SUSPENDED
  │                    │  ▲                 │
  │                    │  └──── reactivate ─┘
  │                    │ archive
  │                    ▼
  └─────────────────► ARCHIVED ──── purge policy ──► DELETED (soft, deleted_at)
```

- `DRAFT`: criado, sem contrato público; visível só no painel.
- `ACTIVE`: contrato público disponível; edição plena.
- `SUSPENDED`: contrato público congela na última publicação; edição bloqueada (ex.: pendência operacional).
- `ARCHIVED`: removido de menus; contrato público desativado; conteúdo preservado.
- `DELETED`: soft delete; sujeito a política de retenção antes de purga física.

### 8.3 Contrato JSON — Product (público resumido)

```json
{
  "data": {
    "productId": "prod_site_cmss",
    "slug": "site-cmss",
    "name": "Corporação Musical São Sebastião",
    "category": "INSTITUTIONAL",
    "defaultLocale": "pt-BR",
    "locales": ["pt-BR"],
    "features": ["pages", "seo", "gallery", "events", "contact"],
    "navigation": { "$ref": "/public/products/site-cmss/navigation" },
    "pages": [
      { "slug": "home", "title": "Início" },
      { "slug": "eventos", "title": "Eventos" }
    ]
  },
  "metadata": {
    "contractVersion": "1.0.0",
    "generatedAt": "2026-06-15T12:00:00Z",
    "tenant": "cmss"
  },
  "links": {
    "self": "/public/products/site-cmss/contract"
  }
}
```

### 8.4 Endpoints administrativos do produto

```txt
GET    /api/v1/admin/products                 # lista (tenant-scoped)
POST   /api/v1/admin/products                 # cria produto
GET    /api/v1/admin/products/{productId}     # detalhe
PATCH  /api/v1/admin/products/{productId}     # atualiza identidade/settings
POST   /api/v1/admin/products/{productId}/archive
POST   /api/v1/admin/products/{productId}/suspend
POST   /api/v1/admin/products/{productId}/activate
DELETE /api/v1/admin/products/{productId}     # soft delete
```

### 8.5 Leis do Produto

1. Produto é a raiz operacional: nenhum recurso editável existe sem `productId`.
2. Todo produto pertence a exatamente um tenant, imutável após criação.
3. `slug` é único por tenant, não globalmente.
4. Categoria orienta o preset inicial de features, mas não trava alterações posteriores.
5. Produto nunca é apagado fisicamente sem política de retenção (§38, §40).
6. Mudança de status de produto gera audit log (§23).
7. Contrato público só existe para produto `ACTIVE`.
8. Desativar produto não apaga conteúdo; congela a projeção pública.

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

#### 9.2.1 Comparação das três estratégias

```txt
                 │ Isolamento │ Custo  │ Migrations │ Onboarding │ MVP?
─────────────────┼────────────┼────────┼────────────┼────────────┼──────
Shared Schema    │   lógico   │ baixo  │   1 set    │  instantâneo│  SIM
Schema/tenant    │   médio    │ médio  │  N sets    │   minutos  │ futuro
DB/tenant        │   físico   │  alto  │ N bancos   │   horas    │ enterprise
```

#### 9.2.2 Como o `tenantId` é resolvido

```txt
API Administrativa:
  token (Keycloak) ─► userId
                       └─► header X-Tenant-Id (intenção do cliente)
                             └─► TenantResolver valida membership ativa(userId, tenantId)
                                   └─► contexto seguro: { userId, tenantId, role }

API Pública:
  domínio / slug / publicKey ─► PublicTenantResolver ─► tenantId (read-only, conteúdo publicado)
```

### 9.3 Estrutura, Ownership e Roles de Tenant

Estrutura do tenant: Products, Users, Memberships, Features, Assets, Forms, Analytics, Integrations, Settings, Billing futuro.

Campos do Tenant: id, name, slug, status (`active`, `suspended`, `archived`, `pending_setup`), type, ownerUserId, metadata, createdAt, updatedAt, archivedAt.

**Três níveis de autorização:**

- **Roles globais** (plataforma): `SUPER_ADMIN` (raro, auditado).
- **Roles de tenant**: `TENANT_OWNER`, `TENANT_ADMIN`, `TENANT_MANAGER`, `TENANT_VIEWER`.
- **Roles de produto**: `PRODUCT_OWNER`, `PRODUCT_MANAGER`, `PRODUCT_EDITOR`, `PRODUCT_VIEWER`.

Regras: role global não substitui membership de tenant para fluxos comuns; role de tenant não autoriza automaticamente todo detalhe de produto; **role de produto nunca excede a autoridade da role de tenant**.

Ownership: todo tenant ativo tem ao menos um `TENANT_OWNER`; o último owner ativo não pode ser removido/rebaixado sem transferência; toda mudança de owner/role gera audit log; `SUPER_ADMIN` pode operar tenants para suporte, mas com motivo registrado e auditado.

### 9.4 Modelo de banco — `tenants` e correlatas

```sql
CREATE TABLE tenants (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug           VARCHAR(60)  NOT NULL UNIQUE,
    name           VARCHAR(200) NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING_SETUP', -- PENDING_SETUP, ACTIVE, SUSPENDED, ARCHIVED
    type           VARCHAR(40)  NOT NULL DEFAULT 'INTERNAL',      -- INTERNAL, CLIENT, PARTNER
    owner_user_id  UUID         NOT NULL,
    metadata       JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    archived_at    TIMESTAMPTZ
);

CREATE TABLE tenant_settings (
    tenant_id                UUID PRIMARY KEY REFERENCES tenants(id),
    default_locale           VARCHAR(10) NOT NULL DEFAULT 'pt-BR',
    supported_locales        JSONB       NOT NULL DEFAULT '["pt-BR"]'::jsonb,
    invitation_ttl_days      INT         NOT NULL DEFAULT 7,
    submission_retention_days INT        NOT NULL DEFAULT 365,
    analytics_retention_days INT         NOT NULL DEFAULT 365,
    features_overrides       JSONB       NOT NULL DEFAULT '{}'::jsonb,
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### 9.5 Diagrama de pertencimento

```txt
                          ┌────────────┐
                          │   USER     │  (identidade global, Keycloak)
                          └─────┬──────┘
                                │ N
                       ┌────────▼─────────┐
                       │ TENANT_MEMBERSHIP│ (role de tenant + status)
                       └────────┬─────────┘
                                │ N
                          ┌─────▼──────┐ 1        N ┌──────────┐
                          │   TENANT   ├────────────►│ PRODUCT  │
                          └─────┬──────┘             └────┬─────┘
                                │                          │ N
                        settings/features          ┌───────▼────────────┐
                                                    │ PRODUCT_MEMBERSHIP │
                                                    └────────────────────┘
```

### 9.6 Casos reais

- `byop`: tenant interno guarda-chuva; abriga produtos de operação e o próprio painel Aegis.
- `cmss`: tenant cliente, um único produto institucional.
- `loki` e `wikidev`: tenants com forte multilíngue e colaboração.

### 9.7 Leis dos Tenants

1. Tenant é a fronteira inegociável: nenhuma query tenant-owned roda sem `tenantId`.
2. Recurso de outro tenant nunca retorna `403` revelador — retorna `404`.
3. Todo tenant ativo tem pelo menos um `TENANT_OWNER` ativo.
4. O último owner não é removível sem transferência de ownership.
5. `SUPER_ADMIN` que opera um tenant deixa motivo auditado.
6. Settings, assets, segredos e analytics são sempre tenant-scoped.
7. Slug de tenant é global e imutável após ativação.
8. Suspender tenant suspende todos os seus produtos e congela contratos públicos.

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

### 10.1 Modelo de banco — memberships

```sql
CREATE TABLE tenant_memberships (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES tenants(id),
    user_id     UUID NOT NULL,
    role        VARCHAR(20) NOT NULL,  -- TENANT_OWNER, TENANT_ADMIN, TENANT_MANAGER, TENANT_VIEWER
    status      VARCHAR(20) NOT NULL DEFAULT 'INVITED', -- INVITED, ACTIVE, SUSPENDED, REMOVED
    invited_by  UUID,
    invited_at  TIMESTAMPTZ,
    accepted_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_membership UNIQUE (tenant_id, user_id)
);
CREATE INDEX idx_membership_user   ON tenant_memberships (user_id);
CREATE INDEX idx_membership_tenant ON tenant_memberships (tenant_id, status);

CREATE TABLE product_memberships (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES tenants(id),
    product_id  UUID NOT NULL REFERENCES products(id),
    user_id     UUID NOT NULL,
    role        VARCHAR(20) NOT NULL,  -- PRODUCT_OWNER, PRODUCT_MANAGER, PRODUCT_EDITOR, PRODUCT_VIEWER
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_product_membership UNIQUE (product_id, user_id)
);
CREATE INDEX idx_product_membership_user ON product_memberships (user_id);
```

### 10.2 Matriz de capacidade por role

| Ação | T_OWNER | T_ADMIN | T_MANAGER | T_VIEWER | P_OWNER | P_EDITOR | P_VIEWER |
|---|:--:|:--:|:--:|:--:|:--:|:--:|:--:|
| Gerenciar tenant settings | ✔ | ✔ | — | — | — | — | — |
| Convidar/gerir memberships tenant | ✔ | ✔ | — | — | — | — | — |
| Criar/arquivar produto | ✔ | ✔ | ✔ | — | — | — | — |
| Habilitar/desabilitar feature | ✔ | ✔ | ✔ | — | ✔* | — | — |
| Editar conteúdo (pages/blocks) | ✔ | ✔ | ✔ | — | ✔ | ✔ | — |
| Publicar conteúdo | ✔ | ✔ | ✔ | — | ✔ | — | — |
| Ler conteúdo/relatórios | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ |
| Exportar dados sensíveis (LGPD) | ✔ | ✔ | — | — | — | — | — |

(✔* = apenas features delegadas ao produto, nunca acima do tenant)

### 10.3 Avaliação de permissão (pseudocódigo)

```txt
function can(user, tenantId, productId, feature, action, resource):
    m = membership(user, tenantId)
    if m is null or m.status != ACTIVE: return DENY(404)
    if productId and product(productId).tenantId != tenantId: return DENY(404)
    if feature and not featureActive(productId, feature): return DENY(403_FEATURE_DISABLED)
    effectiveRole = min(tenantRole(m), productRole(user, productId))   # produto nunca excede tenant
    if not roleAllows(effectiveRole, action): return DENY(403)
    if resource and resource.tenantId != tenantId: return DENY(404)
    if scoped(action) and not owns(user, resource): return DENY(403)
    return ALLOW
```

### 10.4 Leis das Memberships

1. Product Role ≤ Tenant Role, sempre (clamp explícito no avaliador).
2. Membership `INVITED` não concede nenhum acesso.
3. `REMOVED` encerra o vínculo, mas a trilha de auditoria permanece.
4. Um usuário tem no máximo uma membership por tenant.
5. Rebaixar/remover o último owner exige transferência prévia.
6. Toda mudança de role/status gera audit log.
7. Suspender membership bloqueia acesso sem apagar histórico.

## 11. Convites

Operação sensível. Fluxo: admin/owner informa email e role → Aegis valida permissão → cria `TenantInvitation` + membership pendente → envia email com link seguro → usuário autentica/cria conta no Keycloak → aceita → membership vira `ACTIVE`.

- Token opaco/assinado; não expõe `tenantId`/`role`/email manipuláveis.
- Convites expiram (padrão sugerido: **7 dias**, configurável por tenant no futuro).
- Convite pode ser reenviado ou revogado; revogação invalida token e gera audit log.

`TenantInvitation`: id, tenantId, email, role, status, tokenHash, invitedByUserId, invitedAt, expiresAt, acceptedAt, revokedAt, revokedByUserId, createdAt, updatedAt.

### 11.1 Modelo de banco — `tenant_invitations`

```sql
CREATE TABLE tenant_invitations (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL REFERENCES tenants(id),
    email             VARCHAR(254) NOT NULL,
    role              VARCHAR(20)  NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING', -- PENDING, ACCEPTED, EXPIRED, REVOKED
    token_hash        VARCHAR(128) NOT NULL,  -- hash do token; o token cru nunca é persistido
    invited_by_user_id UUID NOT NULL,
    invited_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at        TIMESTAMPTZ NOT NULL,
    accepted_at       TIMESTAMPTZ,
    revoked_at        TIMESTAMPTZ,
    revoked_by_user_id UUID,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_invitation_tenant ON tenant_invitations (tenant_id, status);
CREATE INDEX idx_invitation_email  ON tenant_invitations (email);
```

### 11.2 Fluxo de convite

```txt
[Owner/Admin]
   │ POST /admin/tenants/{id}/invitations { email, role }
   ▼
[Aegis] valida permissão ─► cria invitation(PENDING) + token cru
   │ persiste apenas hash(token) ; envia email com link ?token=<cru>
   ▼
[Convidado] abre link ─► autentica/cria conta no Keycloak
   │ POST /auth/invitations/accept { token }
   ▼
[Aegis] valida hash + expiração ─► membership ACTIVE ─► invitation ACCEPTED ─► audit log
```

### 11.3 Endpoints de convite

```txt
POST   /api/v1/admin/tenants/{tenantId}/invitations          # cria
GET    /api/v1/admin/tenants/{tenantId}/invitations          # lista
POST   /api/v1/admin/tenants/{tenantId}/invitations/{id}/resend
POST   /api/v1/admin/tenants/{tenantId}/invitations/{id}/revoke
POST   /api/v1/auth/invitations/accept                       # { token }
```

### 11.4 Leis dos Convites

1. O token cru nunca é persistido — apenas seu hash.
2. O token não carrega `tenantId`/`role`/email em forma manipulável.
3. Convite expira; convite expirado não pode virar membership.
4. Revogar convite invalida o token imediatamente.
5. Aceitar convite gera audit log e ativa a membership.
6. Quem convida não pode conceder role acima da própria.

## 12. Contexto Ativo (Current Tenant)

Após login, o Aegis resolve os tenants acessíveis e exige seleção quando houver mais de um. O **Current Tenant** define o contexto operacional do painel (menus, produtos, assets, analytics, role, cache).

- Zero tenants → estado sem acesso; um → seleção automática/confirmação; muitos → seletor.
- Troca de tenant é **explícita**: limpa caches/estado do tenant anterior, recarrega permissões/menus/produtos.
- O frontend não é fonte de verdade: o tenant ativo no cliente é intenção; o backend valida token + tenant + membership + role + productId + feature em toda chamada sensível.

### 12.1 Resolução de contexto no login

```txt
login OK (Keycloak)
   └─► GET /api/v1/auth/context
         └─► memberships ativas do usuário
               ├── 0 tenants  → 403 NO_TENANT_ACCESS (tela "sem acesso")
               ├── 1 tenant   → currentTenant = único (confirmação opcional)
               └── N tenants  → seletor obrigatório (X-Tenant-Id nas próximas chamadas)
```

### 12.2 Contrato JSON — Auth Context

```json
{
  "data": {
    "userId": "user_001",
    "displayName": "Alexandre",
    "globalRoles": [],
    "tenants": [
      { "tenantId": "tenant_byop", "slug": "byop", "role": "TENANT_OWNER", "status": "ACTIVE" },
      { "tenantId": "tenant_cmss", "slug": "cmss", "role": "TENANT_ADMIN", "status": "ACTIVE" }
    ],
    "requiresTenantSelection": true
  },
  "metadata": { "generatedAt": "2026-06-15T12:00:00Z" }
}
```

### 12.3 Leis do Contexto Ativo

1. O tenant ativo do cliente é intenção; o backend sempre revalida.
2. Trocar de tenant limpa todo cache/estado do tenant anterior.
3. Sem membership ativa, não há contexto — não há acesso.
4. Toda chamada sensível reenvia `X-Tenant-Id` e é revalidada.

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

```sql
CREATE TABLE feature_catalog (
    key             VARCHAR(60) PRIMARY KEY,  -- imutável: 'pages', 'seo', 'events'...
    name            VARCHAR(120) NOT NULL,
    description     TEXT,
    module          VARCHAR(40) NOT NULL,
    type            VARCHAR(30) NOT NULL,     -- CONTENT, TRANSACTIONAL, COLLABORATION, INTEGRATION
    global_status   VARCHAR(20) NOT NULL DEFAULT 'active',
    default_limits  JSONB NOT NULL DEFAULT '{}'::jsonb,
    billing_metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    menu_impact     JSONB NOT NULL DEFAULT '{}'::jsonb,
    experimental    BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE feature_dependencies (
    feature_key     VARCHAR(60) NOT NULL REFERENCES feature_catalog(key),
    depends_on      VARCHAR(60) NOT NULL REFERENCES feature_catalog(key),
    kind            VARCHAR(20) NOT NULL,  -- REQUIRED, OPTIONAL, RECOMMENDED, INCOMPATIBLE, ALTERNATIVE
    PRIMARY KEY (feature_key, depends_on)
);

CREATE TABLE product_features (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES tenants(id),
    product_id  UUID NOT NULL REFERENCES products(id),
    feature_key VARCHAR(60) NOT NULL REFERENCES feature_catalog(key),
    status      VARCHAR(20) NOT NULL DEFAULT 'enabled',
    config      JSONB NOT NULL DEFAULT '{}'::jsonb,
    limits      JSONB NOT NULL DEFAULT '{}'::jsonb,
    source      VARCHAR(20) NOT NULL DEFAULT 'PRESET', -- PRESET, MANUAL, MIGRATION
    enabled_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_product_feature UNIQUE (product_id, feature_key)
);
CREATE INDEX idx_product_features ON product_features (product_id, status);
```

### 13.6 Resolução de dependências (diagrama)

```txt
enable("events")
   ├─ REQUIRED: "pages"  ──► ativa? sim → ok | não → bloqueia ou ativa em cascata explícita
   ├─ RECOMMENDED: "seo" ──► sugere ao usuário
   └─ INCOMPATIBLE: —    ──► nenhuma
disable("pages")
   └─ dependents: ["events", "navigation"] ──► alerta: desativar em cascata? (confirmação)
```

### 13.7 Serviços e Endpoints

Serviços: `FeatureCatalogService`, `ProductFeatureService`, `FeatureDependencyResolver`, `FeaturePermissionEvaluator`, `FeatureLimitEvaluator`, `FeatureContractAssembler`.

```txt
GET   /api/v1/admin/products/{productId}/features
POST  /api/v1/admin/products/{productId}/features/{featureKey}/enable
POST  /api/v1/admin/products/{productId}/features/{featureKey}/disable
PATCH /api/v1/admin/products/{productId}/features/{featureKey}/config
PATCH /api/v1/admin/products/{productId}/features/{featureKey}/limits
GET   /api/v1/admin/features/catalog
POST  /api/v1/admin/features/catalog
PATCH /api/v1/admin/features/catalog/{featureKey}
```

### 13.8 Contrato JSON — product feature

```json
{
  "data": {
    "featureKey": "events",
    "status": "active",
    "module": "music",
    "config": { "allowRecurring": true, "timezone": "America/Sao_Paulo" },
    "limits": { "maxEvents": 500, "maxPerMonth": 100 },
    "dependencies": { "required": ["pages"], "satisfied": true }
  },
  "metadata": { "tenant": "cmss", "productId": "prod_site_cmss" }
}
```

> Nenhum domínio do Aegis deve assumir que uma feature existe para um produto apenas porque o código do módulo existe.

### 13.9 Leis das Features

1. Feature desabilitada = invisível, inacessível, não-escrevível, ausente do contrato.
2. Key de feature é imutável após publicação no catálogo.
3. Dependência obrigatória não satisfeita bloqueia a ativação.
4. Desativação em cascata é sempre explícita e confirmada.
5. Limites são validados no backend; a UI apenas avisa.
6. Billing metadata existe, mas não bloqueia uso no MVP.
7. Ativar/desativar/alterar limites gera audit log.
8. O contrato público só lista features ativas do produto.

## 14. IAM e Roles (Keycloak)

Decisão (ADR-0005): **Keycloak autentica; Aegis autoriza por tenant.**

- **Keycloak** (realm único no MVP): autenticação, usuários globais, credenciais, sessões, tokens, roles globais, login social futuro.
- **Aegis** (banco próprio): tenants, memberships, product memberships, convites, roles de tenant/produto, permissões efetivas, audit logs de negócio.
- Tokens do Keycloak carregam identidade e roles globais, **não** a autorização tenant-specific final — o backend valida membership ativa, tenant ativo e permissões efetivas.
- Realm por tenant foi rejeitado no MVP (provisionamento complexo, usuário multi-tenant difícil), reservado para enterprise futuro.

### 14.1 Divisão de responsabilidades

```txt
┌──────────────── KEYCLOAK ────────────────┐   ┌──────────────── AEGIS ────────────────┐
│ autentica usuário                         │   │ autoriza por tenant                   │
│ credenciais, sessões, refresh tokens      │   │ memberships (tenant + produto)        │
│ roles GLOBAIS (SUPER_ADMIN)               │   │ roles de TENANT e PRODUTO             │
│ login social (futuro)                     │   │ convites, permissões efetivas         │
│ emite JWT (sub, email, global roles)      │   │ audit logs de negócio                 │
└───────────────────────────────────────────┘   └───────────────────────────────────────┘
        token │                                          ▲ valida
              └──────────────────────────────────────────┘
```

### 14.2 Claims mínimas do token

```json
{
  "sub": "user_001",
  "email": "alexandre@byop.dev",
  "email_verified": true,
  "preferred_username": "alexandre",
  "realm_access": { "roles": [] },
  "iss": "https://auth.byop.dev/realms/aegis",
  "exp": 1750000000
}
```

O token **não** carrega `tenantId` nem role de tenant — esses vivem no Aegis e são resolvidos por membership.

### 14.3 Sincronização Keycloak ↔ base local

```txt
primeiro login OK ─► JWT válido
   └─► Aegis: user local existe? (por sub)
         ├── não → cria user local (sub, email, displayName) status=ACTIVE
         └── sim → atualiza email/displayName se mudou
desativação no Keycloak ─► token deixa de ser emitido (acesso cessa)
remoção definitiva ─► job de anonimização local (§38) preserva audit logs
```

### 14.4 Leis do IAM

1. Keycloak prova identidade; Aegis decide autorização por tenant.
2. O token nunca é a fonte de verdade da autorização tenant-specific.
3. Toda chamada sensível revalida membership ativa + tenant + role + feature.
4. `SUPER_ADMIN` é raro e toda atuação é auditada com motivo.
5. User local é criado/sincronizado no primeiro login válido.
6. Remoção de usuário aciona anonimização, preservando auditoria.

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

### 15.1 Modelo de banco — pages/sections/blocks

```sql
CREATE TABLE pages (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL REFERENCES tenants(id),
    product_id    UUID NOT NULL REFERENCES products(id),
    slug          VARCHAR(160) NOT NULL,
    locale        VARCHAR(10)  NOT NULL DEFAULT 'pt-BR',
    title         VARCHAR(240) NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'DRAFT', -- DRAFT, REVIEW, APPROVED, PUBLISHED, ARCHIVED
    current_revision_id UUID,
    published_revision_id UUID,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_page_slug UNIQUE (product_id, locale, slug)
);
CREATE INDEX idx_pages_product ON pages (product_id, status);

CREATE TABLE sections (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES tenants(id),
    page_id     UUID NOT NULL REFERENCES pages(id),
    position    INT  NOT NULL,
    layout      VARCHAR(40) NOT NULL DEFAULT 'single', -- single, two-column, grid
    settings    JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_sections_page ON sections (page_id, position);

CREATE TABLE blocks (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES tenants(id),
    section_id  UUID NOT NULL REFERENCES sections(id),
    position    INT  NOT NULL,
    type        VARCHAR(40) NOT NULL,  -- HeroBlock, RichTextBlock, GalleryBlock...
    data        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_blocks_section ON blocks (section_id, position);
```

### 15.2 Contrato JSON — Page pública

```json
{
  "data": {
    "slug": "home",
    "locale": "pt-BR",
    "title": "Início",
    "seo": { "title": "CMSS — Início", "description": "Corporação Musical São Sebastião" },
    "sections": [
      {
        "layout": "single",
        "blocks": [
          {
            "type": "HeroBlock",
            "data": {
              "headline": "Tradição em movimento",
              "subline": "Desde 1920",
              "media": { "assetId": "asset_hero_01", "alt": "Banda em apresentação" },
              "cta": { "label": "Próximos eventos", "href": "/eventos" }
            }
          },
          {
            "type": "EventListBlock",
            "data": { "source": "events", "limit": 3, "order": "date_asc" }
          }
        ]
      }
    ]
  },
  "metadata": { "contractVersion": "1.0.0", "publishedAt": "2026-06-10T09:00:00Z" }
}
```

### 15.3 Esquema de Block (validação)

```json
{
  "blockType": "HeroBlock",
  "schemaVersion": "1.0.0",
  "fields": {
    "headline": { "type": "string", "required": true, "maxLength": 120 },
    "subline":  { "type": "string", "required": false, "maxLength": 200 },
    "media":    { "type": "assetRef", "required": false },
    "cta":      { "type": "object", "fields": { "label": "string", "href": "string" } }
  }
}
```

### 15.4 Leis do CMS Core

1. Page → Section → Block é a única hierarquia de composição.
2. Markdown é conteúdo; Block é estrutura. Não se mistura layout em Markdown.
3. Todo block tem `type` registrado e `data` validável por schema.
4. Slug de página é único por (produto, locale).
5. A API pública entrega apenas a revisão `PUBLISHED` (§22).
6. Reordenar seções/blocos é alteração versionada (§22).
7. Preview visual completo é futuro; MVP entrega preview textual/estrutural.

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

### 16.1 Definição genérica de Content Type

Um Content Type é, em essência, um **schema de campos** com metadados editoriais. O Aegis não cria uma tabela por tipo; usa uma tabela genérica `content_entries` com `data` JSONB validado pelo schema do tipo. Isso preserva a modularidade (lei 13) e evita explosão de DDL.

```sql
CREATE TABLE content_type_definitions (
    key         VARCHAR(60) NOT NULL,   -- 'event', 'manifesto', 'job_posting'...
    tenant_id   UUID,                   -- NULL = global; preenchido = override por tenant
    name        VARCHAR(120) NOT NULL,
    module      VARCHAR(40)  NOT NULL,
    schema      JSONB        NOT NULL,  -- fields + validação
    workflow    JSONB        NOT NULL DEFAULT '{}'::jsonb,
    seo_enabled BOOLEAN      NOT NULL DEFAULT true,
    i18n_enabled BOOLEAN     NOT NULL DEFAULT false,
    relations   JSONB        NOT NULL DEFAULT '[]'::jsonb,
    version     INT          NOT NULL DEFAULT 1,
    PRIMARY KEY (key, COALESCE(tenant_id, '00000000-0000-0000-0000-000000000000'::uuid))
);

CREATE TABLE content_entries (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL REFERENCES tenants(id),
    product_id    UUID NOT NULL REFERENCES products(id),
    content_type  VARCHAR(60) NOT NULL,
    slug          VARCHAR(160),
    locale        VARCHAR(10) NOT NULL DEFAULT 'pt-BR',
    status        VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    data          JSONB NOT NULL DEFAULT '{}'::jsonb,
    current_revision_id   UUID,
    published_revision_id UUID,
    created_by    UUID NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at    TIMESTAMPTZ
);
CREATE INDEX idx_entries_product_type ON content_entries (product_id, content_type, status);
CREATE INDEX idx_entries_slug ON content_entries (product_id, content_type, locale, slug);
```

### 16.2 Exemplos de schema por tipo

**Event (Música/Eventos):**

```json
{
  "key": "event",
  "fields": {
    "title":      { "type": "string", "required": true },
    "startsAt":   { "type": "datetime", "required": true },
    "endsAt":     { "type": "datetime" },
    "venue":      { "type": "string" },
    "city":       { "type": "string" },
    "cover":      { "type": "assetRef" },
    "description":{ "type": "markdown" },
    "ticketUrl":  { "type": "url" }
  },
  "relations": [ { "type": "gallery", "cardinality": "0..1" } ]
}
```

**Manifesto (Biblioteca/Loki):**

```json
{
  "key": "manifesto",
  "i18nEnabled": true,
  "fields": {
    "title":     { "type": "string", "required": true },
    "body":      { "type": "markdown", "required": true },
    "anonymous": { "type": "boolean", "default": true },
    "themes":    { "type": "tags" },
    "playlistRef": { "type": "relation", "target": "playlist" }
  }
}
```

**JobPosting (RH/Conecta):**

```json
{
  "key": "job_posting",
  "fields": {
    "title":       { "type": "string", "required": true },
    "company":     { "type": "relation", "target": "company_profile" },
    "seniority":   { "type": "enum", "values": ["JUNIOR","PLENO","SENIOR"] },
    "location":    { "type": "string" },
    "remote":      { "type": "boolean" },
    "salaryRange": { "type": "object" },
    "description": { "type": "markdown", "required": true },
    "status":      { "type": "enum", "values": ["OPEN","CLOSED","DRAFT"] }
  },
  "relations": [ { "type": "candidate_submission", "cardinality": "0..N" } ]
}
```

### 16.3 Mapa de relacionamentos (knowledge graph futuro)

```txt
KnowledgeCategory ──1:N──► KnowledgeTopic ──1:N──► KnowledgeArticle
                                                       │ N:N (related)
                                                       ▼
                                                  KnowledgeArticle
JobPosting ──1:N──► CandidateSubmission
Project ──N:N──► Skill
Manifesto ──0:1──► Playlist ──1:N──► MusicReference
```

### 16.4 Contrato JSON — Content Entry pública (Event)

```json
{
  "data": {
    "contentType": "event",
    "slug": "concerto-natal-2026",
    "title": "Concerto de Natal 2026",
    "startsAt": "2026-12-20T20:00:00-03:00",
    "venue": "Igreja Matriz",
    "city": "São Sebastião",
    "cover": { "assetId": "asset_evt_01", "url": "/public/.../asset_evt_01", "alt": "Concerto" },
    "description": "<p>Apresentação especial...</p>"
  },
  "metadata": { "contractVersion": "1.0.0", "locale": "pt-BR" }
}
```

### 16.5 Leis dos Content Types

1. Um Content Type é um schema versionado, não uma tabela dedicada.
2. `data` JSONB é sempre validado contra o schema do tipo antes de persistir.
3. Content Type pode ser global ou ter override por tenant.
4. Toda entrada pertence a (tenant, produto) e respeita as features ativas.
5. Entradas têm workflow editorial e revisões (§22) quando o tipo o exige.
6. Relações entre tipos são declaradas no schema, nunca inferidas.
7. A API pública entrega apenas entradas publicadas.

## 17. Assets

Asset é qualquer mídia/arquivo gerenciado: imagens, vídeos, áudio, documentos, PDFs, downloads, currículos, capas, anexos.

- **Lifecycle:** upload → processamento (variações de imagem) → metadados → uso → versionamento → arquivamento.
- **Tipos:** Image, Video, Audio, Document, PDF, Download, Resume, Book, Cover, Gallery, Attachment.
- **Storage tenant-scoped:** `tenants/{tenantId}/products/{productId}/assets/{assetId}/{filename}`. O backend nunca confia só no caminho — valida o registro no banco e o `tenantId`.
- Metadados obrigatórios; SEO de assets (alt, title); permissões de upload/delete; assets sensíveis (currículos) seguem LGPD; versionamento e relações; analytics e audit de assets.

### 17.1 Asset lifecycle (diagrama)

```txt
UPLOAD ─► VALIDATE (mime, size, tenant) ─► STORE (tenant-scoped key)
   │                                          │
   │                                          ▼
   │                                  PROCESS (job §33: thumbnails, variants)
   │                                          │
   ▼                                          ▼
METADATA (alt, title, tags) ◄──────────── READY ──► USE (refs em blocks/entries)
                                            │
                                            ▼
                                   VERSION ↺ / ARCHIVE / DELETE (soft + LGPD)
```

### 17.2 Modelo de banco — `assets`

```sql
CREATE TABLE assets (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL REFERENCES tenants(id),
    product_id    UUID NOT NULL REFERENCES products(id),
    type          VARCHAR(20) NOT NULL,  -- IMAGE, VIDEO, AUDIO, DOCUMENT, PDF, RESUME...
    storage_key   VARCHAR(400) NOT NULL, -- tenants/{t}/products/{p}/assets/{id}/{file}
    filename      VARCHAR(255) NOT NULL,
    mime_type     VARCHAR(120) NOT NULL,
    size_bytes    BIGINT NOT NULL,
    checksum      VARCHAR(128),
    alt_text      VARCHAR(300),
    title         VARCHAR(300),
    tags          JSONB NOT NULL DEFAULT '[]'::jsonb,
    variants      JSONB NOT NULL DEFAULT '{}'::jsonb,  -- {thumb, medium, large: storage_key}
    sensitive     BOOLEAN NOT NULL DEFAULT false,       -- currículos, dados pessoais (LGPD)
    status        VARCHAR(20) NOT NULL DEFAULT 'READY',  -- UPLOADING, PROCESSING, READY, ARCHIVED, DELETED
    version       INT NOT NULL DEFAULT 1,
    uploaded_by   UUID NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at    TIMESTAMPTZ
);
CREATE INDEX idx_assets_product ON assets (product_id, type, status);
```

### 17.3 Catálogo de tipos de asset por produto

| Tipo | Usos reais |
|---|---|
| Image | Hero, galerias (CMSS), capas de projeto (Alex Dev) |
| Video | Repertório/vídeos (Maestro Beton) |
| Audio | Referências musicais (Loki) |
| Document/PDF | Downloads, currículo do portfólio, materiais (WikiDev) |
| Resume | Currículos enviados em candidaturas (Conecta) — **sensível, LGPD** |
| Book/Cover | Livros e capas (Loki) |

### 17.4 Contrato JSON — Asset

```json
{
  "data": {
    "assetId": "asset_hero_01",
    "type": "IMAGE",
    "url": "/public/products/site-cmss/assets/asset_hero_01",
    "alt": "Banda em apresentação",
    "title": "Concerto 2025",
    "variants": {
      "thumb":  "/public/.../asset_hero_01?v=thumb",
      "medium": "/public/.../asset_hero_01?v=medium",
      "large":  "/public/.../asset_hero_01?v=large"
    },
    "mimeType": "image/webp",
    "sizeBytes": 184320
  },
  "metadata": { "contractVersion": "1.0.0" }
}
```

### 17.5 Endpoints de assets

```txt
POST   /api/v1/admin/products/{productId}/assets         # upload (multipart)
GET    /api/v1/admin/products/{productId}/assets         # lista (filtra type, tags)
GET    /api/v1/admin/products/{productId}/assets/{id}
PATCH  /api/v1/admin/products/{productId}/assets/{id}     # metadados (alt, title, tags)
DELETE /api/v1/admin/products/{productId}/assets/{id}     # soft delete
GET    /public/products/{productSlug}/assets/{assetId}    # entrega pública
```

### 17.6 Leis dos Assets

1. Todo asset tem `tenantId`, `productId` e metadados obrigatórios (filename, mime, size).
2. A chave de storage é tenant-scoped; o backend valida o registro, nunca confia no caminho cru.
3. Imagens geram variantes via job assíncrono (§33).
4. Asset sensível (currículo) é marcado `sensitive=true` e segue LGPD (§38).
5. Alt text é obrigatório para imagens usadas em conteúdo público (acessibilidade + SEO).
6. Delete é soft; purga física segue política de retenção.
7. Toda operação de upload/delete é auditada (§23).

## 18. Forms

Formulários são parte do domínio do Aegis (não exceções espalhadas em páginas). Um **Form Type** define schema, fields, validação, workflow, notifications, integrations, analytics, LGPD e permissions.

### Catálogo de Form Types

Contact, Quote Request, Corporate Lead, Job Application, Talent Pool, Contributor Application, Bug Report, Content Suggestion, Newsletter Subscription, Donation, Book Interest, Recruiter Contact.

- **Validation Engine** e **Workflow Engine** próprios; catálogo de campos reutilizável.
- **Notification Engine** dispara e-mail/Telegram/in-app; **integração Telegram** para alertas operacionais (orçamentos, candidaturas, bug reports).
- **LGPD e consentimento** explícitos; submissões carregam `tenantId + productId + formId`; exportação exige permissão forte; retenção configurável.

### 18.1 Modelo de banco — forms e submissions

```sql
CREATE TABLE form_definitions (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID NOT NULL REFERENCES tenants(id),
    product_id   UUID NOT NULL REFERENCES products(id),
    key          VARCHAR(60) NOT NULL,  -- 'contact', 'job_application'...
    name         VARCHAR(160) NOT NULL,
    schema       JSONB NOT NULL,        -- fields + validação
    workflow     JSONB NOT NULL DEFAULT '{}'::jsonb,
    notifications JSONB NOT NULL DEFAULT '{}'::jsonb, -- canais: email, telegram, in-app
    consent_text TEXT,                  -- LGPD
    retention_days INT NOT NULL DEFAULT 365,
    status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_form_key UNIQUE (product_id, key)
);

CREATE TABLE form_submissions (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL REFERENCES tenants(id),
    product_id    UUID NOT NULL REFERENCES products(id),
    form_id       UUID NOT NULL REFERENCES form_definitions(id),
    data          JSONB NOT NULL,
    consent_given BOOLEAN NOT NULL DEFAULT false,
    consent_at    TIMESTAMPTZ,
    status        VARCHAR(20) NOT NULL DEFAULT 'NEW', -- NEW, IN_REVIEW, HANDLED, ARCHIVED, ANONYMIZED
    source_ip_hash VARCHAR(128),       -- hash, nunca IP cru
    attachments   JSONB NOT NULL DEFAULT '[]'::jsonb, -- asset refs (ex.: currículo)
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    handled_at    TIMESTAMPTZ,
    anonymized_at TIMESTAMPTZ
);
CREATE INDEX idx_submissions_form ON form_submissions (form_id, status);
CREATE INDEX idx_submissions_product ON form_submissions (product_id, created_at);
```

### 18.2 Catálogo de Form Types (campos típicos)

| Form Type | Campos-chave | Notificação | Sensível? |
|---|---|---|---|
| Contact | name, email, message | email + Telegram | dados de contato |
| Quote Request | name, phone, service, budget | Telegram (Maestro) | sim |
| Job Application | name, email, resume(asset), jobRef | email + Telegram | **alto (currículo)** |
| Talent Pool | name, email, skills, resume | email | **alto** |
| Bug Report | title, steps, severity | Telegram (WikiDev) | baixo |
| Content Suggestion | topic, body, authorRef | in-app | baixo |
| Newsletter | email, consent | — | email |
| Donation | name, amount, message | email | dados financeiros |

### 18.3 Fluxo de submissão

```txt
[Visitante] POST /public/products/{slug}/forms/{formId}/submit
   │ valida schema + consent obrigatório (LGPD)
   ▼
[Aegis] persiste submission(NEW) + attachments (assets sensíveis)
   │ ENQUEUE notification job (§33)
   ▼
[Notification Engine] ─► email/Telegram/in-app aos destinatários do tenant
   │
   ▼
[Painel] submission visível a roles autorizadas ─► IN_REVIEW ─► HANDLED
   │ retenção: job de anonimização após retention_days (§38)
```

### 18.4 Contrato JSON — submissão (request)

```json
{
  "formKey": "job_application",
  "data": {
    "name": "Maria Silva",
    "email": "maria@example.com",
    "jobRef": "job_posting_017",
    "resumeAssetId": "asset_resume_88"
  },
  "consent": { "given": true, "policyVersion": "2026-01" }
}
```

### 18.5 Leis dos Forms

1. Form é domínio do Aegis, não markup espalhado em páginas.
2. Toda submissão carrega `tenantId + productId + formId`.
3. Consentimento LGPD é obrigatório quando há dados pessoais.
4. IP nunca é persistido cru — apenas hash.
5. Anexos sensíveis (currículos) são assets `sensitive=true`.
6. Notificação é assíncrona (job + engine); Telegram é canal trocável (§25).
7. Exportar submissões exige role forte (T_OWNER/T_ADMIN) e gera auditoria.
8. Retenção é configurável; expirada, a submissão é anonimizada (§38).

## 19. Traduções e Internacionalização (i18n)

Tradução no Aegis cobre páginas, content types, blocks, navegação, formulários, SEO e assets.

- Cada produto tem **idioma padrão**; demais idiomas são traduções com **fallback** transparente para o backend e explícito para SEO.
- **Translation Entry** e **Translation Registry** versionáveis; integrados a workflow, revisões, analytics e busca.
- URL strategy por idioma; traduções afetam SEO (hreflang) e busca por locale.
- Casos reais: CMSS (pt-BR), produtos BYOP (pt-BR + en + es).

### 19.1 Modelo conceitual de tradução

```txt
Conteúdo canônico (locale padrão do produto)
        │
        ├── Translation Entry (en)  ─► status: MISSING | DRAFT | TRANSLATED | PUBLISHED | STALE
        ├── Translation Entry (es)  ─► ...
        └── fallback transparente: se 'en' ausente → entrega padrão; SEO marca hreflang correto
```

### 19.2 Modelo de banco — traduções

```sql
CREATE TABLE translation_entries (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL REFERENCES tenants(id),
    product_id    UUID NOT NULL REFERENCES products(id),
    resource_type VARCHAR(40) NOT NULL,  -- PAGE, BLOCK, CONTENT_ENTRY, NAVIGATION, SEO, FORM
    resource_id   UUID NOT NULL,
    field_path    VARCHAR(160) NOT NULL, -- ex.: 'blocks[0].headline'
    locale        VARCHAR(10) NOT NULL,
    value         TEXT,
    status        VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- MISSING, DRAFT, TRANSLATED, PUBLISHED, STALE
    source_version INT,                  -- versão do canônico de onde foi traduzido (detecta STALE)
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_translation UNIQUE (resource_type, resource_id, field_path, locale)
);
CREATE INDEX idx_translation_lookup ON translation_entries (product_id, resource_type, resource_id, locale);
```

### 19.3 Estratégia de URL por idioma

```txt
Padrão recomendado (path prefix):
   /pt-BR/eventos    /en/events    /es/eventos
Alternativa (query):   /eventos?lang=en   (menos amigável a SEO)
hreflang gerado no contrato:
   <link rel="alternate" hreflang="en" href="https://.../en/events" />
```

### 19.4 Fallback e estado STALE

```txt
request locale=en, página "home"
   ├── translation(en) PUBLISHED  → entrega em en
   ├── translation(en) ausente    → entrega padrão pt-BR (fallback) + hreflang aponta canônico
   └── canônico mudou (version+1) ─► translations marcadas STALE → painel sinaliza re-tradução
```

### 19.5 Contrato JSON — recurso multilíngue

```json
{
  "data": {
    "slug": "events",
    "locale": "en",
    "title": "Events",
    "fallbackLocale": null,
    "sections": [ { "blocks": [ { "type": "HeroBlock", "data": { "headline": "Tradition in motion" } } ] } ]
  },
  "metadata": {
    "requestedLocale": "en",
    "servedLocale": "en",
    "hreflang": { "pt-BR": "/pt-BR/eventos", "en": "/en/events", "es": "/es/eventos" }
  }
}
```

### 19.6 Leis da Internacionalização

1. Todo produto tem um locale padrão; demais locales são traduções.
2. Fallback ao padrão é transparente para o usuário e explícito no metadata (servedLocale).
3. Tradução tem revisão e workflow próprios.
4. Mudança no canônico marca traduções dependentes como STALE.
5. SEO multilíngue usa hreflang; nunca duplica conteúdo sem sinalização.
6. Busca respeita locale (§27).
7. Idiomas suportados são governados por feature/limite (§13).

## 20. SEO

SEO é **parte do conteúdo** e é tenant-owned. Inclui: title, description, canonical, robots, sitemap, Open Graph, schema.org, redirects, slugs públicos, hreflang.

- SEO por Content Type e por Page; sitemap gerado por produto e tenant; redirects pertencem a tenant/produto; alterações geram audit log.
- Slugs únicos dentro do produto/escopo definido (não globalmente sem necessidade).

### 20.1 Modelo de banco — SEO e redirects

```sql
CREATE TABLE seo_metadata (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL REFERENCES tenants(id),
    product_id    UUID NOT NULL REFERENCES products(id),
    resource_type VARCHAR(40) NOT NULL,  -- PAGE, CONTENT_ENTRY, PRODUCT
    resource_id   UUID NOT NULL,
    locale        VARCHAR(10) NOT NULL DEFAULT 'pt-BR',
    title         VARCHAR(240),
    description   VARCHAR(320),
    canonical_url VARCHAR(500),
    robots        VARCHAR(60) DEFAULT 'index,follow',
    og            JSONB NOT NULL DEFAULT '{}'::jsonb,   -- open graph
    schema_org    JSONB NOT NULL DEFAULT '{}'::jsonb,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_seo UNIQUE (resource_type, resource_id, locale)
);

CREATE TABLE seo_redirects (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES tenants(id),
    product_id  UUID NOT NULL REFERENCES products(id),
    from_path   VARCHAR(500) NOT NULL,
    to_path     VARCHAR(500) NOT NULL,
    code        INT NOT NULL DEFAULT 301,  -- 301, 302
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_redirect UNIQUE (product_id, from_path)
);
```

### 20.2 Contrato JSON — SEO de página

```json
{
  "data": {
    "title": "Eventos — CMSS",
    "description": "Confira os próximos concertos da Corporação Musical São Sebastião.",
    "canonicalUrl": "https://cmss.byop.dev/eventos",
    "robots": "index,follow",
    "openGraph": { "type": "website", "image": "/public/.../asset_og_01" },
    "schemaOrg": { "@type": "MusicGroup", "name": "CMSS" },
    "hreflang": { "pt-BR": "/eventos" }
  }
}
```

### 20.3 Geração de sitemap

```txt
GET /public/products/{slug}/sitemap.xml
   └─► itera páginas + content entries PUBLISHED do produto
         └─► <url><loc>...</loc><lastmod>...</lastmod></url>
              respeitando robots e locales (hreflang)
```

### 20.4 Leis do SEO

1. SEO é conteúdo: versionado e auditado como o resto.
2. SEO existe por página e por content type.
3. Sitemap é gerado por produto, só com conteúdo publicado.
4. Redirects pertencem a (tenant, produto) e têm origem única.
5. Slug é único no escopo do produto, não global.
6. hreflang é obrigatório em produtos multilíngues (§19).

## 21. Navegação

Navegação é estrutura de informação e descoberta. Entidades: `Navigation` e `Navigation Item`.

- Tipos: menu principal, footer, contextual, relacionada, breadcrumbs, mobile.
- **Menus dinâmicos** montados por tenant ativo, role, produtos e features ativas; multilíngue.
- Navegação integra-se a busca, content types, traduções, SEO, analytics, auditoria e revisões.

### 21.1 Modelo de banco — navegação

```sql
CREATE TABLE navigations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES tenants(id),
    product_id  UUID NOT NULL REFERENCES products(id),
    type        VARCHAR(30) NOT NULL,  -- MAIN, FOOTER, CONTEXTUAL, MOBILE, BREADCRUMB
    locale      VARCHAR(10) NOT NULL DEFAULT 'pt-BR',
    status      VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_nav UNIQUE (product_id, type, locale)
);

CREATE TABLE navigation_items (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    navigation_id UUID NOT NULL REFERENCES navigations(id),
    parent_id     UUID REFERENCES navigation_items(id),
    position      INT NOT NULL,
    label         VARCHAR(160) NOT NULL,
    target_type   VARCHAR(30) NOT NULL,  -- PAGE, CONTENT_ENTRY, EXTERNAL, ANCHOR
    target_ref    VARCHAR(500) NOT NULL, -- slug, url ou id
    requires_feature VARCHAR(60),        -- item só aparece se feature ativa
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_nav_items ON navigation_items (navigation_id, position);
```

### 21.2 Diagrama — navegação pública vs. menu do painel

```txt
PÚBLICA (frontend externo)            ADMIN (painel Eirene)
contrato de navegação do produto      menu montado em runtime:
   MAIN: Início, Eventos, Galeria        tenant ativo + role + features ativas
   FOOTER: Contato, Sobre                  └─► esconde itens sem permissão/feature
   (só itens cujo requires_feature
    está ativo no produto)
```

### 21.3 Contrato JSON — Navigation pública

```json
{
  "data": {
    "type": "MAIN",
    "locale": "pt-BR",
    "items": [
      { "label": "Início", "target": "/", "type": "PAGE" },
      { "label": "Eventos", "target": "/eventos", "type": "PAGE",
        "children": [ { "label": "Próximos", "target": "/eventos#proximos", "type": "ANCHOR" } ] },
      { "label": "Galeria", "target": "/galeria", "type": "PAGE" }
    ]
  },
  "metadata": { "contractVersion": "1.0.0" }
}
```

### 21.4 Leis da Navegação

1. Item de navegação com `requires_feature` só aparece se a feature está ativa.
2. Menu do painel é montado em runtime por tenant + role + features.
3. Navegação tem locale; produtos multilíngues têm um menu por locale.
4. Navegação é versionada e auditada como conteúdo.
5. A navegação pública entrega só itens cujo alvo está publicado.

---

# Parte IV — Operação

## 22. Revisões, Versionamento e Histórico

Uma **revisão** é um registro versionado do estado de um recurso editorial.

- Estratégia MVP: **snapshots completos** (ADR: revisions por snapshot).
- Entidades com revisão: Pages, Blocks, Content Types, Traduções, SEO, Navegação, Formulários, Assets.
- Entidades sem revisão: logs de auditoria (imutáveis), eventos analíticos, dados efêmeros.
- Fluxo editorial: `Draft → Review → Approved → Published → Archived`. Publicação, rollback e compare engine suportados. Publicação programada é futuro.
- API pública entrega apenas conteúdo **publicado**; drafts e revisões não aparecem.

> Nenhum conteúdo publicado é perdido. A fonte de verdade é o histórico; o conteúdo atual é apenas a projeção da última revisão válida.

### 22.1 Distinção fundamental: estado × revisão × publicação

```txt
Conteúdo (estado atual)  = projeção da última revisão de trabalho
Revisão                  = snapshot imutável de um estado em um instante
Publicação               = ponteiro para a revisão ativa (publicada)
```

Publicar **não** altera a revisão; publicar **aponta** para a versão ativa. Editar cria nova revisão de trabalho; só publicar move o ponteiro público.

### 22.2 Camadas de versionamento do Aegis

A estratégia de versionamento atravessa múltiplas camadas, cada uma com regras próprias:

```txt
Platform Version       (semver da plataforma Aegis — release de software)
│
├── API Version        (/api/v1 — path; breaking change → /api/v2)
├── Contract Version   (contractVersion semver por recurso — §31)
├── Product Version    (config/feature snapshot do produto)
├── Content Version    (revisão de page/entry — esta seção)
├── Asset Version      (versão de mídia — §17)
├── Translation Version(versão por locale — §19)
└── Configuration Version (settings de tenant/produto/feature)
```

Distinção de termos:

| Termo | Significado |
|---|---|
| Versão | Estado numerado de um recurso/contrato |
| Revisão | Snapshot editorial imutável de conteúdo |
| Publicação | Ato de apontar para a revisão ativa |
| Release | Versão planejada de software entregue |
| Migração | Transformação de dados/schema entre versões (§35) |
| Evolução | Mudança contínua e compatível ao longo do tempo |

Princípio: **toda mudança deve possuir uma estratégia de versionamento adequada.** Nem toda mudança merece nova versão; nem toda mudança pode ocorrer sem versionamento. Mudanças que quebram contratos/APIs exigem nova versão major (§31, §44).

### 22.3 Modelo de banco — `revisions`

```sql
CREATE TABLE revisions (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL REFERENCES tenants(id),
    product_id    UUID NOT NULL REFERENCES products(id),
    resource_type VARCHAR(40) NOT NULL,  -- PAGE, BLOCK, CONTENT_ENTRY, SEO, NAVIGATION, FORM, ASSET, TRANSLATION
    resource_id   UUID NOT NULL,
    version       INT NOT NULL,          -- incremental por recurso
    status        VARCHAR(20) NOT NULL,  -- DRAFT, REVIEW, APPROVED, PUBLISHED, ARCHIVED
    snapshot      JSONB NOT NULL,        -- estado completo do recurso (snapshot total no MVP)
    locale        VARCHAR(10),
    created_by    UUID NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at  TIMESTAMPTZ,
    CONSTRAINT uq_revision UNIQUE (resource_type, resource_id, version)
);
CREATE INDEX idx_revision_resource ON revisions (resource_type, resource_id, version DESC);
CREATE INDEX idx_revision_published ON revisions (product_id, status) WHERE status = 'PUBLISHED';
```

### 22.4 Contrato JSON — Revisão

```json
{
  "revisionId": "rev_001",
  "resourceId": "manifesto_001",
  "resourceType": "Manifesto",
  "version": 3,
  "status": "PUBLISHED",
  "snapshot": { "title": "Sobre o silêncio", "body": "..." },
  "createdBy": "user_001",
  "createdAt": "2026-01-01T10:00:00Z",
  "publishedAt": "2026-01-02T09:00:00Z"
}
```

### 22.5 Fluxo editorial e rollback

```txt
FLUXO EDITORIAL                       ROLLBACK
Draft                                 Revisão 8 publicada
  ↓                                      ↓
Review                                Problema detectado
  ↓                                      ↓
Approved                              Rollback (publica revisão 7)
  ↓                                      ↓
Published  ←── ponteiro ativo         Revisão 7 volta a ser publicada
  ↓                                   (histórico 8 PRESERVADO; nada é apagado)
Archived
```

Rollback nunca remove histórico: cria a transição publicando uma revisão anterior. Traduções têm revisões independentes do canônico.

### 22.6 Compare engine

```txt
GET /admin/.../revisions/{v1}/diff/{v2}
   └─► compara snapshots JSON ─► saída: campos adicionados / removidos / alterados
       (MVP: diff estrutural de JSON; futuro: diff textual rico de Markdown)
```

### 22.7 Endpoints de revisão

```txt
GET  /api/v1/admin/.../{resourceId}/revisions
GET  /api/v1/admin/.../{resourceId}/revisions/{version}
POST /api/v1/admin/.../{resourceId}/revisions/{version}/publish
POST /api/v1/admin/.../{resourceId}/revisions/{version}/rollback
GET  /api/v1/admin/.../{resourceId}/revisions/{v1}/diff/{v2}
```

### 22.8 Leis das Revisões

1. Nenhum conteúdo publicado é perdido.
2. Publicar não altera a revisão; aponta para a versão ativa.
3. Editar cria revisão de trabalho; só publicar move o ponteiro público.
4. Rollback publica uma revisão anterior sem apagar histórico.
5. Traduções têm revisões independentes.
6. A API pública nunca expõe drafts ou revisões internas.
7. Snapshot completo no MVP; diff é otimização futura.
8. Mudança que quebra contrato exige versionamento de contrato (§31).

## 23. Auditoria, Rastreabilidade e Governança de Dados

Auditoria registra quem fez o quê, quando, em qual tenant, com qual resultado. **Audit logs são imutáveis** (ADR: audit logs imutáveis).

- **Audit Event:** actor, action, resource, result, context (correlation IDs), tenantId, timestamp.
- Auditar: segurança, conteúdo, memberships, assets, formulários, configurações, integrações.
- Não auditar: leitura trivial sem sensibilidade, dados efêmeros, segredos em claro.
- LGPD: classificação, finalidade, retenção, exportação e anonimização aplicáveis por tenant.

> Nenhuma ação relevante deve acontecer sem deixar rastros.

Na operação administrativa, auditoria deve ser exibida como tabela paginada e filtrável, não como feed infinito de cards. Super Admin e Tenant Admin precisam consultar por texto livre, ator, produto, módulo, risco e página mantendo identidade visual, tema claro/escuro e responsividade. A API preserva o endpoint legado de lista para compatibilidade, mas a UI operacional deve consumir paginação explícita para evitar telas gigantes, filtros confusos e carregamento excessivo.

### 23.1 Revisão × Auditoria

```txt
Revision                Audit Log
  ↓                        ↓
O QUE mudou             QUEM fez / QUANDO / DE ONDE / EM QUAL CONTEXTO
```

Você pode restaurar uma revisão. Você **não** restaura uma auditoria. Auditoria existe para rastreabilidade, segurança, compliance, LGPD, investigação de incidentes, governança, suporte e troubleshooting. Auditoria não substitui revisão e revisão não substitui auditoria.

### 23.2 Modelo de banco — `audit_logs` (append-only)

```sql
CREATE TABLE audit_logs (
    id            BIGSERIAL PRIMARY KEY,    -- sequencial; nunca atualizado/deletado
    event_id      UUID NOT NULL DEFAULT gen_random_uuid(),
    timestamp     TIMESTAMPTZ NOT NULL DEFAULT now(),
    tenant_id     UUID,                     -- pode ser NULL em eventos de plataforma
    product_id    UUID,
    actor_id      UUID,                     -- usuário, ou NULL para SYSTEM/JOB
    actor_type    VARCHAR(20) NOT NULL DEFAULT 'USER', -- USER, SYSTEM, JOB, INTEGRATION
    resource_type VARCHAR(40),
    resource_id   VARCHAR(80),
    action        VARCHAR(40) NOT NULL,     -- LOGIN, CREATE, UPDATE, DELETE, PUBLISH, ROLLBACK, ENABLE_FEATURE...
    result        VARCHAR(20) NOT NULL,     -- SUCCESS, FAILURE, DENIED, PARTIAL_SUCCESS
    correlation_id VARCHAR(64),
    trace_id      VARCHAR(64),
    origin        VARCHAR(120),             -- ip hash, user agent class
    metadata      JSONB NOT NULL DEFAULT '{}'::jsonb
);
CREATE INDEX idx_audit_tenant_time ON audit_logs (tenant_id, timestamp DESC);
CREATE INDEX idx_audit_resource    ON audit_logs (resource_type, resource_id);
-- Imutabilidade reforçada por permissão (sem UPDATE/DELETE) e por trigger de bloqueio.
```

### 23.3 Contrato JSON — Audit Event

```json
{
  "eventId": "audit_001",
  "timestamp": "2026-01-01T10:00:00Z",
  "actorId": "user_001",
  "actorType": "USER",
  "tenantId": "tenant_byop",
  "productId": "product_aegis",
  "resourceType": "Page",
  "resourceId": "page_home",
  "action": "UPDATE",
  "result": "SUCCESS",
  "correlationId": "req_abc123",
  "metadata": { "fields": ["title"] }
}
```

### 23.4 O que auditar / o que não auditar

| Auditar (obrigatório) | Não auditar |
|---|---|
| login, logout, falha de login | page views públicos |
| CRUD de tenant, produto, feature, membership, role | eventos efêmeros de UI |
| publicação, rollback, alteração de SEO/tradução | caches e métricas transitórias |
| upload/delete de asset, submissão de form sensível | segredos em claro (nunca logar) |
| alteração de integração/configuração | leitura trivial não sensível |

Registry de resultados: `SUCCESS`, `FAILURE`, `DENIED`, `PARTIAL_SUCCESS`. Atores possíveis: usuário humano, sistema, job, integração, webhook futuro, API client futuro.

### 23.5 Leis da Auditoria

1. Audit log é imutável: nunca UPDATE, nunca DELETE.
2. Toda ação relevante deixa rastro (quem, quando, onde, contexto).
3. Auditoria não substitui revisão e vice-versa.
4. Segredos e dados sensíveis nunca aparecem em claro no log.
5. Cross-tenant negado é auditado como `DENIED`.
6. Correlation/trace IDs ligam auditoria a observabilidade (§34).
7. Retenção de auditoria respeita LGPD, mas a imutabilidade prevalece (§38).

## 24. Notificações e Comunicação

Notificação é evento comunicado a um destinatário por um canal.

- **Canais:** in-app, e-mail, **Telegram**; preferências por usuário/tenant.
- Notification Event Registry, templates, status, retry strategy, fila.
- Integra-se a memberships, comentários, sugestões, formulários, workflow editorial, auditoria, analytics e jobs.

### 24.1 Arquitetura de notificação

```txt
Evento de domínio (form.submitted, membership.invited, content.published)
        │
        ▼
  Notification Event Registry  (mapeia evento → template → canais → destinatários)
        │
        ▼
     Fila (job §33)  ──► Channel Dispatchers
        │                  ├── EmailDispatcher
        │                  ├── TelegramDispatcher (§25)
        │                  └── InAppDispatcher
        ▼
  notification_log (status, retries)
```

### 24.2 Modelo de banco — notificações

```sql
CREATE TABLE notification_events (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL REFERENCES tenants(id),
    event_key     VARCHAR(60) NOT NULL,  -- form.submitted, content.published...
    payload       JSONB NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE notification_deliveries (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    event_id        UUID NOT NULL REFERENCES notification_events(id),
    channel         VARCHAR(20) NOT NULL,  -- EMAIL, TELEGRAM, IN_APP
    recipient       VARCHAR(254) NOT NULL, -- email, chat id, user id
    template_key    VARCHAR(60) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, SENT, FAILED, RETRYING, DEAD
    attempts        INT NOT NULL DEFAULT 0,
    last_error      TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    sent_at         TIMESTAMPTZ
);
CREATE INDEX idx_delivery_status ON notification_deliveries (status, created_at);
```

### 24.3 Estratégia de retry

```txt
attempt 1 → fail → wait 1m  → attempt 2 → fail → wait 5m → attempt 3 → fail → wait 30m
→ attempt 4 → fail → DEAD (dead-letter; alerta operacional)
```

### 24.4 Preferências de notificação

```json
{
  "userId": "user_001",
  "tenantId": "tenant_cmss",
  "preferences": {
    "form.submitted":     { "email": true,  "telegram": true,  "inApp": true },
    "content.published":  { "email": false, "telegram": false, "inApp": true },
    "membership.invited": { "email": true,  "telegram": false, "inApp": true }
  }
}
```

### 24.5 Leis das Notificações

1. Notificação é assíncrona (fila + job), nunca bloqueia a requisição.
2. Todo envio tem status e estratégia de retry; falha final vira dead-letter auditada.
3. Preferências de canal são por usuário e por tenant.
4. Canais são trocáveis; o domínio não acopla a um provider (§25).
5. Templates são versionados e tenant-scoped quando customizados.
6. Notificação respeita LGPD: nunca envia dado sensível por canal inadequado.
7. Onboarding/tutorial tem prioridade sobre notificações modais genéricas. Um aviso de novidade ou feature não pode substituir o tour inicial quando ele ainda não foi concluído.

## 25. Telegram (Comunicação Operacional)

Telegram é **canal/provider operacional, não domínio** (ADR: Telegram como canal). Pode ser substituído por Slack, Discord ou e-mail sem quebrar o domínio.

- Usos: novo orçamento, nova candidatura, novo bug report, alertas operacionais.
- Configuração por tenant; segredos tenant-scoped, criptografados e auditados.

### 25.1 Posição arquitetural

```txt
Domínio ──► NotificationEvent ──► ChannelDispatcher (interface)
                                       ├── TelegramDispatcher   ← hoje
                                       ├── SlackDispatcher      ← trocável
                                       └── EmailDispatcher
   O domínio NÃO conhece "Telegram". Conhece "um canal".
```

### 25.2 Configuração por tenant

```sql
CREATE TABLE integration_secrets (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID NOT NULL REFERENCES tenants(id),
    integration  VARCHAR(40) NOT NULL,  -- TELEGRAM, SLACK...
    key_name     VARCHAR(60) NOT NULL,  -- bot_token, chat_id
    value_encrypted BYTEA NOT NULL,     -- criptografado em repouso
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    rotated_at   TIMESTAMPTZ,
    CONSTRAINT uq_secret UNIQUE (tenant_id, integration, key_name)
);
```

### 25.3 Fluxo de alerta (caso Maestro Beton — novo orçamento)

```txt
POST /public/.../forms/quote_request/submit
   └─► submission(NEW) ─► notification_event(form.submitted)
         └─► TelegramDispatcher: lê bot_token+chat_id do tenant (descriptografa)
               └─► envia mensagem ao grupo operacional ─► delivery SENT (auditado)
```

### 25.4 Leis do Telegram

1. Telegram é canal, não domínio; é substituível sem refatorar o core.
2. Segredos (bot token, chat id) são tenant-scoped, criptografados e auditados.
3. Falha do Telegram não derruba a operação; cai no retry/dead-letter (§24).
4. Rotação de segredos é suportada e auditada.

## 26. Comentários, Sugestões e Colaboração

Sistemas colaborativos do ecossistema (especialmente WikiDev e Loki):

- **Comentários/Discussões:** recursos comentáveis, threads, moderação (pré/pós), anônimos vs. autenticados, denúncias, curtidas/reações/menções (futuro). Comentário ≠ Fórum (fórum é evolução para comunidades).
- **Sugestões/Ideias:** backlog colaborativo rastreável (categorias, status, workflow, votação, priorização), relacionado a conteúdo e bug reports.

> Comentário != Discussão. O sistema nasce com comentários, mas preparado para evoluir sem refatoração.

### 26.1 Evolução planejada

```txt
Comentários → Threads → Discussões → Comunidades → Fóruns
   MVP                                  (futuro, via ADR)
```

### 26.2 Desacoplamento do conteúdo

```txt
Comment ──► ResourceReference (resourceType, resourceId)
   NÃO: Article.comments (propriedade embutida)
   SIM: comment aponta para o recurso, é polimórfico e tenant-scoped
```

Recursos comentáveis iniciais: Article, KnowledgeArticle, Manifesto, Reflection, Poem, Project, e JobPosting (opcional).

### 26.3 Modelo de banco — comentários e sugestões

```sql
CREATE TABLE comments (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL REFERENCES tenants(id),
    product_id    UUID NOT NULL REFERENCES products(id),
    resource_type VARCHAR(40) NOT NULL,
    resource_id   UUID NOT NULL,
    parent_id     UUID REFERENCES comments(id),  -- thread
    author_id     UUID,                           -- NULL = anônimo
    author_label  VARCHAR(80),                    -- nome exibido p/ anônimo
    content       TEXT NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED, PUBLISHED, ARCHIVED, DELETED
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_comments_resource ON comments (resource_type, resource_id, status);

CREATE TABLE suggestions (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL REFERENCES tenants(id),
    product_id    UUID NOT NULL REFERENCES products(id),
    category      VARCHAR(40) NOT NULL,  -- CONTENT, FEATURE, BUG, IMPROVEMENT
    title         VARCHAR(200) NOT NULL,
    body          TEXT,
    related_resource_type VARCHAR(40),
    related_resource_id   UUID,
    status        VARCHAR(20) NOT NULL DEFAULT 'OPEN', -- OPEN, TRIAGED, PLANNED, IN_PROGRESS, DONE, REJECTED
    votes         INT NOT NULL DEFAULT 0,
    author_id     UUID,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_suggestions_product ON suggestions (product_id, status, votes DESC);
```

### 26.4 Contrato JSON — Comentário

```json
{
  "id": "comment_001",
  "resourceType": "KnowledgeArticle",
  "resourceId": "article_001",
  "authorId": "user_001",
  "content": "Excelente conteúdo.",
  "status": "PUBLISHED",
  "createdAt": "2026-06-01T10:00:00Z",
  "updatedAt": "2026-06-01T10:00:00Z"
}
```

### 26.5 Moderação

```txt
Pré-moderação (padrão sensível): novo comentário → PENDING → moderador APPROVED/REJECTED → PUBLISHED
Pós-moderação (comunidade confiável): novo comentário → PUBLISHED → denúncia → revisão → ARCHIVED/DELETED
```

### 26.6 Leis da Colaboração

1. Comentário ≠ discussão ≠ fórum; o modelo evolui sem refatorar.
2. Comentário referencia o recurso (polimórfico); não é propriedade embutida.
3. Comentários respeitam tenant, produto, memberships, LGPD e moderação.
4. Comentário não participa do workflow editorial, mas suas ações são auditadas.
5. Sugestões são backlog rastreável com status e votação.
6. Anônimo é permitido onde a feature autorizar; sempre tenant-scoped.

## 27. Busca, Descoberta e Recuperação

- **MVP:** Full Text Search nativo do **PostgreSQL**.
- **Futuro:** motor de busca dedicado e busca semântica.
- **Search Document** indexa recursos por tenant, produto, content type, categoria, tags, locale e status. Busca administrativa vs. pública; busca respeita permissões; autocomplete; conteúdo relacionado; relevância.

### 27.1 Search Document (modelo de índice)

```sql
CREATE TABLE search_documents (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL REFERENCES tenants(id),
    product_id    UUID NOT NULL REFERENCES products(id),
    resource_type VARCHAR(40) NOT NULL,
    resource_id   UUID NOT NULL,
    locale        VARCHAR(10) NOT NULL DEFAULT 'pt-BR',
    title         TEXT,
    body          TEXT,
    tags          JSONB NOT NULL DEFAULT '[]'::jsonb,
    visibility    VARCHAR(20) NOT NULL DEFAULT 'PUBLIC', -- PUBLIC, ADMIN_ONLY
    status        VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
    tsv           tsvector,   -- vetor FTS gerado de title+body+tags
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_search_doc UNIQUE (resource_type, resource_id, locale)
);
CREATE INDEX idx_search_tsv ON search_documents USING GIN (tsv);
CREATE INDEX idx_search_scope ON search_documents (tenant_id, product_id, locale, visibility, status);
```

### 27.2 Pipeline de indexação (job §33)

```txt
content.published / updated / deleted
   └─► IndexJob: monta Search Document (title, body desnormalizado, tags, locale)
         └─► setweight(to_tsvector(title)) 'A' || setweight(to_tsvector(body)) 'B'
               └─► UPSERT em search_documents ; DELETE quando despublicado
```

### 27.3 Consulta — pública vs. administrativa

```txt
Pública:  visibility=PUBLIC AND status=PUBLISHED AND product_id=? AND locale=?
Admin:    respeita membership/role; pode incluir ADMIN_ONLY e DRAFT do tenant
Ranking:  ts_rank(tsv, query) + boost por tipo/recência
```

### 27.4 Contrato JSON — resultado de busca

```json
{
  "data": {
    "query": "concerto natal",
    "results": [
      { "resourceType": "event", "resourceId": "evt_01", "title": "Concerto de Natal 2026",
        "snippet": "...apresentação especial de <em>natal</em>...", "score": 0.91, "url": "/eventos/concerto-natal-2026" }
    ],
    "total": 1
  },
  "metadata": { "engine": "postgres-fts", "locale": "pt-BR" }
}
```

### 27.5 Gatilhos de migração para motor dedicado

| Gatilho | Sinal |
|---|---|
| Volume | > ~1M documentos por tenant grande |
| Latência | p95 de busca FTS acima do SLO |
| Recursos | necessidade de fuzzy/semântica/facetas ricas |
| Multi-locale | stemming avançado por idioma |

Migração FTS → Elasticsearch/OpenSearch (ou busca vetorial) é decisão de ADR futuro (§37, §43).

### 27.6 Leis da Busca

1. MVP usa FTS nativo do PostgreSQL; motor dedicado é evolução via ADR.
2. Todo Search Document é tenant + produto + locale scoped.
3. Busca pública só vê conteúdo PUBLIC e PUBLISHED.
4. Busca administrativa respeita membership/role.
5. Indexação é assíncrona e reage a publish/update/delete.
6. Despublicar remove o documento do índice público.

## 28. Analytics

Analytics é tenant-owned e por produto.

- Eventos carregam `tenantId` para dashboards/retenção/exportação/cobrança futura por tenant.
- Respeita privacidade (LGPD); agregações agrupadas por tenant; cache de dashboard tenant-scoped.
- Nenhum usuário soma/compara/exporta analytics de tenants aos quais não pertence (salvo roles globais de operação interna auditadas).

### 28.1 Modelo de banco — eventos e agregações

```sql
CREATE TABLE analytics_events (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     UUID NOT NULL,
    product_id    UUID NOT NULL,
    event_type    VARCHAR(40) NOT NULL,  -- PAGE_VIEW, CONTENT_VIEW, FORM_SUBMIT, SEARCH, DOWNLOAD
    resource_type VARCHAR(40),
    resource_id   VARCHAR(80),
    locale        VARCHAR(10),
    session_hash  VARCHAR(64),           -- pseudônimo, nunca PII
    referrer_class VARCHAR(40),          -- direct, search, social (sem URL crua se sensível)
    occurred_at   TIMESTAMPTZ NOT NULL DEFAULT now()
) PARTITION BY RANGE (occurred_at);

CREATE TABLE analytics_daily_rollup (
    tenant_id   UUID NOT NULL,
    product_id  UUID NOT NULL,
    day         DATE NOT NULL,
    event_type  VARCHAR(40) NOT NULL,
    count       BIGINT NOT NULL,
    PRIMARY KEY (tenant_id, product_id, day, event_type)
);
```

### 28.2 Pipeline analytics

```txt
POST /public/.../analytics/events  (anônimo, pseudonimizado)
   └─► fila ─► AnalyticsIngestJob ─► analytics_events (particionado por tempo)
         └─► RollupJob diário (§33) ─► analytics_daily_rollup
               └─► Dashboard (cache tenant-scoped §32)
```

### 28.3 Contrato JSON — evento de analytics (request)

```json
{
  "eventType": "PAGE_VIEW",
  "resourceType": "page",
  "resourceId": "home",
  "locale": "pt-BR",
  "referrerClass": "search"
}
```

### 28.4 Leis do Analytics

1. Todo evento carrega `tenantId` e `productId`.
2. Analytics respeita LGPD: pseudonimização, sem PII crua, retenção configurável.
3. Agregações são agrupadas por tenant; ninguém soma entre tenants alheios.
4. Page views não são auditoria formal (§23).
5. Dashboards usam cache tenant-scoped (§32).
6. Eventos antigos são purgados/anonimizados conforme `analytics.retentionDays` (§38).

---

# Parte V — Plataforma

## 29. Estrutura REST Oficial do MVP (ADR-0006)

REST é a API oficial do MVP, versionada (`/api/v1`), documentada via **OpenAPI**.

> REST é o contrato oficial do MVP. GraphQL é futuro. REST é o que será implementado. A API não expõe entidades JPA, tabelas ou detalhes internos — expõe contratos orientados ao domínio.

### 29.1 Convenções gerais

- Recursos no plural; verbos HTTP semânticos; status codes padronizados.
- Respostas e **erros padronizados** (Error Contract); paginação canônica (Pagination Contract).
- Política de breaking changes; testes de contrato.

#### 29.1.1 Estrutura global de namespaces

```txt
/api/v1
│
├── /public      # frontends externos; somente conteúdo publicado; tenant resolvido por domínio/slug/chave
├── /auth        # login context, aceite de convite, troca de tenant
├── /admin       # painel; sempre tenant-scoped; valida eixo de autorização (§5.1)
└── /internal    # jobs, health, integrações internas (não exposto publicamente)
```

#### 29.1.2 Status codes canônicos

| Código | Uso |
|---|---|
| 200 | OK (leitura/atualização) |
| 201 | Created |
| 204 | No Content (delete/idempotente) |
| 400 | Validação de payload |
| 401 | Não autenticado |
| 403 | Autenticado, sem permissão (feature off, role insuficiente) |
| 404 | Recurso inexistente **ou** de outro tenant (não revela) |
| 409 | Conflito (slug duplicado, dependência) |
| 422 | Regra de negócio violada |
| 429 | Rate limit |
| 500 | Erro interno |

#### 29.1.3 Envelope de resposta padrão

```json
{ "data": {}, "metadata": {}, "links": {} }
```

#### 29.1.4 Erro padrão (Error Contract)

```json
{
  "code": "RESOURCE_NOT_FOUND",
  "message": "Resource not found",
  "details": [],
  "correlationId": "req_abc123"
}
```

#### 29.1.5 Paginação canônica

```json
{ "items": [], "page": 1, "size": 20, "total": 100 }
```

### 29.2 API Pública vs. Administrativa

**API Pública** (frontends externos, somente conteúdo publicado):

```txt
GET  /public/products/{productSlug}/contract
GET  /public/products/{productSlug}/pages/{slug}
GET  /public/products/{productSlug}/content-types/{type}
GET  /public/products/{productSlug}/content-types/{type}/{slug}
GET  /public/products/{productSlug}/navigation
GET  /public/products/{productSlug}/assets/{assetId}
GET  /public/products/{productSlug}/sitemap.xml
GET  /public/products/{productSlug}/search?q=...
POST /public/products/{productSlug}/analytics/events
POST /public/products/{productSlug}/forms/{formId}/submit
```

A API pública resolve tenant por domínio/slug/produto/chave pública antes de buscar dados — permanece tenant-aware mesmo para visitantes anônimos. Drafts e revisões nunca aparecem.

**API Administrativa** (sempre tenant-scoped):

```txt
/admin/products, /admin/products/{id}, /admin/products/{id}/features,
/admin/products/{id}/users, /admin/products/{id}/pages, /admin/products/{id}/assets,
/admin/products/{id}/forms, /admin/products/{id}/seo, /admin/products/{id}/analytics,
/admin/products/{id}/content-types, /admin/products/{id}/navigation,
/admin/tenants/{id}/invitations, /admin/tenants/{id}/memberships,
/admin/.../revisions, /admin/.../audit
```

Toda rota administrativa valida: usuário autenticado, **tenant**, **membership**, **role**, **produto**, **feature**, **ação** e status do recurso. Consultas por id incluem `tenantId`; recursos fora do tenant retornam `404`.

### 29.3 Regras obrigatórias de backend (segurança multi-tenant)

1. Validar `tenantId` (nunca confiar no enviado isolado pelo cliente; validar contra membership).
2. Validar que `productId` pertence ao `tenantId`.
3. Validar membership ativa.
4. Validar role/permissão para a ação + feature ativa.

> Cliente pode informar intenção de contexto. O backend decide se o contexto é válido.

### 29.4 Pipeline de requisição administrativa

```txt
Request ─► [Auth Filter] JWT válido? ──não──► 401
   │ sim
   ▼
[Tenant Resolver] X-Tenant-Id + membership ativa? ──não──► 404
   │ sim
   ▼
[Product Guard] productId ∈ tenant? ──não──► 404
   │ sim
   ▼
[Feature Guard] feature ativa? ──não──► 403 FEATURE_DISABLED
   │ sim
   ▼
[Permission Evaluator] role permite ação? ──não──► 403
   │ sim
   ▼
[Controller → Service → Repository] (todo repo exige tenantId) ─► 200/201
   │
   ▼
[Audit] registra ação (§23)
```

### 29.5 Leis da API REST

1. A API expõe contratos de domínio, nunca entidades JPA/tabelas.
2. Toda rota admin atravessa o eixo de autorização (§5.1).
3. Recurso de outro tenant retorna 404, nunca 403 revelador.
4. Respostas e erros seguem os contratos canônicos.
5. Breaking change exige nova versão de API ou de contrato (§31).
6. A API pública entrega apenas conteúdo publicado.
7. Toda escrita relevante é auditada.

## 30. GraphQL Futuro (ADR-0007)

GraphQL é **camada futura**, não MVP. REST permanece oficial. Quando adotado: conviver com REST, respeitar tenant isolation e permissões, usar persisted queries e limites de complexidade, sem duplicar a fonte de verdade dos contratos. Federation e API Gateway são evoluções posteriores.

### 30.1 Por que não no MVP

| Motivo | Detalhe |
|---|---|
| Complexidade de autorização | Resolver-level auth multiplica a superfície do eixo §5.1 |
| Risco de over-fetch | Consultas profundas cross-tenant exigem guardas finos |
| Custo de tooling | Schema, persisted queries, limites de profundidade |
| Maturidade do domínio | Contratos REST ainda estabilizando |

### 30.2 Condições para adoção

```txt
adotar GraphQL quando:
  - contratos REST estáveis e versionados
  - necessidade real de agregação cliente-dirigida (ex.: dashboards ricos)
  - persisted queries + depth/complexity limits definidos
  - tenant isolation garantido por resolver guard reutilizando o PermissionEvaluator
  - sem duplicar a fonte de verdade: GraphQL lê dos mesmos serviços de domínio
```

### 30.3 Convivência REST + GraphQL (futuro)

```txt
            ┌─────────────┐
Cliente ───►│  REST /api  │──► Domain Services ──► DB
            ├─────────────┤        ▲
Cliente ───►│ GraphQL /gql│────────┘ (mesmos serviços; sem segunda verdade)
            └─────────────┘
```

### 30.4 Leis do GraphQL Futuro

1. REST permanece a fonte de verdade dos contratos até ADR contrário.
2. GraphQL, se adotado, lê dos mesmos serviços de domínio.
3. Tenant isolation e permissões valem por resolver, reutilizando o avaliador.
4. Persisted queries e limites de complexidade são obrigatórios.

## 31. Contratos JSON Canônicos (ADR-0002, ADR-0008)

O contrato é a fronteira estável frontend/backend. **Frontend consome contratos, não entidades internas.**

- **Envelopes:** Metadata, Versioning, Error, Pagination, Collection.
- **Contratos por recurso:** Product, Page, Section, Block, Navigation, ContentType, Form, Asset, Comment, Suggestion, Notification, SearchResult, Translation, Revision, Audit, Analytics.
- Validação runtime (JSON Schema); versionamento explícito; backward compatibility; schema evolution; contract registry.
- Contrato público não expõe entidade interna, tem owner, versão e exemplo. Frontend interpreta features, não infere por categoria.

Detalhes e schemas vivem em `docs/contracts/` e `docs/api/`.

### 31.1 Anatomia de um contrato

```txt
Contract
├── envelope         { data, metadata, links }
├── metadata         { contractVersion, generatedAt, tenant, locale }
├── versioning       semver: MAJOR.MINOR.PATCH
├── schema (JSON Schema)  validação runtime
├── owner            domínio/módulo responsável
└── example          exemplo canônico (em docs/contracts/)
```

### 31.2 Semântica de versionamento de contrato

| Mudança | Incremento | Compatibilidade |
|---|---|---|
| Adicionar campo opcional | MINOR | backward-compatible |
| Adicionar valor de enum | MINOR | backward-compatible* |
| Tornar campo opcional → obrigatório | MAJOR | breaking |
| Remover/renomear campo | MAJOR | breaking |
| Mudar tipo de campo | MAJOR | breaking |
| Correção sem mudança de forma | PATCH | compatible |

(* clientes devem tolerar enums desconhecidos — regra de robustez)

### 31.3 Contract Registry

```sql
CREATE TABLE contract_registry (
    contract_key  VARCHAR(60) NOT NULL,  -- 'page.public', 'product.public', 'asset.public'...
    version       VARCHAR(20) NOT NULL,  -- semver
    json_schema   JSONB NOT NULL,
    owner         VARCHAR(60) NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- DRAFT, ACTIVE, DEPRECATED
    example       JSONB,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (contract_key, version)
);
```

### 31.4 Validação runtime

```txt
build do contrato (assembler) ─► valida contra json_schema do registry (versão ativa)
   ├── válido   → entrega ao cliente + header X-Contract-Version
   └── inválido → 500 + alerta operacional (defeito de assembler, não do cliente)
```

### 31.5 Catálogo de envelopes

```json
// Collection envelope
{ "data": [], "metadata": { "page": 1, "size": 20, "total": 0 }, "links": { "next": null, "prev": null } }

// Single resource envelope
{ "data": {}, "metadata": { "contractVersion": "1.0.0" }, "links": { "self": "..." } }

// Error envelope
{ "code": "VALIDATION_ERROR", "message": "...", "details": [ { "field": "email", "issue": "invalid" } ] }
```

### 31.6 Política de breaking changes

```txt
breaking change detectado ─► NÃO altera contrato existente
   └─► publica nova versão MAJOR no registry (ex.: page.public 2.0.0)
         └─► clientes migram; janela de compatibilidade mantém 1.x.x DEPRECATED
               └─► retirada de 1.x.x só após janela + comunicação (§44)
```

### 31.7 Leis dos Contratos

1. Frontend consome contratos, nunca entidades internas.
2. Todo contrato tem owner, versão semver, schema e exemplo.
3. Contrato é validado em runtime contra o registry.
4. Breaking change cria nova versão MAJOR; não altera a existente.
5. Clientes toleram campos/enums desconhecidos (robustez).
6. O contrato público lista só features ativas; o frontend não infere por categoria.
7. Header `X-Contract-Version` acompanha respostas públicas.

## 32. Cache, Performance e Distribuição

- Estratégia: **Caffeine** (in-memory) no início; **Redis** quando necessário (ADR: cache evolutivo).
- Toda chave de cache tenant-owned inclui `tenantId` (client-side e server-side).
- Cache de contratos públicos; invalidação ao alterar conteúdo/feature/limite/permissão.

### 32.1 Camadas de cache

```txt
[Cliente/CDN futura]  HTTP cache (ETag, Cache-Control) p/ contratos públicos
        │
[App] Caffeine (in-memory, por instância)  ── MVP
        │  evolui para
[App] Redis (compartilhado entre instâncias) ── quando escalar horizontalmente
        │
[DB]  PostgreSQL (fonte de verdade)
```

### 32.2 Convenção de chaves (tenant-scoped)

```txt
contract:{tenantId}:{productSlug}:v{contractVersion}
page:{tenantId}:{productId}:{locale}:{slug}
nav:{tenantId}:{productId}:{type}:{locale}
features:{tenantId}:{productId}
```

Toda chave tenant-owned começa por `tenantId` — sem exceção (lei de isolamento, §36).

### 32.3 Invalidação

```txt
content.published / feature.changed / seo.updated / nav.updated
   └─► EvictEvent(tenantId, productId, keys[])
         └─► remove chaves afetadas em Caffeine/Redis + bump de contractVersion
               └─► CDN futura: purge por surrogate-key {tenantId}:{productId}
```

### 32.4 Matriz cache × dado

| Dado | Cacheável | TTL sugerido | Invalidação |
|---|---|---|---|
| Contrato público | sim | médio | em publish/feature/seo |
| Navegação pública | sim | médio | em nav.updated |
| Features do produto | sim | curto | em feature.changed |
| Conteúdo admin (draft) | não | — | sempre fresco |
| Analytics dashboard | sim | curto | em rollup |
| Submissões de form | não | — | dado sensível |

### 32.5 Leis do Cache

1. Toda chave tenant-owned inclui `tenantId`.
2. Caffeine no MVP; Redis quando houver múltiplas instâncias (ADR).
3. Conteúdo de draft e dados sensíveis não são cacheados.
4. Publicação/alteração de feature invalida cache e faz bump de versão.
5. CDN é evolução futura; quando entrar, usa surrogate-keys por tenant/produto.

## 33. Jobs e Processamento Assíncrono

- Jobs para processamento de imagens, notificações, agregações de analytics, indexação de busca, retenção/anonimização.
- Todo job tenant-owned recebe `tenantId` explícito ou itera por tenants com isolamento por lote; idempotência; observabilidade.

### 33.1 Catálogo de jobs

| Job | Gatilho | Tenant scope | Idempotente |
|---|---|---|---|
| ImageProcessingJob | upload de asset | por asset | sim (checksum) |
| NotificationDispatchJob | notification_event | por delivery | sim (delivery id) |
| SearchIndexJob | content publish/update/delete | por documento | sim (upsert) |
| AnalyticsIngestJob | evento recebido | por lote | sim (event id) |
| AnalyticsRollupJob | cron diário | itera tenants | sim (dia+tipo) |
| RetentionJob | cron diário | itera tenants | sim |
| AnonymizationJob | retenção expirada / remoção de user | por recurso | sim |
| ContractCacheWarmJob | publish | por produto | sim |

### 33.2 Modelo de banco — fila de jobs

```sql
CREATE TABLE jobs (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID,                 -- NULL p/ jobs de plataforma que iteram tenants
    type          VARCHAR(60) NOT NULL,
    payload       JSONB NOT NULL DEFAULT '{}'::jsonb,
    status        VARCHAR(20) NOT NULL DEFAULT 'QUEUED', -- QUEUED, RUNNING, DONE, FAILED, DEAD
    idempotency_key VARCHAR(120),
    attempts      INT NOT NULL DEFAULT 0,
    max_attempts  INT NOT NULL DEFAULT 5,
    scheduled_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    started_at    TIMESTAMPTZ,
    finished_at   TIMESTAMPTZ,
    last_error    TEXT,
    correlation_id VARCHAR(64),
    CONSTRAINT uq_job_idem UNIQUE (type, idempotency_key)
);
CREATE INDEX idx_jobs_queue ON jobs (status, scheduled_at);
```

### 33.3 Ciclo de vida de um job

```txt
QUEUED ─► RUNNING ─┬─► DONE
                   ├─► FAILED ─► retry (backoff) ─► RUNNING
                   └─► (attempts >= max) ─► DEAD (dead-letter; alerta §34)
```

### 33.4 Isolamento multi-tenant em jobs

```txt
Job tenant-owned:        recebe tenantId explícito; toda query usa tenantId.
Job de plataforma (cron): itera tenants em lotes; cada lote isolado;
                          falha de um tenant NÃO contamina os demais.
```

### 33.5 Leis dos Jobs

1. Job tenant-owned sempre recebe `tenantId`; job de plataforma itera com isolamento por lote.
2. Todo job é idempotente (idempotency key).
3. Falha tem retry com backoff; esgotado vira dead-letter auditado.
4. Job carrega correlation id para rastreio (§34).
5. Processamento de imagem, indexação, notificação, rollup e retenção são jobs, não trabalho síncrono na requisição.
6. Falha de um tenant em job de lote não contamina os outros.

## 34. Observabilidade

Logs, métricas, traces (correlation IDs), alertas, dashboards, retenção e cardinalidade. Logs incluem tenant quando há contexto e nunca vazam dados sensíveis. Observabilidade tem owner e padrões (`docs/operations/`).

### 34.1 Os três pilares

```txt
LOGS      structured JSON: { ts, level, msg, tenantId?, correlationId, traceId }
MÉTRICAS  contadores/histogramas: requests, latência p50/p95/p99, jobs, erros
TRACES    correlation/trace id atravessa request → service → job → notificação
```

### 34.2 Correlation/trace flow

```txt
Request ─► correlationId gerado (ou propagado) ─► log + audit + métricas
   └─► enfileira job ─► job herda correlationId ─► dispatcher herda ─► fim
        (uma requisição é rastreável ponta a ponta)
```

### 34.3 Métricas mínimas (SLO)

| Métrica | Alvo inicial |
|---|---|
| Disponibilidade da API | 99.5% |
| Latência p95 (admin) | < 400ms |
| Latência p95 (público/contrato) | < 200ms (com cache) |
| Taxa de erro 5xx | < 0.5% |
| Jobs em dead-letter | alerta se > 0 sustentado |

### 34.4 Regras de log

```txt
SEMPRE: nível, mensagem, correlationId, tenantId (quando há contexto)
NUNCA:  senhas, tokens, segredos, PII crua, conteúdo de submissão sensível
CARDINALIDADE: evitar labels de alta cardinalidade (ex.: userId em métrica)
```

### 34.5 Leis da Observabilidade

1. Toda requisição tem correlation id propagado a logs, auditoria e jobs.
2. Logs nunca contêm segredos ou PII crua.
3. Logs incluem `tenantId` quando há contexto, para fatiar por tenant.
4. Métricas evitam alta cardinalidade.
5. Dead-letter de job/notificação gera alerta.
6. Stack concreta de observabilidade é decisão de Daedalus + ADR (Lacuna B.14).

---

# Parte VI — Arquitetura

## 35. Banco de Dados (ADR-0010)

**PostgreSQL** é o banco inicial. Modelo relacional profundo + **JSONB com uso controlado** (config, limits, metadata, payloads — nunca substituto de domínio).

- Migrations com **Flyway** (ADR: Flyway); migration aplicada não é editada; mudanças destrutivas exigem plano.
- Toda tabela tenant-owned com `tenantId`; índices compostos com `tenantId`; índices seguem consultas reais; soft delete para relações históricas.
- Tabelas-núcleo: `tenants`, `tenant_memberships`, `tenant_settings`, `tenant_invitations`, `tenant_audit_logs`, `products`, `feature_catalog`, `product_features`, e tabelas de conteúdo/assets/forms/seo/analytics.

### 35.1 Mapa relacional do núcleo

```txt
tenants ──1:1── tenant_settings
   │ 1:N
   ├── tenant_memberships ──► (user global, Keycloak)
   ├── tenant_invitations
   ├── products ──1:N── product_features ──► feature_catalog ──N:N── feature_dependencies
   │     │ 1:N
   │     ├── product_memberships
   │     ├── pages ──1:N── sections ──1:N── blocks
   │     ├── content_entries
   │     ├── assets
   │     ├── form_definitions ──1:N── form_submissions
   │     ├── seo_metadata / seo_redirects
   │     ├── navigations ──1:N── navigation_items
   │     ├── translation_entries
   │     ├── revisions
   │     ├── comments / suggestions
   │     ├── search_documents
   │     └── analytics_events / analytics_daily_rollup
   └── audit_logs (append-only)  ·  integration_secrets  ·  jobs  ·  notification_*
```

### 35.2 Uso controlado de JSONB

| Uso permitido (JSONB) | Uso proibido (modelar relacional) |
|---|---|
| `config`, `limits`, `metadata` de feature | relações entre entidades |
| `data` de content_entry (validado por schema) | chaves estrangeiras "escondidas" em JSON |
| `snapshot` de revisão | dados consultados/filtrados em larga escala sem índice |
| `payload` de job/notificação | identidade, tenantId, status (sempre colunas) |

> JSONB serve para o que é genuinamente variável e validado por schema. Nunca para fugir de modelagem de domínio.

### 35.3 Estratégia de migração e evolução de dados

Migrações são versionadas com Flyway (`V{n}__descricao.sql`). Migration aplicada é **imutável**. Mudanças destrutivas seguem o padrão expand → migrate → contract:

```txt
EXPAND   (V_n)   adiciona coluna/tabela nova, nullable, sem remover nada
   │
   ▼
MIGRATE  (job/SQL) backfill de dados para a nova forma; código passa a escrever em ambas
   │
   ▼
CONTRACT (V_n+k)  após validação, remove a forma antiga
```

Compatibilidade histórica: dados antigos sob contrato v1 continuam servíveis; o assembler de contrato converte para a versão pedida quando possível (§31). Migração de dados que altera semântica exige ADR.

#### 35.3.1 Exemplo de migração expand/contract

```sql
-- V12__expand_product_default_locale.sql  (EXPAND)
ALTER TABLE products ADD COLUMN default_locale VARCHAR(10);
-- backfill (MIGRATE, idempotente)
UPDATE products SET default_locale = 'pt-BR' WHERE default_locale IS NULL;
-- V14__contract_product_default_locale.sql  (CONTRACT, após validação)
ALTER TABLE products ALTER COLUMN default_locale SET NOT NULL;
ALTER TABLE products ALTER COLUMN default_locale SET DEFAULT 'pt-BR';
```

### 35.4 Convenções de índices

```txt
- Índice composto SEMPRE começa por tenant_id em tabelas tenant-owned.
- Índice segue consulta real (não especulativo).
- Índice parcial para hot paths (ex.: WHERE status='PUBLISHED').
- GIN para JSONB consultável e para tsvector (busca §27).
- UNIQUE com escopo de tenant/produto (slug por produto, não global).
```

### 35.5 Leis do Banco

1. Toda tabela tenant-owned tem `tenantId` como coluna real (não JSONB).
2. Índices compostos começam por `tenantId`.
3. Migration aplicada é imutável; destrutiva segue expand/migrate/contract.
4. JSONB é para o variável/validado, nunca para esconder relações.
5. Soft delete para histórico; purga física só com política de retenção.
6. UNIQUE de slug é por escopo (produto/locale), não global.
7. Mudança de schema com impacto semântico exige ADR.

## 36. Segurança e Modelo de Ameaças

Vazamento cross-tenant é **incidente severo**. Vetores tratados: tenant spoofing, horizontal privilege escalation, acesso cruzado, consulta indevida, upload indevido, analytics indevido.

Mitigações obrigatórias: `tenantId` em todas as tabelas tenant-owned; índices/constraints compostos; resolução centralizada de tenant; services/repositories que exigem `tenantId`; validação de membership; testes de isolamento (fixtures com dois tenants); cache e storage tenant-scoped; erros que não revelam recursos de outro tenant (`404`); secrets criptografados e rotacionáveis; rate limiting; headers de segurança.

Decisões de segurança que alteram autenticação, autorização, secrets, uploads, rate limit, tenant isolation, deploy pipeline, backup ou logging sensível **exigem ADR**.

### 36.1 Modelo de ameaças (STRIDE aplicado)

| Categoria | Ameaça no Aegis | Mitigação |
|---|---|---|
| Spoofing | Cliente forja `X-Tenant-Id` | TenantResolver valida membership; nunca confia no header isolado |
| Tampering | Alterar `productId` para outro tenant | Product Guard: productId ∈ tenant, senão 404 |
| Repudiation | Negar ação feita | Audit logs imutáveis (§23) |
| Info Disclosure | Ler recurso de outro tenant | Repos exigem tenantId; cross-tenant → 404 |
| Denial of Service | Flood de submissões/API | Rate limiting + jobs assíncronos |
| Elevation of Privilege | Role de produto > role de tenant | Clamp Product ≤ Tenant (§10.3) |

### 36.2 Vetores de ataque e defesa (diagrama)

```txt
[Atacante autenticado no tenant A]
   │ tenta GET /admin/products/{prod_do_tenant_B}/pages
   ▼
[Product Guard] prod_B.tenantId != A  ──►  404 (não revela existência)
   │
   │ tenta header X-Tenant-Id: B (spoof)
   ▼
[Tenant Resolver] membership(userA, B)? não  ──►  404
   │
   │ tenta upload em produto sem feature 'assets'
   ▼
[Feature Guard] feature off  ──►  403 FEATURE_DISABLED
```

### 36.3 Casos de borda e resiliência (cross-cutting)

O Aegis trata explicitamente cenários extremos que, ignorados, viram incidentes:

| Caso de borda | Comportamento esperado |
|---|---|
| Usuário sem nenhum tenant | 403 NO_TENANT_ACCESS, tela "sem acesso" (§12) |
| Último owner tentando sair | bloqueado até transferência (§9, §10) |
| Convite expirado sendo aceito | rejeitado; novo convite necessário (§11) |
| Feature desativada com conteúdo existente | conteúdo preservado, oculto do contrato (§13) |
| Publicar com tradução STALE | permitido, mas sinalizado; SEO usa fallback (§19) |
| Rollback para revisão de schema antigo | assembler converte; se incompatível, bloqueia com aviso (§22, §31) |
| Job parcialmente falho em lote multi-tenant | tenant falho isolado; demais concluem (§33) |
| Submissão sem consentimento LGPD | rejeitada (§18, §38) |
| Asset órfão (referência apagada) | mantido até retenção; reportado em job de limpeza (§17) |
| Tenant suspenso com tráfego público | contrato congelado na última publicação (§8, §9) |
| Race de publicação concorrente | otimista por versão; conflito → 409 (§22) |
| Token de convite reutilizado | invalidado após aceite; segunda tentativa 409/410 (§11) |

### 36.4 Controles obrigatórios

```txt
Autenticação: Keycloak (OIDC), tokens curtos + refresh.
Autorização:  eixo §5.1 em toda rota sensível.
Segredos:     criptografados em repouso, rotacionáveis, nunca em log.
Uploads:      validação de mime/size, storage tenant-scoped, antivírus (futuro).
Rate limit:   por IP e por (tenant, rota) em endpoints públicos.
Headers:      HSTS, X-Content-Type-Options, X-Frame-Options, CSP (SPA).
Testes:       suíte de isolamento com 2 tenants em todo domínio sensível.
```

### 36.5 Leis da Segurança

1. Vazamento cross-tenant é incidente severo; o design previne, não remedia.
2. Cliente informa intenção de tenant; o backend decide a validade.
3. Recurso de outro tenant retorna 404, nunca 403 revelador.
4. Role de produto nunca excede role de tenant.
5. Segredos são criptografados, rotacionáveis e nunca logados.
6. Toda decisão de segurança relevante exige ADR.
7. Testes de isolamento multi-tenant são obrigatórios em domínios sensíveis.

## 37. Escalabilidade

- Crescimento progressivo dentro de Shared Schema; cache evolutivo (Caffeine→Redis); jobs assíncronos; busca evoluível (FTS→motor dedicado).
- Banco/schema por tenant e GraphQL/federation são evoluções via ADR conforme demanda real.

### 37.1 Eixos de crescimento e respostas

| Eixo | Sinal de pressão | Resposta evolutiva |
|---|---|---|
| Tráfego público | latência/throughput de contrato | cache → CDN (surrogate keys) |
| Instâncias da app | estado de cache inconsistente | Caffeine → Redis compartilhado |
| Volume de busca | p95 FTS alto | motor dedicado (Elastic/OpenSearch) |
| Volume de analytics | tabela quente | particionamento por tempo + rollup |
| Tenants grandes (enterprise) | isolamento/limites | schema por tenant → DB por tenant (ADR) |
| Consultas cliente-dirigidas | over/under-fetch | GraphQL (ADR §30) |

### 37.2 Diagrama de evolução

```txt
MVP (1 app + Postgres + Caffeine)
   │ tráfego cresce
   ▼
N apps + Redis + read replicas
   │ busca/analytics crescem
   ▼
motor de busca dedicado + analytics particionado/warehouse
   │ enterprise exige isolamento físico
   ▼
schema-por-tenant / DB-por-tenant (ADR) + possivelmente GraphQL/federation
```

### 37.3 Leis da Escalabilidade

1. Evoluir por necessidade real medida, não por especulação.
2. Cada salto de escala que muda arquitetura exige ADR.
3. Shared Schema é o ponto de partida; isolamento físico é destino enterprise.
4. Não antecipar Redis/Elastic/GraphQL antes do sinal de pressão (evitar Frankenstein).

## 38. LGPD e Governança de Dados

- Classificação, finalidade, retenção, consentimento, exportação e anonimização por tenant.
- Dados sensíveis: submissões de formulário (currículos, contatos), assets sensíveis, comentários, sugestões.
- Retenção configurável (`submissionRetentionDays`, `analytics.retentionDays`); anonimização de usuários removidos; atendimento a solicitações de exportação.
- LGPD pode **bloquear feature**; mudanças que envolvem dados pessoais exigem governança.

### 38.1 Classificação de dados

| Classe | Exemplos | Tratamento |
|---|---|---|
| Pública | conteúdo publicado, SEO | sem restrição especial |
| Interna | drafts, configurações | tenant-scoped, autorizada |
| Pessoal | nome, email em submissão/membership | consentimento + retenção + anonimização |
| Sensível | currículos, dados financeiros | acesso forte, criptografia, retenção curta |
| Segredo | tokens de integração | criptografado, nunca exposto/logado |

### 38.2 Ciclo de vida do dado pessoal

```txt
Coleta (com consentimento) ─► Uso (finalidade declarada) ─► Retenção (prazo configurável)
   └─► Expiração ─► Anonimização (job §33) ─► registro de auditoria preservado
Solicitação do titular:
   ├── Exportação ─► role forte + auditoria ─► pacote de dados do titular
   └── Eliminação ─► anonimização (mantém auditoria sem PII)
```

### 38.3 Direitos do titular (endpoints)

```txt
POST /api/v1/admin/tenants/{tenantId}/lgpd/export      # { subjectEmail } → pacote
POST /api/v1/admin/tenants/{tenantId}/lgpd/erase       # { subjectEmail } → anonimização
GET  /api/v1/admin/tenants/{tenantId}/lgpd/data-map    # mapa de onde há PII
```

### 38.4 Retenção e anonimização (configuração por tenant)

```json
{
  "tenantId": "tenant_conecta",
  "retention": {
    "submissionRetentionDays": 180,
    "analyticsRetentionDays": 365,
    "resumeRetentionDays": 90
  },
  "anonymization": { "onUserRemoval": true, "onRetentionExpiry": true }
}
```

### 38.5 LGPD pode bloquear feature

```txt
feature 'talent_pool' exige armazenar currículos (dado sensível)
   └─► se tenant não aceitou termos de tratamento de dados sensíveis
         └─► feature fica pending_dependency / bloqueada até conformidade (§13)
```

### 38.6 Leis da LGPD

1. Todo dado pessoal tem finalidade declarada e prazo de retenção.
2. Consentimento é obrigatório para coleta de dados pessoais (§18).
3. Anonimização preserva a trilha de auditoria sem reter PII.
4. Exportação/eliminação do titular são suportadas e auditadas.
5. Dados sensíveis têm acesso forte, criptografia e retenção curta.
6. LGPD pode bloquear feature; conformidade precede capacidade.
7. Segredos nunca são exportados nem logados.

## 39. Deploy, Ambientes e Entrega Contínua

> Deploy é processo controlado de entrega, validação, ativação e recuperação — não cópia de arquivos.

- Ambientes: `local → development → staging → production` (promotion).
- Distinções: Build (código→artefato), Release (versão planejada), Deploy (ativa em ambiente), Rollback, Migration.
- **Docker Compose** como deploy inicial (ADR: Docker Compose inicial); **Genesis Lab** como ambiente inicial.
- Deploy repetível, auditável, observável, seguro, reversível e documentado; health checks e migrations no deploy.
- **Entrega do frontend:** o Spring Boot serve a SPA na mesma origem e no mesmo deploy (ver Seção transversal "Estratégia Oficial de Frontend" e ADR-0009).

### 39.1 Pipeline de entrega

```txt
[commit] ─► [CI: build backend + build SPA dist/] ─► [testes: unit, contrato, isolamento]
   └─► [empacota dist/ no .jar / imagem Docker] ─► [push imagem]
         └─► [deploy ambiente] ─► [Flyway migrate] ─► [health check] ─► [tráfego]
               └─► falha em qualquer etapa ─► ROLLBACK (imagem anterior + plano de migration)
```

### 39.2 Promotion entre ambientes

```txt
local ──(dev OK)──► development ──(QA OK)──► staging ──(aprovação)──► production
  cada promotion: mesma imagem, configuração por ambiente (12-factor), segredos por ambiente
```

### 39.3 Distinções de deploy

| Conceito | Definição |
|---|---|
| Build | Código → artefato (.jar com SPA embarcada) |
| Release | Versão planejada e numerada do software |
| Deploy | Ativação de um release em um ambiente |
| Rollback | Reativar release anterior + reverter/compensar migration |
| Migration | Transformação de schema/dados aplicada no deploy (Flyway) |

### 39.4 Health checks e migrations no deploy

```txt
startup ─► Flyway valida/aplica migrations pendentes (expand-safe)
   └─► /internal/health/liveness  → app viva?
   └─► /internal/health/readiness → DB + dependências OK? só então recebe tráfego
```

### 39.5 Leis do Deploy

1. Deploy é repetível, auditável, observável, seguro, reversível e documentado.
2. Mesma imagem promovida entre ambientes; só a configuração muda.
3. Migrations rodam no deploy e seguem expand/contract (§35).
4. Sem health check OK, sem tráfego.
5. Rollback é sempre possível (imagem anterior + plano de migration).
6. SPA é empacotada no artefato do backend (ADR-0009, Seção A).

## 40. Backup e Disaster Recovery

- Política de backup com retenção; restore testado; RPO/RTO definidos.
- Disaster Recovery: continuidade operacional e sobrevivência da plataforma.
- Backup considera tenant isolation e dados sensíveis (`docs/operations/`).

### 40.1 Estratégia de backup

```txt
PostgreSQL:  base backup periódico + WAL archiving (point-in-time recovery)
Keycloak:    export do realm + backup do banco do Keycloak
Assets:      backup do storage (versionado/replicado)
Segredos:    backup do material de chave (separado, cofre)
Cadência:    full diário + WAL contínuo ; retenção em camadas (diário 30d, semanal 12s, mensal 12m)
```

### 40.2 RPO/RTO (alvos iniciais a validar)

| Métrica | Alvo inicial | Observação |
|---|---|---|
| RPO (perda máxima de dados) | ≤ 15 min | via WAL archiving |
| RTO (tempo de retorno) | ≤ 4 h | restore + health check |
| Restore test | mensal | restaurar em ambiente isolado |

(Valores numéricos finais são Lacuna B.12 — definir em `docs/operations/`.)

### 40.3 Disaster Recovery — runbook resumido

```txt
1. Detectar incidente (alerta §34) ─► declarar DR
2. Provisionar ambiente de recuperação (Docker Compose / infra Daedalus)
3. Restaurar PostgreSQL (base + WAL até ponto pré-incidente)
4. Restaurar realm Keycloak + storage de assets
5. Reaplicar segredos do cofre
6. Health check (readiness) ─► reabrir tráfego gradual
7. Post-mortem + audit review (incidente exige revisão de governança §46)
```

### 40.4 Backup e tenant isolation

```txt
Backup é global (todo o banco compartilhado), mas o RESTORE seletivo por tenant
exige exportação lógica filtrada por tenantId (suporte/forense). Dados sensíveis
no backup herdam a criptografia em repouso; nenhum dump é compartilhado sem
política e auditoria.
```

### 40.5 Leis do Backup e DR

1. Backup sem restore testado não é backup.
2. RPO/RTO são definidos, medidos e revisados.
3. Restore é testado periodicamente em ambiente isolado.
4. Backup respeita tenant isolation e criptografia de dados sensíveis.
5. Incidente de DR exige post-mortem e revisão de governança (§46).
6. Segredos têm backup separado e protegido.

## 41. Stack e Arquitetura do Backend

Stack recomendada: Java, Spring Boot, Spring Security, Keycloak, PostgreSQL, Flyway, OpenAPI, Docker, JUnit, Mockito, Bean Validation, Testcontainers (futuro).

Arquitetura modular por domínio:

```txt
core | identity | products | content | assets | forms | seo | analytics
modules/rh | modules/wiki | modules/library | modules/portfolio | modules/music
```

Camadas internas: controller, application/service, domain, repository, integration, config, security, dto, mapper, exception.

### 41.1 Diagrama de camadas

```txt
HTTP ─► [controller] ─► [application/service] ─► [domain] ─► [repository] ─► DB
                              │                        ▲
                              ├─► [integration] (Telegram, email, Keycloak)
                              ├─► [security] (PermissionEvaluator, TenantResolver)
                              ├─► [mapper] (entity ↔ contract DTO)
                              └─► [exception] (Error Contract §29)
```

### 41.2 Fronteiras de módulo

```txt
core     : tenants, memberships, features, auditoria, contratos, segurança (compartilhado)
identity : integração Keycloak, sync de user, sessão de contexto
content  : pages/sections/blocks, content types, revisões, SEO, navegação, i18n, busca
assets   : upload, processamento, storage tenant-scoped
forms    : definitions, submissions, validation/workflow engine, notificações
analytics: ingestão, rollup, dashboards
modules/*: domínios específicos (RH, wiki, library, portfolio, music) — habilitados por feature
```

Regra: módulo de domínio depende do core; o core nunca depende de módulo de domínio. Promover a core só quando vários produtos dependem (§44).

### 41.3 Leis da Stack/Arquitetura

1. O core é estável e compartilhado; módulos de domínio são habilitáveis por feature.
2. Módulo depende do core; o core não depende de módulo.
3. Toda camada de repositório exige `tenantId`.
4. Contrato (DTO) é separado da entidade de domínio por mapper.
5. Erros saem como Error Contract; nunca stack trace ao cliente.
6. Promoção de módulo a core exige justificativa de governança (§44).

---

# Parte VII — Governança

## 42. Governança Arquitetural

> Arquitetura sem governança vira opinião. Governança transforma decisões em patrimônio técnico.

- Decisões importantes devem sobreviver à memória de quem as tomou.
- Toda decisão arquitetural relevante possui contexto, motivação e consequência.

Objetivos: preservar decisões, evitar retrabalho/contradições, orientar humanos e agentes de IA, manter coerência, reduzir dívida técnica, impedir arquitetura paralela, tornar a evolução rastreável.

### 42.1 Fonte da verdade do ecossistema

```txt
Documento Mestre (este arquivo)  = constituição (o porquê e o quê)
   │
   ├── ADRs (docs/adr/)          = decisões formais (o porquê de cada escolha)
   ├── Contracts (docs/contracts) = fronteira estável (o que o frontend recebe)
   ├── API/OpenAPI (docs/api/)    = superfície técnica (como chamar)
   └── docs por área              = detalhamento operacional
```

A constituição prevalece sobre código; quando código e documento divergem, ou o código está errado, ou o documento precisa de revisão formal — nunca silêncio.

### 42.2 Matriz de classificação de decisões

| Classe | Exemplo | Exige ADR? |
|---|---|---|
| LOW | ajuste de cópia, typo | não |
| MEDIUM | refactor local, novo campo opcional | não (registrar em PR) |
| HIGH IMPACT | nova feature de core, mudança de contrato | sim |
| STRATEGIC | adoção de tecnologia, modelo multi-tenant | sim |
| IRREVERSIBLE | escolha de banco, formato de token | sim |

### 42.3 Leis da Governança

1. Decisão relevante vira ADR; ADR não desaparece.
2. A constituição prevalece; divergência exige revisão formal.
3. Nenhuma arquitetura paralela: tudo deriva do Documento Mestre.
4. HIGH/STRATEGIC/IRREVERSIBLE sempre exigem ADR.

## 43. ADRs

ADR (Architecture Decision Record) é o registro formal de uma decisão arquitetural relevante. Responde: contexto, decisão, motivação, alternativas, consequências e ponto de revisão.

**Template oficial:** Status, Contexto, Decisão, Consequências, Alternativas Consideradas, Impactos, Links Relacionados.

**Status:** `PROPOSED`, `ACCEPTED`, `SUPERSEDED`, `REJECTED`, `DEPRECATED`.

**Numeração:** `ADR-NNNN`, nunca reutilizada; ADR removido vira `REJECTED`/`DEPRECATED`/`SUPERSEDED`, não desaparece. Localização: `docs/adr/ADR-NNNN-slug.md`.

**Quando criar:** mudança de banco, autenticação, arquitetura, contrato, provider, adoção/remoção de tecnologia, breaking change, modelo multi-tenant, deploy, segurança, versionamento, fronteira entre módulos.

**Quando não criar:** ajuste visual, typo, refactor local, bugfix sem decisão, rename irrelevante, alteração temporária.

### ADRs V1 (catálogo de referência em `docs/adr/`)

Os ADRs completos vivem em `docs/adr/`. Esta é a tabela de referência:

| ADR | Decisão | Status | Arquivo |
|---|---|---|---|
| ADR-0001 | Product First | ACCEPTED | `docs/adr/ADR-0001-product-first.md` |
| ADR-0002 | Contract First | ACCEPTED | `docs/adr/ADR-0002-contract-first.md` |
| ADR-0003 | CMS First | ACCEPTED | `docs/adr/ADR-0003-cms-first.md` |
| ADR-0004 | Shared Database, Shared Schema, TenantId obrigatório | ACCEPTED | `docs/adr/ADR-0004-shared-schema.md` |
| ADR-0005 | Keycloak como IAM | ACCEPTED | `docs/adr/ADR-0005-keycloak.md` |
| ADR-0006 | REST First | ACCEPTED | `docs/adr/ADR-0006-rest-first.md` |
| ADR-0007 | GraphQL Future | ACCEPTED | `docs/adr/ADR-0007-graphql-future.md` |
| ADR-0008 | JSON Contracts | ACCEPTED | `docs/adr/ADR-0008-json-contracts.md` |
| ADR-0009 | SPA servida pelo Spring Boot | ACCEPTED | `docs/adr/ADR-0009-spa-pages.md` |
| ADR-0010 | PostgreSQL como banco inicial | ACCEPTED | `docs/adr/ADR-0010-postgresql.md` |

Decisões adicionais registradas conceitualmente (a formalizar em ADRs futuros): JSONB com uso controlado, Markdown não é layout, Blocks como estrutura, Revisions por snapshot, Audit logs imutáveis, Telegram como canal, SaaS Ready não SaaS Enabled, Caffeine/Redis evolutivo, Flyway, Docker Compose inicial, Genesis Lab.

### 43.1 Leis dos ADRs

1. ADR relevante é imutável após `ACCEPTED`; muda de status, não some.
2. Numeração nunca é reutilizada.
3. ADR responde contexto, decisão, motivação, alternativas, consequências.
4. Substituir uma decisão cria novo ADR que marca o anterior `SUPERSEDED`.

## 44. Governança de Contratos, APIs, Módulos e Features

- **Contratos:** owner, versão, exemplo, validação; breaking change exige versão e revisão de compatibilidade; não expõem entidade interna.
- **APIs:** OpenAPI para REST; versionamento `/api/v1`; depreciação documentada; respostas/erros padronizados; testes de contrato.
- **Módulos:** criar quando há domínio/owner/contratos/dados próprios; promover a core quando vários produtos dependem; core não cresce por conveniência.
- **Features:** existem no Feature Catalog com key, descrição, owner, dependências, status, impacto em UI/API/billing e estratégia de desativação.

### 44.1 Ciclo de depreciação

```txt
ACTIVE ─► DEPRECATED (anunciado, janela definida) ─► RETIRED (removido)
   contrato/feature/endpoint nunca some sem aviso + janela + migração
```

### 44.2 Critérios de promoção módulo → core

```txt
promover quando:
  - 2+ produtos dependem da mesma capacidade de forma estável
  - a capacidade tem contrato e dados próprios maduros
  - o core não acopla a um domínio específico ao absorvê-la
NÃO promover por conveniência ou para "economizar" um módulo.
```

### 44.3 Leis da Governança de Contratos/APIs/Módulos/Features

1. Breaking change cria versão nova; nunca altera a existente em uso.
2. Depreciação é anunciada, com janela e caminho de migração.
3. Módulo vira core por dependência real, não por conveniência.
4. Toda feature tem owner, status e estratégia de desativação.

## 45. Governança de IA

Agentes de IA atuam **dentro** da arquitetura, não criam arquitetura paralela:

- Consultar o Documento Mestre primeiro.
- Não alterar contratos sem registrar decisão; não adotar/remover tecnologia sem justificativa registrada.
- Preservar product-first, tenant isolation, contract-first e REST-first (MVP); manter GraphQL como futuro até ADR contrário.
- Sempre proteger LGPD e segurança; evitar refatoração ampla sem motivação.

### 45.1 Checklist do agente antes de agir

```txt
[ ] Consultei o Documento Mestre e ADRs relevantes?
[ ] A mudança respeita product-first, tenant isolation, contract-first?
[ ] Estou alterando um contrato? Se sim, registrei ADR/versão?
[ ] Estou adotando/removendo tecnologia? Se sim, há ADR?
[ ] LGPD e segurança preservadas?
[ ] Evitei refatoração ampla sem motivação registrada?
```

### 45.2 Leis da Governança de IA

1. O agente consulta a constituição antes de decidir.
2. Agente não cria arquitetura paralela.
3. Mudança de contrato/tecnologia por agente exige registro formal.
4. Tenant isolation, LGPD e segurança são invioláveis por automação.

## 46. Fonte da Verdade e Estrutura Documental

```txt
docs/
├── AEGIS_PMS_V1.md   # constituição do produto (este documento)
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

### 46.1 Cadência de revisão

```txt
mensal (desenvolvimento ativo)  ─► revisar lacunas (Seção B), ADRs pendentes
pós-incidente crítico           ─► revisão obrigatória + ADR de mitigação
pós-breaking change             ─► atualizar contratos e versão do documento
```

### 46.2 Leis da Fonte da Verdade

1. O Documento Mestre é a constituição, não documentação operacional.
2. ADRs são imutáveis; o documento versiona por V1/V2.
3. Revisão é mensal em desenvolvimento ativo e obrigatória pós-incidente.
4. Decisões HIGH/STRATEGIC/IRREVERSIBLE exigem ADR.

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

### 47.1 Dependências entre fases

```txt
Fase 0 (fundação: tenant/membership/feature/auth/audit)
   └─► Fase 1 (CMS Core: precisa de produto+feature+revisão)
         └─► Fase 2 (forms/analytics: precisa de produto+notificação+jobs)
               └─► Fase 3 (módulos reais: combinam features das fases anteriores)
                     └─► Fase 4 (biblioteca/wiki + colaboração + busca)
                           └─► Fase 5 (SaaS: billing sobre tenant)
```

Regra de ouro: **tenant isolation e contratos antes de domínios ricos.**

## 48. Evolução e SaaS

SaaS Ready, não SaaS Enabled: o MVP reserva espaço arquitetural para billing (Plan, Subscription, BillingCycle, Invoice, PaymentProvider, ProductPurchase) sem executar cobrança nem bloquear uso. Billing futuro tem o **tenant como unidade de cobrança**. Evoluções: GraphQL/federation, motor de busca dedicado, CDN para a SPA, banco/schema por tenant para enterprise — todas via ADR.

### 48.1 Modelo de billing reservado (futuro, não no MVP)

```txt
Tenant ──1:N── Subscription ──► Plan ──1:N── PlanFeature (mapeia features/limites)
   │                │ 1:N
   │                └── BillingCycle ──1:N── Invoice ──► PaymentProvider (Stripe/...)
   └── ProductPurchase (compras avulsas: e-commerce/LMS futuro)
```

```sql
-- TABELAS RESERVADAS (não criadas no MVP; documentam o espaço arquitetural)
-- plans(id, key, name, price, currency, limits JSONB, status)
-- subscriptions(id, tenant_id, plan_id, status, started_at, current_period_end)
-- billing_cycles(id, subscription_id, period_start, period_end, amount)
-- invoices(id, subscription_id, amount, status, due_at, paid_at, provider_ref)
-- product_purchases(id, tenant_id, product_id, sku, amount, status)
```

### 48.2 Separação cobrança × permissão

```txt
billing_metadata existe na feature (§13) ───► informa custo/plano
   MAS:
   permissão técnica (acesso/uso) NÃO depende de pagamento no MVP
   no SaaS futuro: status de subscription PODE limitar features via PlanFeature
   sempre por decisão comercial explícita, nunca acoplada ao core de autorização.
```

### 48.3 Tenant como unidade de cobrança

```txt
Por que o tenant?
  - já é a fronteira de isolamento, uso, limites e analytics
  - métricas de consumo (storage, submissões, API calls) já são tenant-scoped (§13, §28)
  - billing apenas projeta o consumo já medido por tenant em faturas
```

### 48.4 Roadmap de evolução técnica (via ADR)

| Evolução | Gatilho | ADR |
|---|---|---|
| Redis | múltiplas instâncias | futuro |
| Motor de busca dedicado | volume/latência FTS | futuro |
| CDN para a SPA | escala de entrega estática | estende ADR-0009 |
| Schema/DB por tenant | enterprise/isolamento físico | futuro |
| GraphQL/federation | agregação cliente-dirigida | ADR-0007 evoluído |
| Billing real | decisão comercial | futuro |

### 48.5 Leis da Evolução e SaaS

1. SaaS Ready: o espaço de billing existe; SaaS Enabled (cobrança) não, no MVP.
2. Permissão técnica não depende de pagamento no MVP.
3. O tenant é a unidade de cobrança futura.
4. Toda evolução estrutural (Redis, busca dedicada, CDN, DB/tenant, GraphQL) exige ADR.
5. Evoluir sem virar Frankenstein: por necessidade medida, não por especulação.

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
2. **Sincronização de identidade Keycloak ↔ base local** (quando criar `user` local, como tratar exclusão/desativação, claims mínimas no token) — ver §14.3; ADR futuro.
3. **Formato concreto dos tokens de convite** (assinatura vs. armazenamento, TTL, invalidação) — ver §11; spec em `docs/security/`.
4. **Estratégia de validação runtime de contratos** (biblioteca de JSON Schema, ponto de validação, versionamento de schema) — ver §31; `docs/contracts/`.
5. **Política de invalidação de cache** ao publicar conteúdo/alterar feature (eventos, TTL, chaves) — ver §32; `docs/operations/` quando Redis entrar.
6. **Modelo de permissões efetivas** consolidado (matriz role×ação×feature) — ver §10.2/§10.3; formalizar `PermissionEvaluator`.
7. **Estratégia de processamento de imagens** (síncrono vs. job, variações geradas, storage) — ver §17/§33; `docs/operations/`.
8. **Migração FTS → motor de busca dedicado**: gatilhos e critérios — ver §27.5; ADR futuro.
9. **Billing futuro**: apenas metadados reservados; modelo comercial real (planos, métricas, provider) não especificado — ver §48; fora do MVP.
10. **Estratégia de versionamento de contratos públicos** (header vs. path, janela de compatibilidade) — ver §31.2/§31.6; `docs/api/`.
11. **Testes de isolamento multi-tenant**: definidos como obrigatórios, mas o harness/fixtures concretos ainda não existem — ver §36.4.
12. **RPO/RTO numéricos** de backup/DR — ver §40.2; `docs/operations/`.
13. **Self-service onboarding de tenant** (criação de conta → tenant + owner + settings + produto em transação): previsto como fluxo futuro, não implementado.
14. **Observabilidade**: stack concreta (logs/métricas/traces) não escolhida — ver §34; Daedalus + ADR.

## C. Próximos Passos

### C.1 Aegis Backend

**Backlog arquitetural:**
- Formalizar os 10 ADRs (feito) e os ADRs conceituais pendentes (JSONB, Markdown, Blocks, Revisions, Audit, Telegram, Flyway, Docker, cache).
- Definir `PermissionEvaluator` e a matriz de permissões efetivas (tenant→produto→feature→ação) — §10.3.
- Definir contratos públicos canônicos e o contract registry (§31; `docs/contracts/`).
- Definir sincronização Keycloak ↔ base local (§14.3).

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

**Fluxos:** onboarding de produto por preset de categoria; tutorial inicial com prioridade sobre modais de novidade; convite/aceite de membro; troca de tenant (limpa estado); ativação/desativação de feature com avisos de dependência; publicação editorial (draft→review→publish) com diferenciação clara de status.

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

# Documento Mestre V1 — Resumo Executivo

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

## Mapa de seções

| Parte | Seções | Conteúdo |
|---|---|---|
| I | §1–§7 | Visão, problema, filosofia, princípios, leis-raiz |
| II | §8–§14 | Produto, Tenants, Memberships, Convites, Contexto, Features, IAM |
| III | §15–§21 | CMS Core, Content Types, Assets, Forms, i18n, SEO, Navegação |
| IV | §22–§28 | Revisões, Auditoria, Notificações, Telegram, Colaboração, Busca, Analytics |
| V | §29–§34 | REST, GraphQL, Contratos JSON, Cache, Jobs, Observabilidade |
| VI | §35–§41 | Banco, Segurança, Escalabilidade, LGPD, Deploy, Backup/DR, Stack |
| VII | §42–§46 | Governança, ADRs, Governança de contratos/IA, Fonte da verdade |
| VIII | §47–§48 | Roadmap de fases, Evolução e SaaS |
| Transversais | A/B/C | Frontend, Lacunas, Próximos Passos |

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

**Fim do Documento Mestre V1 (consolidado e expandido).** Documentação complementar e detalhamento por área vivem nos diretórios listados na Parte VII e em `docs/README.md`. Os ADRs completos estão em `docs/adr/` (tabela de referência em §43).
