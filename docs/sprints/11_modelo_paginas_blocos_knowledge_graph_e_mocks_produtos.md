# Sprint 11 — Modelo de Páginas (Page/Section/Block), correção do Knowledge Graph e seleção real de módulos

> Pré-requisito: Sprint 09 (services + fluxo Super Admin) e Sprint 10 (refinamento de ações pendentes) concluídas. Esta sprint também define trabalho novo para a Sprint 02 (backend, via GPT) — ver Seção D — a ser executado como extensão, mesma estratégia de etapas pequenas.

## Contexto

Foram analisados 6 contratos de negócio/conteúdo de clientes reais que serão os primeiros produtos hospedados no Aegis: **Maestro Beton** (site pessoal de músico), **Conecta Talentos** (consultoria de RH/recrutamento), **CMSS** (site institucional da Corporação Musical São Sebastião), **Alexandre Dev** (portfólio pessoal), **Loki** (biblioteca filosófica anônima) e **WikiDev** (base de conhecimento técnico). A partir deles, simulei o fluxo de uso de cada um dentro do PMS hoje (criar produto → configurar módulos → popular conteúdo → publicar) e comparei com o que o backend planejado (`docs/sprints/sprint-02-fundacao-backend-gpt/`) e o frontend atual realmente suportam.

### O que a simulação confirmou que já funciona

- O mecanismo de **habilitar/desabilitar módulo por produto** existe na fundação do backend (`POST/.../modules/{moduleKey}/enable|disable`, etapa 06) — a ideia de módulo ligado a produto, não ao sistema todo, está correta.
- O **Knowledge Graph como módulo opcional** (`moduleKey: KNOWLEDGE_GRAPH`) já existe no catálogo e pode ser ligado/desligado por produto — o mecanismo de ativação está certo.
- O caso da **Loki** (poema ↔ música, manifesto ↔ playlist) é bem servido pelo Knowledge Graph atual: `MUSIC_REFERENCE` já é um `nodeType` do catálogo (etapa 07), `INSPIRED_BY`/`PART_OF` já são `edgeType`s do catálogo, e o seed (etapa 20) já modela exatamente essa relação. **Nenhuma mudança necessária para a Loki.**
- O caso da **Conecta Talentos** já tem `JOB_POSTING` e `CANDIDATE` previstos no catálogo de `nodeType` do grafo (etapa 07) — alguém já havia pensado nesse domínio antes, só falta a entidade de negócio em si (ver Seção D).

### Achado 1 — Não é possível selecionar módulos ao criar um produto hoje

`CreateProductForm.tsx` (linha 11) define `INITIAL_MODULES = ["Conteúdo", "Assets", "Forms", "Analytics", "SEO", "Workflow"]` — uma lista **fixa**, igual para qualquer produto, independente do tipo escolhido. Os campos "Tipo", "Idioma padrão" e "Template inicial" do formulário são `SelectLike` travados (`locked`), e os módulos aparecem como badges somente leitura, não como opções marcáveis. Ou seja: hoje, **nenhum produto novo nasce com Knowledge Graph habilitado**, nem com nenhuma seleção deliberada de módulos — todos nascem iguais, e a única forma de mudar isso é depois, na tela de catálogo de módulos (`ModuleCatalog.tsx`), cujos botões de habilitar/desabilitar estão entre os corrigidos pela Sprint 10.

Isso contradiz a pergunta central desta análise ("dá para criar um produto e selecionar suas funcionalidades?") — a resposta hoje é **não**.

### Achado 2 — O Knowledge Graph atual não atende ao caso WikiDev

A intuição estava correta. O diferencial que o documento de negócio da WikiDev define como **o motivo de existir da plataforma** ("Conhecimento Conectado") é especificamente isto: dentro do corpo de um artigo, palavras como "Spring Boot", "Docker", "JPA" são referências vivas — ao passar o mouse, aparece um popup com resumo, categoria, nível de dificuldade e um botão para abrir o conteúdo. Isso é diferente de "ter um grafo de nós e arestas" — é uma **experiência de leitura**, embutida no editor de conteúdo, que o grafo só serve como mecanismo de dados por trás.

Comparando com o que existe:

