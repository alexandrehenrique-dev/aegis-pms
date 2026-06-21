# Sprint 16 — Knowledge Graph real: conexão entre conteúdos durante a autoria

> Pré-requisito: nenhum específico de outra sprint do frontend — mas pressupõe o estado atual de `domains/content/` já com `ContentEditor.tsx`/`ContentArticleEditor.tsx`/`MarkdownEditModal.tsx` como estão hoje (Sprint 15, Tarefas A/D/E já em produção: editor dedicado por `contentService`, debounce, toolbar com H1-H4/código/citação). Esta sprint não refaz nada disso — só liga o Knowledge Graph ao que já existe.

## Contexto

O usuário relatou, em teste manual, que o Knowledge Graph "parece só uma ferramenta bonita" — nunca viu uma jornada real de criar um post e vincular uma palavra a outro conteúdo de outra página. Uma investigação dedicada confirmou que a percepção está certa, com causa raiz precisa:

1. **As funções de escrita existem, mas são código morto.** `knowledgeService.createEdge`/`ensureNodeForContent` (`domains/knowledge/services/knowledgeService.ts:64-74`) nunca são chamadas por nenhum fluxo de salvamento de conteúdo. `contentService.updateContent` (`contentService.ts:31-37`) só faz `Object.assign(row, patch)` — nenhum parsing do corpo, nenhuma chamada ao domínio de grafo.
2. **Mesmo que fossem chamadas, a tela do grafo não veria o resultado.** `knowledgeService.listNodes()`/`listEdges()` (linhas 13-18) retornam `kgNodes`/`kgEdges` — os arrays de mock **originais**, importados diretamente — enquanto `createEdge`/`ensureNodeForContent` escrevem em `allNodes`/`allEdges` (linhas 9-10), um array **diferente** (união dos três produtos). São dois stores desconectados: escrever num nunca aparece no outro.
3. **Não existe UI para inserir uma referência sem saber o ID de cabeça.** O único "caminho" documentado é digitar manualmente `{{kg-ref:nodeId:Label}}` no corpo em markdown — inclusive há uma dica de texto em `ContentArticleEditor.tsx:82-84` prometendo que "a conexão é criada automaticamente ao salvar", o que hoje é falso (consequência direta do item 1). `EntityPicker.tsx` (busca de entidade por label, `domains/knowledge/components/EntityPicker.tsx`) já existe, pronto para ser embutido, mas não está ligado a nenhum editor.
4. **Conclusão da investigação**: as peças certas já existem (`EntityPicker`, parser de `kg-ref` em leitura via `ArticleBody.tsx`/`KgRefMark.tsx`, contratos `KGNode`/`KGEdge` aderentes ao backend) — falta só a fiação entre escrever conteúdo e o grafo reagir a isso. Não é um redesenho, é concluir uma jornada que ficou pela metade.

Esta investigação também resolveu uma ambiguidade de escopo nunca escrita antes — formalizada agora em **ADR-0016**: o Knowledge Graph modela relações entre **conteúdos** (`Content`, `Asset`, `Form`, etc.), nunca entre os componentes/seções de uma `Page` (isso já é resolvido pelo domínio `pages`). A jornada de criação de conexão é **sempre a partir da autoria de conteúdo** (selecionar entidade ao escrever), nunca um desenho manual no canvas do grafo.

**Regra adicional, também formalizada em ADR-0016**: uma edge só pode conectar dois nós do **mesmo produto** — não existe (nem é suportada) uma conexão cross-produto. Cada nó pertence a exatamente um produto (o produto do `Content` que o originou). `EntityPicker` busca só entre os nós do produto do conteúdo em edição; `createEdge` rejeita silenciosamente (loga aviso) qualquer tentativa de ligar nós de produtos diferentes, mesmo via `{{kg-ref}}` digitado manualmente.

## Objetivo

Fazer o Knowledge Graph reagir de verdade a ações reais do usuário: escrever um conteúdo, vincular uma palavra a outro conteúdo existente via busca (não digitação de ID), salvar, e ver a nova conexão na tela do grafo sem precisar de backend nem de reload forçado.

## Tarefas

### A. Unificar o store do `knowledgeService` (causa raiz #2)

