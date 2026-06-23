# Sprint 15 — Correções pós-refinamento: modais, navegação, módulos e domínio de conteúdo

> Pré-requisito: nenhum específico (independe de Sprint 13/14 estarem concluídas), mas a Tarefa C reabre `domains/content/` e a Tarefa D reabre a sidebar (`AppShell.tsx`) — se a Sprint 14 já estiver em execução em paralelo, coordenar para não colidir em `AppShell.tsx`.

## Contexto

Teste manual de refinamento (sessão de uso real do protótipo) encontrou 15 problemas distintos, todos confirmados no código nesta investigação — citados abaixo com arquivo:linha exato. Não são hipóteses: cada um foi localizado e a causa raiz identificada antes de escrever esta sprint.

1. **Modais fechando sozinhas.** `ConfirmDialog.tsx:8`, `CreateProductModal.tsx:28`, `EditProductModal.tsx:19`, `NotificationModal.tsx:14` seguem todos o mesmo padrão (`onClick={onCancel/onClose}` no overlay + `onClick={(e) => e.stopPropagation()}` no conteúdo), que está correto em princípio — a investigação não encontrou um listener global de `click`/`mousedown` no `document`, mas o padrão é frágil: qualquer elemento novo dentro do conteúdo que não propague corretamente, ou qualquer biblioteca de terceiro (ex.: dropdown nativo, popover) que monte fora da árvore do conteúdo, fecha a modal. Tratar como bug de robustez do padrão, não de um caso isolado.
2. **Modal de perfil nunca fecha ao clicar fora.** `AppShell.tsx:101-115` (`showUserMenu`) é um dropdown `absolute` **sem** overlay e **sem** handler de clique-fora — diferente de `Notifications.tsx:75-80`, que já tem `onClick={() => setOpen(false)}` no overlay. O menu de perfil é a exceção que não segue o padrão que o resto do app já usa.
3. **Modal de notificação reaberta pelo sino aparece cortada, fora do centro.** `NotificationModal.tsx:14-15` usa o mesmo `fixed inset-0 flex items-center justify-center` que as outras modais (que centralizam corretamente) — a causa provável é um ancestral com `transform` (animação Framer Motion do `AppShell`) virando "containing block" de um `position: fixed` descendente, quebrando a centralização relativa à viewport.
4. **Enviar página para revisão não navega de volta ao workflow.** `ContentEditor.tsx:111-113` chama a transição e mostra um toast, mas nunca chama `navigate(...)` depois — usuário fica preso na tela de edição de um conteúdo que já não está mais em edição.
5. **Criar produto pelo Dashboard não navega para o produto criado.** `DashboardGlobal.tsx:30` navega para `/products/new`, rota que não corresponde a nenhum componente de criação real (só existe `CreateProductModal.tsx`, um modal, não uma tela própria) — o usuário fica "preso" na tela anterior porque a rota de destino não renderiza nada de útil.
6. **(Mais grave) Conteúdo: "Novo conteúdo" e "Abrir" não levam para a tela certa.** `NewContentModal.tsx:31-34` cria o conteúdo e navega para `/content/list` (lista), não para o editor do item recém-criado. Pior: `ContentEditor.tsx` (a tela de `/content/{id}/editor`) carrega os dados via `pagesService.getPageBySlug(...)` — **o editor de conteúdo editorial está, por engano, carregando e editando uma `Page` institucional (domínio `pages`), não um `Content`** (domínio `content`, etapa 10). É por isso que "abrir" um conteúdo mostra uma tela de criação/edição de produto/página, não do conteúdo em si.
7. **Sidebar mostra módulo desabilitado no produto.** `AppShell.tsx:86` filtra a navegação só por `roleVisibleNav[viewAsRole]` (papel do usuário) — nunca verifica `product.modulesList`. Um produto sem Knowledge Graph habilitado mostra o item "Knowledge Graph" para qualquer papel com permissão. Mesmo gap já identificado do lado do backend (ADR-0015) — aqui é a contraparte do frontend, e já é um problema **hoje**, não só uma preocupação futura.
8. **Sem opção de desfavoritar produto.** `ProductSelectScreen.tsx:49` só renderiza a estrela visualmente quando `isFavorite === true`; `ProductContextMenu.tsx` (right-click) só tem "Editar"/"Excluir" — não existe nenhum caminho para tirar um produto dos favoritos.
9. **Sem botão para ver todos os eventos do produto.** `EventsManagerDrawer.tsx:106-110` só abre a partir de dentro do editor de um bloco `event-list` — não há rota nem botão de nível de produto/sidebar para "ver agenda completa".
10. **Logging a cada caractere digitado.** Causa raiz: `ContentEditor.tsx:74-79` (`handleChangeContent`) chama `pagesService.updateSection(...)` (que loga via `logApiCall`) a cada `onChange` do editor de bloco, sem debounce, e em seguida `refreshPage(page)` → `pagesService.getPage(...)` (mais um log) — duas chamadas simuladas de API por tecla digitada. `logApiCall` (`devLog.ts:1-3`) em si está correto (é o ponto único de log mock); o problema é a ausência de debounce na escrita.
11. **Empty-state de "Páginas" com botões sem ação e com o texto errado.** `Primitives.tsx:101-113` (`EmptyState`) tem `title`/`description` padrão sobre "produto" e dois botões hardcoded (`Criar produto`, `Ver documentação`) **sem `onClick`** — usado genericamente em várias telas, inclusive na lista de páginas, onde o texto e a ação corretos seriam "Criar página" e "Ver documentação"/Central de Ajuda.
12. **Markdown sem títulos/código na toolbar.** `MarkdownEditModal.tsx:64-69` (JSX da toolbar) e `:9-31` (`applyToolbarAction`) só cobrem `bold`/`italic`/`list`/`link` — sem H1-H4, bloco de código, citação.
13. **Knowledge Graph difícil de validar/testar.** `knowledgeService.ts` confirma que a única forma real (não mock fixo) de criar uma `GraphEdge` é via referência inline `{{kg-ref:nodeId:Label}}` dentro do corpo de um `Content` — não existe (nem está prevista) uma UI manual de "desenhar uma conexão" em `GraphCanvasView.tsx`. Isso é uma decisão de design válida, mas hoje não é comunicada em lugar nenhum da UI — daí a sensação de "só fica no mock, não sei como criar uma conexão".
14. **Tipos de conteúdo (ADR-0012) sem telas/campos próprios.** `content.mocks.ts` confirma 6 tipos reais em uso (`Post`, `Manifesto`, `Reflexão`, `Poema`, `Livro`, `Playlist`), cada um com metadados específicos guardados em `metadata?: Record<string, unknown>` (`playlistId`, `isbn`, `pdfUrl`, `musicReferenceId`...) — mas o editor é 100% genérico, sem nenhum painel que exponha esses campos por tipo. É consequência direta do bug 6: o editor de conteúdo nem deveria estar reaproveitando o motor de blocos de `pages`.
15. **Backend**: a auditoria também encontrou e já corrigiu dois gaps de documentação que esta sprint expõe na prática — `Event` (agenda) não era uma entidade própria no backend (etapa 21 do `sprint-02-fundacao-backend-gpt`, agora corrigido com a Seção A.1) e a integração `content` → Knowledge Graph via `kg-ref` não estava documentada como gatilho automático (etapa 07, agora com a Seção C.1). Nenhuma ação de código necessária aqui — só ciência de que a documentação do backend já reflete o que esta sprint vai construir no frontend.

