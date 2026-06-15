# ADR-0005 — Keycloak como Provedor de Identidade (IAM)

## Status

ACCEPTED

## Contexto

O Aegis precisa de autenticação robusta (login, sessões, tokens, login social futuro) sem reimplementar segurança de identidade. Ao mesmo tempo, a autorização multi-tenant (memberships, roles de tenant e produto, permissões contextuais) é regra de negócio do próprio Aegis.

## Decisão

**Keycloak** é o provedor de identidade do Aegis. Ele responde por **autenticação**: usuários globais, credenciais, sessões, tokens, roles globais (ex.: `SUPER_ADMIN`) e login social futuro.

O **Aegis** responde por **autorização contextual**: tenants, memberships, roles de tenant/produto, convites, permissões efetivas e audit logs de negócio.

Estratégia do MVP: **realm único** no Keycloak; tenants e permissões contextuais no banco do Aegis.

> Keycloak autentica. Aegis autoriza por tenant.

Tokens do Keycloak podem carregar identidade, roles globais e claims úteis, mas **não** são a fonte final da autorização tenant-specific — o backend valida membership ativa, tenant ativo e permissões efetivas.

## Consequências

Positivas:
- Não se reimplementa autenticação; segurança madura desde o início.
- Realm único simplifica usuário multi-tenant, troca de tenant e onboarding.

Negativas / trade-offs:
- Dependência operacional do Keycloak (deploy, backup, configuração).
- Sincronização de identidade global entre Keycloak e base local.

## Alternativas Consideradas

- **Auth própria**: rejeitado por custo e risco de segurança.
- **Realm por tenant**: isolamento de identidade mais forte, mas provisionamento complexo e usuário multi-tenant difícil. Reservado para enterprise futuro.

## Impactos

- **Backend**: Spring Security + Keycloak; camada de autorização própria por tenant.
- **Frontend**: login integrado ao Keycloak; tenant context após login.
- **Operação**: Keycloak no Docker Compose; backup de realm.

## Links Relacionados

- Documento Mestre — Parte II (IAM, Roles) e Parte VI (Segurança).
- ADR-0004 (Shared Schema).
