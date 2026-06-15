# docs/contracts — Contratos JSON Canônicos

## Propósito

Repositório dos **contratos JSON canônicos** do Aegis — a fronteira estável entre o backend e os frontends que o consomem.

O Aegis é **Contract First**: frontends nunca dependem da estrutura interna do banco; eles consomem contratos versionados e estáveis (ver `adr/ADR-0002` e `adr/ADR-0008`).

## Conteúdo esperado

- JSON Schemas dos contratos públicos (Product, Page, Section, Block, ContentType, Form, Asset, Navigation, SearchResult, etc.).
- Contratos administrativos para uso do painel Eirene.
- Contratos de envelope: Metadata, Versioning, Error, Pagination, Collection.
- Registro de versões de cada contrato (Contract Registry).

## Regras

- Todo contrato público deve ter **owner**, **versão** e **exemplo**.
- Contrato público **não expõe entidade interna** do banco.
- Breaking change em contrato exige nova versão e passa por revisão de compatibilidade.
- Contratos devem ser validáveis em runtime (JSON Schema).
- Convenção de nome de arquivo sugerida: `public-product.v1.schema.json`.
