# Sprint 12 — Correções pós-teste manual: editor de blocos, preview, Knowledge Graph e mobile

> Pré-requisito: Sprint 11 concluída e em execução manual (esta sprint corrige bugs encontrados rodando a aplicação de verdade, não é planejamento especulativo). Todos os achados abaixo foram confirmados lendo o código atual, não apenas o relato do usuário.

## Contexto

A Sprint 11 já foi executada (domínio `pages`, editor de blocos por seções, Knowledge Graph com referência inline). Testando a aplicação rodando, foram reportados problemas em duas rodadas. Cada um foi confirmado no código antes de entrar nesta sprint — nenhum item aqui é suposição.

### Padrão sistêmico: "lista sem CRUD" se repete em vários domínios, não só na galeria

A causa do bug da galeria (Tarefa D) não é exclusiva dela: `ArrayFieldEditor` (genérico, usado para qualquer bloco com array de itens) só edita campos de itens que já existem — não tem botão de adicionar nem remover. Isso afeta `gallery`, `card-list`, `feature-grid`, `timeline`, `faq`, links de `navbar`/`footer`, e — confirmado numa segunda rodada de teste — também o **FormBuilder** (dá para criar um formulário, mas não para adicionar/editar/remover seus campos, nem para editar/excluir um formulário já existente a partir da listagem). Em vez de corrigir um por um, esta sprint cria um componente único de CRUD de itens e o aplica em todos os lugares afetados.

### Causa raiz mais importante: dois domínios de conteúdo colididos

`domains/content/pages/ContentEditor.tsx` (acessado pelo botão "Novo conteúdo" de `EditorialDashboard.tsx`) não edita um artigo do domínio `content` — ele carrega `pages?.[0]` e edita **seções de uma `Page`** do domínio `pages`. Ou seja: o botão pensado para criar um **artigo editorial** (post de blog, manifesto, reflexão — o domínio `content` da Sprint 02-gpt, com workflow Draft→Review→Published) na prática abre o **editor de página institucional** (domínio `pages`, seções/blocos), sempre na primeira página (`pages[0]`, geralmente Home). Isso explica por si só o bug "clico em novo conteúdo e ele manda direto para a Home" — não é falta de uma pergunta "qual tela", é a tela errada sendo aberta. A Tarefa B resolve a causa, não o sintoma.

### Causa raiz do bug do `two-column`: divergência entre o que o frontend implementou e o que o backend já espera

`docs/sprints/sprint-02-fundacao-backend-gpt/21_dominio_pages_secoes_e_blocos.md` (escrito na Sprint 11, pronto para o GPT) já define a regra: *"`two-column` com `left`/`right` só podem conter tipos da allowlist: `text`, `rich-text`, `image`, `cta`"* — ou seja, o backend já foi especificado esperando que cada coluna seja uma **lista de mini-blocos tipados** (podendo incluir imagem). O frontend que foi implementado (`domains/pages/blockDefaults.ts`) ignorou essa especificação e shipou `left`/`right` como `{ title, body }` fixo, sem imagem e sem suportar mini-blocos. Se isso não for corrigido agora, a integração de amanhã vai falhar: o frontend vai mandar um shape que o backend (já escrito conforme a etapa 21) vai rejeitar.

### Mesmo problema de novo: bloco `contact` não referencia formulário nenhum

A mesma etapa 21 também especifica que o bloco `contact` exige `formId` referenciando o domínio `form`. O frontend implementado (`blockDefaults.ts`, linhas 34-37) não tem `formId` em lugar nenhum — tem um array `fields` próprio e desconectado, reinventando uma definição de formulário dentro do bloco em vez de referenciar um `FormDefinition` já criado no módulo Forms. Resultado confirmado pelo usuário: criar um formulário em Forms não dá nenhuma forma de usá-lo em qualquer página/tela — os dois domínios nunca se conectam. Corrigido na Tarefa K.4.

## Objetivo

Corrigir os problemas reportados, com prioridade para os que bloqueiam a integração com o backend (logging, shape do `two-column`, preview, Knowledge Graph) antes dos de polimento visual (mobile, badges). Incluir também o padrão sistêmico de CRUD de itens (Tarefa D) e a capacidade de upload de arquivos além de imagem (Tarefa L), confirmados como necessários para os contratos de Alexandre Dev (currículo PDF), WikiDev (downloads) e Conecta Talentos (currículo do candidato).