| O que a WikiDev precisa | O que o Aegis tem hoje | Gap |
|---|---|---|
| Referência inline dentro do corpo do artigo (`Spring Boot` sublinhado no meio do texto) | `GraphNode`/`GraphEdge` administrados via API explícita (POST/GET) — não há noção de "referência dentro do rich text" | O editor de conteúdo (`ContentEditor.tsx`) não tem um tipo de marca/inline-node para "isto é uma referência a outra entidade do grafo" |
| Popup leve ao passar o mouse (`ArticleLinkPreview`: `title`, `summary`, `difficulty`, `thumbnail`) | `GET /graph/nodes/{nodeId}` retorna o nó completo (`label`, `type`, `status`, `x`, `y`, `props[]`) — não tem `summary`/`difficulty`/`thumbnail` como campos garantidos, e é uma resposta "pesada" para um tooltip | Falta um endpoint de **preview leve**, e falta convenção de onde `summary`/`difficulty` vivem |
| "Relacionados" com peso/ordem (`RelatedContent.related[].weight`) | `GraphEdge.weight` já existe (etapa 07) | **Sem gap** — só falta o frontend consumir isso como lista ordenada, não é problema de modelo |
| Filtrar/exibir artigo por nível de dificuldade (`beginner`/`intermediate`/`advanced`) — atributo central para as personas da WikiDev (estudante vs. sênior) | `ContentRow` (contrato do domínio `content`) tem `title, type, lang, author, status, updatedAt, publication, version` — **não tem `difficulty` nem `summary`** | Campo ausente no modelo de conteúdo, não só no grafo |
| Criar a referência inline *enquanto escreve* (autor seleciona um trecho e linka a outro artigo) | Nenhum fluxo de autoria para isso existe — grafo só é populado via chamada de API direta | Falta o fluxo editorial completo: selecionar texto → buscar entidade → criar `GraphEdge` automaticamente |

Conclusão: o Knowledge Graph como módulo de **dados** (nós, arestas, vizinhos) está certo e é reaproveitável. O que falta é a camada de **experiência de leitura/autoria** que o WikiDev efetivamente vende. Ver Tarefa C.

### Achado 3 — Falta um modelo de Página feita de Seções/Blocos (gap maior que o do grafo)

Olhando os 4 contratos que são "sites institucionais/pessoais" (Maestro Beton, CMSS, Conecta Talentos — página inicial —, Alexandre Dev), todos compartilham o mesmo padrão, **documentado de forma quase idêntica em dois contratos diferentes** (Maestro Beton e a auditoria CMS-first da CMSS, escritas por pessoas/processos diferentes, convergindo no mesmo modelo): uma página é uma lista ordenada de seções tipadas (`hero`, `texto`, `imagem+texto`, `lista de cards`, `galeria`, `timeline`, `FAQ`, `contato`), cada uma com `content` e `settings` próprios.

O domínio `content` que a Sprint 02 (GPT, etapa 10) já especifica modela exatamente o oposto disso: um `Content` é um **artigo/documento plano** (`title`, `type`, `lang`, `status`, `version` — bom para post de blog, manifesto, reflexão, artigo da WikiDev) com workflow editorial. Isso é o modelo certo para conteúdo editorial linear, mas **não tem como representar uma `home.json` com hero + seções**.

Curiosamente, o frontend já "sabe" disso de forma desorganizada: `ContentEditor.tsx`/`EditorPanels.tsx` já tem uma lista hardcoded `INITIAL_BLOCKS = ["Hero", "Experiências", "Vídeo destaque", "Sobre", "Galeria", "Depoimentos", "CTA final", "SEO"]` e uma `<ContentStructureTree />` — ou seja, a UI já antecipa edição por blocos, mas **isso nunca foi formalizado em contrato/tipo nenhum**, nem no frontend (`contracts/responses.ts` não tem nada disso) nem no backend.

Isso é o achado de maior alcance desta análise: sem um domínio `Page`/`Section`/`Block`, **4 dos 6 produtos analisados não têm como ser modelados de verdade no Aegis** — eles ficariam presos a "um Content gigante" ou exigiriam gambiarra. Ver Tarefa B.

### Achado 4 — Domínios de negócio específicos ainda não cobertos

