# ADR-0001 — Product First Architecture

## Status

ACCEPTED

## Contexto

O ecossistema BYOP possui múltiplos produtos digitais de naturezas diferentes (sites institucionais, comerciais, portfólios, bibliotecas, wikis, portais de RH, blogs). Sem uma raiz conceitual comum, cada produto tende a nascer como um conjunto isolado de arquivos JSON, componentes específicos e regras hardcoded, gerando duplicação, acoplamento e Frankenstein arquitetural.

Era preciso decidir qual seria a entidade raiz do sistema: site, página, cliente ou produto.

## Decisão

A entidade raiz operacional do Aegis é o **Produto**.

Produto é qualquer manifestação digital administrada pelo Aegis (site institucional, landing page, blog, wiki, e-commerce futuro, portal de RH, portfólio, biblioteca). Páginas são apenas uma forma de materializar um produto. O Aegis **gerencia produtos, não páginas**.

## Consequências

Positivas:
- Modelo coerente que acomoda domínios muito diferentes sob uma mesma plataforma.
- Features, conteúdo, SEO, analytics, assets e contratos sempre têm um contexto (o produto).
- Onboarding de novos produtos é incremental, sem novo backend por produto.

Negativas / trade-offs:
- Exige disciplina para não tratar o Aegis como "CMS de páginas".
- Toda modelagem precisa carregar `productId` (e `tenantId`), aumentando o cuidado em queries e autorização.

## Alternativas Consideradas

- **Page-First (CMS tradicional)**: rejeitado por não acomodar domínios transacionais (RH, biblioteca, wiki) e por induzir hardcode.
- **Client/Customer-First**: rejeitado porque cliente é melhor representado por Tenant; produto é a unidade de entrega digital.
- **Site-First**: rejeitado por ser um caso particular de produto.

## Impactos

- **Backend**: domínio `products` é central; toda autorização passa por produto.
- **Frontend**: o painel é organizado por produto; menus são derivados das features do produto.
- **Banco**: `products` é tabela central, sempre tenant-owned.
- **Produto/Operação**: presets de categoria sugerem features iniciais por produto.

## Links Relacionados

- Documento Mestre — Parte I (Princípios) e Parte II (Produtos).
- ADR-0002 (Contract First), ADR-0003 (CMS First), ADR-0004 (Shared Schema).
