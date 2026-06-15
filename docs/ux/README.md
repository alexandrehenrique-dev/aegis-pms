# docs/ux — Diretrizes de UX/UI (Eirene)

## Propósito

Diretrizes de **experiência e interface** do painel administrativo do Aegis, de responsabilidade de **Eirene**.

## Conteúdo esperado

- Princípios de design (dark-first, leve, modular, hierárquico, sem poluição visual).
- Design system, tokens, componentização por feature.
- Fluxos obrigatórios e telas mínimas do MVP.
- Protótipos e referências.

## Princípios

- O Aegis deve parecer um **produto SaaS moderno**, não um painel administrativo genérico.
- Referências de sensação: Linear, Vercel, Stripe Dashboard, Notion, Raycast, GitHub Projects.
- Menus dinâmicos por **tenant ativo**, role e features ativas por produto.
- A UI deve parecer configurável por produto, não um painel gigante com tudo sempre exposto.
- A UI esconde ações sem permissão por experiência — mas o **backend é a fonte de segurança**.
- Indicar sempre o **tenant ativo** e o produto em edição.

## Telas mínimas (MVP)

Login (Keycloak), seleção de tenant/produto, dashboard global, dashboard do produto, lista e cadastro de produtos, gestão de usuários, gestão de features, editor de páginas/blocos, asset manager, form builder, SEO manager, analytics, audit log, configurações.

## Editor de Conteúdo (MVP)

Mostra página, seções, blocos, campos, status e preview textual/estrutural. **Preview visual completo é evolução futura** — o editor não renderiza o site final no MVP.
