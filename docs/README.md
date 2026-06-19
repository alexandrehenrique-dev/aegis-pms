# Documentação do Aegis PMS

Bem-vindo à documentação oficial do **Aegis** — a plataforma multi-tenant, multi-produto, modular e orientada a contratos do ecossistema **BYOP (Build Your Own Path)**.

> O Aegis gerencia **produtos digitais**, não páginas. Ele **administra conteúdo**; **não renderiza páginas públicas**. Frontends externos consomem **contratos**.

## Fonte da Verdade

O arquivo `AEGIS_PMS_V1.md` é o **documento mestre** do projeto — a versão completa e detalhada (DDL de banco, contratos JSON, diagramas, "Leis" por domínio) das oito partes que definem o Aegis. Toda decisão estrutural, princípio, política e estratégia oficial vive nele.

`AEGIS_DOCUMENTO_TECNICA.md` é a **versão condensada** do mesmo documento mestre — mesma estrutura (Parte I–VIII, seções 1–48, Seções Transversais A–D, Resumo Executivo), porém resumida. Use-a para leitura rápida ou onboarding; em caso de divergência, `AEGIS_PMS_V1.md` prevalece.

`AEGIS_DOCUMENTO_MESTRE_V1.md` foi **descontinuado** (era uma terceira versão, redundante e com boilerplate excessivo, sem a profundidade de CMS_V1 nem a objetividade de TECNICA). O arquivo permanece no histórico do git, mas não deve mais ser lido nem referenciado.

## Estrutura Documental

```txt
docs/
├── AEGIS_PMS_V1.md                # Documento mestre completo (fonte de verdade)
├── AEGIS_DOCUMENTO_TECNICA.md     # Versão condensada do documento mestre
├── Aegis-Vision.md                # Visão de produto e narrativa estratégica
├── Aegis-Domain-Map.md            # Mapa de domínios (core/supporting/generic) e dependências
├── README.md                      # Este índice
├── WORKTREE.md                    # Snapshot da árvore do repositório
├── implementation/                # Roteiros de implementação numerados (001–016) + specs de Figma Make
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

Fora de `docs/`, o repositório também contém `frontend/` (SPA React/Vite gerada via Figma Make, ainda 100% mockada) e, na raiz, `AGENTS.md` + `CONTRIBUTING.md` com as convenções de gitflow e o protocolo para agentes de IA (Claude, Codex ou outros).

## Por onde começar

| Você é... | Leia primeiro |
|---|---|
| Novo no projeto | `AEGIS_PMS_V1.md` (Parte I e Resumo Executivo) ou `AEGIS_DOCUMENTO_TECNICA.md` para uma versão rápida |
| Backend (Aegis) | `AEGIS_PMS_V1.md` Partes II–VI + `adr/` + `database/` + `api/` + `implementation/001` em diante |
| UX/UI (Eirene) | `AEGIS_PMS_V1.md` Parte III + `ux/` + `implementation/002-011` (UX/Figma Make) |
| Infra (Daedalus) | `AEGIS_PMS_V1.md` Parte VI + `deploy/` + `operations/` |
| Agente de IA | `AGENTS.md` primeiro, depois `AEGIS_PMS_V1.md` + `governance/` (Governança de IA) antes de qualquer mudança |

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
