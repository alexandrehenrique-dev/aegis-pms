# ADR-0008 — JSON Contracts como Fronteira Frontend/Backend

## Status

ACCEPTED

## Contexto

O Aegis é Contract First (ADR-0002). Era preciso definir o **formato e a governança** concretos da fronteira entre backend e frontends: serialização, envelopes, versionamento e validação.

## Decisão

A fronteira oficial é o **contrato JSON canônico**. Frontends consomem contratos JSON estáveis, não entidades internas.

Padrões:
- Envelopes canônicos: Metadata, Versioning, Error, Pagination, Collection.
- Contratos por recurso: Product, Page, Section, Block, ContentType, Form, Asset, Navigation, SearchResult, Translation, Revision, Audit, Analytics, Comment, Suggestion, Notification.
- Validação em runtime via **JSON Schema**.
- Versionamento explícito; breaking change exige nova versão.
- Contrato público não expõe entidade interna; tem owner, versão e exemplo.

## Consequências

Positivas:
- Estabilidade e validação para consumidores.
- Evolução compatível e depreciação planejada.
- Base direta para futura camada GraphQL e para OpenAPI.

Negativas / trade-offs:
- Manutenção de schemas, exemplos e versões.
- Camada de montagem (assemblers) separada do domínio.

## Alternativas Consideradas

- **Serialização direta de entidades**: rejeitado por acoplamento.
- **Protobuf/Avro**: rejeitado para contratos públicos web por ergonomia de consumo.

## Impactos

- **Backend**: assemblers, validação, contract registry.
- **Frontend**: consumo previsível e validável.
- **Governança**: contratos versionados em `docs/contracts/`.

## Links Relacionados

- Documento Mestre — Parte V (Contratos JSON Canônicos).
- ADR-0002 (Contract First), ADR-0006 (REST First).
