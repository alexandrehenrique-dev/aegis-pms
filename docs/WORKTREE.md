# Worktree do Aegis CMS

**Gerado em:** 2026-06-15

Este documento descreve a arvore atual do projeto, explica o papel de cada pasta e resume cada arquivo existente. No momento, o projeto esta estruturado como um repositorio documental: nao ha codigo-fonte de backend, frontend, infraestrutura ou migrations versionadas nesta pasta.

## Arvore

```txt
.
└── docs/
    ├── AEGIS_BKP.md
    ├── AEGIS_CMS_V1.md
    ├── AEGIS_DOCUMENTO_MESTRE_V1.md
    ├── README.md
    ├── adr/
    │   ├── ADR-0001-product-first.md
    │   ├── ADR-0002-contract-first.md
    │   ├── ADR-0003-cms-first.md
    │   ├── ADR-0004-shared-schema.md
    │   ├── ADR-0005-keycloak.md
    │   ├── ADR-0006-rest-first.md
    │   ├── ADR-0007-graphql-future.md
    │   ├── ADR-0008-json-contracts.md
    │   ├── ADR-0009-spa-pages.md
    │   ├── ADR-0010-postgresql.md
    │   └── README.md
    ├── api/
    │   └── README.md
    ├── contracts/
    │   └── README.md
    ├── database/
    │   └── README.md
    ├── deploy/
    │   └── README.md
    ├── governance/
    │   └── README.md
    ├── modules/
    │   └── README.md
    ├── operations/
    │   └── README.md
    ├── security/
    │   └── README.md
    └── ux/
        └── README.md
```

## Pastas

### `.`

Raiz do projeto `aegis-cms`. Atualmente contem apenas a pasta `docs/`. Nao ha arquivos de configuracao, codigo de aplicacao, manifestos de build, Dockerfiles ou metadados Git nesta raiz.

### `docs/`

Centro documental do Aegis CMS. Guarda a constituicao do produto, documentos consolidados, indice principal e subpastas tematicas. A documentacao define o Aegis como uma plataforma multi-produto, multi-tenant, modular e orientada a contratos, em que o backend administra conteudo e frontends externos consomem contratos JSON.

### `docs/adr/`

Catalogo de Architecture Decision Records. Registra decisoes arquiteturais aceitas, com contexto, decisao, consequencias, alternativas, impactos e links relacionados. E a memoria formal das escolhas estruturais do projeto.

### `docs/api/`

Espaco reservado para documentacao das APIs do Aegis. Define que REST e a API oficial do MVP, com OpenAPI versionada, enquanto GraphQL permanece como evolucao futura.

### `docs/contracts/`

Espaco dos contratos JSON canonicos. Representa a fronteira estavel entre backend e frontends consumidores, incluindo schemas publicos, administrativos, envelopes, versionamento e validacao.

### `docs/database/`

Documentacao do modelo de dados. Cobre schema relacional, migrations com Flyway, PostgreSQL, uso controlado de JSONB, indices e estrategia multi-tenant com `tenantId` obrigatorio.

### `docs/deploy/`

Documentacao de build, release, ambientes, CI/CD e entrega. Registra a estrategia de servir a SPA administrativa pelo Spring Boot na mesma origem e no mesmo deploy.

### `docs/governance/`

Documentacao de governanca arquitetural e governanca de IA. Define como decisoes, contratos, APIs, modulos, features, seguranca e agentes de IA devem respeitar a arquitetura oficial.

### `docs/modules/`

Espaco para documentacao por modulo. Lista os modulos previstos, como `core`, `identity`, `content`, `assets`, `forms`, `seo`, `analytics` e dominios especificos como RH, musica, portfolio, biblioteca e wiki.

### `docs/operations/`

Documentacao operacional. Cobre observabilidade, jobs, notificacoes, backup, disaster recovery, runbooks, incidentes e regras de processamento tenant-aware.

### `docs/security/`

Documentacao de seguranca. Cobre modelo de ameacas, isolamento multi-tenant, Keycloak, autorizacao contextual, secrets, uploads, headers, rate limiting e LGPD.

### `docs/ux/`

Diretrizes de UX/UI do painel administrativo Aegis, sob responsabilidade de Eirene. Define principios de interface, referencias de produto SaaS, menus dinamicos, telas minimas e limites do editor de conteudo no MVP.

## Arquivos

### `docs/AEGIS_BKP.md`

Backup consolidado da documentacao Aegis. Reune em um unico arquivo a fonte de verdade atual e os documentos auxiliares existentes em `docs/`, preservando regras de negocio, decisoes arquiteturais, ADRs, contratos conceituais, governanca, operacao, seguranca e diretrizes. E um documento grande de referencia e recuperacao.

### `docs/AEGIS_CMS_V1.md`

Documento mestre expandido do produto. Detalha a especificacao AMPS do Aegis em oito partes: visao, produto, conteudo, operacao, plataforma, arquitetura, governanca e roadmap. Inclui leis normativas por dominio, modelos de banco, contratos JSON, endpoints REST, diagramas e decisoes de backend, frontend, seguranca, deploy e operacao.

### `docs/AEGIS_DOCUMENTO_MESTRE_V1.md`

