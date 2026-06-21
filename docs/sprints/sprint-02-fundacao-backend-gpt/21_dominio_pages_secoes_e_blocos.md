# Etapa 21 — Domínio `pages` (páginas compostas por seções/blocos)

> Cole este arquivo inteiro numa conversa nova do GPT. Pré-requisito: etapas 10 (domínio `content` já existe — `pages` convive com ele, não o substitui), 11 (`asset`, necessário para o bloco `audio` com upload e para `seoOgImageAssetId`/imagens) e 12 (`form`, necessário para o bloco `contact` referenciar um `formId` real) concluídas. Pode ser feita em paralelo com as etapas 13-20. Etapa adicionada depois da análise dos primeiros contratos de produto reais (Sprint 11 do frontend) e atualizada pela Sprint 13 do frontend (`docs/sprints/13_engine_de_blocos_entidades_globais_e_responsividade.md`) — motor genérico de sub-blocos, blocos `audio`/`social-links`, remoção de `navbar`/`footer` do catálogo de seção em favor de `ProductGlobals`, e sanitização de markdown.

## Contexto fixo

A etapa 10 modela `Content` como um artigo/documento plano (bom para blog, manifesto, artigo de base de conhecimento). Mas analisando os contratos reais dos primeiros produtos a serem hospedados no Aegis (Maestro Beton, CMSS, Conecta Talentos, Alexandre Dev), todos precisam de outra coisa: uma página institucional (`home.json`, `sobre.json`, `quem-somos.json`) é uma **lista ordenada de seções tipadas** (`hero`, `texto`, `imagem+texto`, `lista de cards`, `galeria`, `linha do tempo`, `FAQ`, `contato`), cada uma com seu próprio conteúdo e configurações — não um texto corrido com workflow editorial.

Isso não é hipotético: o frontend já tem um editor de blocos parcialmente construído (`ContentEditor.tsx`/`EditorPanels.tsx`/`ContentStructureTree`) com uma lista hardcoded de blocos (`Hero`, `Experiências`, `Vídeo destaque`, `Sobre`, `Galeria`, `Depoimentos`, `CTA final`, `SEO`) que nunca foi ligada a um contrato real. Esta etapa formaliza isso no backend.

## Objetivo

CRUD de páginas compostas por seções tipadas e reordenáveis, com catálogo fechado de tipos de bloco e validação de conteúdo por tipo.

## Tarefas

### A. Entidades

**Page**: `id`, `tenantId`, `productId`, `slug` (único por produto), `title`, `locale`, `status` (`"draft"|"review"|"published"|"archived"`), `version`, `seoTitle?`, `seoDescription?`, `seoCanonical?`, `seoOgImageAssetId?`, `seoNoIndex` (boolean, default false), `createdAt`, `updatedAt`.

**PageSection**: `id`, `pageId`, `type` (`BlockType`, catálogo fechado abaixo), `variant?`, `order` (inteiro, define a posição na página), `contentJson` (conteúdo específico do bloco — schema validado por `type`, ver Tarefa C), `settingsJson?` (configurações visuais — `columns`, `align`, `loop`, etc., nunca classe CSS), `createdAt`, `updatedAt`.

### A.1 `Event` (agenda) — entidade própria, não "content type" genérico

> Corrigido depois de uma auditoria de aderência entre este documento e o frontend real: a Seção C (regra do bloco `event-list`) descrevia `source` como uma referência a um "content type" genérico (`{ type: "contentType", contentType: string }`) — mas o frontend já implementou eventos como **domínio próprio**, com `EventsManagerDrawer.tsx`/`eventsService.ts` e contrato dedicado (`domains/pages/contracts/events.ts`), nunca como um `Content` do domínio da etapa 10. O backend formaliza isso como entidade própria, não como busca por tipo de conteúdo.

**Event**: `id`, `tenantId`, `productId`, `title`, `datetime` (instante único, ISO 8601 com data **e** hora — nunca dois campos separados de data/hora; o frontend já manda um único valor combinado, ex. `"2026-07-12T16:00"`), `location`, `type` (`"public"|"private"`), `visibility` (mesmo catálogo que `domains/pages/contracts/events.ts` já define no frontend — copiar os valores exatos de lá, não inventar um novo), `description` (markdown, mesma sanitização da Seção C abaixo), `imageAssetId?`, `createdAt`, `updatedAt`.

```txt
GET    /api/v1/products/{productId}/events
POST   /api/v1/products/{productId}/events
GET    /api/v1/products/{productId}/events/{eventId}
PUT    /api/v1/products/{productId}/events/{eventId}
DELETE /api/v1/products/{productId}/events/{eventId}
```