## Tarefas

### A. Logging centralizado de toda chamada de API (mock ou real)

1. Criar `frontend/src/shared/services/devLog.ts`:
   ```ts
   export function logApiCall(method: string, path: string, payload?: unknown) {
     console.log(`[mock→backend] ${method} ${path}`, payload ?? "");
   }
   ```
2. Hoje só `pagesService` loga (`updateSection`/`createSection`, formato `[mock→backend] PUT ...`). Auditar **todos** os services (`contentService`, `assetsService`, `usersService`, `tenantsService`, `productsService`, `knowledgeService`, `formsService`, etc. — todo arquivo em `domains/*/services/*Service.ts` e `core/*/services/*`) e garantir que **toda** função que simula uma chamada de API chame `logApiCall(...)` com o método/rota/payload exatos que ela chamaria de verdade — mesmo padrão usado em `pagesService`, sem inventar um novo.
3. Em `shared/services/apiClient.ts`, adicionar a mesma chamada `logApiCall` dentro de `request()`, antes do `fetch` — assim quando a Sprint 07 trocar mock por real, o log continua existindo sem precisar lembrar de adicionar de novo.

### B. Separar "Criar artigo" de "Criar/editar página" — tela de Páginas

1. Criar `domains/pages/pages/PagesList.tsx`, rota `/products/:productSlug/pages` — lista as páginas do produto (`pagesService.listPages`), com slug/título/status/versão, e ação "+ Nova página" (pede `title`, `slug`, `locale` antes de criar — não cria direto).
2. `ContentEditor.tsx`/o editor de blocos passa a ser acessado **a partir dessa lista** (clicar numa página existente) ou a partir de "+ Nova página" — nunca mais auto-seleciona `pages[0]`. Remover o `useEffect` que faz isso (linhas 36-40 hoje).
3. `EditorialDashboard.tsx`/"Novo conteúdo" volta a ser exclusivamente sobre o domínio `content` (artigo/post — título, tipo, idioma, autor, workflow), não sobre `pages`. Se o produto não tiver o módulo de páginas relevante para o tipo de conteúdo (ex.: WikiDev não usa `pages`, só `content`), o botão nem aparece a opção de página.
4. Atualizar a navegação lateral/rotas para refletir os dois conceitos como itens distintos quando o produto tiver ambos os módulos habilitados: "Conteúdo" (artigos) e "Páginas" (institucional).

### C. Bloco `two-column`: alinhar com a especificação já escrita para o backend

1. Em `domains/pages/blockDefaults.ts`, trocar o shape de `two-column`:
   ```ts
   "two-column": {
     left: [{ type: "text", content: { title: "Coluna 1", body: "Conteúdo da primeira coluna." } }],
     right: [{ type: "text", content: { title: "Coluna 2", body: "Conteúdo da segunda coluna." } }],
   },
   ```
   onde cada item de `left`/`right` é `{ type: "text"|"rich-text"|"image"|"cta", content: {...} }` — mesma allowlist da etapa 21 do backend.
2. O editor (`BlockEditorCanvas`) precisa de um sub-editor que, para `two-column`, renderiza cada coluna como uma mini-lista de blocos (reaproveitar o mesmo seletor de tipo de bloco já usado no nível de página, restrito à allowlist `text`/`rich-text`/`image`/`cta`), com botão "+ Adicionar bloco" por coluna.
3. Atualizar `domains/pages/mocks/pages.mocks.ts` para refletir o novo shape em qualquer seção `two-column` existente no mock.

### D. CRUD genérico de itens em blocos com lista (gallery, card-list, feature-grid, timeline, faq, navbar/footer)