## Objetivo

Corrigir, nesta ordem de prioridade (mais grave primeiro): roteamento de criação/edição de conteúdo por tipo (bug 6/14, o mais grave), módulo-gating na sidebar (bug 7), navegação pós-ação (bugs 4/5), comportamento de modais (bugs 1/2/3), e as correções menores (bugs 8-13).

## Tarefas

### A. Domínio de conteúdo — editor correto por tipo (prioridade máxima)

1. `ContentEditor.tsx` deixa de carregar dados via `pagesService.getPageBySlug(...)` — passa a usar `contentService.getContent(id)` (criar o método se não existir) e a editar um `Content` real (`title`, `bodyMarkdown`, `type`, `metadata`), não uma `Page` institucional. Remover totalmente a dependência de `pagesService`/`BlockEditorCanvas` deste fluxo — esse motor de blocos é do domínio `pages`, não de `content`.
2. Criar um editor de conteúdo dedicado (ex.: `ContentArticleEditor.tsx`) com: campo de título, corpo em markdown (reaproveitar `MarkdownField`/`MarkdownEditModal` — Sprint 13, já em produção), e um painel de metadados que muda **conforme o `type`** do conteúdo:
   - `Post`, `Manifesto`, `Reflexão`, `Poema`: campo opcional "Vincular música" (busca/seleciona um node `MUSIC_REFERENCE` do Knowledge Graph do produto, se o módulo estiver habilitado) e campo de imagem de capa (asset picker, Sprint 13).
   - `Livro`: campos `isbn`, `pdfUrl`/upload de asset PDF, `epubUrl`/upload, `physicalAvailable` (toggle).
   - `Playlist`: campo `playlistId`/URL do Spotify.
   - Tipo `Página de produto` (presente em `NewContentModal.tsx`, `TYPES`) **não é conteúdo do domínio `content`** — ao selecioná-lo no momento de criação, não cria um `Content`; em vez disso, navega para o fluxo de criação de página em branco do domínio `pages` (`domains/pages/`). Ajustar `NewContentModal.tsx` para esse desvio de fluxo específico.
