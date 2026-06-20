# Sprint 13 — Engine de blocos genérica, entidades globais, mídia/markdown e responsividade

> Pré-requisito: Sprint 12 concluída (ou em execução simultânea — algumas tarefas aqui dependem de componentes que a Sprint 12 cria, como `ItemsCrudEditor`/Tarefa D e `eventsService`/Tarefa E daquela sprint; ver referências cruzadas em cada tarefa). Esta sprint é densa de propósito — várias decisões de modelo foram resolvidas durante a análise (não deixadas em aberto) para o agente executor não precisar interpretar nem reinventar. Onde uma decisão já foi tomada, ela está escrita como fato, não como pergunta.

## Contexto

Sessão de teste manual + revisão de código identificou um padrão recorrente: o editor de páginas (Sprint 11) tem a estrutura certa (seções tipadas), mas vários blocos/fluxos foram implementados de forma mais simples do que o necessário — sem sub-blocos genéricos, sem picker de mídia real, sem markdown, sem drag-and-drop (apesar da lib já instalada), e dois conceitos que deveriam ser entidades globais (navbar, footer) foram modelados como blocos de página comuns. Paralelamente, uma auditoria de permissões encontrou um padrão sistemático de dashboards que não respeitam o papel do usuário.

### Decisão 1 — Separação de tipos de conteúdo (não é mais uma pergunta aberta)

O Aegis passa a reconhecer formalmente **três famílias de conteúdo**, cada uma com seu próprio domínio e finalidade — não tentar forçar tudo em "Content" ou tudo em "Page":

| Família | Domínio | Quando usar | Exemplos |
|---|---|---|---|
| **Page** (institucional) | `domains/pages` | Site composto por seções/blocos visuais, navegado por URL própria | Home, Sobre, Serviços, Agenda do Maestro Beton; Home/Quem Somos/História da CMSS |
| **Content** (editorial) | `domains/content` | Texto corrido com workflow (Draft→Review→Published), pensado para ser lido e referenciado, não composto em seções visuais | Artigos da WikiDev, manifestos/reflexões/poemas da Loki, posts do blog da Conecta Talentos |
| **Tipos futuros registrados** (ainda não implementados, ver `docs/trace/00_endpoints_esperados.md` Seção B.10) | domínios próprios quando chegar a hora | Conteúdo transacional/estruturado que não é nem página nem artigo | `JobPosting` (Conecta Talentos), `Book`/`Playlist` (Loki, hoje modelados como `type` extra de Content — aceitável por ora) |

Um produto pode ter os dois primeiros simultaneamente (ex.: BYOP tem páginas institucionais E pode ter um blog). A regra de decisão: **se o conteúdo é navegado como uma URL própria e composto visualmente em seções, é Page; se é lido de forma linear e tem workflow editorial de aprovação, é Content.**

### Decisão 2 — Entidades globais: Navbar, Footer e Redes Sociais não são blocos de página

Hoje `navbar` e `footer` estão no catálogo de `BlockType` (`domains/pages/contracts/responses.ts`, linhas 6-23) como mais um tipo de seção — ou seja, cada página teoricamente poderia ter seu próprio navbar/footer, e editar o navbar significa editar um bloco dentro de uma página específica. **Isso está errado** e contradiz o próprio contrato global já usado nos exemplos (`global.json` do Maestro Beton, `cms-metadata.json` da CMSS) — navbar, footer e redes sociais são **uma única configuração por produto**, editada uma vez, refletida em todas as páginas.

Decisão: criar uma entidade `ProductGlobals` (por produto, não por página): `{ navbar: { logo, links[] }, footer: { address, social[], links[] }, floatingWhatsapp?, socialLinks[] }`. Remover `navbar`/`footer` do catálogo de `BlockType`. Ver Tarefa G.

### Decisão 3 — Modelo de blocos: motor genérico de sub-blocos, não caso especial do `two-column`

Hoje só `two-column` tem alguma noção de aninhamento (e mesmo assim, sub-objetos fixos, não sub-blocos tipados de verdade — corrigido na Sprint 12, Tarefa C). A decisão para esta sprint: **qualquer bloco pode declarar que aceita filhos**, via uma propriedade `acceptsChildren: BlockType[]` no catálogo — não é exclusividade do `two-column`. Isso vira um motor genérico (`SubBlockEditor`), reaproveitável por qualquer bloco atual ou futuro que precise de composição (ex.: um futuro bloco `tabs`, ou `accordion`). Ver Tarefa E.

