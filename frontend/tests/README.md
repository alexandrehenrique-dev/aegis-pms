# Smoke tests (Playwright)

Scripts que dirigem o app de verdade num Chromium headless — não são uma
suíte unitária, nem substituem `npm run typecheck`/`npm run lint`. Nasceram
da verificação manual da Sprint 18
(`docs/sprints/18_blocos_de_video_cor_no_markdown_e_correcoes.md`) e foram
depois expandidos pra cobrir um script por domínio funcional do app
(auth, dashboard, products, content, pages, assets, forms, analytics,
knowledge graph, settings, users, audit, notifications/help).

## Quando usar

- Antes de abrir PR de qualquer mudança que toque navegação, formulários,
  modais ou fluxos entre telas — `tsc`/`eslint` não pegam regressão de UI
  (botão com texto errado, rota que para de navegar, campo que não some).
- Depois de alterar `domains/pages/pages/PageEditor.tsx`
  (`handleChangeContent`) — é a área do bug de digitação corrigido na
  Sprint 18 (`typing-regression.mjs`), fácil de reintroduzir numa
  refatoração futura.
- Depois de alterar `app/layouts/navConfig.ts`, `AppShell.tsx`,
  `core/permissions/roles.ts` — qualquer um dos 18 scripts pode quebrar
  (todos navegam pela sidebar/abas/breadcrumb reais).
- Quando o navegador real é necessário pra confirmar algo que `tsc`/`eslint`
  não enxergam: cor computada, sanitização de HTML renderizado, upload de
  arquivo real, contraste em tema claro/escuro, payload de uma chamada mock.

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
```

Cada script imprime uma linha `✅`/`❌` por verificação e termina com exit
code `1` se algo falhar. Para rodar **todos** de uma vez (resumo no final,
exit code agregado — é o que um pipeline deve chamar):

```bash
node tests/run-all.mjs
```

## O que cada script verifica

Nascidos na Sprint 18 (cobertura focada nas tarefas daquela sprint + o bug de digitação encontrado nela):

| Script | Cobre | Login usado |
|---|---|---|
| `video-blocks.mjs` | `BlockType: "video"`/`"video-gallery"` no catálogo; editor dedicado (upload com `typeFilter="vídeo"`, YouTube); validação da URL do YouTube; filtro "vídeo" pré-selecionado no `AssetPickerModal` | `editor@byop.io` |
| `markdown-color-and-sanitization.mjs` | Popover de cor na toolbar do `MarkdownEditModal` (6 opções fixas); `<span class="text-aegis-*">` aplicado; sanitização client-side do `Markdown.tsx` remove `<script>`/`<img onerror>`/`style`, mantém `class` permitida | `editor@byop.io` |
| `preview-light-mode.mjs` | Com o app em tema escuro, preview de conteúdo (`ResponsivePreviewFrame`) e de formulário (`FormPreviewFrame`) continuam com texto escuro legível sobre fundo branco | `editor@byop.io` |
| `feedback-attachment.mjs` | `FeedbackModal` usa `<input type="file">` real; upload chama `assetsService.upload`; `feedbackService.create` recebe `attachmentAssetId`; sucesso mostra `AGS-####` | `editor@byop.io` |
| `typing-regression.mjs` | Regressão do bug de digitação: colar/digitar rápido num campo de bloco (mais rápido que o debounce de 500ms do `PageEditor`) não perde caracteres | `editor@byop.io` |

Um script por domínio funcional (cobertura geral do app, fora do escopo original da Sprint 18):