- **Conecta Talentos**: precisa de `JobPosting` (vaga: cidade, modalidade, tipo, salário, requisitos, benefícios, diferenciais, status) e `CandidateSubmission` (candidatura: dados do candidato + currículo anexado + referência à vaga) e `Lead` (contato comercial de empresa). O domínio genérico `forms`/`submission` (etapa 12) não cobre bem isso: uma vaga não é um formulário preenchido pelo usuário, é conteúdo estruturado publicado pela empresa; uma candidatura tem upload de arquivo (currículo) amarrado a uma vaga específica, o que o `Submission` genérico (`answersJson` solto) não modela com a clareza necessária.
- **WikiDev**: precisa de `Comment` (comentários em artigo, com moderação `pending/approved/rejected`), `ContributorApplication`, `ContentSuggestion`, `BugReport` — todos com integração de notificação via Telegram. Os module keys `COMMENTS` e `CONTRIBUTORS` **já existem no enum do catálogo de módulos** (etapa 06) mas **nenhuma etapa implementa as entidades correspondentes** — é um módulo "fantasma", declarado mas vazio.
- **Loki**: os domínios (`Manifesto`, `Reflection`, `Poem`, `Book`, `Playlist`, `MusicReference`) são, na prática, tipos de `Content` com campos extras — não precisam de domínio novo, só de `type` adicionais no catálogo de tipos de conteúdo e o relacionamento com o grafo já mencionado no Achado 1.

## Objetivo

1. Permitir selecionar módulos de verdade na criação de um produto, com um conjunto padrão sensato por tipo de produto (e sem inflar produtos com módulos que eles não usam — caso do Knowledge Graph).
2. Modelar `Page`/`Section`/`Block`, fechando o gap que afeta a maioria dos produtos analisados.
3. Estender o Knowledge Graph com o que a WikiDev realmente precisa: preview leve por referência, campos de dificuldade/resumo, e o fluxo de autoria de referência inline.
4. Registrar (não necessariamente implementar agora) os domínios de negócio específicos que faltam, como extensão futura da Sprint 02.
5. Criar mocks fiéis aos 6 contratos analisados, incluindo os 3 produtos que ainda não existem em nenhum mock (CMSS, Alexandre Dev, Loki).

## Tarefas

### A. Seleção real de módulos na criação de produto

1. Em `CreateProductForm.tsx`, substituir `INITIAL_MODULES` fixo por uma lista de checkboxes reais, pré-marcada com o conjunto padrão do tipo de produto escolhido (Tarefa A.2), mas editável antes de confirmar a criação.
2. Definir a tabela de módulos padrão por tipo de produto (mover para `core/products`, reaproveitável pelo backend quando a etapa 06 da Sprint 02 for revisitada):

   | Tipo de produto | Módulos padrão | Knowledge Graph |
   |---|---|---|
   | Site Institucional (Maestro Beton, CMSS, Aegis Labs Site) | Páginas, Conteúdo, Assets, Forms, SEO, Analytics | **Desligado por padrão** — nenhum dos dois contratos pede cross-referência de conteúdo |
   | Portal (Conecta Talentos, Portal Norte) | Páginas, Conteúdo, Vagas¹, Submissions, Forms, SEO, Analytics | Desligado por padrão (opcional para o blog) |
   | Knowledge Base (WikiDev, Aegis Docs) | Conteúdo, Knowledge Graph, Comentários¹, Contribuidores¹, SEO, Analytics | **Ligado por padrão** — é o módulo central do produto |
   | Portfolio (Alexandre Dev) | Portfolio, Páginas, Conteúdo, Assets, SEO, Analytics | Desligado por padrão, ligável manualmente (projetos/artigos relacionados é "nice to have", não é central) |
   | Library/Books/Music (Loki) | Library, Books, Music, Conteúdo, Knowledge Graph, SEO, Analytics | **Ligado por padrão** — é o módulo do relacionamento obra↔música |
   | Produto SaaS / Design System (Genesis, Eirene UI, Aegis Core) | conforme já modelado hoje | sem alteração |

   ¹ Módulos ainda não implementados no backend — ver Tarefa D. Até lá, aparecem na seleção marcados como "em breve"/desabilitados.

