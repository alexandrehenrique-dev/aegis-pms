# docs/adr — Architecture Decision Records

## Propósito

Este diretório guarda os **Architecture Decision Records (ADRs)** do Aegis.

Um ADR é o registro formal e imutável de uma decisão arquitetural relevante. Ele preserva o **contexto**, a **decisão tomada**, as **consequências** aceitas e as **alternativas** consideradas, para que o motivo de cada escolha sobreviva à memória de quem a tomou.

## Regras

- Todo ADR segue o template oficial (Status, Contexto, Decisão, Consequências, Alternativas, Impactos, Links).
- O nome do arquivo contém número sequencial e slug curto: `ADR-0001-product-first.md`.
- Números **nunca** são reutilizados.
- Um ADR aceito **não é apagado**. Quando substituído, vira `SUPERSEDED by ADR-XXXX`; quando descontinuado, vira `DEPRECATED`; quando recusado, vira `REJECTED`.
- Status válidos: `PROPOSED`, `ACCEPTED`, `SUPERSEDED`, `REJECTED`, `DEPRECATED`.

## Quando criar um ADR

Mudança de banco, autenticação, arquitetura, contrato público, provider externo, modelo multi-tenant, estratégia de deploy, segurança, versionamento ou fronteira entre módulos.

## Quando NÃO criar

Ajuste visual pequeno, correção de typo, refactor local sem impacto, bugfix sem decisão arquitetural, rename interno irrelevante.

## ADRs Atuais

| ADR | Título | Status |
|---|---|---|
| ADR-0001 | Product First | ACCEPTED |
| ADR-0002 | Contract First | ACCEPTED |
| ADR-0003 | CMS First | ACCEPTED |
| ADR-0004 | Shared Database, Shared Schema | ACCEPTED |
| ADR-0005 | Keycloak como IAM | ACCEPTED |
| ADR-0006 | REST First | ACCEPTED |
| ADR-0007 | GraphQL Future | ACCEPTED |
| ADR-0008 | JSON Contracts | ACCEPTED |
| ADR-0009 | SPA servido pelo Spring Boot | ACCEPTED |
| ADR-0010 | PostgreSQL como banco inicial | ACCEPTED |
| ADR-0011 | Framework de Frontend: React (não Angular) | ACCEPTED |