A regra do bloco `event-list` (Seção C) passa a referenciar este endpoint, com filtro opcional (ex.: por período/visibilidade), nunca um "content type" — `source: { filter?: { from?: string; to?: string; visibility?: string } }`.

### B. Catálogo fechado de `BlockType`

```txt
hero, text, rich-text, two-column, image, image-text,
feature-grid, card-list, gallery, timeline, event-list,
cta-section, faq, contact, form, download, audio, social-links
```

`type` desconhecido na criação/edição de uma seção é rejeitado com 400 — mesmo princípio já usado no catálogo de módulos (etapa 06) e no catálogo de tipos de nó do grafo (etapa 07): catálogo fechado no backend, nunca aberto a qualquer string vinda do frontend.

> **Atualização (Sprint 13 do frontend):** `footer` e `navbar` **saíram** deste catálogo — não são mais um tipo de seção de página. Navbar, footer e redes sociais passaram a ser uma entidade própria por produto (`ProductGlobals`, Seção F) e nunca mais devem aparecer como `PageSection.type`. `audio` e `social-links` **entraram** — ver regras de validação na Seção C.

Cada entrada do catálogo tem também uma propriedade `acceptsChildren: BlockType[] | null` — define se aquele tipo de bloco aceita sub-blocos e, se sim, quais tipos são permitidos como filho. Hoje só `two-column` tem essa propriedade preenchida (`["text", "rich-text", "image", "cta"]`), mas o mecanismo de validação (Seção C) deve ser genérico — qualquer `BlockType` futuro pode ganhar `acceptsChildren` sem precisar de uma regra nova no código, só uma entrada nova nesta tabela.

### C. Validação de conteúdo por tipo de bloco

Cada `BlockType` tem regras mínimas de validação sobre `contentJson` antes de salvar:

| `BlockType` | Regra mínima |
|---|---|
| `hero` | `title` obrigatório; se houver `image`, `image.alt` obrigatório |
| `image`, `gallery` | todo item de imagem exige `alt` |
| `card-list`, `feature-grid` | `items` com pelo menos 1 e no máximo 12 elementos |
| qualquer tipo com `acceptsChildren` preenchido (hoje só `two-column`) | os filhos (`left`/`right` no caso do `two-column`) só podem ser de um `type` presente na lista `acceptsChildren` daquele bloco; um filho do mesmo tipo do pai (recursão) é sempre rejeitado, mesmo que o tipo esteja na lista — evita layout recursivo descontrolado. Esta é uma regra **genérica** (validar contra `acceptsChildren` do catálogo, não um `if (type === "two-column")` hardcoded), para já valer para qualquer bloco futuro que ganhe sub-blocos |
| `event-list` | `source` referencia o domínio próprio `Event` da Seção A.1 (`{ filter?: { from?: string; to?: string; visibility?: string } }`) — a seção não embute a lista de eventos diretamente, ela aponta para onde buscar; **nunca** um "content type" genérico (ver Seção A.1 — correção de aderência) |
| `contact` | `formId` deve existir e referenciar um `FormDefinition` do mesmo produto (domínio `form`, etapa 12) — **não tem mais um array de campos próprio**; se o frontend mandar `fields[]` em vez de `formId`, rejeitar com 400 (é o formato antigo, divergente do contrato) |
| `form` | mesma regra do `contact`: `formId` deve existir e referenciar um `FormDefinition` do mesmo produto — bloco genérico para embutir um formulário já criado em qualquer página, não só na seção de contato |
| `download` | `items[]` cada um com `fileAssetId` de um `Asset` existente (qualquer categoria, tipicamente `pdf`/`document`) e `title` não vazio |
| `audio` | `source` ∈ `"upload" \| "spotify-track" \| "spotify-playlist"`; se `upload`, exige `fileAssetId` de um `Asset` existente com `type` de áudio; se `spotify-*`, exige `spotifyUrl` válida (`https://open.spotify.com/...`); `autoplay` é `boolean`, default `false` |
| `social-links` | `items[]` cada um com `platform` (string não vazia) e `href` (URL válida) |
| demais tipos | apenas validação de schema JSON genérica (campos esperados presentes) |

Reaproveitar a mesma estratégia de validação por chave dinâmica já usada na etapa 06 (catálogo de módulos) e etapa 07 (catálogos de node/edge type) — uma função `validateSectionContent(type, contentJson)` central, não validação espalhada.

> **Markdown e sanitização (Sprint 13):** qualquer campo de texto longo dentro de `contentJson` (`body` de `text`/`rich-text`/`two-column`, `description` de qualquer bloco) é markdown — mesma convenção e mesma regra de sanitização da etapa 10 (allowlist `p, strong, em, ul, ol, li, blockquote, h2, h3, a, br`; bloquear `javascript:`/`data:`; nunca `<script>`/`<iframe>`). Aplicar ao salvar a seção (`POST`/`PUT` de section), não só na hora de renderizar.