### Decisão 4 — Modelo de permissões: dashboards devem respeitar papel por widget, não só por rota

Confirmado: `RequireRole` (`app/guards/RequireRole.tsx`) só bloqueia a **rota inteira**. Dentro da rota, nenhum dashboard verifica papel por widget — `DashboardGlobal.tsx` (linha 48), `EditorialDashboard.tsx` (linhas 44-52), `FormsDashboard.tsx` (linhas 17-24), `AnalyticsOverview.tsx` (linhas 17-24), `ProductDashboard.tsx` (linhas 32-43) e `KnowledgeOverview.tsx` (linhas 27-35) renderizam todos os widgets para qualquer papel que tenha acesso à rota — por isso o Editor vê o card "Produtos ativos" no Dashboard Global mesmo sem poder criar produtos. `PermGate` (`app/guards/PermGate.tsx`) já existe e resolve isso, mas só é usado num lugar (botão "Criar produto"). Decisão: todo widget que expõe uma ação ou dado fora do escopo de `roleVisibleNav`/`roleBlockedRoutePrefixes` do papel atual deve estar dentro de um `PermGate`. Ver Tarefa N.

### Decisão 5 — Engine para novos produtos: catálogo de blocos do backend + e-commerce como módulo registrado

Para o Aegis suportar produtos futuros sem reescrever o editor a cada vez: o catálogo de `BlockType` deixa de ser só uma constante do frontend e passa a ser **consultado de um service** (`pagesService.listBlockTypes()`), que por enquanto retorna a constante atual como mock — mas a partir de agora todo código novo consulta o service, nunca a constante direto. Isso prepara a troca para um catálogo vindo do backend (que pode variar por tipo de produto/plano) sem exigir outro refactor. E-commerce (carrinho, checkout, pagamento) não é implementado agora, mas o module key `ECOMMERCE` é registrado no catálogo de módulos (hoje vazio/desabilitado) para já existir como conceito quando for a hora — ver Tarefa Q.

### Decisão 6 — Estratégia de armazenamento de assets escolhida na criação do produto

Markdown em todo lugar e blocos de mídia (Decisões/Tarefas B-D) significam mais e mais variados arquivos por produto (PDF, áudio, imagem) — isso tem impacto direto no backend, já refletido em `docs/sprints/sprint-02-fundacao-backend-gpt/11_dominio_assets.md` (Seção D, atualizada nesta mesma sessão): cada produto escolhe, na criação, entre armazenamento **local** (default — o backend cria a estrutura de pastas do produto por categoria de arquivo) ou **S3/bucket externo**. A resolução de qualquer asset por outro domínio é sempre por `assetId` (UUID); quem quiser a URL chama `GET /api/v1/assets/{assetId}/resolve`, que decide internamente se busca em disco local ou gera uma URL pré-assinada do S3 — nenhum service além do `asset` precisa saber qual provider está em uso. A pasta raiz de storage (onde as pastas por produto/categoria vivem) é criada automaticamente — em Java multiplataforma (etapa 04) para quem roda sem Docker, e via volume nomeado do Docker Compose (etapa 19) para quem roda containerizado — nunca uma etapa manual de "criar a pasta no servidor". Isso é 100% backend, já resolvido nos arquivos da Sprint 02; o frontend só precisa do passo do wizard (Tarefa H.3) e nada mais.

## Objetivo

Implementar as 5 decisões acima e fechar os gaps de mídia, markdown, drag-and-drop, formulário, ajuda e responsividade encontrados na sessão de teste — com detalhe suficiente para o agente executor não precisar tomar decisões de modelo por conta própria.

## Tarefas

### A. ADRs das decisões de modelo

Antes de qualquer código, registrar as Decisões 1-3 e 5 acima como ADRs (regra do projeto: toda decisão de arquitetura tem ADR em `docs/adr/`). Próximos números disponíveis: `ADR-0012`, `ADR-0013` (`docs/adr/` vai até `ADR-0011` hoje).

- **ADR-0012 — Separação de tipos de conteúdo (Page vs Content vs futuros)**: conteúdo da Decisão 1, no formato já usado pelas ADRs existentes (Status/Contexto/Decisão/Consequências/Alternativas Consideradas/Impactos/Links Relacionados — seguir `ADR-0003-cms-first.md` como modelo).
- **ADR-0013 — Entidades globais por produto (Navbar/Footer/Redes Sociais)**: conteúdo da Decisão 2, linkando ADR-0003 (CMS First) e ADR-0008 (JSON Contracts, que já lista `Navigation` como contrato próprio — esta ADR formaliza isso).
- Decisões 3 e 5 não precisam de ADR própria — são detalhadas o suficiente nas Tarefas E e H abaixo; mencionar em uma nota no rodapé do PR, não em ADR dedicada (não inflar o número de ADRs para decisões de implementação, só para decisões de arquitetura de fato).

