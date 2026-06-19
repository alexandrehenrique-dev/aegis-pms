# Worktree do Aegis PMS

**Gerado em:** 2026-06-19 (regenerado — a versão anterior, de 2026-06-15, estava obsoleta: referenciava `AEGIS_BKP.md` que não existe mais, não listava `docs/implementation/`, `docs/AEGIS_DOCUMENTO_TECNICA.md`, `docs/Aegis-Vision.md`, `docs/Aegis-Domain-Map.md`, nem o `frontend/`, e afirmava que o projeto "não tem código-fonte" — o que não é mais verdade).

Este documento descreve a árvore atual do projeto, explica o papel de cada pasta e resume os arquivos mais relevantes. O repositório real chama-se **`aegis-pms`** (não `aegis-cms` — nome usado apenas no título de alguns documentos legados).

## Árvore

```txt
.
├── AGENTS.md                       # Protocolo para agentes de IA (gitflow, commits, branches)
├── CONTRIBUTING.md                 # Convenções de contribuição, agnósticas de agente/ferramenta
├── docs/
│   ├── AEGIS_CMS_V1.md             # Documento mestre completo (fonte de verdade)
│   ├── AEGIS_DOCUMENTO_TECNICA.md  # Versão condensada do documento mestre
│   ├── AEGIS_DOCUMENTO_MESTRE_V1.md# Descontinuado — apenas aviso de redirecionamento
│   ├── Aegis-Vision.md             # Visão de produto e narrativa estratégica
│   ├── Aegis-Domain-Map.md         # Mapa de domínios (core/supporting/generic) e dependências
│   ├── README.md
│   ├── WORKTREE.md                 # Este arquivo
│   ├── implementation/             # Roteiros numerados 001–016 + specs de Figma Make
│   │   ├── 001_aegis_pms_roteiro_fundacao_operacional.md
│   │   ├── 002_aegis_pms_ux_master_roadmap.md
│   │   ├── 003_aegis_pms_ux_architecture.md
│   │   ├── 004_aegis_pms_screen_inventory.md
│   │   ├── 005_aegis_pms_user_journeys.md
│   │   ├── 006_aegis_pms_ux_rules_and_interaction_patterns.md
│   │   ├── 007_aegis_pms_design_system_mapping.md
│   │   ├── 008_aegis_pms_ux_master_blueprint.md
│   │   ├── 009_aegis_pms_figma_master_specification.md
│   │   ├── 010_aegis_pms_visual_language_specification.md
│   │   ├── 011_aegis_pms_frontend_architecture_blueprint.md
│   │   ├── 012_aegis_pms_domain_driven_product_blueprint.md
│   │   ├── 013_aegis_pms_backend_architecture_blueprint.md
│   │   ├── 014_aegis_pms_data_persistence_blueprint.md
│   │   ├── 015_aegis_pms_platform_evolution_roadmap.md
│   │   ├── 016_aegis_constitution.md
│   │   ├── figma-make.md
│   │   └── figma-make-sprints-refinamento-v2.md
│   ├── sprints/                    # Sprints executáveis (criadas nesta sessão)
│   ├── adr/                        # ADR-0001 a ADR-0010 + README
│   ├── api/README.md
│   ├── contracts/README.md
│   ├── database/README.md
│   ├── deploy/README.md
│   ├── governance/README.md
│   ├── modules/README.md
│   ├── operations/README.md
│   ├── security/README.md
│   └── ux/README.md
└── frontend/                       # SPA React/Vite gerada via Figma Make (mockada, sem backend real)
    ├── README.md                    # Boilerplate genérico do Figma Make (não específico do Aegis)
    ├── ATTRIBUTIONS.md
    ├── package.json                 # "@figma/my-make-file" — React 18, Vite 6, Tailwind 4, MUI, Radix, react-dnd
    ├── vite.config.ts
    ├── pnpm-workspace.yaml
    ├── postcss.config.mjs
    ├── default_shadcn_theme.css
    ├── index.html
    ├── guidelines/Guidelines.md
    ├── plans/
    │   └── background-estou-construindo-zesty-clock.md   # Plano "Sprint 09" (obsoleto vs. código atual)
    └── src/
        ├── main.tsx
        ├── app/
        │   ├── App.tsx              # Aplicação inteira em arquivo único (~1030 linhas)
        │   └── components/
        │       ├── ui/              # ~50 componentes shadcn/ui (button, dialog, table, sidebar, etc.)
        │       └── figma/ImageWithFallback.tsx
        └── imports/
            ├── 002...016_*.md       # Cópias dos roteiros de implementation/ usadas pelo Figma Make
            └── pasted_text/
                ├── sprint-02-product-dashboard.md
                ├── content-module.md
                ├── knowledge-graph-module.md
                ├── aegis-pms-sprint-04.md
                ├── aegis-pms-sprint-06.md
                └── aegis-pms-sprint-08.md
```