1. Criar `domains/pages/components/ItemsCrudEditor.tsx` — substitui o `ArrayFieldEditor` genérico em todo lugar onde ele é usado hoje. Diferente do atual (só edita campos de itens existentes), este:
   - Botão "+ Adicionar item" no final da lista (cria um item vazio com o shape correto do bloco — ex.: `{ src: "", alt: "" }` para `gallery`, `{ title: "", body: "", icon: "" }` para `feature-grid`, `{ question: "", answer: "" }` para `faq`).
   - Botão remover (ícone lixeira) por item, com confirmação se o item tiver conteúdo preenchido (evitar perda acidental).
   - Reordenar (setas para cima/baixo ou drag handle).
   - Validação client-side configurável por bloco (ex.: `gallery` exige `alt` em cada item antes de salvar — `rules: { altRequired: true, maxItems: 50 }` já documentado na auditoria CMS-first; `faq` exige `question` e `answer` não vazios).
2. Aplicar esse componente em **todos** os blocos com array de itens: `gallery`, `card-list`, `feature-grid`, `timeline`, `faq`, e os links de `navbar`/`footer` — hoje todos sofrem do mesmo problema (só editam item existente, não adicionam/removem).
3. `event-list` (Tarefa E) e `downloads` (Tarefa L) têm necessidades um pouco diferentes (fonte externa / upload de arquivo) e por isso ganham editor próprio, não o `ItemsCrudEditor` genérico — mas a barra de ações (adicionar/remover/reordenar) deve ter a mesma aparência para consistência visual.

### E. Bloco `event-list`/Agenda: gestão de eventos

1. O bloco `event-list` referencia uma fonte externa (`source: { type: "contentType", contentType: "evento", filter: {...} }`) mas não existe nenhum lugar para gerenciar esses eventos. Criar `domains/pages/services/eventsService.ts` (mock-backed, mesmo padrão dos demais services) com `listEvents(productSlug)`, `createEvent`, `updateEvent`, `deleteEvent` — shape do evento espelhando `agenda.json` do contrato Maestro Beton: `{ id, title, date, location, type: "public"|"private", visibility: "public"|"public-summary"|"private", description, image? }`.
2. No editor do bloco `event-list`, adicionar botão "Gerenciar eventos" que abre um `Drawer` listando os eventos da fonte referenciada, com criar/editar/excluir — sem precisar saltar para outra tela.
3. Regra de privacidade do contrato (Maestro Beton, Seção 12): evento `type: "private"` deve permitir ocultar local/descrição, mostrando só "Data reservada" — aplicar essa regra também no formulário de edição do evento (campo "Visibilidade" controla o que é exibido publicamente, com preview do que ficará visível).

### F. Preview: refletir o conteúdo real, não mock fixo

1. `ResponsivePreviewFrame.tsx` hoje ignora `pagesService` e mostra HTML fixo por produto (`productContent`/`wikidevArticle` hardcoded). Reescrever para: dado o `pageId`/`sections` atualmente em edição (ou publicados, dependendo de onde o preview é acionado), renderizar cada `Section` através de um `BlockRenderer` novo.
2. Criar `domains/pages/components/BlockRenderer.tsx`: recebe um `Section` (`type` + `content`) e renderiza a representação visual aproximada de cada `BlockType` (`hero`, `card-list`, `gallery`, `two-column`, `contact`, etc.) — não precisa ser pixel-perfect, mas precisa refletir o conteúdo real editado, não um texto estático.
3. Esse mesmo `BlockRenderer` deve ser reaproveitado futuramente para a renderização pública real (fora de escopo desta sprint, mas não implementar o preview de um jeito que depois precise ser refeito).

### G. Visualizar/exportar JSON da página e revisar exclusão

1. Adicionar uma ação "Ver JSON" (aba `Histórico` do editor, ou um botão dedicado) que mostra o objeto `Page` completo (`{ id, slug, title, locale, status, version, seo, sections[] }`) formatado — permite ao usuário comparar visualmente com os contratos fornecidos (`home.json`, `agenda.json`, etc.) durante o teste de integração, sem precisar abrir devtools.
2. Confirmar/implementar exclusão de página e de seção pela UI (`DELETE /pages/{pageId}`, `DELETE /pages/{pageId}/sections/{sectionId}` já especificados na etapa 21 do backend) — hoje só há remoção de bloco da lista local (`Remover bloco`, visto no canvas); confirmar que isso chama `pagesService.deleteSection` (com seu `logApiCall`, Tarefa A) e que existe ação equivalente para excluir a página inteira a partir da tela de Páginas (Tarefa B.1).

