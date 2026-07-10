# ADR-0017 — Templates de produto: esqueleto de páginas por tipo, e o tipo Custom

## Status

ACCEPTED

## Contexto

A jornada do Tenant Admin (`docs/implementation/005_aegis_pms_user_journeys.md`, Journey 02) sempre previu "Novo Produto → **Escolher Template** → Configurar → Publicar", mas uma auditoria de uso real encontrou que isso nunca foi implementado como conceito próprio: `CreateProductForm.tsx` tem um campo "Template inicial" **travado** (`locked`, valor fixo `"Produto operacional padrão"`, sem nenhuma opção), com um comentário no próprio código admitindo a simplificação ("fixo por enquanto — ADR/Sprint 09"). O que de fato varia hoje na criação de um produto é só o `type` (`ProductTypeKey`), que pré-marca módulos default (`core/products/moduleDefaults.ts`) — mas nunca gera conteúdo nenhum além disso.

Ao mesmo tempo, o projeto já tem 6 contratos de produto reais e detalhadamente levantados (Sprint 11): Maestro Beton e CMSS (Site Institucional), Conecta Talentos (Portal), Alexandre Dev (Portfolio), WikiDev (Knowledge Base), Loki (Library/Books/Music) — com páginas, seções e tipos de bloco reais documentados em `frontend/src/domains/pages/mocks/pages.mocks.ts` e em `docs/sprints/11_modelo_paginas_blocos_knowledge_graph_e_mocks_produtos.md`. Esse material nunca foi conectado à jornada de criação de produto — um Tenant Admin criando um produto novo hoje sempre começa de uma tela 100% vazia, mesmo quando o tipo escolhido (ex.: Site Institucional) já tem um formato de página conhecido e repetido entre vários produtos reais do catálogo.

## Decisão

"Template" deixa de ser um campo decorativo e passa a ser uma consequência direta do `type` escolhido na criação do produto: cada tipo (exceto o novo tipo **Custom**) gera automaticamente um **esqueleto de páginas e seções** (estrutura e tipo de bloco, nunca conteúdo de demonstração) e pré-habilita os módulos recomendados daquele tipo.

**Catálogo de esqueleto por tipo** (só os tipos cujo módulo `Páginas` é `default: true` — `core/products/moduleDefaults.ts` — recebem páginas automaticamente; tipos que oferecem `Páginas` como opcional nascem sem scaffold e podem receber páginas depois que o módulo for habilitado):

| Tipo | Referência real | Páginas geradas | Módulos pré-habilitados |
|---|---|---|---|
| Site Institucional | CMSS (a referência mais completa, Sprint 11) + Galeria (de Maestro Beton, ausente no contrato da CMSS) | Home (`hero`, `feature-grid`, `event-list`, `cta-section`), Quem Somos (`image-text`, `two-column`), História (`timeline`), Agenda (`event-list`), Galeria (`gallery`), Apoie (`rich-text`, `faq`), Contato (`contact`) | Páginas, Conteúdo, Assets, Forms, SEO, Analytics |
| Portal | Conecta Talentos | Home (`hero`, `text`, `card-list` "Vagas", `card-list` "Blog") | Páginas, Conteúdo, Forms, SEO, Analytics |
| Portfolio | Alexandre Dev | Home (`hero`, `card-list` "Projetos", `feature-grid` "Skills", `timeline` "Experiência", `download` "Downloads") | Portfolio, Páginas, Conteúdo, Assets, SEO, Analytics |
| Knowledge Base | WikiDev | nenhuma (módulo `Páginas` não é default neste tipo — produto é predominantemente `content`/`knowledge`) | Conteúdo, Assets, Knowledge Graph, SEO, Analytics |
| Library/Books/Music | Loki | nenhuma por padrão (`Páginas` é opcional, `default: false`) | Library, Books, Music, Conteúdo, Assets, Knowledge Graph, SEO, Analytics; Páginas opcional |
| Produto SaaS | — (nenhum contrato real ainda usa páginas institucionais) | nenhuma | Conteúdo, Assets, Forms, Analytics, SEO, Workflow |
| **Custom** (tipo novo) | — | nenhuma | nenhum — produto nasce 100% em branco, todo módulo desabilitado, Tenant Admin habilita manualmente depois |