### B. Markdown em todo conteúdo de texto + modal dedicada de edição

1. Adicionar `react-markdown` (renderização) e um editor leve (ex.: campo de texto com toolbar básica — negrito, itálico, lista, link — gerando a sintaxe markdown; não é necessário um WYSIWYG completo). Hoje **não existe nenhuma biblioteca de markdown** no `package.json` nem parsing de `**bold**`/listas/links em nenhum editor — todo texto é string plana.
2. Qualquer campo de texto longo (`body` de `text`/`rich-text`/`two-column`/`image-text`, `description` de qualquer bloco) passa a aceitar markdown, renderizado via `react-markdown` no preview/produção.
3. Criar uma **modal dedicada de edição de markdown** (`shared/components/MarkdownEditModal.tsx`): em vez de editar o texto inline no painel lateral (hoje um `<textarea>` simples), um botão "Editar texto" abre essa modal com a toolbar e uma área maior de edição — usar como referência o padrão de modal já existente no app (ex.: o modal "Novo Pomodoro" do Aion Logbook, que tem título, campos claros, separação visual de seções e ações primária/secundária bem distintas no rodapé — manter esse padrão visual para consistência entre produtos).
4. **Dropdown de tipo de conteúdo por campo**: em campos que hoje são só texto, adicionar um seletor "Este campo aceita: Texto / Imagem / PDF / Áudio / Link" — ao escolher "Texto", aparece o botão que abre a `MarkdownEditModal` (criar, editar ou remover o markdown); ao escolher "Imagem"/"PDF"/"Áudio", aparece o seletor de asset (Tarefa C); ao escolher "Link", um campo de URL simples. Essa é a base para um bloco de "conteúdo flexível" reutilizável — não precisa existir em todo bloco hoje, mas o componente (`FlexibleContentField`) deve existir pronto para qualquer bloco novo usar.

### C. Picker de assets real para qualquer campo de mídia

1. Hoje campos de imagem (`src`/`alt` em `hero`, `image`, `gallery`, `image-text`) são editados como texto puro (`EditorPanels.tsx`, linhas 70-76) — o componente `AssetPicker.tsx` (`domains/assets/pages/AssetPicker.tsx`) já existe mas **não está conectado ao editor de blocos**.
2. Trocar todo campo de imagem do editor de blocos por um botão "Selecionar imagem" que abre `AssetPicker` (ou uma variante modal dele) — ao escolher um asset, preenche `src` (e sugere `alt` a partir do nome do arquivo, mas deixa editável). Mesmo padrão para remover (limpar seleção) e trocar (reabrir o picker).
3. Generalizar `AssetPicker` para aceitar um filtro de tipo (`imagens`, `pdf`, `audio`, `qualquer`) — usado pelo `FlexibleContentField` da Tarefa B.4 e pelo bloco de música da Tarefa D.

### D. Bloco de música/áudio e bloco de redes sociais

Confirmado: **não existe hoje nenhum bloco de música/áudio em nenhum lugar do catálogo**, apesar de ser requisito explícito do contrato da Loki (player de música vinculado a poemas/manifestos, playlists) e implícito no Maestro Beton (apresentações musicais, vídeos). Redes sociais hoje só existem como array de strings dentro do mock do `footer` (`pages.mocks.ts`, linha 362), sem bloco dedicado.

1. Novo `BlockType: "audio"`: `content: { title?, source: "upload" | "spotify-track" | "spotify-playlist", fileAssetId?, spotifyUrl?, autoplay: boolean }`. Quando `source: "upload"`, usa o picker de assets (Tarefa C, filtro áudio) para anexar um arquivo de música avulso. Quando `spotify-track`/`spotify-playlist`, um campo de URL do Spotify, renderizado como embed. `autoplay` controla se a faixa toca automaticamente ao abrir a página ou só quando o visitante clicar — default `false` (autoplay agressivo é ruim para UX e alguns navegadores bloqueiam mesmo se ligado; deixar a decisão visível e por bloco, não global).
2. Novo `BlockType: "social-links"`: `content: { items: [{ platform: string, href: string }] } }` — usa o `ItemsCrudEditor` (Sprint 12, Tarefa D) para adicionar/remover/reordenar redes sociais. Pode ser usado tanto dentro de `ProductGlobals` (footer, Tarefa G) quanto como bloco solto numa página, se o produto quiser destacar redes sociais em outro lugar além do footer.

