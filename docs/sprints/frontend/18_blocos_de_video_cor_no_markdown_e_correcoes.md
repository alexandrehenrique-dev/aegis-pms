# Sprint 18 — Blocos de vídeo, cor de texto no markdown, e correções (preview claro, anexo de feedback)

> Pré-requisito: nenhum específico de outra sprint do frontend — reaproveita `AudioBlockEditor.tsx`/`blockDefaults.ts`/`itemsCrudConfig.ts` (domínio `pages`, já em produção), `MarkdownEditModal.tsx` (Sprint 13/15, já em produção) e `FeedbackModal.tsx` (já em produção, mas decorativo). Companion desta sprint no backend: `docs/sprints/sprint-02-fundacao-backend-gpt/21_dominio_pages_secoes_e_blocos.md` (Seção B/C atualizadas), `11_dominio_assets.md` (limite de tamanho de vídeo) e a nova `27_dominio_feedback.md`.

## Contexto

Quatro pontos resolvidos nesta sprint, encontrados em revisão de documentação/jornada e em teste manual:

1. **Não existe bloco de vídeo.** Caso de uso real confirmado (`docs/AEGIS_PMS_V1.md` §17 — Maestro Beton precisa de vídeos de shows/repertório) e a categoria de asset `"video"` já está provisionada desde a etapa 11 do backend, mas nenhum `BlockType` a usa. O padrão mais parecido já existe e funciona: o bloco `audio` (`AudioBlockEditor.tsx`, `blockDefaults.ts:49`) — fonte dual (upload ou link externo), mesmo espírito a replicar para vídeo (upload ou YouTube).
2. **Editor de markdown não permite cor de texto.** `MarkdownEditModal.tsx` (toolbar, linhas 81-93) já tem H1-H4/negrito/itálico/lista/link/citação/código (Sprint 15) — falta cor.
3. **Tela de preview ilegível em tema escuro.** `ResponsivePreviewFrame.tsx:83` e `FormPreviewFrame.tsx:24` têm `bg-white` fixo (sem variante dark); `Markdown.tsx:20` usa `dark:prose-invert`, que inverte o texto para claro quando o tema do app está escuro — resultado: fundo branco fixo + texto branco (invertido) = ilegível. Não é um bug de cor isolado, é um conflito entre "fundo fixo" e "texto reagindo ao tema".
4. **Botão "Anexar arquivo" do feedback é decorativo.** `FeedbackModal.tsx:79` só alterna um nome de arquivo fake (`fakeFile`) em memória — nunca abre o seletor de arquivos do sistema, nunca envia nada. `handleSubmit` (linhas 20-29) também é fake — gera um ID local e não persiste em lugar nenhum. Criada a etapa 25 do backend (domínio `feedback`) para isso ter onde persistir, reaproveitando o upload de assets já existente (etapa 11) para o anexo — não um upload próprio.

## Objetivo

1. Blocos `video` (card único) e `video-gallery` (lista), upload ou YouTube.
2. Cor de texto na toolbar do editor de markdown, com paleta fechada (nunca cor livre/hex arbitrário).
3. Preview (conteúdo e formulário) sempre legível, independente do tema do app.
4. Anexo de feedback usa arquivo real da máquina do usuário; submissão persiste de fato (mock-backed, pronto para a etapa 25 quando o backend existir).

## Tarefas

### A. Blocos `video` e `video-gallery`

1. `domains/pages/contracts/responses.ts`, `BLOCK_TYPES`: adicionar `"video"`, `"video-gallery"` ao catálogo fechado (mesmo array que já tem `"audio"`, `"social-links"`, etc. — etapa 21 do backend já documenta os dois).
2. `domains/pages/blockDefaults.ts`, `DEFAULT_BLOCK_CONTENT`: adicionar
   ```ts
   video: { title: "", source: "upload", fileAssetId: "", youtubeUrl: "", autoplay: false },
   "video-gallery": { items: [] },
   ```
