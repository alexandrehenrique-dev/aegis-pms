# ADR-0016 — Knowledge Graph: relação entre conteúdos, não entre componentes de página

## Status

ACCEPTED

## Contexto

Uma auditoria de uso real (não só de código) encontrou que o Knowledge Graph, embora modelado corretamente no backend (etapas 07/17) e renderizado corretamente no frontend (`GraphCanvasView.tsx`, `RelationshipExplorer.tsx`, `EntityDetails.tsx`), **não tem nenhum caminho de escrita funcional**: `knowledgeService.createEdge`/`ensureNodeForContent` existem mas nunca são chamadas por nenhum fluxo real de autoria; a única forma "documentada" de criar uma referência é digitar manualmente a sintaxe `{{kg-ref:nodeId:Label}}` sabendo de cabeça o ID interno de outro nó — o que não é uma jornada usável. O resultado percebido é exatamente o relatado: "uma tela bonita com mock que nunca se altera".

Isso expôs uma ambiguidade de escopo que nunca tinha sido escrita explicitamente em nenhum ADR: o Knowledge Graph existe para modelar **relações entre conteúdos** (um artigo cita/referencia outro, uma música inspira um poema, um node de tópico conecta vários artigos) — **nunca** para modelar a composição interna de uma página (`Page` → `PageSection`, já resolvido por ADR-0009/ADR-0013 e pelo próprio modelo de `pages`). Sem essa distinção escrita, ficou ambíguo se o "produto certo" do grafo seria visualizar a estrutura de blocos de uma página ou as conexões de conteúdo soltas entre si — e a falta de uma jornada de autoria clara é consequência direta dessa ambiguidade nunca ter sido resolvida.

## Decisão

O Knowledge Graph modela exclusivamente **relações entre entidades de conteúdo/conhecimento** (`Content`, `Page` como referência única — não suas seções —, `Asset`, `Form`, `JobPosting`, `Book`, `Poem`, `Manifesto`, `MusicReference`, etc. — o catálogo de `nodeType` já existente na etapa 07), **nunca** a relação entre os componentes/seções de uma página (isso é `PageSection.order` dentro de uma `Page`, já resolvido pelo domínio `pages`).

A jornada de autoria real e primária é **sempre a partir do conteúdo**, nunca do canvas do grafo:

1. Usuário está escrevendo um `Content` (post, artigo, manifesto...) em `ContentArticleEditor.tsx`/`MarkdownEditModal.tsx`.
2. Seleciona uma palavra/trecho e aciona "Vincular a outro conteúdo" (toolbar) — abre `EntityPicker.tsx` (componente já existente, hoje desconectado), que busca por label entre os nós existentes do produto.
3. Ao escolher um resultado, o editor insere a marca inline `{{kg-ref:nodeId:Label}}` no corpo — nunca exige que o autor saiba ou digite o `nodeId` de cabeça.
4. Ao salvar o conteúdo, o sistema garante o node de origem (o próprio conteúdo) e cria a(s) edge(s) `RELATED_TO` para cada referência encontrada no corpo — automaticamente, sem ação extra do autor.

`GraphCanvasView` continua existindo como **visualização** dessas conexões (e como ferramenta de curadoria — resolver órfãos, revisar insights), não como ferramenta primária de criação manual de nó/edge. Desenhar uma edge arrastando no canvas **não é** a jornada suportada — se vier a ser necessária no futuro, é uma decisão nova, não implícita nesta.

**Uma edge só pode conectar dois nós do mesmo produto.** Cada nó do grafo pertence a exatamente um produto (o nó é criado a partir de um `Content` que existe dentro de um produto específico — não existe `Content` "global" cross-produto). `EntityPicker.tsx`, ao buscar entidade para vincular durante a autoria, busca exclusivamente entre os nós do produto do conteúdo em edição — nunca lista nem permite selecionar um nó de outro produto. Pela mesma razão, `knowledgeService.createEdge` rejeita (não cria, loga aviso) qualquer tentativa de ligar dois nós de produtos diferentes — inclusive quando a referência `{{kg-ref:nodeId:Label}}` foi digitada manualmente apontando para um `nodeId` de outro produto. Esta restrição não tinha sido escrita explicitamente até a implementação da Sprint 16 — formalizada aqui porque o Knowledge Graph nunca existiu, em nenhum produto real do catálogo (Maestro Beton, CMSS, WikiDev, Loki, Conecta Talentos, Alexandre Dev), como um grafo compartilhado entre tenants/produtos distintos.