**Regra de conteúdo do esqueleto — nunca inventar dado de demonstração**: toda seção criada usa o **mesmo conteúdo default vazio** que o editor de páginas já usa ao adicionar manualmente um bloco novo (`DEFAULT_BLOCK_CONTENT[type]` no frontend, `blockDefaults.ts`; equivalente a formalizar no backend, etapa 24) — nunca o texto/imagem de demonstração dos mocks de Maestro Beton/CMSS/etc. (esses são fiéis aos produtos reais que os originaram, não fazem sentido em um produto novo de um tenant qualquer). Isso garante, sem nenhuma lógica especial por tipo de bloco, exatamente a regra pedida: blocos que dependem de asset (`gallery`, `download`, `audio`, `image`, `image-text`) nascem com listas/referências vazias — não porque alguém tratou esse caso à parte, mas porque é assim que o default de qualquer bloco novo já funciona.

`Custom` é um valor novo de `ProductTypeKey`/`type` — não é "nenhum template", é um tipo de produto explícito que significa "sem nenhum esqueleto, sem nenhum módulo pré-marcado".

Templates que pré-habilitam `Knowledge Graph` também pré-habilitam `Assets`, além de `Conteúdo`, por dependência operacional definida na ADR-0015/ADR-0016. Isso evita produtos de conhecimento nascerem com grafo ativo mas sem biblioteca de mídia/documentos para evidências e vínculos.

## Consequências

Positivas:
- Fecha a lacuna entre a visão original (Journey 02) e a implementação, sem inventar um conceito novo — reaproveita os 6 contratos de produto já analisados e os defaults de bloco que já existem.
- Reduz o trabalho de configurar um produto novo do zero para os tipos mais comuns (Site Institucional, Portal, Portfolio), sem forçar isso em tipos onde não faz sentido (Knowledge Base, Library, SaaS).
- `Custom` dá uma saída explícita e honesta para quem realmente quer começar do zero — em vez de "enganar" o esqueleto para ficar vazio escondendo módulos depois.

Negativas / trade-offs:
- O catálogo de esqueleto por tipo é mais um lugar para manter sincronizado se o catálogo de `BlockType` (etapa 21) ou de módulos (etapa 06) mudar — mitigado por reaproveitar exatamente os mesmos catálogos, nunca duplicar a lista de tipos válidos.
- Produtos Knowledge Base/Library/SaaS continuam sem nenhum ponto de partida visual — aceito porque nenhum contrato real desses tipos usa o domínio `pages`; se isso mudar, é uma revisão futura desta ADR, não uma omissão.

## Alternativas Consideradas

- **Um único esqueleto genérico para todo tipo de produto**: rejeitado — descaracterizaria a diferença real entre os tipos (um Knowledge Base não tem por que ganhar uma página "Home" institucional) e contrariaria os 6 contratos já levantados.
- **Esqueleto com conteúdo de exemplo preenchido (texto/imagens fictícias)**: rejeitado — gera a falsa impressão de produto já configurado, e exigiria assets fictícios que não existem (violaria a regra explícita de manter blocos dependentes de asset vazios).
- **Deixar "Template inicial" travado como está, só remover o campo**: rejeitado — não resolve a lacuna real da jornada (Journey 02 sempre previu uma escolha real nesse ponto).

## Impactos

- **Backend**: nova etapa 24 (`docs/sprints/sprint-02-fundacao-backend-gpt/24_templates_de_produto_e_seed_de_esqueleto.md`) — `PageScaffoldService` disparado em `POST /products` conforme `type`; reaproveita `PageRepository`/`PageSectionRepository` (etapa 21) e `ProductModuleService.enable` (etapa 06, já com validação de dependência).
- **Frontend**: Sprint 17 (`docs/sprints/17_templates_de_produto_e_alerta_de_tenant_suspenso.md`) — `CreateProductForm.tsx`/`CreateProductModal.tsx` perdem o campo travado, ganham a opção `Custom`, e mostram um preview do esqueleto que será criado.
- **Documentação**: `docs/trace/00_endpoints_esperados.md` não precisa de endpoint novo — o scaffold é um efeito colateral de `POST /products` já documentado, não uma rota própria.

## Links Relacionados

- ADR-0001 (Product First), ADR-0012 (tipos de conteúdo), ADR-0013 (entidades globais por produto), ADR-0015 (módulos como portão de acesso — mesma validação de dependência reaproveitada aqui).
- `docs/sprints/11_modelo_paginas_blocos_knowledge_graph_e_mocks_produtos.md` (origem dos 6 contratos de produto).
- `docs/implementation/005_aegis_pms_user_journeys.md`, Journey 02 (Tenant Admin — "Escolher Template").
