# Documentação do Aegis CMS

Bem-vindo à documentação oficial do **Aegis** — a plataforma multi-tenant, multi-produto, modular e orientada a contratos do ecossistema **BYOP (Build Your Own Path)**.

> O Aegis gerencia **produtos digitais**, não páginas. Ele **administra conteúdo**; **não renderiza páginas públicas**. Frontends externos consomem **contratos**.

## Fonte da Verdade

O arquivo `AEGIS_DOCUMENTO_MESTRE_V1.md` é a **constituição** do projeto. Toda decisão estrutural, princípio, política e estratégia oficial vive nele. Os demais diretórios detalham e operacionalizam o que o Documento Mestre estabelece.

## Estrutura Documental

```txt
docs/
├── AEGIS_DOCUMENTO_MESTRE_V1.md   # Constituição do produto (fonte de verdade)
├── README.md                      # Este índice
├── adr/                           # Architecture Decision Records (decisões formais)
├── api/                           # REST/OpenAPI e GraphQL futuro
├── contracts/                     # Contratos JSON canônicos (fronteira frontend/backend)
├── database/                      # Schema, migrations (Flyway), modelagem
├── deploy/                        # Ambientes, build, entrega (SPA servida pelo Spring Boot)
├── modules/                       # Documentação por módulo (core + domínios)
├── operations/                    # Observabilidade, jobs, backup, DR, runbooks
├── security/                      # Ameaças, isolamento multi-tenant, LGPD, políticas
├── ux/                            # Diretrizes de UX/UI (Eirene)
└── governance/                    # Processo de governança arquitetural e de IA
```

## Por onde começar

| Você é... | Leia primeiro |
|---|---|
| Novo no projeto | `AEGIS_DOCUMENTO_MESTRE_V1.md` (Parte I e Resumo Executivo) |
| Backend (Aegis) | Documento Mestre Partes II–VI + `adr/` + `database/` + `api/` |
| UX/UI (Eirene) | Documento Mestre Parte III + `ux/` |
| Infra (Daedalus) | Documento Mestre Parte VI + `deploy/` + `operations/` |
| Agente de IA | Documento Mestre + `governance/` (Governança de IA) antes de qualquer mudança |

## Mapa de ADRs

| ADR | Decisão |
|---|---|
| [ADR-0001](adr/ADR-0001-product-first.md) | Product First |
| [ADR-0002](adr/ADR-0002-contract-first.md) | Contract First |
| [ADR-0003](adr/ADR-0003-cms-first.md) | CMS First |
| [ADR-0004](adr/ADR-0004-shared-schema.md) | Shared Database, Shared Schema, TenantId obrigatório |
| [ADR-0005](adr/ADR-0005-keycloak.md) | Keycloak como IAM |
| [ADR-0006](adr/ADR-0006-rest-first.md) | REST First |
| [ADR-0007](adr/ADR-0007-graphql-future.md) | GraphQL Future |
| [ADR-0008](adr/ADR-0008-json-contracts.md) | JSON Contracts |
| [ADR-0009](adr/ADR-0009-spa-pages.md) | SPA servida pelo Spring Boot |
| [ADR-0010](adr/ADR-0010-postgresql.md) | PostgreSQL como banco inicial |

## Papéis do Panteão BYOP

- **Zeus** — visão sistêmica.
- **Athena** — estratégia e requisitos (orquestração).
- **Aegis** — arquitetura backend.
- **Eirene** — UX/UI.
- **Daedalus** — infraestrutura.
- **Loki** — execução e autoria do desenvolvimento.
