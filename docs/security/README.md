# docs/security — Segurança, Ameaças e Políticas

## Propósito

Documentação de **segurança** do Aegis: modelo de ameaças, vetores de ataque, isolamento multi-tenant, autenticação/autorização, LGPD e políticas de proteção.

## Conteúdo esperado

- Modelo de segurança e matriz de ameaças.
- **Isolamento multi-tenant**: tenant spoofing, horizontal privilege escalation, acesso cruzado, consulta indevida, upload indevido, analytics indevido.
- Autenticação via **Keycloak** e autorização contextual no Aegis (ver `adr/ADR-0005`).
- Políticas de secrets, uploads, headers, rate limiting.
- **LGPD**: classificação, finalidade, retenção, consentimento, exportação, anonimização.

## Regra de ouro do multi-tenant

> Cliente pode informar intenção de contexto. O backend decide se o contexto é válido.

- Nunca confiar apenas no `tenantId` enviado pelo cliente — validar contra membership ativa.
- Toda query tenant-owned filtra por `tenantId`; consultas por id incluem `tenantId`.
- Recursos filhos validam a cadeia de pertencimento ao tenant.
- Erros não devem revelar existência de recursos de outro tenant (preferir `404`).
- Toda decisão de segurança que altera autenticação, autorização, secrets, uploads, rate limit, tenant isolation ou backup exige ADR.
