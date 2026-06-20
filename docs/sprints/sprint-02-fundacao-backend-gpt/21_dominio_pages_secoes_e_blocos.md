# Etapa 21 — Domínio `pages` (páginas compostas por seções/blocos)

> Cole este arquivo inteiro numa conversa nova do GPT. Pré-requisito: etapa 10 concluída (domínio `content` já existe — `pages` convive com ele, não o substitui). Pode ser feita em paralelo com as etapas 11-20. Etapa adicionada depois da análise dos primeiros contratos de produto reais (Sprint 11 do frontend, `docs/sprints/11_modelo_paginas_blocos_knowledge_graph_e_mocks_produtos.md`).

## Contexto fixo

A etapa 10 modela `Content` como um artigo/documento plano (bom para blog, manifesto, artigo de base de conhecimento). Mas analisando os contratos reais dos primeiros produtos a serem hospedados no Aegis (Maestro Beton, CMSS, Conecta Talentos, Alexandre Dev), todos precisam de outra coisa: uma página institucional (`home.json`, `sobre.json`, `quem-somos.json`) é uma **lista ordenada de seções tipadas** (`hero`, `texto`, `imagem+texto`, `lista de cards`, `galeria`, `linha do tempo`, `FAQ`, `contato`), cada uma com seu próprio conteúdo e configurações — não um texto corrido com workflow editorial.

Isso não é hipotético: o frontend já tem um editor de blocos parcialmente construído (`ContentEditor.tsx`/`EditorPanels.tsx`/`ContentStructureTree`) com uma lista hardcoded de blocos (`Hero`, `Experiências`, `Vídeo destaque`, `Sobre`, `Galeria`, `Depoimentos`, `CTA final`, `SEO`) que nunca foi ligada a um contrato real. Esta etapa formaliza isso no backend.

## Objetivo

CRUD de páginas compostas por seções tipadas e reordenáveis, com catálogo fechado de tipos de bloco e validação de conteúdo por tipo.

## Tarefas

### A. Entidades

**Page**: `id`, `tenantId`, `productId`, `slug` (único por produto), `title`, `locale`, `status` (`"draft"|"review"|"published"|"archived"`), `version`, `seoTitle?`, `seoDescription?`, `seoCanonical?`, `seoOgImageAssetId?`, `seoNoIndex` (boolean, default false), `createdAt`, `updatedAt`.

**PageSection**: `id`, `pageId`, `type` (`BlockType`, catálogo fechado abaixo), `variant?`, `order` (inteiro, define a posição na página), `contentJson` (conteúdo específico do bloco — schema validado por `type`, ver Tarefa C), `settingsJson?` (configurações visuais — `columns`, `align`, `loop`, etc., nunca classe CSS), `createdAt`, `updatedAt`.

### B. Catálogo fechado de `BlockType`

```txt
hero, text, rich-text, two-column, image, image-text,
feature-grid, card-list, gallery, timeline, event-list,
cta-section, faq, contact, footer, navbar
```

`type` desconhecido na criação/edição de uma seção é rejeitado com 400 — mesmo princípio já usado no catálogo de módulos (etapa 06) e no catálogo de tipos de nó do grafo (etapa 07): catálogo fechado no backend, nunca aberto a qualquer string vinda do frontend.

### C. Validação de conteúdo por tipo de bloco

Cada `BlockType` tem regras mínimas de validação sobre `contentJson` antes de salvar:

| `BlockType` | Regra mínima |
|---|---|
| `hero` | `title` obrigatório; se houver `image`, `image.alt` obrigatório |
| `image`, `gallery` | todo item de imagem exige `alt` |
| `card-list`, `feature-grid` | `items` com pelo menos 1 e no máximo 12 elementos |
| `two-column` | `left`/`right` só podem conter tipos da allowlist: `text`, `rich-text`, `image`, `cta` (nunca outro `two-column` aninhado, para evitar layout recursivo descontrolado) |
| `event-list` | `source` deve referenciar um content type válido (`{ type: "contentType", contentType: string, filter?: object }`) — a seção não embute a lista de eventos diretamente, ela aponta para onde buscar |
| `contact` | `formId` deve existir (referência ao domínio `form`, etapa 12) |
| demais tipos | apenas validação de schema JSON genérica (campos esperados presentes) |

Reaproveitar a mesma estratégia de validação por chave dinâmica já usada na etapa 06 (catálogo de módulos) e etapa 07 (catálogos de node/edge type) — uma função `validateSectionContent(type, contentJson)` central, não validação espalhada.

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

## Critérios de aceite

- [ ] Criar página, adicionar seções, reordenar e editar funcionam.
- [ ] Seção com `type` fora do catálogo é rejeitada com 400.
- [ ] `hero` sem `title` é rejeitado; imagem sem `alt` é rejeitada em qualquer bloco que tenha imagem.
- [ ] `two-column` com um `two-column` aninhado dentro é rejeitado.
- [ ] Reordenar seções persiste a nova ordem corretamente.
- [ ] Excluir página remove as seções em cascata.

## Validação

```bash
curl -X POST http://localhost:8080/api/v1/products/<productId>/pages \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"slug":"home","title":"Home","locale":"pt-BR"}'

curl -X POST http://localhost:8080/api/v1/products/<productId>/pages/<pageId>/sections \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"type":"hero","order":0,"content":{"title":"Maestro Beton","image":{"assetId":"<assetId>","alt":"Maestro Beton tocando saxofone"}}}'

# esperado 400: tipo de bloco invalido
curl -X POST http://localhost:8080/api/v1/products/<productId>/pages/<pageId>/sections \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"type":"carousel-3d","order":1,"content":{}}'
```

## Commit sugerido

```bash
git add backend/
git commit -m "feat(backend): dominio pages com secoes/blocos tipados e validacao por catalogo"
```