Constituicao principal do projeto. Resume a visao oficial do Aegis como plataforma product-first, contract-first, CMS-first, multi-tenant, modular e SaaS-ready. Define principios arquiteturais, produto, tenants, features, conteudo, operacao, plataforma, arquitetura, governanca e roadmap. O proprio `docs/README.md` aponta este arquivo como fonte de verdade.

### `docs/README.md`

Indice da documentacao. Apresenta o Aegis, indica a fonte da verdade, descreve a estrutura documental, sugere rotas de leitura por perfil, lista os ADRs atuais e registra os papeis do Panteao BYOP.

### `docs/adr/README.md`

Guia dos ADRs. Explica o proposito dos registros de decisao arquitetural, suas regras, quando criar ou nao criar um ADR, status validos e o catalogo atual de decisoes aceitas.

### `docs/adr/ADR-0001-product-first.md`

Decide que a entidade raiz operacional do Aegis e o Produto, nao pagina, site ou cliente. Essa escolha centraliza features, conteudo, SEO, analytics, assets e contratos ao redor de produtos digitais.

### `docs/adr/ADR-0002-contract-first.md`

Decide que o Aegis e contract-first. Frontends nao devem depender do banco ou de entidades internas; eles consomem contratos versionados, estaveis e governados.

### `docs/adr/ADR-0003-cms-first.md`

Decide que conteudo publico deve ser administrado pelo Aegis, nao hardcoded no frontend. Reforca o CMS como fonte de verdade de conteudo, sem transformar o Aegis em renderizador publico.

### `docs/adr/ADR-0004-shared-schema.md`

Decide pela estrategia multi-tenant inicial de shared database/shared schema com `tenantId` obrigatorio. Estabelece isolamento logico forte por query, indices, autorizacao e validacao de pertencimento.

### `docs/adr/ADR-0005-keycloak.md`

Decide usar Keycloak como provedor de identidade. O Keycloak cuida de autenticacao e identidade; o Aegis mantem autorizacao contextual, tenants, memberships, produtos e features.

### `docs/adr/ADR-0006-rest-first.md`

Decide que REST e a API oficial do MVP. Define REST versionado como caminho inicial por simplicidade, previsibilidade, documentacao via OpenAPI e compatibilidade com consumidores externos.

### `docs/adr/ADR-0007-graphql-future.md`

Decide que GraphQL e evolucao futura, nao parte do MVP. Mantem REST como oficial ate novo ADR, preservando espaco arquitetural para GraphQL quando houver maturidade e necessidade.

### `docs/adr/ADR-0008-json-contracts.md`

Decide que contratos JSON canonicos sao a fronteira frontend/backend. Define envelopes, schemas, versionamento, validacao em runtime e proibicao de expor entidades internas como contrato publico.

### `docs/adr/ADR-0009-spa-pages.md`

Decide que a SPA administrativa sera servida pelo Spring Boot na mesma origem e no mesmo deploy. A SPA fica desacoplada no codigo, mas e empacotada junto ao backend para simplificar deploy, sessao e CORS no MVP.

### `docs/adr/ADR-0010-postgresql.md`

Decide que PostgreSQL e o banco inicial do Aegis. Estabelece modelo relacional como base, JSONB com uso controlado, migrations com Flyway, FTS nativo para o MVP e indices orientados por consultas reais.

### `docs/api/README.md`

Define o proposito da documentacao de API. Registra conteudo esperado como OpenAPI, convencoes de versionamento, paginacao, erros, status codes, separacao entre API publica e administrativa, e regras de autorizacao tenant-scoped.

### `docs/contracts/README.md`

Define o proposito dos contratos JSON canonicos. Lista schemas esperados, contratos administrativos, envelopes, contract registry e regras de owner, versao, exemplos, compatibilidade e validacao.

### `docs/database/README.md`

Define o proposito da documentacao de banco. Lista conteudo esperado para schema, migrations, JSONB, multi-tenancy, indices e regras como `tenantId` obrigatorio, migrations imutaveis e preferencia por soft delete em relacoes historicas.

### `docs/deploy/README.md`

Define a estrategia de deploy. Cobre ambientes, pipeline de backend e frontend, Docker/Compose, health checks, migrations, rollback e o modelo em que o Spring Boot entrega API e SPA no mesmo artefato.

### `docs/governance/README.md`

Define governanca arquitetural e de IA. Estabelece fontes de verdade, processo de ADRs, revisao de documentacao, classificacao de impacto e comportamento esperado de agentes para evitar arquitetura paralela.

### `docs/modules/README.md`

Define a documentacao por modulo. Lista modulos previstos e regras para criar novo modulo, reutilizar modulo existente, promover responsabilidades ao core e evitar crescimento do core por conveniencia.

### `docs/operations/README.md`

Define operacao, observabilidade e continuidade. Cobre logs, metricas, traces, alertas, jobs, idempotencia, notificacoes, backup, RPO/RTO, disaster recovery, runbooks e post mortems.

### `docs/security/README.md`

Define seguranca, ameacas e politicas. Registra isolamento multi-tenant, Keycloak, autorizacao contextual, LGPD e regra central de que o cliente pode informar contexto, mas o backend decide se ele e valido.

### `docs/ux/README.md`

Define diretrizes de UX/UI para o painel. Estabelece sensacao de SaaS moderno, menus por tenant/role/features, backend como fonte de seguranca, telas minimas do MVP e editor de conteudo sem preview visual completo no MVP.