## Consequências

Positivas:
- Resolve a ambiguidade de escopo que vinha causando a sensação de "ferramenta bonita mas vazia" — o grafo finalmente reflete ações reais de autoria, validável sem precisar do backend (a Sprint 16 resolve isso ainda em modo mock, com o store em memória de fato mutando).
- Reaproveita componentes que já existem e estão corretos (`EntityPicker.tsx`, `KgRefMark.tsx`, `ArticleBody.tsx`) — o gap era só a ausência de fiação entre eles, não a falta de peças.
- Mantém o grafo desacoplado da estrutura de blocos de página — uma página pode mudar de seções livremente sem nunca precisar tocar no grafo.

Negativas / trade-offs:
- Conteúdo que vive dentro de uma `Page` (ex.: texto de uma seção `text`/`rich-text`) não participa do grafo da mesma forma que um `Content` — se um produto institucional (ex.: Maestro Beton) quiser referências cruzadas dentro do texto de uma seção, isso fica fora do escopo desta decisão e exigiria extensão futura deliberada.
- A jornada exclusivamente "a partir do conteúdo" significa que não há, por design, uma forma rápida de criar uma relação entre dois nós que não tenham `Content` algum (ex.: ligar manualmente duas `Asset` entre si) — aceitável por ora, pois nenhum produto atual (Maestro Beton, CMSS, WikiDev, Loki, Conecta Talentos, Alexandre Dev) precisa disso.

## Alternativas Consideradas

- **UI de desenho manual no canvas (arrastar uma linha entre dois nós)**: rejeitada como jornada primária — exige que o usuário já saiba quais dois nós quer conectar e por quê, fora do contexto onde essa decisão realmente nasce (durante a escrita). Pode ser reavaliada no futuro como ferramenta de curadoria complementar, não substituta.
- **Modelar relações entre seções de página no mesmo grafo**: rejeitado — contraria ADR-0009/ADR-0013 e duplicaria responsabilidade já resolvida por `PageSection.order`/`acceptsChildren` (etapa 21).

## Impactos

- **Frontend**: Sprint 16 (`docs/sprints/16_knowledge_graph_real_conexao_durante_autoria.md`) — fiar `EntityPicker` no editor de conteúdo, parsear `kg-ref` ao salvar e chamar `createEdge`/`ensureNodeForContent` de fato, corrigir `knowledgeService.listNodes()`/`listEdges()` para ler do mesmo store mutável (`allNodes`/`allEdges`), não do array de mock estático original. `ensureNodeForContent`/`createEdge`/`searchNodes` passam a exigir/aceitar `productSlug` para que a busca do `EntityPicker` e a criação de edge nunca atravessem produtos.
- **Backend**: etapa 07 (`GET /graph/nodes` ganha busca por `q`/label) e etapa 17 (endpoints de `orphans`/`insights` que já eram chamados pelo frontend mock sem estar documentados) — ver atualização desta sessão.
- **Documentação**: `docs/trace/00_endpoints_esperados.md` atualizado para refletir os dois pontos acima antes do backend ser construído.

## Links Relacionados

- ADR-0009 (SPA servido pelo Spring Boot), ADR-0012 (tipos de conteúdo), ADR-0013 (entidades globais por produto).
- `docs/sprints/sprint-02-fundacao-backend-gpt/07_knowledge_graph_mvp.md` (Seção C.1), `17_knowledge_graph_extras_layout_e_orphans.md`.
- `docs/sprints/16_knowledge_graph_real_conexao_durante_autoria.md`.