### H. Knowledge Graph: dois bugs concretos na referência inline

1. **`edgeType` fora do catálogo**: `handleLinkEntity` (EditorPanels, ao criar uma referência `kg-ref`) chama `knowledgeService.createEdge(section.id, node.id, "relacionado a")` — `"relacionado a"` **não está no catálogo fechado de `edgeType`** já definido para o backend (etapa 07: `CONTAINS, BELONGS_TO, REFERENCES, RELATED_TO, INSPIRED_BY, USES, IMPLEMENTS, PUBLISHED_AS, SUBMITTED_TO, TAGGED_WITH, PART_OF, DEPENDS_ON`). Trocar para `"RELATED_TO"` — qualquer string fora do enum seria rejeitada (400) pelo backend real.
2. **Nó de origem pode não existir**: `createEdge(section.id, ...)` usa o `id` da `Section` como `sourceNodeId`, mas nada garante que existe um `GraphNode` correspondente a essa seção/conteúdo no grafo. Antes de permitir criar uma referência inline, garantir (ou criar automaticamente, via `knowledgeService.ensureNodeForContent(sectionId, ...)`) que o nó de origem existe — senão a aresta referencia um nó inexistente e o backend rejeitaria com 404 (regra já definida na etapa 07: "Edge exige sourceNodeId e targetNodeId existentes").
3. Confirmado e **sem mudança necessária**: `kg-ref` só está disponível em blocos `text`/`rich-text` (`canLinkEntity`) — isso está correto, é o esperado (referência inline faz sentido em texto corrido, não em `hero`/`gallery`).

### I. Menu mobile consistente em `/select-tenant` e `/select-product`

1. Hoje cada card tem um botão `MoreVertical` individual (`lg:hidden`) abrindo um menu de contexto por item — funciona, mas é inconsistente com o padrão de `AppShell.tsx` (hamburguer único no header abrindo um drawer com as opções, linhas ~111 e ~169-176).
2. Extrair o padrão hamburguer+drawer de `AppShell.tsx` para um componente reutilizável (`shared/components/MobileDrawerMenu.tsx` ou similar) e usá-lo em `TenantSelectScreen.tsx`/`ProductSelectScreen.tsx` para as ações que hoje quebram/ficam apertadas em mobile — sem remover o menu de contexto por card (que continua fazendo sentido por item), mas garantindo que qualquer ação de nível de tela (ex.: filtros, ordenação, "Criar Tenant") tenha o mesmo padrão de menu mobile do resto do app.

### J. Badges fora de posição no `PageHeader`

1. `PageHeader` (`shared/components/Primitives.tsx`) tem um slot `children` pensado para botões de ação, mas `EditorialDashboard.tsx` (e possivelmente outras telas) passa um `<Badge tone="blue">Conteúdo</Badge>` dentro desse slot, junto dos botões — em mobile, badge e botões competem pelo mesmo `flex-wrap`, quebrando o layout.
2. Auditar todo uso de `<PageHeader>` em `domains/**/pages/*.tsx` procurando `<Badge` dentro do bloco de `children` — mover qualquer badge informativo para a linha do título (junto ao `<h1>`/badge de produto já existente), deixando o slot de `children` exclusivamente para botões de ação.

### K. Formulários: CRUD do formulário e dos campos dentro do builder

