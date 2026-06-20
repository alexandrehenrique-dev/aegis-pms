# ADR-0013 — Entidades globais por produto (Navbar/Footer/Redes Sociais)

## Status

ACCEPTED

## Contexto

O catálogo de `BlockType` (`domains/pages/contracts/responses.ts`) trata `navbar` e `footer` como mais um tipo de seção de página — cada página poderia, em tese, ter seu próprio navbar/footer, e editá-los significaria editar um bloco dentro de uma página específica. Isso contradiz o próprio contrato `Navigation` já previsto em ADR-0008 (JSON Contracts) e os exemplos reais usados como referência (`global.json` do Maestro Beton, `cms-metadata.json` da CMSS): navbar, footer e redes sociais são **uma única configuração por produto**, editada uma vez, refletida em todas as páginas — não uma escolha por página.

## Decisão

Navbar, footer e redes sociais deixam de ser `BlockType` e passam a ser uma entidade própria por produto: `ProductGlobals`.

Shape: `ProductGlobals = { navbar: { logoAssetId?, links: NavLink[] }, footer: { addressText?, links: NavLink[] }, socialLinks: SocialLink[], floatingWhatsapp?: { enabled, number, message } }`.

- `navbar` e `footer` são removidos do array `BLOCK_TYPES` e de qualquer mock de página que os usa como seção.
- Editados uma única vez por produto, em `/products/:slug/globals`.
- O `BlockRenderer`/preview de qualquer página renderiza `ProductGlobals.navbar` no topo e `.footer` no final automaticamente — a página em si nunca inclui navbar/footer nas próprias seções.

## Consequências

Positivas:
- Elimina a possibilidade (hoje latente) de páginas divergentes terem navbars/footers diferentes por acidente de edição.
- Edição centralizada: uma mudança de link no menu reflete em todas as páginas do produto instantaneamente.
- Alinha o modelo de dados ao contrato `Navigation` já formalizado em ADR-0008.

Negativas / trade-offs:
- Produtos que eventualmente precisem de navbar/footer diferente por página (caso de uso não identificado em nenhum produto atual) exigiriam uma extensão deliberada do modelo, não suportada por padrão.
- Migração de dados: páginas existentes com seções `navbar`/`footer` precisam ser convertidas para `ProductGlobals` no momento da remoção do tipo do catálogo.

## Alternativas Consideradas

- **Manter navbar/footer como `BlockType` comum**: rejeitado — é o status quo que motivou esta ADR, contradiz o contrato `Navigation` e abre a possibilidade de inconsistência entre páginas do mesmo produto.
- **Navbar/footer configuráveis por página com fallback para um global**: rejeitado por adicionar complexidade (resolução de precedência) sem caso de uso real entre os produtos atuais (Maestro Beton, CMSS, WikiDev, Loki, Conecta Talentos).

## Impactos

- **Backend**: nova entidade `ProductGlobals` por produto, endpoints `GET/PUT /products/{productId}/globals` (ver `docs/sprints/sprint-02-fundacao-backend-gpt/21_dominio_pages_secoes_e_blocos.md`, Seção F).
- **Frontend**: `domains/pages/services/globalsService.ts` (ou `core/products/`), tela `domains/pages/pages/GlobalsSettings.tsx`, `BlockRenderer` passa a injetar navbar/footer automaticamente.
- **Dados**: mocks de página com seções `navbar`/`footer` precisam migrar para `ProductGlobals` mockado equivalente.

## Links Relacionados

- ADR-0003 (CMS First), ADR-0008 (JSON Contracts — contrato `Navigation`).
- Sprint 13 — Tarefa G.