1. Em `knowledgeService.ts`, `listNodes()`/`listEdges()` passam a retornar `allNodes`/`allEdges` (o store mutável, união dos produtos) — não mais `kgNodes`/`kgEdges` diretamente. As telas que hoje chamam `listNodes()`/`listEdges()` sem argumento (`GraphCanvasView`, `RelationshipExplorer`, `EntityDetails`, `EntitySearch`) continuam sem filtro por produto — esse filtro de **visualização** é um problema pré-existente e maior, fora do escopo desta sprint (que é sobre a jornada de criação de conexão, não sobre re-arquitetar as telas de visualização do grafo).
2. `searchNodes`/`ensureNodeForContent`/`createEdge` passam a exigir `productSlug` — cada nó do store passa a ter um produto-dono associado internamente (via os três grupos de seed já existentes — Maestro Beton, WikiDev, Loki — mais os nós criados em runtime por `ensureNodeForContent`). Isso é o que viabiliza a regra de "mesmo produto" (ver Contexto): `searchNodes` só retorna nós do produto informado; `createEdge` rejeita (loga aviso, não lança erro) se origem e destino forem de produtos diferentes.
3. Confirmar que `getNodePreview`/`listRelated` (que já leem de `allNodes`/`allEdges`) continuam consistentes depois da mudança.

### B. Ligar a escrita: parsing de `kg-ref` ao salvar `Content` (causa raiz #1)

1. Em `contentService.ts`, `updateContent(id, patch)` (linha ~31) e `createContent(payload)` (linha ~63): sempre que o patch/payload tiver `body` (ou `bodyMarkdown`, conforme o campo real de `ContentRow`) com texto, extrair todas as ocorrências de `{{kg-ref:([\w-]+):([^}]+)}}` via regex.
2. Para cada `Content` salvo com pelo menos uma referência: chamar `knowledgeService.ensureNodeForContent(content.id, content.title, productSlug, "Página")` (ou o `KGEntityType` mais adequado a conteúdo — decidir e documentar a escolha; ver ADR-0016) uma vez, depois `knowledgeService.createEdge(content.id, nodeId, productSlug, "RELATED_TO")` para cada `nodeId` encontrado — **evitar duplicar** a mesma edge se o usuário salvar várias vezes sem mudar as referências (checar se já existe antes de dar `push`, mesma lógica defensiva que `ensureNodeForContent` já usa para nós). `productSlug` vem do produto efetivo da sessão (mesmo padrão `product ? slugify(product.name) : "maestro-beton"` já usado por `PagesList.tsx`/`PageEditor.tsx`/`NewContentModal.tsx`).
3. `nodeId` referenciado que não existe em `allNodes`, **ou que existe mas pertence a outro produto** (regra de "mesmo produto", ver Contexto/ADR-0016): por ora (mock), não bloquear o salvamento — apenas não criar a edge e logar um aviso de console (`console.warn`) citando o `nodeId` e o motivo (ausente vs. produto diferente). Isso é deliberadamente mais permissivo que o backend real (etapa 07, Seção C.1, que rejeita com 400) porque o mock não tem validação de transação — documentar essa diferença no código com um comentário.
4. **Module-gating** (mesma regra de ADR-0015, aplicada aqui no frontend): antes de processar qualquer `kg-ref` encontrado no `body`, verificar se o produto do conteúdo tem o módulo `"Knowledge Graph"` habilitado (`resolveEnabledModules`, mesmo helper de `AppShell.tsx`) — se não tiver, ignorar silenciosamente as referências (não cria node/edge, não loga erro, o texto `{{kg-ref}}` fica só como texto bruto no corpo). O backend real (etapa 07) já faz o equivalente via `@RequireModule(ModuleKey.KNOWLEDGE_GRAPH)` no endpoint — aqui é a mesma garantia do lado do mock, para o comportamento não divergir quando o backend existir.

### C. UI de inserção — `EntityPicker` na toolbar do markdown

1. `MarkdownEditModal.tsx`: adicionar um novo `ToolbarAction` (`"kg-ref"`), com botão próprio na toolbar (ícone sugerido: `Network` ou `Link2` variante — escolher um que não colida visualmente com o botão "Link" já existente) e título "Vincular a outro conteúdo".
2. Ao clicar, abrir um modal pequeno (reaproveitar `ModalShell`, mesmo padrão já usado pelo próprio `MarkdownEditModal`) contendo `EntityPicker`. `EntityPicker` recebe o `productSlug` do conteúdo em edição e busca **só** entre os nós desse produto (regra de "mesmo produto", ADR-0016) — nunca lista nó de outro produto. Ao selecionar um resultado, inserir `{{kg-ref:${node.id}:${node.label}}}` na posição do cursor (reaproveitar a mesma mecânica de inserção de `applyToolbarAction`, parametrizada pelo nó escolhido em vez de um texto fixo) e fechar o modal.
3. Atualizar o texto de dica em `ContentArticleEditor.tsx:82-84` para refletir o caminho real: "Use o botão 'Vincular a outro conteúdo' na barra de ferramentas do editor de markdown para linkar este texto a outra entidade do Knowledge Graph." — a sintaxe `{{kg-ref}}` deixa de ser algo que o autor precisa digitar de cabeça; passa a ser um detalhe de implementação.
4. **O botão "Vincular a outro conteúdo" só aparece se o produto tiver o módulo `"Knowledge Graph"` habilitado** — mesmo gate já usado pela sidebar (`AppShell.tsx:92-94`, `resolveEnabledModules(effectiveProduct).includes("Knowledge Graph")`, Sprint 15 Tarefa B). `MarkdownEditModal`/`MarkdownField` não conhecem produto hoje — adicionar um prop opcional (ex.: `enableEntityLink?: boolean`) que `ContentArticleEditor` calcula a partir do produto efetivo do contexto (mesmo hook/contexto que `AppShell` já usa) e passa para baixo. Sem módulo habilitado: o botão não é renderizado (não é só desabilitado/cinza — não aparece), e nenhum texto de dica sobre `kg-ref` é mostrado, para não sugerir uma funcionalidade que o produto não tem. Outros usos de `MarkdownField`/`MarkdownEditModal` fora do domínio `content` (ex.: descrição de evento, FAQ) continuam sem esse botão por padrão (`enableEntityLink` default `false`).

