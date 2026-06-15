# ADR-0006 — REST First

## Status

ACCEPTED

## Contexto

O Aegis precisa de uma API oficial para o MVP. As opções principais eram REST e GraphQL. REST é maduro, simples de versionar, cachear e documentar (OpenAPI); GraphQL traz flexibilidade de query mas adiciona complexidade de schema, caching, segurança e performance.

## Decisão

**REST é a API oficial do MVP.** A API é versionada (`/api/v1`), documentada via **OpenAPI**, com respostas e erros padronizados, paginação canônica e política de breaking changes.

A API separa **pública** (consumo de contratos por frontends externos, somente conteúdo publicado) de **administrativa** (operação no painel, sempre tenant-scoped e validando membership/role/feature).

## Consequências

Positivas:
- Simplicidade, maturidade, cache HTTP, OpenAPI, ferramental amplo.
- Foco e velocidade de entrega no MVP.

Negativas / trade-offs:
- Possível over/under-fetching que GraphQL resolveria.
- Múltiplos round-trips para composições ricas (mitigável com contratos agregados).

## Alternativas Consideradas

- **GraphQL no MVP**: rejeitado por adicionar complexidade antes da necessidade (ver ADR-0007).
- **gRPC**: rejeitado por não ser ideal para consumo por frontends web e por contratos públicos.

## Impactos

- **Backend**: controllers REST, OpenAPI, versionamento, testes de contrato.
- **Frontend**: consome REST; contratos JSON canônicos.
- **Governança**: toda API REST com OpenAPI versionada (`docs/api/`).

## Links Relacionados

- Documento Mestre — Parte V (APIs REST).
- ADR-0007 (GraphQL Future), ADR-0002/ADR-0008 (Contratos).