3. `NewContentModal.tsx` (`handleCreate`, hoje linha ~34) passa a navegar para a rota do editor dedicado do item recém-criado (não mais `/content/list`).
4. `ContentDataGrid.tsx` (botão "Abrir", hoje linha ~59) passa a apontar para o mesmo editor dedicado, com o `id` do conteúdo (não `title.toLowerCase()` como slug improvisado — usar o `id` real).

### B. Sidebar — filtro por módulo do produto **e** papel (ADR-0015)

1. `AppShell.tsx:86` (filtro de `nav`): hoje só `roleVisibleNav[viewAsRole].has(item.path)`. Cada item de `navConfig.ts` que corresponde a um módulo opcional (`/knowledge`, `/forms`, `/analytics`, `/assets`, `/pages` — mapear 1:1 com o catálogo de `moduleKey` já usado em `core/products/moduleDefaults.ts`/mocks) passa a exigir **também** `effectiveProduct.modulesList?.includes(moduleKeyDoItem)`. Item sem módulo associado (ex.: Dashboard, Configurações, Auditoria) continua só checando papel.
2. Documentar no próprio `navConfig.ts` (comentário) o mapeamento item de nav → `moduleKey`, para a próxima pessoa não reintroduzir o gap.

### C. Navegação pós-ação

1. `ContentEditor.tsx`/fluxo de "Enviar para revisão" (hoje só mostra toast, ~linha 111-113): depois da transição bem-sucedida, navegar para `/content/workflow` (o Kanban de `WorkflowBoard.tsx`), para o usuário ver o item já na coluna "Em revisão".
2. `DashboardGlobal.tsx:30`: trocar `navigate("/products/new")` por abrir `CreateProductModal` (mesmo padrão já usado em outras telas) e, no callback de sucesso, `navigate` para a tela do produto recém-criado (mesmo padrão de `ProductSelectScreen.tsx:117-122`).

### D. Modais — robustez de fechar/abrir

1. Padronizar: todo modal "tipo diálogo" (overlay + conteúdo centralizado) usa o mesmo componente-base (se não existir um, extrair de `ConfirmDialog.tsx` um `ModalShell`/`DialogBase` reutilizável) — reduz o risco de algum modal futuro divergir do padrão de `stopPropagation()`.
2. `AppShell.tsx` (`showUserMenu`, linhas 101-115): adicionar overlay invisível com `onClick={() => setShowUserMenu(false)}` (mesmo padrão de `Notifications.tsx:75-80`) ou um `useEffect` com listener de `mousedown` no `document` que fecha o menu se o clique for fora do ref do menu — escolher uma abordagem e aplicar de forma consistente com o restante do app.
3. `NotificationModal.tsx`: renderizar via `createPortal(..., document.body)` (ou confirmar que já há um portal no `ModalShell` da Tarefa D.1) para eliminar qualquer interferência de `transform` de ancestrais — depois, confirmar visualmente que abre centralizada ao clicar numa notificação do sino.

### E. Correções menores de UI