### D. Endpoints

```txt
GET    /api/v1/products/{productId}/pages
GET    /api/v1/products/{productId}/pages/{pageId}
POST   /api/v1/products/{productId}/pages
PUT    /api/v1/products/{productId}/pages/{pageId}
DELETE /api/v1/products/{productId}/pages/{pageId}
POST   /api/v1/products/{productId}/pages/{pageId}/sections
PUT    /api/v1/products/{productId}/pages/{pageId}/sections/{sectionId}
DELETE /api/v1/products/{productId}/pages/{pageId}/sections/{sectionId}
PUT    /api/v1/products/{productId}/pages/{pageId}/sections/reorder
```

Payloads:

```ts
type PageSummary = {
  id: string; slug: string; title: string; locale: string;
  status: "draft" | "review" | "published" | "archived"; version: number;
};
// GET /pages → PageSummary[]

type PageDetail = PageSummary & {
  seo: { title?: string; description?: string; canonical?: string; ogImageAssetId?: string; noIndex: boolean };
  sections: Array<{
    id: string; type: string; variant?: string; order: number;
    content: Record<string, unknown>; settings?: Record<string, unknown>;
  }>;
};
// GET /pages/{pageId} → PageDetail

type ReorderSectionsRequest = { sectionIds: string[] };
// PUT /sections/reorder — nova ordem completa, na sequência desejada
```

### E. Regras de negócio

- `slug` único por produto; mudar o `slug` de uma página publicada deve manter redirecionamento básico (fora de escopo desta etapa — só não permitir colisão).
- Página em `draft` não aparece em nenhuma API pública (fora de escopo desta sprint, mas a regra de status já deve existir para a Sprint 02-ext de API pública, futura).
- Reordenar seções é sempre uma substituição completa da lista de IDs (`reorder`), nunca um incremento/decremento posicional isolado — evita race condition de duas pessoas reordenando ao mesmo tempo.
- Página excluída remove suas seções em cascata.
- Excluir um `FormDefinition` (etapa 12) que esteja referenciado por algum bloco `contact`/`form` de alguma página deve ser **bloqueado** pelo domínio `form` (não por `pages`) — `pages` só expõe a consulta "quais seções referenciam este `formId`" para o domínio `form` checar antes de excluir.
- **Isolamento por produto** (`00_padrao_qualidade_e_arquitetura.md`, Seção 10): página/seção de um produto fora do escopo do usuário retorna 404, nunca 403. `ProductGlobals` (Seção F) segue a mesma regra, exceto pelo caso de "ainda não existe" (que é objeto vazio, não 404 — não confundir as duas situações).
- **Module-gating** (Seção 9.2 do padrão): `PageController` anotado com `@RequireModule(ModuleKey.PAGES)`. `ProductGlobalsController` **não** é gateado por módulo (navbar/footer fazem parte da estrutura básica do produto, não de um módulo opcional).

### F. Entidade `ProductGlobals` (navbar, footer, redes sociais — Sprint 13 do frontend)

Navbar, footer e redes sociais são **uma configuração por produto**, não um bloco de página — editados uma vez, refletidos em todas as páginas daquele produto. Modelar como entidade própria, 1:1 com `Product` (não como mais uma seção de uma `Page` específica).

**ProductGlobals**: `id`, `productId` (único — relação 1:1), `navbarJson` (`{ logoAssetId?, links: { label, href }[] }`), `footerJson` (`{ addressText?, links: { label, href }[] }`), `socialLinksJson` (`{ platform, href }[]`), `floatingWhatsappJson?` (`{ enabled, number, message }`), `updatedAt`.

```txt
GET /api/v1/products/{productId}/globals
PUT /api/v1/products/{productId}/globals
```

```ts
type ProductGlobals = {
  navbar: { logoAssetId?: string; links: { label: string; href: string }[] };
  footer: { addressText?: string; links: { label: string; href: string }[] };
  socialLinks: { platform: string; href: string }[];
  floatingWhatsapp?: { enabled: boolean; number: string; message: string };
};
```

Regra: se o produto ainda não tem `ProductGlobals` (produto recém-criado), `GET` retorna um objeto vazio com arrays vazios (nunca 404) — simplifica o frontend, que sempre pode renderizar a tela de edição sem checar existência primeiro. `PUT` faz upsert (cria se não existir, atualiza se existir).

### G. Padrão de qualidade e entrega (obrigatório)

> Resumo — detalhe completo em `00_padrao_qualidade_e_arquitetura.md`.