1. **Listagem de formulários** (`FormsList.tsx`/`FormsDashboard`): hoje dá para criar um formulário, mas não há ação de editar ou excluir um formulário já existente na lista — adicionar essas duas ações por linha (editar abre o `FormBuilder` carregado com o formulário; excluir pede confirmação via `ConfirmDialog`, igual ao padrão já definido para ações destrutivas na Sprint 10).
2. **Campos dentro do `FormBuilder`**: a paleta de tipos de campo (Texto, Email, Telefone, Upload, etc.) precisa realmente adicionar um campo novo ao formulário ao ser clicada — confirmar se isso já foi corrigido pela Sprint 10 (que registrou este mesmo problema) e, se ainda não funciona na prática, corrigir agora: clicar num tipo da paleta adiciona um campo ao array de campos do formulário (estado local do builder), que aparece no canvas e pode ser selecionado para editar suas propriedades (label, placeholder, obrigatório, validação) no painel lateral.
3. Cada campo do formulário, uma vez adicionado, precisa de ação de remover (Tarefa D não cobre isso diretamente, mas é o mesmo padrão de UI — usar o mesmo `ItemsCrudEditor` ou um editor de campos dedicado, mantendo consistência visual).
4. **Conectar formulário criado a qualquer página** (gap confirmado, ver nota no Contexto): em `domains/pages/blockDefaults.ts`, o bloco `contact` troca o array `fields` próprio por `{ title, infoItems: [], formId: "" }`, alinhado com a etapa 21 do backend. No editor do bloco `contact` (`BlockEditorCanvas`), o campo `formId` deixa de ser texto livre e vira um seletor "Selecionar formulário", populado por `formsService.listForms(productSlug)` — lista os formulários já criados no módulo Forms do produto, com opção de criar um novo ali mesmo (atalho para `FormBuilder`) se nenhum servir.
5. **Bloco `form` genérico** (novo tipo de bloco, além de `contact`): para os casos em que o usuário quer embutir um formulário já criado no meio de qualquer página, não só na seção de contato (ex.: captura de lead numa landing page) — `content: { formId: string }`, mesmo seletor de formulário do item 4. Adicionar ao catálogo de `BlockType` (frontend e etapa 21 do backend).
6. **Integridade ao excluir formulário** (Tarefa K.1 introduz exclusão): antes de excluir um formulário, verificar se algum bloco `contact`/`form` de alguma página do produto referencia esse `formId` — se sim, bloquear a exclusão com mensagem clara ("Este formulário está em uso na página X") em vez de deixar a página com uma referência quebrada.
7. Campo do tipo "Upload" (já previsto no catálogo de tipos de campo, `field-types`) deve, ao ser configurado, permitir escolher quais formatos de arquivo são aceitos (ex.: apenas PDF, ou PDF+imagem) — relevante para o formulário de candidatura da Conecta Talentos (upload de currículo), mas é uma propriedade genérica do campo, não exclusiva desse produto.

### L. Upload de arquivos (PDF e qualquer formato) — assets genéricos e bloco de download

Confirmado nos contratos: Alexandre Dev precisa de download de currículo em PDF (PT-BR e EN) e outros documentos (apresentação, case study, certificado — `downloads.json`); WikiDev precisa de uma lista de downloads (`DOWNLOADS`, `type: "pdf"`); Conecta Talentos precisa que o candidato anexe um currículo em PDF na candidatura (`CandidateSubmission.resume`). Os três casos dependem da mesma capacidade de base: assets não podem ser só imagem.

1. Revisar `AssetUploadScreen.tsx`/`AssetPicker.tsx`/`domains/assets`: confirmar se o fluxo de upload já aceita qualquer tipo de arquivo (PDF, DOCX, ZIP...) ou se assume implicitamente imagem (input `accept="image/*"`, preview só de imagem, etc.). Se assumir imagem, generalizar: aceitar qualquer formato, exibir um ícone genérico de arquivo (com extensão) quando não for imagem, em vez de tentar renderizar uma miniatura.
2. Criar o tipo de bloco `download` no catálogo de `pages` (`content: { title, description, fileAssetId, fileType, fileSizeLabel? }`), com o mesmo padrão de `ItemsCrudEditor` (Tarefa D) para páginas que têm múltiplos downloads (ex.: Alexandre Dev: currículo PT-BR, currículo EN, apresentação, case study, certificado).
3. **Não implementar agora** (fora de escopo, já registrado em `docs/trace/00_endpoints_esperados.md` Seção B.10): o formulário de candidatura completo da Conecta Talentos (`CandidateSubmission`) — mas a capacidade de upload genérico construída aqui (item 1) é exatamente o que aquele fluxo futuro vai reaproveitar para o anexo de currículo. Deixar isso explícito no código (comentário ou nota no service) para quem for implementar depois.
4. Atualizar os mocks de Alexandre Dev e WikiDev (Sprint 11, Tarefa E, ainda pendente de conteúdo profundo) para já incluir os itens de download reais do contrato, usando o novo bloco `download`.

