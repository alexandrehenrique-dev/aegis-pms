# Smoke tests manuais (Playwright)

Scripts ad-hoc para verificar visualmente/funcionalmente o app rodando de
verdade num browser headless — não são uma suíte automatizada de CI, nem
substituem `npm run typecheck`/`npm run lint`. Nasceram da verificação manual
da Sprint 18 (`docs/sprints/18_blocos_de_video_cor_no_markdown_e_correcoes.md`)
e cobrem só as áreas tocadas por ela até agora. **Cobertura de "toda
funcionalidade do sistema" é um trabalho futuro separado**, não o que existe
aqui hoje.

## Quando usar

- Antes de abrir PR de uma mudança que toca blocos de página, editor de
  markdown, previews (conteúdo/formulário) ou o `FeedbackModal`.
- Depois de qualquer alteração em `domains/pages/pages/PageEditor.tsx`
  (`handleChangeContent`) — é a área do bug de digitação corrigido na Sprint
  18, fácil de reintroduzir numa refatoração futura.
- Quando o navegador real é necessário para confirmar algo que `tsc`/`eslint`
  não enxergam: cor computada, sanitização de HTML renderizado, upload de
  arquivo real, contraste em tema claro/escuro.

## Pré-requisitos

1. Dev server rodando numa aba de terminal separada:
   ```bash
   npm run dev
   ```
   Por padrão os scripts apontam para `http://localhost:5173`. Para outra
   porta/host, exporte `AEGIS_BASE_URL`:
   ```bash
   AEGIS_BASE_URL=http://localhost:5174 node tests/video-blocks.mjs
   ```
2. Playwright já está em `devDependencies` (`npm install` resolve). Se o
   Chromium ainda não estiver baixado na máquina:
   ```bash
   npx playwright install chromium
   ```

## Como rodar

Cada arquivo é standalone — roda com `node` direto, sem test runner:

```bash
node tests/video-blocks.mjs
node tests/markdown-color-and-sanitization.mjs
node tests/preview-light-mode.mjs
node tests/feedback-attachment.mjs
node tests/typing-regression.mjs
```

Cada script imprime uma linha `✅`/`❌` por verificação e termina com exit
code `1` se algo falhar (`echo $?` depois, ou encadeie com `&&`/`||`). Para
rodar todos em sequência e parar no primeiro que falhar:

```bash
for f in tests/*.mjs; do [ "$f" = "tests/helpers.mjs" ] && continue; node "$f" || break; done
```

## O que cada script verifica

| Script | Cobre | Login usado |
|---|---|---|
| `video-blocks.mjs` | `BlockType: "video"`/`"video-gallery"` aparecem no catálogo; editor dedicado (upload com `typeFilter="vídeo"`, YouTube); validação da URL do YouTube (rejeita fora do regex, aceita dentro); filtro "vídeo" pré-selecionado no `AssetPickerModal` | `editor@byop.io` |
| `markdown-color-and-sanitization.mjs` | Popover de cor na toolbar do `MarkdownEditModal` (6 opções fixas); `<span class="text-aegis-*">` aplicado corretamente; sanitização client-side do `Markdown.tsx` remove `<script>`/`<img onerror>`/`style`, mantém `class` permitida | `editor@byop.io` |
| `preview-light-mode.mjs` | Com o app em tema escuro, o preview de conteúdo (`ResponsivePreviewFrame`) e o de formulário (`FormPreviewFrame`) continuam com texto escuro legível sobre fundo branco (`.light` em `theme.css` + `forceLightProse` do `Markdown.tsx`) | `editor@byop.io` |
| `feedback-attachment.mjs` | `FeedbackModal` usa `<input type="file">` real (não mais o botão fake); upload chama `assetsService.upload`; `feedbackService.create` é chamado com `attachmentAssetId`; tela de sucesso mostra `AGS-####` | `editor@byop.io` |
| `typing-regression.mjs` | Regressão do bug encontrado na verificação manual da Sprint 18: digitar/colar texto rápido num campo de bloco (mais rápido que o debounce de 500ms do `PageEditor`) não perde caracteres | `editor@byop.io` |

## Helpers (`helpers.mjs`)

Não é um script executável — funções reaproveitadas pelos outros 5:

- `loginAndOpenProduct(page, { user, theme })` — login, seleciona o tenant
  BYOP e o produto Maestro Beton (único disponível nos mocks atuais);
  `theme: "dark"|"light"` seta `localStorage.aegis-theme` antes do login.
- `openFirstPageEditor(page)` — abre `/pages` e entra no editor da primeira
  página listada (`Home`, no mock).
- `addBlock(page, blockType)` — adiciona um bloco do tipo informado pelo
  fluxo "Estrutura → dropdown de tipo → Adicionar bloco".
- `collectPageErrors(page)` — array que acumula `console.error`/`pageerror`;
  cheque `.length === 0` no fim do teste.
- `report(name, passed, detail?)` — imprime `✅`/`❌` e devolve o booleano,
  para encadear com `ok = report(...) && ok`.

## Limitações conhecidas

- Dependem do estado atual dos mocks (ex.: `depoimento-video.mp4` em
  `domains/assets/mocks/assets.mocks.ts`, a página `home` em
  `domains/pages`, o produto "Maestro Beton"). Se os mocks mudarem de nome,
  os seletores por texto destes scripts quebram — é esperado, ajuste o
  seletor junto com o mock.
- `Meta+A` (não `Ctrl+A`) é usado para "selecionar tudo" porque o Chromium
  no macOS trata `Ctrl+A` como atalho de edição de texto (move o cursor para
  o início da linha), não como select-all do navegador. Em Linux/CI,
  `Ctrl+A` seria o correto — troque se este projeto rodar smoke tests em CI
  algum dia.
- Sem isolamento entre execuções: cada script faz login do zero e usa o
  produto/página padrão do mock; rodar dois scripts em paralelo contra o
  mesmo dev server pode dar falso negativo por interferência de estado (o
  "backend" é só um store em memória do processo do Vite).