### E. Motor genérico de sub-blocos (Decisão 3) + CRUD completo

1. No catálogo de `BlockType` (`domains/pages/contracts/responses.ts`), cada entrada ganha um campo `acceptsChildren?: BlockType[]` — hoje só `two-column` teria isso preenchido (`["text", "rich-text", "image", "cta"]`, conforme já alinhado na Sprint 12, Tarefa C), mas o mecanismo não é exclusivo dele.
2. Criar `domains/pages/components/SubBlockEditor.tsx`: dado um bloco com `acceptsChildren` preenchido, renderiza a lista de filhos (cada um com seu próprio mini-editor, igual ao bloco-pai usaria), com "+ Adicionar bloco" (restrito à allowlist), reordenar, e remover — ou seja, qualquer bloco com filhos ganha as mesmas 4 operações (criar, editar, remover, reordenar) que um bloco de topo tem.
3. Este motor é o que o `two-column` (Sprint 12) deve usar — não implementar uma solução específica para `two-column` e depois generalizar; implementar o motor genérico direto e o `two-column` é só o primeiro consumidor dele.

### F. Drag-and-drop real para reordenar seções/blocos

1. `react-dnd`/`react-dnd-html5-backend` já estão instalados (`package.json`) e em uso em `WorkflowBoard.tsx` (domínio `content`) — mas o editor de seções de página (`ContentStructureTree`, em `EditorPanels.tsx` linhas 15-42) reordena hoje só pela chamada a `pagesService.reorderSections()`, sem nenhuma interação de arrastar visível (a Sprint 11/12 entregaram a reordenação "de trás para frente": o service existe, a UI de arrastar não).
2. Adicionar `useDrag`/`useDrop` (mesmo padrão de `WFCard.tsx`/`WFLane.tsx`) ao `ContentStructureTree`, permitindo arrastar uma seção para nova posição na lista — ao soltar, chama `pagesService.reorderSections()` com a nova ordem completa. Mesmo padrão aplicado dentro do `SubBlockEditor` (Tarefa E) para reordenar sub-blocos.

### G. Entidades globais por produto: Navbar, Footer, Redes Sociais (Decisão 2)

1. Criar `domains/pages/services/globalsService.ts` (ou em `core/products/`, já que é por produto, não por página): `getGlobals(productSlug)`, `updateGlobals(productSlug, req)`. Shape: `ProductGlobals = { navbar: { logoAssetId?, links: NavLink[] }, footer: { addressText?, links: NavLink[] }, socialLinks: SocialLink[], floatingWhatsapp?: { enabled, number, message } }`.
2. Remover `"navbar"` e `"footer"` do array `BLOCK_TYPES` (`domains/pages/contracts/responses.ts`) e dos mocks de página que os usam como seção (`pages.mocks.ts`, linhas ~359-362 e ~74).
3. Nova tela `domains/pages/pages/GlobalsSettings.tsx`, rota `/products/:productSlug/globals` (ou dentro de Configurações do produto) — edita navbar/footer/redes sociais uma vez, reaproveitando `ItemsCrudEditor` (Sprint 12) para os links/redes sociais.
4. O `BlockRenderer`/preview (Sprint 12, Tarefa F) passa a sempre renderizar `ProductGlobals.navbar` no topo e `.footer` no final de qualquer página, automaticamente — a página em si nunca mais inclui navbar/footer nas suas próprias seções.

### H. Catálogo de blocos vindo de um service (não mais constante direta) + módulo `ECOMMERCE` registrado + step de storage no wizard de produto