3. Corrigir a etapa 20 da Sprint 02 (GPT) — `docs/sprints/sprint-02-fundacao-backend-gpt/20_seed_inicial_e_grafo.md` — para that o seed de `maestro-beton` e dos produtos do tipo "Site Institucional" **não** habilite `KNOWLEDGE_GRAPH` (estava inconsistente com a tabela acima); manter `KNOWLEDGE_GRAPH` apenas em `wikidev` e `loki`.
4. Atualizar `ModuleCatalog.tsx`/mock correspondente para refletir a dependência já sugerida no mock atual (Knowledge Graph com status "dependência") — formalizar a regra: **Knowledge Graph exige o módulo Conteúdo habilitado** (não faz sentido ligar grafo sem conteúdo para conectar).

### B. Domínio `Page` / `Section` / `Block` (novo)

1. Frontend: criar `frontend/src/domains/pages/` (novo domínio, paralelo a `content`) com:
   - `contracts/responses.ts`: `Page = { id, slug, title, locale, status: "draft"|"review"|"published"|"archived", version, seo: PageSeo, sections: Section[] }`, `Section = { id, type: BlockType, variant?, order, content: Record<string, unknown>, settings?: Record<string, unknown> }`.
   - Catálogo fechado de `BlockType` — reaproveitando o levantamento já feito para a CMSS (que é o mais completo dos 6 contratos): `hero`, `text`, `rich-text`, `two-column`, `image`, `image-text`, `feature-grid`, `card-list`, `gallery`, `timeline`, `event-list`, `cta-section`, `faq`, `contact`, `footer`, `navbar`.
   - `services/pagesService.ts` seguindo o mesmo padrão de `tenantsService`/`usersService` (Sprint 09).
   - Página de edição: reaproveitar `ContentEditor.tsx`/`EditorPanels.tsx`/`ContentStructureTree` já existentes, mas trocando a lista hardcoded `INITIAL_BLOCKS` por dados reais vindos de `pagesService`, com cada bloco do `ContentStructureTree` correspondendo a uma `Section` real, não a uma string solta.
2. Backend — **já criado nesta sessão** como etapa `21_dominio_pages_secoes_e_blocos.md` da Sprint 02 (GPT), pronta para colar amanhã junto das demais: `Page`/`PageSection` com o mesmo catálogo de `BlockType` acima, endpoints:
   ```txt
   GET  /api/v1/products/{productId}/pages
   GET  /api/v1/products/{productId}/pages/{pageId}
   PUT  /api/v1/products/{productId}/pages/{pageId}
   POST /api/v1/products/{productId}/pages/{pageId}/sections
   PUT  /api/v1/products/{productId}/pages/{pageId}/sections/{sectionId}
   DELETE /api/v1/products/{productId}/pages/{pageId}/sections/{sectionId}
   PUT  /api/v1/products/{productId}/pages/{pageId}/sections/reorder   body: { sectionIds: string[] }
   ```
   Regra: `content` de cada `Section` é validado contra um schema por `BlockType` (ex.: `hero` exige `title`+`image.alt`; `gallery` exige `items[].alt`) — espelhando as regras já descritas na auditoria da CMSS (`rules: { titleRequired, imageAltRequired, maxItems, ... }`).
3. Este domínio **não substitui** `content` (etapa 10) — os dois convivem: `content` continua sendo o artigo/post/manifesto/reflexão (conteúdo editorial linear, com workflow Draft→Review→Published→Archived); `pages` é a composição de seções de uma página institucional. Um `Section` do tipo `event-list`/`card-list` pode referenciar itens de `content` ou de outro domínio (ex.: vagas, eventos) por `source: { type: "contentType", contentType: "...", filter: {...} }`, igual ao `EventListBlock` já desenhado na auditoria da CMSS.

### C. Extensão do Knowledge Graph para referência inline (caso WikiDev)

1. Backend — **já adicionado nesta sessão à etapa `17_knowledge_graph_extras_layout_e_orphans.md`** (Seção D daquele arquivo), endpoint leve distinto do `GET /graph/nodes/{nodeId}` já existente:
   ```txt
   GET /api/v1/products/{productId}/graph/nodes/{nodeId}/preview
   ```
   ```ts
   type GraphNodePreview = { id: string; label: string; type: string; summary: string; difficulty?: "beginner"|"intermediate"|"advanced"; thumbnail?: string };
   ```
   Implementação: pode ler de `metadataJson`/`props` do nó, ou do `Content` associado quando o nó referenciar um (ver item 2) — não precisa de tabela nova —, mas a resposta tem forma fixa e leve, pensada para um tooltip, não para a tela de detalhe completa.
