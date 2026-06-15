# docs/database — Banco de Dados, Schema e Migrations

## Propósito

Documentação do **modelo de dados** do Aegis: schema relacional, decisões de modelagem, estratégia de migrations e uso controlado de JSONB.

## Conteúdo esperado

- Modelo relacional profundo (tabelas, relacionamentos, índices).
- Estratégia de **migrations com Flyway** (ver `adr/ADR-0010`).
- Decisões de uso de **JSONB** (ferramenta para config/limits/metadata, nunca substituto de domínio).
- Estratégia multi-tenant no banco: **Shared Database, Shared Schema, `tenantId` obrigatório** (ver `adr/ADR-0004`).
- Índices recomendados e justificativa por consulta real.

## Regras

- PostgreSQL é o banco inicial até ADR contrário.
- Toda tabela tenant-owned carrega `tenantId`, indexado e validado.
- Índices compostos devem incluir `tenantId` nas consultas críticas.
- Migration aplicada **não é editada** — evolui-se com nova migration.
- Mudanças destrutivas exigem plano de migração e, quando estruturais, ADR.
- Soft delete é preferido para relações históricas.