## Critérios de aceite

- [ ] Toda chamada de service loga `[mock→backend] MÉTODO ROTA payload`, não só `pagesService`.
- [ ] "Novo conteúdo" cria um artigo do domínio `content`, nunca abre o editor de páginas na Home.
- [ ] Existe uma tela "Páginas" listando as páginas do produto, com "+ Nova página" pedindo título/slug/locale.
- [ ] `two-column` aceita mini-blocos tipados (`text`/`rich-text`/`image`/`cta`) em `left`/`right`, com imagem possível em qualquer coluna.
- [ ] `gallery`, `card-list`, `feature-grid`, `timeline`, `faq` e links de `navbar`/`footer` têm botões reais de adicionar/remover/reordenar item, com validação por bloco (ex.: `alt` obrigatório em `gallery`).
- [ ] `event-list` tem um jeito de gerenciar (criar/editar/excluir) os eventos que aparecem nele, respeitando a regra de privacidade de eventos `private`.
- [ ] Preview reflete o conteúdo real editado (qualquer mudança num bloco aparece no preview), não mock fixo.
- [ ] Existe uma forma de visualizar o JSON completo da página no próprio editor.
- [ ] Excluir página e excluir seção funcionam pela UI.
- [ ] Referência inline (`kg-ref`) cria aresta com `edgeType: "RELATED_TO"` (catálogo fechado), e só é permitida quando o nó de origem existe (criado automaticamente se necessário).
- [ ] `/select-tenant` e `/select-product` usam o mesmo padrão de menu mobile do `AppShell`.
- [ ] Nenhum `<Badge>` está dentro do slot de botões de `PageHeader` em nenhuma tela.
- [ ] Dá para editar e excluir um formulário existente a partir da listagem, e adicionar/remover campos dentro do `FormBuilder` (clicar num tipo da paleta efetivamente adiciona o campo).
- [ ] O bloco `contact` referencia um `formId` real (selecionado de uma lista, não texto livre) em vez de um array de campos próprio e desconectado.
- [ ] Existe um bloco `form` genérico para embutir um formulário já criado em qualquer página, não só na seção de contato.
- [ ] Excluir um formulário em uso por algum bloco `contact`/`form` é bloqueado com mensagem clara, não deixa referência quebrada.
- [ ] Campo de formulário do tipo "Upload" permite restringir formatos de arquivo aceitos.
- [ ] Upload de asset aceita qualquer tipo de arquivo (não só imagem), com ícone genérico para não-imagens.
- [ ] Existe o bloco `download` no catálogo de `pages`, com CRUD de itens, e Alexandre Dev/WikiDev têm seus downloads reais no mock.

## Comandos de Git

```bash
git checkout develop && git pull origin develop
git checkout -b sprint/12-correcoes-pos-teste

git commit -m "feat(shared): logging centralizado de chamadas de api em todos os services"
git commit -m "fix(pages): separa fluxo de novo artigo do editor de paginas; adiciona tela de paginas"
git commit -m "fix(pages): two-column aceita mini-blocos tipados, alinhado com a etapa 21 do backend"
git commit -m "feat(pages): editor de itens de galeria e gestao de eventos do bloco agenda"
git commit -m "fix(pages): preview renderiza conteudo real via blockrenderer, nao mock fixo"
git commit -m "feat(pages): visualizar json da pagina e confirmar exclusao de pagina/secao"
git commit -m "fix(knowledge): edgetype da referencia inline usa catalogo fechado e garante no de origem"
git commit -m "fix(core): menu mobile consistente em select-tenant/select-product"
git commit -m "fix(shared): remove badges do slot de botoes do pageheader"
git commit -m "fix(forms): crud de formulario na listagem e de campos dentro do builder"
git commit -m "fix(pages): bloco contact e novo bloco form referenciam formId real do dominio forms"
git commit -m "feat(assets): upload generico de arquivos e bloco de download nas paginas"

git push -u origin sprint/12-correcoes-pos-teste
```

Ao final, finalize a sprint no gitflow:

```bash
./scripts/gitflow-finish-sprint.sh sprint/12-correcoes-pos-teste
```