| Script | Cobre | Login usado |
|---|---|---|
| `auth-flows.mjs` | Login com senha errada/conta bloqueada (mensagens certas); "Esqueci minha senha"; login válido → seleção de tenant → seleção de produto → dashboard; logout; `/invite` acessível sem sessão | `editor@byop.io` + `blocked@byop.io` |
| `dashboard.mjs` | `ProductDashboard` (landing pós-login) e `DashboardGlobal` (`/dashboard`, via sidebar); KPI restrito a outros papéis fica oculto pra `editor`; KPI navega pro destino certo | `editor@byop.io` |
| `products.mjs` | Lista de produtos, criar produto (preview do esqueleto de páginas por tipo), página de módulos do produto | `admin@byop.io` (tenant_admin — `editor` não vê "Criar produto") |
| `content.mjs` | Dashboard editorial, lista (DataGrid), abrir no editor, preview, histórico de versões, Workflow board (Kanban) | `editor@byop.io` |
| `pages-crud.mjs` | Lista de páginas, criar página nova, remover bloco (com confirmação), tela de globais (navbar/footer/redes sociais) | `editor@byop.io` |
| `assets.mjs` | Biblioteca, detalhe de asset, upload real de arquivo, criar tag | `editor@byop.io` |
| `forms.mjs` | Dashboard, lista, builder, preview, submissions (tabela + detalhe), publicação, analytics | `editor@byop.io` |
| `analytics.mjs` | Overview + as 6 sub-abas (Saúde, Conteúdo, Forms, Canais, Relatórios, Tendências) | `editor@byop.io` |
| `knowledge-graph.mjs` | Overview, Graph Canvas, busca de entidade, Órfãos, Insights → abrir entidade | `admin@byop.io`, tenant **Aegis Labs**, produto **Aegis Docs** (módulo só vem habilitado por padrão em produtos tipo "Knowledge Base"; Maestro Beton não tem) |
| `settings.mjs` | Visão geral, Produto, Tenant, Permissões (+ "Salvar permissões"), Segurança | `super-admin@byop.io` (tenant_admin é bloqueado em `/settings/security`) |
| `users.mjs` | Lista (via "Próximas ações" do `ProductDashboard`), convidar usuário, abrir detalhe | `super-admin@byop.io` |
| `audit.mjs` | Timeline, "Ver detalhe" de um evento, link cruzado "Abrir recurso" → `/settings/roles` | `admin@byop.io` (tenant_admin) |
| `notifications-and-help.mjs` | "Criar Notificação" (super_admin, em `/select-tenant`), sino de notificações, Central de Ajuda, link "Reportar um problema" → `FeedbackModal` | `super-admin@byop.io` |

## Helpers (`helpers.mjs`)

Não é um script executável — funções reaproveitadas por todos os outros:

- `loginAndOpenProduct(page, { user, theme, tenantName, productName })` —
  login completo. Sem `tenantName`/`productName`, assume 1 card só (caso de
  `editor`/`viewer`/`product_manager` no tenant BYOP). Usuários com vários
  tenants/produtos (`super-admin`, `admin`) **precisam** dos dois parâmetros,
  senão o clique em "Entrar →"/"Abrir →" colide em vários cards (Playwright
  recusa por "strict mode violation"). `theme: "dark"|"light"` seta
  `localStorage.aegis-theme` antes do login.
- `goToNav(page, label)` — clica um item da sidebar principal
  (`app/layouts/navConfig.ts`), escopado a `<aside>` (não colide com abas de
  módulo de mesmo texto, ex. "Dashboard").
- `goToTab(page, label)` — clica numa aba de módulo (`moduleTabs` do mesmo
  arquivo), ex. "Lista"/"Workflow" dentro de Conteúdo.
- `openFirstPageEditor(page)` / `addBlock(page, blockType)` — específicos do
  editor de blocos de página (Sprint 18).
- `collectPageErrors(page)` — acumula `console.error`/`pageerror`, já
  filtrando ruído conhecido de biblioteca (ver comentário no arquivo:
  warning benigno do Radix `Dialog` + `react-remove-scroll` em React 18,
  sem efeito funcional); confira `.length === 0` no fim do teste.
- `report(name, passed, detail?)` — imprime `✅`/`❌` e devolve o booleano,
  pra encadear com `ok = report(...) && ok`.
- `DEMO_USERS` / `BASE_URL` — credenciais e URL base compartilhadas.

## Integrar num pipeline/CI

`tests/run-all.mjs` é o ponto de entrada único: descobre todo `*.mjs` em
`tests/` (exceto `helpers.mjs` e ele mesmo), roda cada um como processo
filho contra um dev server já no ar, imprime um resumo e termina com exit
code `1` se qualquer um falhar — é exatamente o contrato que um step de CI
espera (sucesso = exit 0).

