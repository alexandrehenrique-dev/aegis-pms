/**
 * Conteúdo da Central de Ajuda (Sprint 13, Tarefa O) — fonte: comportamento
 * real de cada tela e os documentos de sprint/ADR revisados nesta sessão.
 * Mantido como mock no frontend, sem endpoint de backend, mesmo padrão dos
 * demais domínios. Duas seções: `PLATFORM_OVERVIEW_TOPICS` (visão geral
 * sempre visível, sem depender de módulos habilitados — explica toda a
 * plataforma) e `MODULE_FAQ_TOPICS` (perguntas frequentes por módulo,
 * escopadas pelos módulos habilitados no produto atual).
 */

export type HelpOverviewTopic = { title: string; description: string };
export type HelpFaqItem = { question: string; answer: string };

export const PLATFORM_OVERVIEW_TOPICS: HelpOverviewTopic[] = [
  {
    title: "O que é o Aegis PMS",
    description:
      "Um Product Operating System: a plataforma onde o BYOP cria e administra todos os produtos digitais (sites institucionais, portais, bases de conhecimento, portfólios, produtos SaaS...). Todo conteúdo público de um produto é editável aqui — não há texto final hardcoded no frontend de cada produto (ADR-0003, CMS First).",
  },
  {
    title: "Tenants e Produtos",
    description:
      "Um tenant é uma organização (ex.: BYOP, um cliente). Dentro de um tenant, cada produto digital tem tipo (Site Institucional, Portal, Knowledge Base, Portfolio, Library/Books/Music, Produto SaaS), módulos habilitados, idioma e uma estratégia de armazenamento de assets (local — padrão — ou bucket S3 externo, escolhida na criação). Super Admin gerencia tenants; Tenant Admin e Product Manager operam produtos dentro do seu tenant.",
  },
  {
    title: "Papéis e permissões",
    description:
      "Cinco papéis: Super Admin (acesso total), Tenant Admin (administra tenant e equipe), Product Manager (opera produtos e módulos), Editor (cria e revisa conteúdo) e Viewer (somente leitura). Cada papel vê apenas a navegação e os widgets relevantes ao seu escopo — rotas fora do escopo são bloqueadas, e dashboards escondem ou restringem ações que o papel atual não pode executar. No menu do seu avatar (canto superior direito), \"Simular perfil\" deixa visualizar a plataforma como outro papel sem trocar de conta.",
  },
  {
    title: "Dashboards",
    description:
      "O Dashboard Global mostra a visão operacional de todos os produtos do tenant. Cada módulo (Conteúdo, Forms, Analytics, Knowledge Graph) tem seu próprio dashboard com KPIs e atividade recente, e cada produto tem um cockpit próprio com saúde, pendências e módulos habilitados.",
  },
  {
    title: "Conteúdo (domínio editorial)",
    description:
      "Para texto corrido com workflow de aprovação (Draft → In Review → Published → Archived) — artigos, posts, manifestos. Aceita markdown em qualquer campo de texto longo e referências inline (kg-ref) que linkam um trecho a outra entidade do Knowledge Graph, com preview ao passar o mouse. Use Conteúdo quando o material é lido de forma linear, não composto visualmente em seções.",
  },
  {
    title: "Páginas (domínio institucional)",
    description:
      "Para sites compostos visualmente por seções e blocos (hero, texto, imagem, galeria, áudio, redes sociais, agenda de eventos, formulário embutido...), navegados por URL própria. O editor permite reordenar seções e sub-blocos por arrastar, editar texto via markdown numa modal dedicada, e selecionar imagens/arquivos por um picker de assets real — nunca como texto puro.",
  },
  {
    title: "Entidades globais: navbar, footer e redes sociais",
    description:
      "Configuradas uma única vez por produto (em Páginas → \"Navbar, footer e redes sociais\"), não como bloco de uma página específica. Qualquer página do produto renderiza esse navbar/footer automaticamente no topo e no final.",
  },
  {
    title: "Assets",
    description:
      "Biblioteca de arquivos do produto (imagens, PDFs, áudio, vídeo) com tags, busca e rastreamento de uso. Qualquer campo de mídia no editor de blocos abre o picker de assets em vez de pedir um nome de arquivo digitado. O local de armazenamento real (disco local ou S3) é resolvido pelo backend — o frontend só fala com o picker e o endpoint de upload/resolve.",
  },
  {
    title: "Forms",
    description:
      "Formulários com campos configuráveis (tipo, validação, máscara, condição de exibição), submissões com qualificação de leads, e publicação por múltiplos canais (URL, embed, script, iframe). A entrega de respostas pode ser configurada para e-mail, WhatsApp, Telegram e/ou webhook simultaneamente.",
  },
  {
    title: "Analytics",
    description: "Métricas de tráfego, conversão e tendências do produto, com relatórios e painéis de insight por período.",
  },
  {
    title: "Knowledge Graph",
    description:
      "Mapa de entidades (conteúdo, páginas, assets, formulários) e relações entre elas — alimentado principalmente pelas referências kg-ref inseridas em textos. Permite encontrar entidades órfãs (sem relação) e explorar como diferentes peças de conteúdo se conectam.",
  },
  {
    title: "Auditoria",
    description: "Trilha de eventos críticos do tenant (mudanças de permissão, exclusões, publicações) — visível para Super Admin e Tenant Admin.",
  },
  {
    title: "Configurações",
    description: "Dados gerais do produto/tenant, branding, SEO padrão, segurança, equipe e papéis — escopado por permissão (algumas seções são exclusivas de Tenant Admin/Super Admin).",
  },
];