2. Backend — **já adicionado nesta sessão à etapa `10_dominio_content.md`**: dois campos opcionais no `Content`: `summary: string` e `difficultyLevel: "beginner"|"intermediate"|"advanced"` — campos de primeira classe, não escondidos em metadata, porque a WikiDev precisa filtrar/ordenar por eles (não é só metadado decorativo).
3. Frontend: no editor de conteúdo (`ContentEditor.tsx`), adicionar suporte a uma marca inline de rich text `{ type: "kg-ref", nodeId, label }` — ao autor selecionar um trecho de texto e escolher "Linkar a outra entidade" (busca por `EntitySearch.tsx`, já existente no domínio `knowledge`), o editor: (a) insere a marca no corpo do texto, (b) chama `POST /graph/edges` criando automaticamente uma aresta `RELATED_TO` entre o conteúdo atual e o nó referenciado, fechando o ciclo "escrevi uma referência → o grafo já sabe disso", em vez de exigir que o autor crie a aresta manualmente em outra tela.
4. Frontend: ao renderizar o corpo de um artigo publicado, qualquer marca `kg-ref` deve, ao passar o mouse, chamar `GET .../preview` (cacheável) e mostrar um popover leve — esse é o componente que hoje não existe em nenhuma tela de `domains/knowledge` (confirmado na simulação: todas as telas atuais do grafo são painel cheio ou navegação de página, nunca tooltip).
5. `relatedTopics`/`RelatedContent` (lista de relacionados com peso) **não precisa de endpoint novo** — já é exatamente `GET /graph/nodes/{nodeId}/related`, ordenado por `GraphEdge.weight` (já existe desde a etapa 07). Só formalizar isso no frontend (`RelationshipExplorer.tsx`/painel de "conteúdos relacionados" na página do artigo).

### D. Registro de domínios de negócio específicos (não implementar agora — registrar para extensão futura da Sprint 02)

Adicionar ao `docs/trace/00_endpoints_esperados.md` (Seção B, nova subseção B.10) as entidades abaixo, para serem implementadas como etapas **23-25** da Sprint 02 (GPT) numa sessão futura — não fazem parte do critério de aceite desta sprint, só precisam estar registradas:

- **23 — `JobPosting`/`CandidateSubmission`/`Lead`** (Conecta Talentos): vaga como conteúdo estruturado publicável (workflow próprio Draft→Review→Published→Archived, igual artigos), candidatura com upload de currículo (reaproveita o domínio `asset` para o arquivo, com `targetType: "candidate_submission"`), lead comercial simples.
- **24 — `Comment`/`ContributorApplication`/`ContentSuggestion`/`BugReport` + integração Telegram** (WikiDev): fecha os module keys `COMMENTS`/`CONTRIBUTORS` que já existem no enum mas estão vazios.
- **25 — Tipos de conteúdo adicionais para Loki**: `manifesto`, `reflection`, `poem`, `book`, `playlist` como `type` válidos dentro do domínio `content` já existente (não são entidades novas, são variações de `Content` com campos extras — `Book` precisa de `isbn`/`pdfUrl`/`epubUrl`/`amazonUrl`/`physicalAvailable`, modelável como `metadataJson` específico do tipo).

### E. Mocks dos 6 produtos