3. Criar `domains/pages/components/VideoBlockEditor.tsx` — cópia estrutural de `AudioBlockEditor.tsx`, trocando `AUDIO_SOURCES = ["upload", "spotify-track", "spotify-playlist"]` por `VIDEO_SOURCES = ["upload", "youtube"]`, `fileAssetId` filtrado por `typeFilter="vídeo"` (Tarefa A.4), e o campo de URL externa rotulado "URL do YouTube" (placeholder `https://www.youtube.com/watch?v=...` ou `https://youtu.be/...`) em vez de Spotify. Mantém o checkbox de autoplay.
4. `shared/components/MediaField.tsx`: `AssetTypeFilter` (`domains/assets/components/AssetPickerModal.tsx`) ganha o valor `"vídeo"`; `FILTER_LABEL` (`MediaField.tsx:7`) ganha `"vídeo": "vídeo"`.
5. `itemsCrudConfig.ts`: adicionar entrada para `video-gallery`, mesmo padrão de `gallery`/`download`:
   ```ts
   "video-gallery": { key: "items", newItem: { title: "", source: "upload", fileAssetId: "", youtubeUrl: "" }, rules: { requiredFields: ["title"], maxItems: 50 } },
   ```
6. `BlockRenderer.tsx`: novo `case "video"` (mesmo padrão do `case "audio"` já existente — placeholder `[player de vídeo: ...]` para upload, `[embed YouTube: ...]` para link externo) e `case "video-gallery"` (grid de cards, mesmo espírito do `case "gallery"`, um placeholder por item).
7. Validação de URL do YouTube no frontend (antes de salvar, mesma regra documentada na etapa 21 do backend): `^https:\/\/(www\.)?youtube\.com\/watch\?v=[\w-]{6,}$` ou `^https:\/\/youtu\.be\/[\w-]{6,}$` — rejeitar com mensagem clara se não bater.
8. **Permissões: nenhuma nova** — os dois blocos seguem o mesmo `PermGate`/gate de papel que qualquer outro `BlockType` no editor de páginas (edição: `super_admin`/`tenant_admin`/`product_manager`/`editor`; leitura: todos, incluindo `viewer`). Não criar nenhum gate específico para vídeo.

### B. Cor de texto no editor de markdown (paleta fechada, nunca hex livre)

1. `MarkdownEditModal.tsx`: novo `ToolbarAction` (`"color"`), com um botão que abre um pequeno popover de paleta (reaproveitar `ModalShell`/popover já estabelecido) com **6 cores fixas** (ex.: vermelho, azul, verde, âmbar, violeta, e "padrão"/remover cor) — nunca um input de cor livre/hex. Ao escolher, envolve o texto selecionado em `<span class="text-aegis-{cor}">texto</span>` (classes fixas, mapeadas 1:1 às 6 opções).
2. `Markdown.tsx`: trocar `ReactMarkdown` puro por `ReactMarkdown` com `rehype-raw` (permite o `<span>` ser interpretado) **e** `rehype-sanitize` com um schema customizado — allowlist = exatamente as tags já documentadas na sanitização do backend (`p, strong, em, ul, ol, li, blockquote, h2, h3, a, br`) **mais** `span` com **apenas** o atributo `class`, e só os 6 valores fixos da Tarefa B.1 (nunca `style`, nunca qualquer outro atributo) — replicar fielmente, do lado do cliente, a mesma allowlist que as etapas 10/21 do backend já impõem no servidor, agora também aplicada na renderização.
3. Definir as 6 classes (`text-aegis-red`, `text-aegis-blue`, etc.) no CSS global do app com uma cor que funcione em ambos os temas (claro/escuro) — nunca uma cor que dependa só do tema claro (mesmo cuidado da Tarefa C).

### C. Preview sempre legível, independente do tema

1. `ResponsivePreviewFrame.tsx:83` e `FormPreviewFrame.tsx:24`: o preview simula a página publicada de verdade — **decisão**: o canvas de preview é deliberadamente "papel branco" (como a página real apareceria para um visitante), não deve reagir ao tema escuro do editor. Em vez de só ter `bg-white` sem variante dark (que é o bug — o fundo fica fixo mas o texto não), o conteúdo renderizado dentro do preview precisa **também** ficar fixo no esquema claro, independente do tema do app por fora.
2. Implementação: envolver o conteúdo do preview (incluindo qualquer `<Markdown>` renderizado dentro dele) numa classe wrapper que força o escopo claro (ex.: `<div className="light isolate">`, com uma regra CSS `.light` que zera/sobrescreve as variáveis de tema para os valores claros dentro daquele escopo) — ou, alternativa mais simples, passar uma prop nova `forceLightProse` ao `Markdown`/`ArticleBody` usado dentro dos dois previews, que renderiza `prose prose-sm` **sem** `dark:prose-invert` nesse caso específico (nunca remover `dark:prose-invert` do uso normal do `Markdown` no resto do app — só dentro do preview).
3. Confirmar visualmente nos dois temas (claro e escuro do app) que o preview de conteúdo e o preview de formulário permanecem com fundo claro e texto escuro em ambos os casos.