export const MODULE_FAQ_TOPICS: Record<string, HelpFaqItem[]> = {
  Páginas: [
    { question: "Como adiciono um bloco a uma página?", answer: "No editor da página, escolha o tipo no seletor do painel Estrutura e clique em \"Adicionar bloco\". O bloco aparece no fim da lista e já pode ser editado no painel central." },
    { question: "Como reordeno seções ou sub-blocos?", answer: "Arraste pelo ícone de grip (⠿) à esquerda de cada seção, ou de cada sub-bloco dentro de um bloco como two-column." },
    { question: "Como escrevo texto com formatação (negrito, lista, link)?", answer: "Clique em \"Editar texto\" abaixo do campo — abre uma modal com toolbar e pré-visualização lado a lado, em markdown." },
    { question: "Como troco a imagem de um bloco?", answer: "Clique em \"Selecionar imagem\" (ou \"Trocar\", se já houver uma) para abrir o picker de assets filtrado por imagem; o texto alternativo (alt) é sugerido a partir do nome do arquivo, mas pode ser editado." },
    { question: "Onde edito navbar, footer e redes sociais?", answer: "Em Páginas → botão \"Navbar, footer e redes sociais\" — é uma configuração única por produto, não um bloco de página." },
  ],
  Conteúdo: [
    { question: "Qual a diferença entre Conteúdo e Páginas?", answer: "Conteúdo é para texto corrido com workflow editorial (artigos, posts); Páginas é para sites institucionais compostos visualmente em seções. Um produto pode ter os dois." },
    { question: "O que é uma referência kg-ref?", answer: "Uma marca inline que linka um trecho do texto a outra entidade do Knowledge Graph — aparece com preview ao passar o mouse na versão publicada." },
    { question: "Como movo um conteúdo entre etapas do workflow?", answer: "Pelo quadro de Workflow (arraste o card entre colunas Draft/In Review/Published/Archived) — cada transição pode exigir comentário ou papel mínimo." },
    { question: "Texto de artigo aceita markdown?", answer: "Sim — o corpo de um artigo aceita markdown junto com as referências kg-ref, ambos no mesmo campo." },
  ],
  Assets: [
    { question: "Quais tipos de arquivo posso enviar?", answer: "Imagens, PDFs, áudio e vídeo. Ao usar um asset num campo específico do editor (ex.: imagem de um bloco), o picker já filtra pelo tipo esperado." },
    { question: "Como uso um asset já existente num bloco?", answer: "Clique no botão de seleção do campo de mídia — o picker abre com busca e filtro por tipo, sem precisar digitar nome de arquivo." },
    { question: "Onde os arquivos ficam armazenados?", answer: "Depende da estratégia escolhida na criação do produto: local (padrão, pasta própria no servidor) ou bucket S3 externo. O frontend não precisa saber qual — sempre resolve a URL pelo mesmo endpoint." },
  ],
  Forms: [
    { question: "Como configuro para onde vão as respostas?", answer: "Em Publicação do formulário, seção \"Receber respostas por\" — marque um ou mais canais (e-mail, WhatsApp, Telegram, webhook); cada um expande os campos necessários ao ser marcado." },
    { question: "Como publico um formulário no site?", answer: "Em Publicação, copie o embed/script/iframe e clique em \"Publicar alterações\"." },
    { question: "Como qualifico ou atribuo uma submissão?", answer: "Na lista de submissões, marque como qualificado ou atribua a um responsável da equipe." },
  ],
  SEO: [
    { question: "Onde configuro o SEO de uma página?", answer: "Na aba SEO do editor da página (painel Propriedades) — título, descrição e palavras-chave específicos daquela página." },
  ],
  Analytics: [
    { question: "O que o Analytics mostra?", answer: "Tráfego, conversão e tendências do produto, com relatórios por período e painéis de insight." },
  ],
  "Knowledge Graph": [
    { question: "O que é uma entidade órfã?", answer: "Uma entidade do grafo sem nenhuma relação com outra — aparece destacada para limpeza ou vinculação." },
    { question: "Como crio uma relação entre dois conteúdos?", answer: "Inserindo uma referência kg-ref no corpo de um texto (linkando a outra entidade) ou pelo explorador de relações do grafo." },
  ],
  Workflow: [
    { question: "O que o módulo Workflow controla?", answer: "O fluxo de aprovação de conteúdo (Draft → In Review → Published → Archived), com regras de quem pode mover cada transição." },
  ],
};