- **Java 25** / **Spring Boot 4.1.x**. Javadoc obrigatório na interface e em todo método de `PageRepository`, `PageSectionRepository`, `ProductGlobalsRepository`, `EventRepository`. Mappers via MapStruct (`PageMapper`, `PageSectionMapper`, `ProductGlobalsMapper`, `EventMapper`). 100% de cobertura nas classes funcionais — incluindo `SectionContentValidationService` (a função `validateSectionContent` da Seção C, que é a classe mais crítica desta etapa: cada linha da tabela de validação precisa de pelo menos um teste de caminho feliz e um de rejeição) e o sanitizador de markdown reaproveitado da etapa 10.
- Entregar em rodadas:
  1. `Page`, `PageSection`, `ProductGlobals`, `Event` (entities, Seção A.1 incluída) + `PageRepository`, `PageSectionRepository`, `ProductGlobalsRepository`, `EventRepository` + testes `@DataJpaTest`.
  2. `PageMapper`, `PageSectionMapper`, `ProductGlobalsMapper`, `EventMapper` (MapStruct) + testes de mapper.
  3. `SectionContentValidationService` + `PageService` + `ProductGlobalsService` + `EventService` (regras das Seções C/E/F/A.1) + testes com mocks — cada `BlockType` da tabela da Seção C com pelo menos 2 testes (válido e inválido).
  4. `PageController`, `ProductGlobalsController`, `EventController` (endpoints da Seção D/F/A.1) + testes `@WebMvcTest` + validação via `curl`.

## Critérios de aceite

- [ ] Criar página, adicionar seções, reordenar e editar funcionam.
- [ ] Seção com `type` fora do catálogo é rejeitada com 400 — `footer`/`navbar` não são mais aceitos como `type` de seção.
- [ ] `hero` sem `title` é rejeitado; imagem sem `alt` é rejeitada em qualquer bloco que tenha imagem.
- [ ] `two-column` com um `two-column` aninhado dentro é rejeitado (regra genérica via `acceptsChildren`, não hardcoded).
- [ ] `contact` exige `formId` de um formulário existente do mesmo produto; payload com `fields[]` solto é rejeitado.
- [ ] `audio` com `source: "upload"` exige `fileAssetId` de um asset de áudio existente; com `source: "spotify-track"` exige `spotifyUrl` válida.
- [ ] Reordenar seções persiste a nova ordem corretamente.
- [ ] Excluir página remove as seções em cascata.
- [ ] `GET /products/{productId}/globals` nunca retorna 404 (objeto vazio se ainda não configurado); `PUT` faz upsert.
- [ ] Página/seção de produto fora do escopo do usuário retorna 404 (não 403).
- [ ] Produto com módulo `PAGES` desabilitado retorna 403 `MODULE_DISABLED` em `PageController` (não afeta `ProductGlobalsController`).
- [ ] `Event` é entidade própria (`EventController`), nunca resolvido como "content type"; CRUD completo funciona; `datetime` é um único valor ISO com data e hora (nunca dois campos separados).
- [ ] Bloco `event-list` referencia `Event` via filtro (período/visibilidade), nunca um `contentType`.
- [ ] `mvn clean verify` confirma 100% de cobertura nas classes elegíveis desta etapa, incluindo `SectionContentValidationService` (JaCoCo).
- [ ] `PageRepository`/`PageSectionRepository`/`ProductGlobalsRepository` têm Javadoc na interface e em todo método.

## Validação

```bash
curl -X POST http://localhost:8080/api/v1/products/<productId>/pages \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"slug":"home","title":"Home","locale":"pt-BR"}'

curl -X POST http://localhost:8080/api/v1/products/<productId>/pages/<pageId>/sections \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"type":"hero","order":0,"content":{"title":"Maestro Beton","image":{"assetId":"<assetId>","alt":"Maestro Beton tocando saxofone"}}}'

# esperado 400: tipo de bloco invalido (e tambem o caso de footer/navbar, removidos do catalogo)
curl -X POST http://localhost:8080/api/v1/products/<productId>/pages/<pageId>/sections \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"type":"carousel-3d","order":1,"content":{}}'

curl -X PUT http://localhost:8080/api/v1/products/<productId>/globals \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"navbar":{"links":[{"label":"Home","href":"/"}]},"footer":{"links":[]},"socialLinks":[{"platform":"instagram","href":"https://instagram.com/maestrobeton"}]}'

curl -X POST http://localhost:8080/api/v1/products/<productId>/pages/<pageId>/sections \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"type":"audio","order":2,"content":{"source":"spotify-track","spotifyUrl":"https://open.spotify.com/track/exemplo","autoplay":false}}'
```

## Commit sugerido

```bash
git add backend/
git commit -m "feat(backend): dominio pages com secoes/blocos tipados, acceptschildren generico, audio/social-links e product globals"
```
