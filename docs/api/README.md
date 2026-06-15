# docs/api — Contratos de API

## Propósito

Documentação das **APIs do Aegis**: REST (oficial no MVP) e GraphQL (evolução futura).

## Conteúdo esperado

- Especificação **OpenAPI** da API REST (`/api/v1`).
- Convenções gerais: versionamento, paginação, erros padronizados, status codes.
- Separação entre **API Pública** (consumo de contratos por frontends externos) e **API Administrativa** (operação no painel Eirene, sempre tenant-scoped).
- Esquema **GraphQL** futuro, mantido aqui como referência até que um ADR autorize sua implementação.

## Regras

- REST é a API oficial do MVP; GraphQL permanece futuro até ADR contrário (ver `adr/ADR-0007`).
- Toda API REST deve possuir OpenAPI versionada.
- Breaking changes exigem nova versão e plano de depreciação.
- A API pública entrega apenas conteúdo **publicado** — nunca drafts ou revisões internas.
- Endpoints administrativos validam: usuário autenticado, tenant, membership, role, produto, feature e status do recurso.