### D. Botão "Anexar arquivo" do feedback — arquivo real, submissão real

1. `FeedbackModal.tsx:79`: substituir o botão fake (`fakeFile`) por um `<input type="file" hidden>` real (mesmo padrão já usado em `AssetPickerModal.tsx:64`) — clicar no botão visível aciona o input nativo, que abre o seletor de arquivos do sistema operacional do usuário (nunca o picker de assets já existentes — o caso de uso aqui é sempre um arquivo novo, ex. um screenshot do bug).
2. Ao escolher um arquivo: chamar `assetsService.upload(...)` (mesmo service já usado por outros pickers de asset, `domains/assets/services/assetsService.ts`) num produto técnico/contexto disponível (`effectiveProduct` se houver, ou um fallback documentado se o feedback for reportado fora de um produto — mesma decisão que a etapa 25 do backend registra como "a definir pelo GPT"), guardando o `assetId` retornado.
3. Criar `core/notifications/services/feedbackService.ts` (mock-backed, mesmo padrão `logApiCall` dos demais services): `create(req: CreateFeedbackRequest): Promise<{ id: string }>` — gera um id `AGS-####`, loga `POST /api/v1/feedback`, guarda em memória. `FeedbackModal.handleSubmit` passa a chamar este service de fato (com `category`, `priority`, `description`, `screenName`, `attachmentAssetId?`), em vez do `setTimeout` fake atual.
4. Critério de UX: se o upload do arquivo falhar, o feedback ainda pode ser enviado sem anexo (não bloquear o relato do bug por causa do anexo) — mostrar um aviso, não impedir o envio.

## Critérios de aceite

- [ ] Bloco `video` aceita upload (filtrado por `typeFilter="vídeo"`) ou URL do YouTube; URL fora do padrão é rejeitada com mensagem clara antes de salvar.
- [ ] Bloco `video-gallery` lista itens de vídeo, cada um upload ou YouTube, até 50 itens.
- [ ] Toolbar de markdown tem botão de cor com paleta fechada de 6 opções; nenhuma forma de inserir cor livre/hex.
- [ ] `<span class="text-aegis-*">` é renderizado corretamente pelo `Markdown.tsx`; qualquer outra tag/atributo continua sendo removida pela sanitização do cliente (testar colando markdown com `<script>`/`<img onerror>` e confirmar que é removido).
- [ ] Preview de conteúdo e preview de formulário ficam legíveis (fundo claro, texto escuro) tanto com o app em tema claro quanto em tema escuro.
- [ ] Botão "Anexar arquivo" do feedback abre o seletor de arquivos do sistema operacional, nunca o picker de assets existentes.
- [ ] Enviar feedback com e sem anexo funciona; falha no upload do anexo não impede o envio do feedback.
- [ ] Feedback enviado é persistido no mock (consultável via `feedbackService`, não só um toast local).

## Comandos de Git

```bash
git checkout develop && git pull origin develop
git checkout -b sprint/18-video-cor-markdown-correcoes

git commit -m "feat(pages): blocos video e video-gallery (upload ou youtube)"
git commit -m "feat(markdown): cor de texto na toolbar com paleta fechada e sanitizacao no cliente"
git commit -m "fix(preview): forca esquema claro no preview de conteudo e formulario independente do tema"
git commit -m "fix(feedback): anexo usa arquivo real da maquina; submissao persiste no mock"

git push -u origin sprint/18-video-cor-markdown-correcoes
```

Ao final, finalize a sprint no gitflow:

```bash
./scripts/gitflow-finish-sprint.sh sprint/18-video-cor-markdown-correcoes
```
