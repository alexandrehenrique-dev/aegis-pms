# ADR-0002 — Contract First

## Status

ACCEPTED

## Contexto

Frontends que dependem diretamente da estrutura interna do banco ou de entidades do backend tornam-se frágeis: qualquer mudança interna quebra o consumidor. No ecossistema BYOP, múltiplos frontends externos consomem o conteúdo administrado pelo Aegis e precisam de uma fronteira estável.

## Decisão

Toda entrega pública do Aegis é baseada em **contratos**. O contrato é a fronteira entre o Aegis, sua API e o frontend externo.

O frontend **nunca depende da estrutura interna do banco**; ele consome contratos JSON estáveis e versionados. Contratos públicos não expõem entidades internas.

## Consequências

Positivas:
- Backend evolui internamente sem quebrar frontends.
- Contratos versionáveis permitem evolução compatível e depreciação planejada.
- Validação runtime (JSON Schema) protege consumidores.

Negativas / trade-offs:
- Custo de manter e versionar contratos e seus exemplos.
- Necessidade de uma camada de montagem de contrato (assemblers) separada do domínio.

## Alternativas Consideradas

- **Expor entidades do ORM diretamente**: rejeitado por acoplamento e fragilidade.
- **Contrato implícito (sem schema)**: rejeitado por falta de validação e governança.

## Impactos

- **Backend**: assemblers de contrato; separação entre modelo de domínio e contrato público.
- **Frontend**: consome contratos, interpreta features — não infere por categoria.
- **Governança**: contratos têm owner, versão, exemplo e passam por revisão de compatibilidade (`docs/contracts/`).

## Links Relacionados

- Documento Mestre — Parte V (Contratos JSON).
- ADR-0008 (JSON Contracts), ADR-0006 (REST First).