O pipeline só precisa, nesta ordem:

1. Instalar dependências (já inclui Playwright como devDependency).
2. Instalar o browser do Playwright (não vem com `npm install`).
3. Subir o dev server **em background** e esperar a porta responder antes
   de rodar os testes — não dá pra só `npm run dev &&` porque o dev server
   nunca termina.
4. Rodar `node tests/run-all.mjs`.
5. Encerrar o dev server (senão o job de CI não finaliza).

Exemplo (GitHub Actions, já que o repo está no GitHub — adapte o `runs-on`/
cache conforme o restante do workflow do projeto):

```yaml
# .github/workflows/smoke-tests.yml
name: Smoke tests
on: [pull_request]
jobs:
  smoke:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: frontend
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: 20
          cache: npm
          cache-dependency-path: frontend/package-lock.json
      - run: npm ci
      - run: npx playwright install --with-deps chromium
      - run: npm run dev &
      - run: npx wait-on http://localhost:5173 --timeout 30000
      - run: node tests/run-all.mjs
```

Equivalente em qualquer shell script de pipeline (sem GitHub Actions
específico — é a parte que importa, o resto é sintaxe do runner):

```bash
#!/usr/bin/env bash
set -euo pipefail
cd frontend
npm ci
npx playwright install --with-deps chromium
npm run dev & DEV_PID=$!
trap 'kill $DEV_PID' EXIT
timeout 30 bash -c 'until curl -sf http://localhost:5173 >/dev/null; do sleep 1; done'
node tests/run-all.mjs
```

Pontos de atenção específicos de CI (diferentes do ambiente local em que
estes scripts foram escritos/validados):

- **`Meta+A` vs `Ctrl+A`** (ver Limitações abaixo) — runners de CI são
  Linux; troque `Meta+A` por `Ctrl+A` em `markdown-color-and-sanitization.mjs`
  se for rodar em CI de verdade (hoje está fixo em `Meta+A` porque foi
  validado em macOS local).
- **Headless já é o padrão** (`chromium.launch()` sem `headless: false`) —
  nenhuma mudança necessária aqui pra CI.
- **Paralelismo**: os 18 scripts compartilham o mesmo dev server (mock em
  memória do processo do Vite) — rodá-los em paralelo (ex. matriz de CI)
  pode causar interferência de estado entre eles (ver Limitações). Em série
  via `run-all.mjs` é o modo validado.
- **Tempo total**: a suíte inteira leva ~3-4 minutos em série localmente;
  dimensione o timeout do job de CI com folga (ex. 10 minutos).

## Limitações conhecidas

- Dependem do estado atual dos mocks (nomes de produtos/tenants/usuários
  demo, página `home`, asset `depoimento-video.mp4`, etc.). Se um mock for
  renomeado, os seletores por texto destes scripts quebram — é esperado,
  ajuste o seletor junto com o mock.
- `Meta+A` (não `Ctrl+A`) é usado pra "selecionar tudo" porque o Chromium
  no macOS trata `Ctrl+A` como atalho de edição de texto (move o cursor
  pro início da linha), não como select-all do navegador. Em Linux/CI,
  `Ctrl+A` é o correto — ver seção acima.
- Sem isolamento entre execuções: cada script faz login do zero; rodar
  dois scripts **em paralelo** contra o mesmo dev server pode dar falso
  negativo por interferência de estado (o "backend" é só um store em
  memória do processo do Vite — criar um produto num script é visível nos
  outros). Em série (`run-all.mjs`) isso nunca foi um problema na prática.
- `/settings/access-preview` (`AccessPreviewPanel`) não tem nenhum link de
  entrada na UI hoje — só a rota existe (`app/routes/index.tsx`). Não é
  coberto por nenhum script (exigiria navegação direta por URL, que perde a
  sessão mock client-side). Acompanha o gap real do produto; se um link for
  adicionado, adicione a verificação em `settings.mjs`.
- Cobertura é por fluxo principal de cada domínio, não exaustiva por tela —
  não testa todo filtro/edge case de cada tabela, nem ações destrutivas
  (arquivar tenant, excluir produto) que mudariam o estado do mock pra
  execuções seguintes.
