# ADR-0007 — GraphQL como Evolução Futura

## Status

ACCEPTED

## Contexto

GraphQL resolve problemas reais (over/under-fetching, composição flexível, federation). Porém, adotá-lo no MVP adicionaria complexidade de schema, caching, persisted queries, segurança (depth/complexity limits) e performance antes de a plataforma precisar.

## Decisão

GraphQL é uma **camada futura**, **não** parte do MVP. REST permanece a API oficial (ver ADR-0006). GraphQL permanece como futuro **até que um ADR contrário o autorize**.

Quando adotado, GraphQL deverá: conviver com REST sem substituí-lo abruptamente; respeitar tenant isolation e permissões; usar persisted queries e limites de complexidade; e não duplicar a fonte de verdade dos contratos.

## Consequências

Positivas:
- Foco e simplicidade no MVP.
- Caminho evolutivo registrado e intencional.

Negativas / trade-offs:
- Consumidores que se beneficiariam de GraphQL aguardam.
- Risco de pressão para antecipar adoção sem ADR.

## Alternativas Consideradas

- **GraphQL agora**: rejeitado por complexidade prematura.
- **Nunca GraphQL**: rejeitado por limitar evolução de consumo de dados rico (wiki, knowledge graph).

## Impactos

- **Backend**: nenhum no MVP além de manter contratos consistentes para futura camada.
- **Governança**: agentes de IA e devs devem manter GraphQL como futuro até ADR contrário.

## Links Relacionados

- Documento Mestre — Parte V (GraphQL Futuro).
- ADR-0006 (REST First).