1. **Já feito nesta sessão**: os três produtos que ainda não existiam foram adicionados em `frontend/src/core/auth/mocks/users.ts` (`mockProductsByUser`) e em `frontend/src/domains/products/mocks/products.mocks.ts`, no tenant `BYOP` (`t1`), junto dos demais — `CMSS` (Site Institucional), `Alexandre Dev` (Portfolio), `Loki` (Biblioteca Filosófica). Falta o trabalho mais profundo do item 2 abaixo: popular o conteúdo real de cada um (hoje eles existem como produto selecionável, mas sem páginas/conteúdo próprio).
2. Para os 6 produtos (os 3 novos + Maestro Beton, Conecta Talentos e WikiDev, que já existem mas com mock genérico), popular `domains/content/mocks`, `domains/pages/mocks` (novo, depende da Tarefa B existir) e, onde aplicável, `domains/knowledge/mocks`, com conteúdo **fiel aos contratos enviados** — não inventar dados genéricos:
   - **Maestro Beton**: página Home com seções `hero` (artista em primeiro plano, CTA "Solicitar orçamento"), `card-list` de serviços (Casamentos, Eventos corporativos, Shows), `gallery`, `event-list` (agenda), `contact` com o formulário rico de orçamento (tipo de evento, data, cidade, convidados).
   - **Conecta Talentos**: Home com hero "Conectando Pessoas e Empresas com Propósito", mock de 3-4 `JobPosting` (mesmo que a entidade ainda não exista no backend — mock no frontend já no formato definido na Tarefa D), Quem Somos, Blog com 2-3 artigos.
   - **CMSS**: páginas Home/Quem Somos/História/Agenda/Apoie/Contato usando o catálogo de blocks da Tarefa B (esta é a fonte mais rica e já praticamente pronta para virar mock, dado o nível de detalhe do levantamento).
   - **Alexandre Dev**: Hero com métricas (12 projetos, 10 anos de experiência), 3-5 `projects` (Aion Logbook, Aegis CMS, Eirene UI, Pomodoro App, Genesis Lab), skills por categoria, experiências.
   - **Loki**: 2-3 manifestos, 2-3 reflexões, 2-3 poemas (ao menos um com `musicReferenceId` preenchido), 1 livro, 1 playlist — e os nós/arestas do Knowledge Graph já descritos no seed (Tarefa A não muda isso, já está correto).
   - **WikiDev**: estrutura de categoria→tópico→artigo (Programação→Java→Spring Boot/JPA), com ao menos um artigo tendo `summary`+`difficultyLevel` preenchidos e uma referência inline (`kg-ref`) de exemplo no corpo do texto, para servir de prova de conceito da Tarefa C.

## Critérios de aceite

- [ ] Criar um produto novo permite marcar/desmarcar módulos antes de confirmar, com defaults por tipo de produto (tabela da Tarefa A.2).
- [ ] Knowledge Graph não vem mais habilitado por padrão em produtos do tipo Site Institucional/Portfolio.
- [ ] Domínio `pages` existe no frontend (contratos + service + integração no editor) e a etapa 21 da Sprint 02 (GPT) já existe pronta para ser executada amanhã.
- [ ] `GET .../graph/nodes/{nodeId}/preview` existe e retorna o shape leve `GraphNodePreview`.
- [ ] `Content` tem `summary`/`difficultyLevel` opcionais.
- [ ] Existe ao menos um exemplo funcional de referência inline (`kg-ref`) com popover ao passar o mouse, no mock da WikiDev.
- [x] Os 6 produtos existem como produto selecionável nos mocks do frontend (`mockProductsByUser`/`products.mocks.ts`) — já feito nesta sessão.
- [ ] Cada um dos 6 produtos tem conteúdo (páginas/seções/artigos) fiel ao seu contrato, não genérico — pendente, depende da Tarefa B existir em código.
- [ ] `docs/trace/00_endpoints_esperados.md` tem a subseção B.10 com os domínios registrados para extensão futura (Tarefa D).

## Comandos de Git

```bash
git checkout develop && git pull origin develop
git checkout -b sprint/11-paginas-blocos-knowledge-graph-mocks

git commit -m "feat(products): selecao real de modulos na criacao, com defaults por tipo de produto"
git commit -m "feat(pages): novo dominio page/section/block com catalogo de blocks"
git commit -m "feat(knowledge): preview leve de no, summary/difficulty no content e referencia inline kg-ref"
git commit -m "docs(trace): registra dominios pendentes (rh, comentarios/contribuidores, tipos loki)"
git commit -m "feat(mocks): popula os 6 produtos (maestro-beton, conecta-talentos, cmss, alexandre-dev, loki, wikidev) com conteudo fiel aos contratos"

git push -u origin sprint/11-paginas-blocos-knowledge-graph-mocks
```

Ao final, finalize a sprint no gitflow:

```bash
./scripts/gitflow-finish-sprint.sh sprint/11-paginas-blocos-knowledge-graph-mocks
```