1. **Favoritar/desfavoritar** (`ProductSelectScreen.tsx`): adicionar opção "Favoritar"/"Desfavoritar" (texto dinâmico conforme `p.isFavorite`) no `ProductContextMenu.tsx` (right-click), chamando `productsService`/mock para alternar `isFavorite` e atualizar a lista.
2. **Empty-state genérico** (`Primitives.tsx`, `EmptyState`): tornar `title`/`description`/textos dos botões/`onClick` de cada botão totalmente parametrizáveis via props (sem default hardcoded de "produto"). Atualizar a tela de lista de páginas vazia para passar: título "Nenhuma página criada", botão "Criar página" (abre o fluxo de criação de página) e botão "Ver documentação" → navega para a Central de Ajuda (Sprint 13) se existir, senão para uma rota/link de documentação real. Auditar os demais usos de `EmptyState` no app para garantir que nenhum outro ficou com botão sem `onClick` por herdar o default.
3. **Botão "Ver todos os eventos"**: adicionar um ponto de entrada de nível de produto (ex.: item dentro de "Páginas" ou botão na tela de páginas) que abre `EventsManagerDrawer` fora do contexto de um bloco — julgar o melhor lugar na jornada (sugestão: botão "Agenda"/"Eventos" na lista de páginas, já que `Event` é entidade própria do produto, não de uma página específica — ver etapa 21, Seção A.1 do backend).
4. **Toolbar de markdown** (`MarkdownEditModal.tsx`): adicionar botões H1-H4, bloco de código (` ``` `) e citação (`>`) — estender o tipo `ToolbarAction` e o switch de `applyToolbarAction` (hoje linhas 9-31) com os novos casos, seguindo o mesmo padrão dos existentes (`bold`/`italic`/`list`/`link`).

### F. Performance — debounce na edição de blocos

1. `handleChangeContent` (`ContentEditor.tsx`, ou onde a Tarefa A realocar essa lógica para o domínio `pages` de verdade) passa a debounçar a chamada de update (ex.: 500ms após parar de digitar), em vez de chamar o service a cada `onChange`. Aplicar o mesmo princípio em qualquer outro editor de texto ligado diretamente a uma chamada de service por tecla (auditar `GlobalsSettings.tsx`, `AudioBlockEditor.tsx`, sinalizados na investigação com o mesmo padrão de `onChange` direto).
2. Resultado esperado: console deixa de logar uma chamada mock por tecla digitada; loga só quando o debounce de fato disparar o "save".

### G. Knowledge Graph — tornar o fluxo de criação visível

1. `GraphCanvasView.tsx`: adicionar um estado vazio/dica explicando como conexões são criadas hoje — "Conexões são criadas automaticamente ao referenciar `{{kg-ref}}` no corpo de um conteúdo" — para o usuário não concluir que o grafo é "só mock" sem entender o mecanismo real.
2. Confirmar que o editor de conteúdo (Tarefa A) expõe de forma clara a sintaxe/UI de `kg-ref` (idealmente um botão na toolbar de markdown, não só a sintaxe de chaves duplas memorizada) — avaliar se cabe nesta sprint ou fica registrado como melhoria futura (decisão do executor, documentar a escolha).

## Critérios de aceite

- [ ] Editar um conteúdo existente abre o editor de conteúdo real (`contentService`), nunca o editor de páginas/blocos.
- [ ] Criar um conteúdo do tipo "Página de produto" não cria um `Content` — abre o fluxo de criação de página em branco do domínio `pages`.
- [ ] Cada tipo de conteúdo (Post/Manifesto/Reflexão/Poema/Livro/Playlist) mostra os campos de metadado próprios no editor.
- [ ] "Novo conteúdo" e "Abrir" levam ao editor do item certo, nunca à lista nem a uma tela de produto.
- [ ] Sidebar não mostra item de módulo desabilitado no produto atual, mesmo para Super Admin — só aparece quando módulo habilitado **e** papel permite.
- [ ] Enviar página/conteúdo para revisão navega de volta para o workflow.
- [ ] Criar produto pelo Dashboard navega para o produto recém-criado.
- [ ] Nenhuma modal fecha sozinha ao interagir com seu próprio conteúdo; menu de perfil fecha ao clicar fora; modal de notificação reaberta pelo sino aparece centralizada.
- [ ] Produto pode ser desfavoritado via right-click em `/select-product`.
- [ ] Empty-state de páginas mostra "Criar página"/"Ver documentação" funcionais, com texto certo.
- [ ] Existe um ponto de entrada para ver todos os eventos do produto, fora do editor de um bloco específico.
- [ ] Editar texto markdown não gera mais um log de chamada mock por tecla digitada — só ao persistir (debounce).
- [ ] Toolbar de markdown tem H1-H4, código e citação, além dos já existentes.
- [ ] `GraphCanvasView` comunica como uma conexão é criada (via `kg-ref`), mesmo sem UI manual de desenho de edge.

## Comandos de Git

```bash
git checkout develop && git pull origin develop
git checkout -b sprint/15-correcoes-pos-refinamento

git commit -m "fix(content): editor de conteudo usa contentService, nao mais pagesService"
git commit -m "feat(content): paineis de metadado por tipo e desvio para pages em 'pagina de produto'"
git commit -m "fix(content): novo conteudo e abrir navegam para o editor certo, nao para a lista"
git commit -m "fix(sidebar): filtra navegacao por modulo do produto alem do papel do usuario"
git commit -m "fix(navigation): enviar para revisao volta ao workflow; criar produto no dashboard navega ao produto criado"
git commit -m "fix(modals): menu de perfil fecha ao clicar fora; notificacao reabre centralizada via portal"
git commit -m "feat(ui): favoritar/desfavoritar produto via right-click; empty-state de paginas com acoes reais"
git commit -m "feat(ui): toolbar de markdown com titulos, codigo e citacao; botao de ver todos os eventos"
git commit -m "perf(content): debounce na edicao de bloco, remove logging por tecla digitada"
git commit -m "feat(knowledge): estado vazio do grafo explica criacao de conexao via kg-ref"

git push -u origin sprint/15-correcoes-pos-refinamento
```

Ao final, finalize a sprint no gitflow:

```bash
./scripts/gitflow-finish-sprint.sh sprint/15-correcoes-pos-refinamento
```