1. Criar `pagesService.listBlockTypes()` retornando a constante `BLOCK_TYPES` atual (mock, mesmo conteúdo de hoje) — todo lugar do editor que hoje importa `BLOCK_TYPES` direto de `contracts/responses.ts` passa a chamar esse método do service. Mantém o comportamento idêntico hoje, mas abre o caminho para o catálogo vir do backend/variar por produto sem outro refactor.
2. **Já feito nesta sessão** (`docs/sprints/sprint-02-fundacao-backend-gpt/06_modelo_core_tenant_product_modulos.md`, enum de `moduleKey`): `ECOMMERCE` adicionado à lista — desabilitado/não implementado, só registrado para existir como conceito (carrinho, checkout, pagamento ficam para uma sprint futura dedicada).
3. **Novo passo no wizard de criação de produto** (Sprint 09, `domains/products/pages/CreateProductForm.tsx`): depois da seleção de módulos (Sprint 11, Tarefa A), adicionar um passo "Armazenamento de assets" com duas opções em cards — **Local** (recomendado/já selecionado por padrão: "Os arquivos do produto ficam guardados no próprio servidor") e **Bucket externo (S3)** (“Para quem já usa ou vai usar um provedor de nuvem próprio — exige configuração adicional”). Ao escolher S3, mostrar campos opcionais de bucket/região (podem ficar em branco e ser configurados depois em Configurações do produto — não bloquear a criação do produto por falta dessa config). O valor escolhido vai no `POST /products` como `assetStorageStrategy` (já especificado em `docs/sprints/sprint-02-fundacao-backend-gpt/06_modelo_core_tenant_product_modulos.md` e `11_dominio_assets.md`, Seção D — backend já atualizado e pronto para receber esse campo).
4. Em qualquer tela que envolva upload (Tarefa C, picker de assets), nenhuma mudança é necessária no frontend para a escolha local/S3 — o frontend sempre fala com `POST /assets` e `GET /assets/{assetId}/resolve` (já especificado na etapa 11) e recebe de volta uma `url` pronta para uso, sem precisar saber qual provider está por trás.

### I. Eventos: lista selecionável ao adicionar o bloco + nota sobre calendário global

1. O bloco `event-list` hoje só referencia uma fonte (`source: { contentType: "evento", filter }`) sem nenhuma UI. A Sprint 12 (Tarefa E) já propõe um `eventsService` com CRUD — esta sprint complementa: ao adicionar um bloco `event-list` numa página, em vez de só "Gerenciar eventos" (criar do zero), mostrar primeiro uma **lista dos eventos já existentes no produto** (de qualquer página/fonte), com checkbox para selecionar quais aparecem nesse bloco específico — cobre o caso de um evento já cadastrado aparecer em mais de um lugar (ex.: agenda da Home E agenda dedicada).
2. **Calendário global**: registrar como ideia para avaliação futura (não implementar agora) — um calendário que cruza eventos de todos os produtos de um tenant, útil para o Super Admin ver toda a operação. Antes de implementar, revisitar os contratos (Maestro Beton tem regra de privacidade para eventos `private` — Seção 12 do contrato) para garantir que um calendário cross-produto não vaze dados de um evento privado de um produto para a visão de outro.

### J. Entrega de formulário configurável (múltiplos canais)

1. Hoje, submissions de formulário só geram notificação interna (toast) — não existe nenhum lugar para configurar "para onde mandar" (`PublicationPanel.tsx` só tem opções de publicação do formulário em si — embed/script/iframe —, não de destino das respostas; `formsService` não tem método de delivery; `core/notifications` só tem `toast.ts`).
2. Criar, na configuração do formulário (`FormBuilder` ou um painel "Notificações" dedicado), uma seção "Receber respostas por": checkboxes múltiplos — E-mail, WhatsApp, Telegram, Webhook — cada opção, quando marcada, expande os campos necessários (E-mail: endereço; WhatsApp: número; Telegram: chat ID/token do bot; Webhook: URL + método). Pode marcar mais de um canal simultaneamente.
3. Shape: `FormDelivery = { channels: Array<{ type: "email"|"whatsapp"|"telegram"|"webhook"; config: Record<string,string>; enabled: boolean }> }`, persistido junto com a definição do formulário. A entrega de fato (disparar o webhook, enviar o WhatsApp) é trabalho de backend, fora de escopo do frontend nesta sprint — aqui só a **configuração** precisa existir e ser salva corretamente.

### K. Preview: validar com mocks variados

A Sprint 12 (Tarefa F) já reescreve o preview para ler o conteúdo real via `BlockRenderer`. Esta sprint adiciona uma validação: depois daquela correção, testar o preview com pelo menos 3 produtos diferentes (Maestro Beton, WikiDev, um produto novo criado durante o teste) para confirmar que ele varia de fato conforme o conteúdo e não está só trocando uma constante por outra constante igualmente fixa.

### L. Simular e ajustar a jornada completa: criar um produto do zero