## Pastas

### `.`

Raiz do repositório `aegis-pms`. Git inicializado, remote `git@github.com:alexandrehenrique-dev/aegis-pms.git`, branch `main`. Contém `docs/` (documentação) e `frontend/` (SPA mockada). Backend (Spring Boot) ainda não foi iniciado.

### `docs/`

Centro documental do Aegis. Define a plataforma como multi-produto, multi-tenant, modular e orientada a contratos. Fonte de verdade: `AEGIS_CMS_V1.md`.

### `docs/implementation/`

Roteiros de implementação concretos e numerados. `001` cobre a fundação operacional (repo, env, Docker, Keycloak, Spring Boot base). `002`–`011` documentam o processo de geração de UX via Figma Make (roadmap, arquitetura de UX, inventário de telas, jornadas, regras de interação, design system, blueprint visual). `012`–`015` cobrem domínio, arquitetura de backend, persistência e evolução da plataforma. `016` é a constituição do projeto. `figma-make.md` e `figma-make-sprints-refinamento-v2.md` são specs de geração/refinamento de UI usadas como prompt para o Figma Make.

### `docs/sprints/`

Pasta com as sprints executáveis criadas para fechar as lacunas identificadas (criação de tenant/produto, integração frontend-backend, roles/permissões por feature, alinhamento do login mockado com Keycloak, modo standalone vs. integrado).

### `docs/adr/`

ADR-0001 a ADR-0010: Product First, Contract First, CMS First, Shared Schema, Keycloak como IAM, REST First, GraphQL Future, JSON Contracts, SPA servida pelo Spring Boot, PostgreSQL.

### `frontend/`

SPA React gerada pelo Figma Make a partir das specs em `docs/implementation/002-011`. 100% mockada: nenhuma chamada de API real, nenhuma integração com Keycloak, ações assíncronas simuladas via `setTimeout`. Toda a aplicação vive em `src/app/App.tsx`, um único arquivo de ~1030 linhas com máquina de estados (`useState<Screen>`) ao invés de roteamento por URL (React Router está instalado mas não é usado). `frontend/plans/background-estou-construindo-zesty-clock.md` é um plano de sprint ("Sprint 09 — Autenticação") que descreve o login como não implementado, mas o código já o implementa de forma mais completa — este plano está desatualizado e deve ser ignorado ou substituído pelas sprints em `docs/sprints/`.

## Inconsistências corrigidas nesta sessão

1. Três documentos competindo como "fonte de verdade" (`AEGIS_CMS_V1.md`, `AEGIS_DOCUMENTO_TECNICA.md`, `AEGIS_DOCUMENTO_MESTRE_V1.md`) → `AEGIS_DOCUMENTO_MESTRE_V1.md` descontinuado, `docs/README.md` corrigido para apontar `AEGIS_CMS_V1.md` como mestre e `AEGIS_DOCUMENTO_TECNICA.md` como versão condensada.
2. ARTIGO V da Constituição (`016_aegis_constitution.md`) contradizia a previsão de um produto de domínio RH (Conecta Talentos) ao dizer "Aegis não será plataforma de RH" → clarificado: Aegis não é um RH-SaaS vertical, mas pode hospedar produtos de domínio RH como qualquer outro produto.
3. Este arquivo (`WORKTREE.md`) estava desatualizado desde 2026-06-15 → regenerado para refletir o estado real do repositório, incluindo `frontend/` e `docs/implementation/`.
