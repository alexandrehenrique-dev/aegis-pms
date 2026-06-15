# ADR-0004 — Shared Database, Shared Schema, TenantId Obrigatório

## Status

ACCEPTED

## Contexto

O Aegis é multi-tenant por princípio. Era preciso escolher a estratégia de isolamento de dados entre as opções: banco por tenant, schema por tenant, ou base/schema compartilhados com isolamento lógico. A escolha impacta migrations, deploy, custo, onboarding e segurança.

## Decisão

O MVP adota **Shared Database + Shared Schema + `tenantId` obrigatório**.

Todos os tenants compartilham o mesmo banco e as mesmas tabelas. Cada registro de domínio tenant-owned carrega `tenantId` de forma **explícita, indexada, validada e auditável**. O isolamento é lógico forte, aplicado por regras de domínio, autorização, índices, constraints, middleware, testes e auditoria.

## Consequências

Positivas:
- Uma única estrutura de migrations; deploy simples; menor custo.
- Onboarding rápido de tenant; consultas operacionais e analytics internos viáveis.
- Boa compatibilidade com ORMs e testes diretos.

Negativas / trade-offs (riscos a mitigar):
- Risco de vazamento por query sem `tenantId`, cache mal chaveado, job sem filtro ou exportação sem escopo.
- Exige disciplina: `tenantId` é campo de **segurança**, não metadado opcional.

Mitigações obrigatórias: índices compostos com `tenantId`; services e repositories que exigem `tenantId`; middleware de tenant context; validação de membership; testes de isolamento; cache e storage tenant-scoped; code review focado em queries sem tenant.

## Alternativas Consideradas

- **Banco por tenant**: isolamento físico forte, mas migrations multiplicadas, deploy/observabilidade complexos, custo alto. Reservado para enterprise futuro.
- **Schema por tenant**: reduz custo vs. banco por tenant, mas mantém migrations multiplicadas, roteamento dinâmico de schema e risco de drift. Prematuro.

## Impactos

- **Backend**: tenant context obrigatório; repositories tenant-scoped.
- **Banco**: `tenantId` em todas as tabelas tenant-owned; índices compostos.
- **Segurança**: vazamento cross-tenant tratado como incidente severo.
- **Operação**: backup/restore e jobs tenant-aware.

## Alternativas para o futuro

Banco/schema por tenant pode ser adotado para clientes enterprise via novo ADR, sem invalidar este.

## Links Relacionados

- Documento Mestre — Parte II (Tenants) e Parte VI (Banco, Segurança).
- ADR-0010 (PostgreSQL), ADR-0005 (Keycloak).