Percorrer manualmente (e ajustar o que travar): Super Admin cria um tenant → cria um produto → no wizard de criação, seleciona módulos (Sprint 11, Tarefa A) → entra no produto → cria as páginas (Home, Sobre, Contato) usando blocos → faz upload de assets e os usa nas páginas (via picker da Tarefa C) → habilita Knowledge Graph e cria pelo menos um nó/aresta ligando conteúdo de duas páginas diferentes (ou um artigo e uma página). Qualquer travamento nessa jornada (ex.: falta de uma tela, botão sem ação, dado que não persiste entre etapas) deve ser corrigido como parte desta tarefa — documentar o que foi encontrado e corrigido num resumo no PR.

### M. Simular a jornada da WikiDev (orientada a conteúdo+links, não páginas)

A WikiDev não deveria usar o domínio `pages` para seu conteúdo principal (artigos técnicos) — conforme a Decisão 1, ela é puramente `Content` com referências inline (`kg-ref`, já implementado na Sprint 11). Percorrer manualmente: criar um artigo (domínio `content`, não `pages`) → escrever um texto que referencia outro artigo via `kg-ref` (selecionar trecho → linkar a outra entidade) → publicar → verificar que a referência aparece com preview ao passar o mouse (Sprint 11) e que a aresta criada usa `edgeType: "RELATED_TO"` (corrigido na Sprint 12, Tarefa H). Se a WikiDev hoje tiver páginas (`pages`) sendo usadas para algo que deveria ser artigo, migrar/ajustar.

### N. Auditoria de permissões: dashboards respeitando papel por widget (Decisão 4)

1. Envolver com `PermGate` (ou ocultar diretamente) qualquer widget que exponha dado/ação fora do escopo do papel atual, nos 5 arquivos confirmados: `DashboardGlobal.tsx` (linha 48, widget "Produtos ativos" — Editor/Viewer não deveriam ver contagem/link de produtos se `/products` não fizer parte do fluxo deles, **decisão de produto a confirmar com o usuário durante a execução**: ver nota abaixo), `EditorialDashboard.tsx` (linhas 44-52), `FormsDashboard.tsx` (linhas 17-24), `AnalyticsOverview.tsx` (linhas 17-24), `ProductDashboard.tsx` (linhas 32-43), `KnowledgeOverview.tsx` (linhas 27-35).
2. **Nota importante para o agente executor**: `roleVisibleNav.editor` já inclui `/products` (Editor pode navegar para a lista de produtos) — o que está errado não é o Editor ver produtos, é o Dashboard Global mostrar um **card de contagem/atalho de produtos** quando o foco do Editor é conteúdo, não gestão de produto. Antes de remover o widget, confirmar com o usuário se a intenção é (a) esconder o widget para Editor/Viewer, ou (b) manter o widget mas remover a ação de "criar" dele (que já está coberta por `PermGate`) — não assumir a interpretação mais agressiva sem confirmar, já que é uma decisão de produto, não só técnica.
3. Documentar a regra geral resultante em `core/permissions/roles.ts` como comentário: "widgets de dashboard que exibem dados/ações fora de `roleVisibleNav` do papel atual devem estar em `PermGate`", para próximos dashboards seguirem o padrão desde o início.

### O. Central de Ajuda/FAQ em accordion, escopada por módulos habilitados

1. Hoje o botão "?" do `AppShell.tsx` (linha 127) abre `FeedbackModal` (reportar bug/sugestão) — isso não é uma central de ajuda, é um canal de feedback. **Não existe nenhuma rota de ajuda/FAQ hoje.**
2. Mover a ação de "Reportar problema" para outro lugar acessível (ex.: dentro do próprio menu de ajuda, como uma opção "Reportar um problema" no rodapé da nova central — não removida, só reposicionada) e trocar a ação principal do botão "?" para abrir a nova **Central de Ajuda**.
3. Nova tela `core/help/pages/HelpCenter.tsx`, rota `/help` — lista tópicos em accordion (cada item é um `<details>`/componente de collapse: clicar expande o clicado e recolhe os demais — comportamento de "um aberto por vez", não múltiplos simultâneos). Conteúdo gerado a partir da documentação e da implementação real: para cada **módulo habilitado no produto atual** (Conteúdo, Páginas, Assets, Forms, Analytics, Knowledge Graph, Auditoria, Configurações...), um tópico com 3-6 perguntas frequentes sobre como usar aquele módulo especificamente — não uma lista genérica de FAQ, e não incluir tópicos de módulos que o produto atual não tem habilitado.
4. Fonte de conteúdo: revisar `docs/sprints/` (09-13), `docs/adr/`, e o comportamento real de cada tela para escrever as perguntas/respostas — não inventar funcionalidade que não existe, e não deixar de documentar algo que existe. Estrutura de dados sugerida: `helpTopics: Record<ModuleKey, { question: string; answer: string }[]>`, mantida como mock no frontend por enquanto (mesmo padrão dos demais domínios — sem endpoint de backend para isso nesta sprint).

