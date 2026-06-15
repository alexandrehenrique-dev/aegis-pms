# docs/governance — Governança Arquitetural

## Propósito

Documentação da **governança arquitetural** do Aegis: como decisões são tomadas, registradas, revisadas e preservadas.

> Arquitetura sem governança vira opinião. Governança transforma decisões em patrimônio técnico.

## Conteúdo esperado

- Processo de criação e revisão de ADRs (o catálogo vive em `docs/adr/`).
- Matriz de decisão e classificação de impacto (LOW, MEDIUM, HIGH, STRATEGIC, IRREVERSIBLE).
- Processos de depreciação e substituição.
- Política de revisão periódica da documentação.
- Governança de contratos, APIs, módulos, features, tenants, segurança, LGPD, dados, deploy e observabilidade.
- **Governança de IA**: como agentes do Panteão BYOP devem atuar dentro da arquitetura.

## Fonte da Verdade

- `docs/AEGIS_DOCUMENTO_MESTRE_V1.md` — constituição do produto.
- `docs/adr/` — decisões arquiteturais formais.
- `docs/contracts/` — contratos JSON canônicos.
- `docs/api/` — REST, OpenAPI e GraphQL futuro.
- `docs/modules/`, `docs/database/`, `docs/deploy/`, `docs/security/`, `docs/ux/`, `docs/operations/`.

## Governança de IA (resumo)

Agentes de IA devem: consultar o Documento Mestre primeiro; respeitar decisões existentes; propor ADR quando necessário; não criar arquitetura paralela; preservar product-first, tenant isolation, contract-first e REST-first (MVP); não remover regras nem adotar tecnologia sem registro.
