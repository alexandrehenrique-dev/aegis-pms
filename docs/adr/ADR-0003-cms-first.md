# ADR-0003 — CMS First

## Status

ACCEPTED

## Contexto

Os produtos do ecossistema nasciam com texto e dados hardcoded no frontend, dificultando atualização sem deploy e sem controle editorial. Era necessário decidir onde o conteúdo final reside.

## Decisão

Todo conteúdo público deve ser **editável pelo Aegis**. Não deve haver texto final hardcoded no frontend.

Podem existir **componentes** hardcoded no frontend (estrutura visual), mas os **dados** renderizados vêm do Aegis. O Aegis é a fonte de verdade do conteúdo.

## Consequências

Positivas:
- Conteúdo atualizável sem deploy do frontend.
- Workflow editorial, revisões, traduções e SEO centralizados.
- Frontends viram renderizadores de contratos.

Negativas / trade-offs:
- Exige modelagem de conteúdo (pages, sections, blocks, content types) desde cedo.
- Editor administrativo precisa ser bom o suficiente para uso real.

## Alternativas Consideradas

- **Conteúdo no código do frontend**: rejeitado por exigir deploy a cada mudança e impedir governança editorial.
- **CMS headless de terceiros**: rejeitado por não atender multi-tenant + product-first + domínios transacionais do BYOP de forma integrada.

## Impactos

- **Backend**: domínio de conteúdo (pages/sections/blocks/content types), workflow, revisões.
- **Frontend**: renderiza dados do contrato; estrutura visual pode ser componente fixo.
- **UX (Eirene)**: editor de páginas/blocos é tela central do MVP.

## Links Relacionados

- Documento Mestre — Parte III (Conteúdo).
- ADR-0001 (Product First), ADR-0002 (Contract First).