### P. Auditoria responsiva ampla (não só os componentes já reportados)

Confirmado um novo caso: o dropdown de notificações (sino, no `AppShell.tsx`) estende além da largura da viewport em mobile, sobrepondo as abas da tela por trás (visto em teste real, print anexado na conversa). Isso indica que o problema não é isolado aos componentes já corrigidos nas Sprints 09-12 — é preciso uma varredura sistemática:

1. Percorrer toda tela/overlay (dropdowns, modals, drawers, popovers) listada em `shared/components/ui/` e nos componentes específicos de cada domínio, em viewport mobile (375px de largura, o padrão usado nos prints já compartilhados) — qualquer elemento que ultrapasse a borda da tela, fique sobreposto de forma ilegível, ou corte texto/botões sem scroll é um bug a corrigir (ex.: aplicar `max-width: calc(100vw - 1rem)`, `overflow-y: auto` com altura máxima, ou reposicionar para abrir centralizado em vez de ancorado a um botão pequeno, dependendo do caso).
2. Componentes já confirmados com problema até agora (consolidado, não repetir investigação): dropdown de notificações (`AppShell.tsx`), menu de filtros/ações em `TenantSelectScreen`/`ProductSelectScreen` (Sprint 12, Tarefa I), badges dentro do slot de botões de `PageHeader` (Sprint 12, Tarefa J). Adicionar a esta lista qualquer outro encontrado durante a varredura desta tarefa.
3. Não é necessário (nem possível, sem acesso a ferramentas de captura de tela do ambiente do usuário) gerar prints automaticamente — em vez disso, o agente executor deve abrir cada tela relevante em modo responsivo (devtools do navegador, 375px) e listar os problemas encontrados antes de corrigir, como checklist no PR.

### Q. Atualizar documentação das sprints do backend para alinhar com os ajustes desta sprint

**Já feito nesta sessão** (o usuário ia executar a Sprint 02 via GPT na mesma tarde — os arquivos já saíram atualizados, não ficou como tarefa para o agente do frontend):

1. Etapa 06 (`06_modelo_core_tenant_product_modulos.md`): `ECOMMERCE` adicionado ao enum de `moduleKey`; campo `Product.assetStorageStrategy` (`"local"|"s3"`, default `"local"`) adicionado, com a regra de que a criação do produto provisiona a pasta local quando aplicável.
2. Etapa 10 (`10_dominio_content.md`): convenção de markdown + regra de sanitização para `ContentVersion.snapshotJson`.
3. Etapa 11 (`11_dominio_assets.md`): reescrita com a Seção D completa — Strategy pattern (`LocalStorageProvider`/`S3StorageProvider`), endpoint universal `GET /assets/{assetId}/resolve`, variáveis de ambiente S3, regra de não-migração automática ao trocar de estratégia, tabela de riscos mapeados e melhorias futuras (CDN, thumbnails, ferramenta de migração).
4. Etapa 12 (`12_dominio_forms.md`): `FormDefinition.deliveryChannelsJson` + endpoint `PUT .../forms/{formId}/delivery` (Tarefa J) e `acceptedFileTypes` no campo `Upload`.
5. Etapa 21 (`21_dominio_pages_secoes_e_blocos.md`): `navbar`/`footer` removidos do catálogo de `BlockType`; `audio`/`social-links` adicionados; `acceptsChildren` generalizado (não mais hardcoded para `two-column`); regra de markdown/sanitização; nova Seção F com a entidade `ProductGlobals` e seus endpoints (`GET/PUT /products/{productId}/globals`).
6. Etapa 01 (`01_estrutura_repo_e_env.md`): variáveis de ambiente do S3 adicionadas (opcionais); comentário esclarecendo que a pasta de storage é sempre criada automaticamente.
7. Etapa 04 (`04_backend_base_e_persistencia.md`): nova Seção D — bootstrap automático da pasta raiz de storage local via `Files.createDirectories` (Java NIO, multiplataforma: Linux/macOS/Windows), com falha explícita na subida se não conseguir criar/escrever.
8. Etapa 19 (`19_dockerfile_e_compose_completo.md`): volume Docker nomeado `aegis_assets_data` (persistente, criado automaticamente no primeiro `docker compose up`), complementando o bootstrap da etapa 04 para quem roda sem Docker.
9. Etapa 22 (`22_openapi_testes_e_checklist_final.md`): checklist e cenários de teste atualizados para cobrir tudo acima.
10. `docs/trace/00_endpoints_esperados.md`: nova seção com os endpoints de `ProductGlobals`, `FormDelivery` e `GET /assets/{assetId}/resolve`.

