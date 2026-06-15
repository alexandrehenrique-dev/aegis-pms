# ADR-0010 — PostgreSQL como Banco Inicial

## Status

ACCEPTED

## Contexto

O Aegis precisa de um banco de dados confiável para um modelo relacional rico (produtos, tenants, memberships, conteúdo, formulários, auditoria) com necessidade ocasional de estruturas flexíveis (config, limits, metadata) e de busca textual no MVP.

## Decisão

**PostgreSQL** é o banco inicial do Aegis, até ADR contrário.

Diretrizes:
- Modelo **relacional** como base; **JSONB** com **uso controlado** (config, limits, billing metadata, payloads), nunca como substituto de domínio.
- Migrations versionadas com **Flyway**; migration aplicada não é editada.
- Full Text Search nativo do PostgreSQL atende a busca do MVP (motor dedicado é evolução futura).
- Índices seguem consultas reais; tabelas tenant-owned usam índices compostos com `tenantId`.

## Consequências

Positivas:
- Banco maduro, relacional forte, JSONB, FTS, ampla comunidade e ferramental.
- Atende multi-tenant Shared Schema (ADR-0004) com segurança.

Negativas / trade-offs:
- JSONB exige disciplina para não virar esquema oculto.
- FTS nativo tem limites para busca semântica avançada (futuro).

## Alternativas Consideradas

- **MySQL/MariaDB**: viável, mas PostgreSQL oferece JSONB e FTS mais robustos.
- **MongoDB**: rejeitado por enfraquecer o modelo relacional central e a integridade.
- **Motor de busca dedicado (Elasticsearch/Meilisearch) no MVP**: adiado; FTS nativo basta no início.

## Impactos

- **Backend**: JPA/Hibernate + Flyway.
- **Banco**: schema relacional + JSONB controlado; índices por consulta.
- **Operação**: backup/restore PostgreSQL; FTS no MVP.

## Links Relacionados

- Documento Mestre — Parte VI (Banco de Dados) e Parte IV (Busca).
- ADR-0004 (Shared Schema).