### D. Confirmar reflexo na visualização

1. `GraphCanvasView.tsx`/`RelationshipExplorer.tsx`/`EntityDetails.tsx`: depois da Tarefa A, confirmar que essas telas (que já usam `useAsyncData(() => knowledgeService.listNodes(), [])` e equivalente) mostram a nova edge ao **navegar** para a tela depois de salvar um conteúdo com referência — não precisa de polling nem WebSocket, só o remonte natural da rota já buscar o store atualizado.
2. Se alguma dessas telas cachear o resultado de forma que sobreviva à navegação (ex.: estado global fora do componente), invalidar/refazer o fetch ao montar — não é esperado, mas vale confirmar.

## Critérios de aceite

- [ ] Criar um conteúdo novo (Post), escrever um texto, selecionar uma palavra e usar "Vincular a outro conteúdo" para linkar a outro conteúdo já existente do mesmo produto, salvar.
- [ ] Navegar para a tela do Knowledge Graph do produto e ver a nova edge `RELATED_TO` entre o node do novo conteúdo e o node referenciado — sem reload da página, sem ação manual extra.
- [ ] Editar o mesmo conteúdo de novo, salvar sem mudar as referências: a edge não duplica.
- [ ] Referenciar um `nodeId` inexistente não quebra o salvamento; console mostra aviso claro.
- [ ] `EntityPicker` dentro do modal de vínculo busca por label em tempo real (mesmo padrão de busca já usado em outras telas do app) e funciona sem o usuário saber nenhum ID de antemão.
- [ ] A dica de texto em `ContentArticleEditor.tsx` reflete o fluxo real (botão na toolbar), não mais a sintaxe bruta como única instrução.
- [ ] `listNodes()`/`listEdges()` refletem o mesmo store que `createEdge`/`ensureNodeForContent` escrevem — confirmado lendo o código, não só pela UI.
- [ ] Produto **sem** o módulo `"Knowledge Graph"` habilitado nunca mostra o botão "Vincular a outro conteúdo" na toolbar de markdown (testar pelo menos um produto com módulo desabilitado e um com habilitado, ex. `maestro-beton` vs `wikidev`/`loki` nos mocks de `core/auth/mocks/users.ts`).
- [ ] Salvar um conteúdo com `{{kg-ref}}` digitado manualmente num produto sem o módulo habilitado **não** cria node/edge — o gate vale tanto na UI de inserção quanto no parsing ao salvar (Tarefa B), não só no botão.
- [ ] `EntityPicker` aberto a partir de um conteúdo do produto WikiDev nunca lista um nó do produto Loki (ou vice-versa) — a busca é sempre restrita ao produto do conteúdo em edição.
- [ ] Salvar um conteúdo com `{{kg-ref:nodeId:Label}}` digitado manualmente apontando para um `nodeId` que existe, mas pertence a outro produto, **não** cria a edge — console mostra aviso claro de que a referência cruza produtos.

## Comandos de Git

```bash
git checkout develop && git pull origin develop
git checkout -b sprint/16-knowledge-graph-real

git commit -m "fix(knowledge): listNodes/listEdges leem do store mutavel unico, nao do mock original"
git commit -m "feat(content): parseia kg-ref ao salvar conteudo e cria node/edge real no grafo"
git commit -m "feat(markdown): botao 'vincular a outro conteudo' na toolbar, abre EntityPicker e insere kg-ref"
git commit -m "docs(content): corrige dica de uso do kg-ref no editor de conteudo"

git push -u origin sprint/16-knowledge-graph-real
```

Ao final, finalize a sprint no gitflow:

```bash
./scripts/gitflow-finish-sprint.sh sprint/16-knowledge-graph-real
```