Se o agente do frontend, ao executar esta sprint, encontrar mais algum ponto de divergência entre o que foi implementado em código e o que está nesses arquivos, atualizar os arquivos de novo — eles são a fonte da verdade para a sessão de backend, não o contrário.

## Critérios de aceite

- [ ] ADR-0012 e ADR-0013 existem e documentam as Decisões 1 e 2.
- [ ] Texto longo em qualquer bloco aceita markdown, com modal dedicada de edição.
- [ ] Campos de mídia abrem o picker de assets real, não texto puro; picker filtra por tipo (imagem/pdf/áudio).
- [ ] Existe bloco de áudio (upload ou Spotify, com opção de autoplay) e bloco de redes sociais.
- [ ] Qualquer bloco pode declarar `acceptsChildren` e ganhar CRUD de sub-blocos via `SubBlockEditor` — não é mais exclusividade do `two-column`.
- [ ] Reordenar seções e sub-blocos funciona por drag-and-drop, não só por chamada de service sem UI.
- [ ] Navbar/Footer/Redes Sociais são editados uma vez por produto (`/products/:slug/globals`), não mais como bloco de página; toda página renderiza esses globais automaticamente.
- [ ] Catálogo de blocos é consultado via `pagesService.listBlockTypes()`, não importado direto da constante.
- [ ] `ECOMMERCE` está registrado no enum de módulos (desabilitado).
- [ ] Adicionar um bloco `event-list` mostra eventos já existentes para selecionar, não só criação do zero.
- [ ] Formulário tem configuração de entrega por múltiplos canais (e-mail/WhatsApp/Telegram/webhook), salvando corretamente.
- [ ] Jornada completa de criação de produto (Tarefa L) percorrida sem travar; qualquer ajuste feito está documentado no PR.
- [ ] Jornada da WikiDev (Tarefa M) usa `content`+`kg-ref`, não `pages`, e a aresta criada usa `edgeType` do catálogo fechado.
- [ ] Os 5 dashboards (Tarefa N) só mostram widgets compatíveis com o papel atual; decisão sobre o widget de produtos do Dashboard Global foi confirmada com o usuário antes de implementar.
- [ ] Botão "?" abre uma Central de Ajuda em accordion (um aberto por vez), com tópicos escopados pelos módulos habilitados do produto atual — "Reportar problema" continua acessível, só reposicionado.
- [ ] Varredura responsiva ampla concluída, com checklist de problemas encontrados e corrigidos no PR.
- [ ] Documentação da Sprint 02 (GPT) e do trace report atualizada conforme Tarefa Q.

## Comandos de Git

```bash
git checkout develop && git pull origin develop
git checkout -b sprint/13-engine-blocos-entidades-globais

git commit -m "docs(adr): adiciona adr-0012 (tipos de conteudo) e adr-0013 (entidades globais)"
git commit -m "feat(shared): suporte a markdown com modal dedicada e campo de conteudo flexivel"
git commit -m "feat(pages): conecta asset picker real aos campos de midia do editor de blocos"
git commit -m "feat(pages): blocos de audio e redes sociais"
git commit -m "feat(pages): motor generico de sub-blocos (subblockeditor) com crud completo"
git commit -m "feat(pages): drag-and-drop real para reordenar secoes e sub-blocos"
git commit -m "feat(pages): entidades globais por produto (navbar, footer, redes sociais)"
git commit -m "feat(pages): catalogo de blocos via service; registra modulo ecommerce"
git commit -m "feat(pages): selecao de eventos existentes ao adicionar bloco de agenda"
git commit -m "feat(forms): configuracao de entrega por multiplos canais"
git commit -m "fix(permissions): widgets de dashboard respeitam papel via permgate"
git commit -m "feat(help): central de ajuda em accordion escopada por modulos habilitados"
git commit -m "fix(responsive): correcoes de overlay/dropdown/modal em mobile"
git commit -m "docs(sprints): atualiza sprint 02 gpt e trace report com ajustes desta sprint"

git push -u origin sprint/13-engine-blocos-entidades-globais
```

Ao final, finalize a sprint no gitflow:

```bash
./scripts/gitflow-finish-sprint.sh sprint/13-engine-blocos-entidades-globais
```
