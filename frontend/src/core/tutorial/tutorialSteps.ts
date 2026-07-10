import type { Step } from "react-joyride";
import type { UserRole } from "../../shared/types";

/**
 * Gate de visibilidade de um step (Sprint 22, ajuste pós-revisão do
 * humano): o tour não pode oferecer uma ação que o papel atual não pode
 * executar (ex.: "criar produto" para um Product Manager) nem apontar
 * para um painel de um módulo que o produto selecionado não tem
 * habilitado (ex.: editor de blocos sem o módulo "Páginas"). As listas de
 * papel espelham a fonte de verdade de permissão já usada pelo resto do
 * app (`core/permissions/roles.ts` — `roleBlockedRoutePrefixes`/
 * `roleActions`), não um novo critério inventado para o tutorial.
 */
export type TutorialStepGate = {
  /** Se ausente, o step é visível para qualquer papel. */
  roles?: UserRole[];
  /** Chave de `resolveEnabledModules` (`core/products/moduleDefaults.ts`) exigida no produto selecionado. */
  requiredModule?: string;
  /**
   * Rota para navegar antes de mostrar este step — os alvos do tour vivem
   * em 5 telas diferentes (dashboard/produtos/conteúdo/usuários/settings),
   * e o react-joyride não navega sozinho. Ausente = fica na rota atual
   * (usado pelos steps `target: "body"` e pela sidebar, sempre presente).
   */
  route?: string;
};

export type TutorialStep = Step & { data?: TutorialStepGate };

/** Só quem pode de fato criar produto (`roleActions.tenant_admin`/ADR-0018 — product_manager tem `/products/new` bloqueado em `roleBlockedRoutePrefixes`). */
const CAN_CREATE_PRODUCT: UserRole[] = ["super_admin", "tenant_admin"];
/** `/users` e `/users/invite` bloqueados para product_manager/editor/viewer em `roleBlockedRoutePrefixes`. */
const CAN_MANAGE_USERS: UserRole[] = ["super_admin", "tenant_admin"];
/** `/settings` inteiro bloqueado para editor/viewer em `roleBlockedRoutePrefixes`. */
const CAN_SEE_SETTINGS: UserRole[] = ["super_admin", "tenant_admin", "product_manager"];
/** `/settings/tenant` (zona de perigo) também bloqueado para product_manager. */
const CAN_SEE_DANGER_ZONE: UserRole[] = ["super_admin", "tenant_admin"];
/** `/content` bloqueado para super_admin (ADR-0018, LGPD — não acessa conteúdo de produtos de clientes). */
const CAN_SEE_CONTENT: UserRole[] = ["product_manager", "editor", "viewer"];
/** Editor de blocos: `/pages/*\/editor` bloqueado para viewer, e a rota `/pages` inteira bloqueada para super_admin. */
const CAN_EDIT_CONTENT_BLOCKS: UserRole[] = ["product_manager", "editor"];

/**
 * Textos gerados por IA e calibrados na Sprint 22
 * (docs/sprints/frontend/22_tutorial_de_onboarding_interativo.md, Seção G)
 * — não alterar tom/conteúdo sem revisão. `disableBeacon` do v2 do
 * react-joyride não existe mais no v3 instalado (`skipBeacon`); o mesmo
 * efeito (abrir o tooltip direto, sem o beacon pulsante) é aplicado
 * globalmente via `options.skipBeacon` no `TutorialProvider`.
 */
export const TUTORIAL_STEPS: TutorialStep[] = [
  // ─── 1. Boas-vindas ──────────────────────────────────────────────────────
  {
    target: "body",
    placement: "center",
    title: "👋 Seja bem-vindo ao Aegis",
    content:
      "O Aegis é o seu sistema central de gestão de produtos digitais. Em menos de 2 minutos, vamos mostrar os principais recursos para você começar com confiança. Você pode pular a qualquer momento — o tour fica disponível até o final.",
  },

  // ─── 2. Dashboard Global ─────────────────────────────────────────────────
  {
    target: '[data-tour="dashboard-global"]',
    placement: "bottom",
    title: "🏠 Seu painel de comando",
    content:
      "O Dashboard Global é a primeira tela que você vê ao entrar. Aqui você acompanha a saúde operacional de todos os seus produtos digitais em tempo real — sem precisar abrir cada produto separadamente.",
    data: { route: "/dashboard" },
  },
  {
    target: '[data-tour="dashboard-kpis"]',
    placement: "bottom",
    title: "📊 Indicadores em tempo real",
    content:
      "Estes cartões resumem o que está acontecendo agora: conteúdos pendentes de revisão, aprovações em aberto, formulários recebidos hoje e muito mais. Clique em qualquer cartão para ir direto à área correspondente.",
    data: { route: "/dashboard" },
  },
  {
    target: '[data-tour="dashboard-criar-produto"]',
    placement: "bottom-end",
    title: "✨ Criar um produto digital",
    content:
      'Produtos são os espaços de trabalho do Aegis — cada site, portal, base de conhecimento ou aplicação que você gerencia vive aqui. Clique em "Criar produto" para começar um novo.',
    data: { roles: CAN_CREATE_PRODUCT, route: "/dashboard" },
  },
  {
    target: '[data-tour="dashboard-timeline"]',
    placement: "left",
    title: "🕒 Linha do tempo operacional",
    content:
      "A linha do tempo registra as atividades recentes de toda a equipe: publicações, edições, aprovações e convites. É a sua fonte de verdade para entender o que aconteceu e quando.",
    data: { route: "/dashboard" },
  },

  // ─── 3. Produtos ─────────────────────────────────────────────────────────
  {
    target: '[data-tour="product-card"]',
    placement: "right",
    title: "📦 Produtos digitais",
    content:
      "Cada card representa um produto do seu workspace. O card mostra o status atual, os módulos habilitados e o último acesso da equipe. Clique no card para entrar no produto e gerenciar seu conteúdo.",
    data: { route: "/products" },
  },
  {
    target: '[data-tour="products-novo"]',
    placement: "bottom-end",
    title: "🆕 Novos produtos em segundos",
    content:
      "Ao criar um produto, você escolhe um modelo (blog, portal, e-commerce, base de conhecimento e outros). O Aegis já prepara a estrutura de páginas e ativa os módulos recomendados para aquele tipo — você começa com o essencial pronto.",
    data: { roles: CAN_CREATE_PRODUCT, route: "/products" },
  },

  // ─── 4. Conteúdo ─────────────────────────────────────────────────────────
  {
    // O atributo vive no <thead>, não no wrapper da tabela inteira: a
    // tabela costuma ser mais alta e mais larga que a viewport, e um
    // placement relativo a um alvo desse tamanho empurra o tooltip pra
    // fora da tela (bug real encontrado em produto — Sprint 22).
    target: '[data-tour="content-list"]',
    placement: "bottom",
    title: "✍️ Gerenciando conteúdo",
    content:
      "Aqui ficam todos os artigos, posts e materiais do produto selecionado. Você pode filtrar por status, autor ou data, e ver de um relance o que está em rascunho, em revisão ou já publicado.",
    data: { roles: CAN_SEE_CONTENT, requiredModule: "Conteúdo", route: "/content/list" },
  },
  {
    target: '[data-tour="content-status-badge"]',
    placement: "right",
    title: "🔄 Fluxo editorial",
    content:
      "O Aegis tem um workflow editorial embutido: Rascunho → Em Revisão → Aprovado → Publicado. Cada conteúdo avança pelo fluxo com a aprovação das pessoas certas — nada vai ao ar sem passar pelo processo definido pelo seu time.",
    data: { roles: CAN_SEE_CONTENT, requiredModule: "Conteúdo", route: "/content/list" },
  },
  {
    target: '[data-tour="content-editor-blocks"]',
    placement: "left",
    title: "🧱 Editor em blocos",
    content:
      "O editor funciona com blocos — cada parágrafo, imagem, vídeo ou formulário é um bloco independente que você pode arrastar, duplicar ou remover. É intuitivo e não exige conhecimento técnico.",
    // Sem rota fixa: o editor de blocos vive em `/pages/:id/editor` (id
    // dinâmico, não navegável direto). Se o usuário não estiver numa página
    // aberta no editor, o handler de TARGET_NOT_FOUND pula este step.
    data: { roles: CAN_EDIT_CONTENT_BLOCKS, requiredModule: "Páginas" },
  },

  // ─── 5. Usuários ─────────────────────────────────────────────────────────
  {
    target: '[data-tour="users-table"]',
    placement: "top",
    title: "👥 Sua equipe",
    content:
      "Aqui você vê todos os membros do workspace e os produtos que cada um pode acessar. O Aegis tem papéis bem definidos: Super Admin, Tenant Admin, Product Manager, Editor e Viewer — cada um com acesso calibrado ao que precisa.",
    data: { roles: CAN_MANAGE_USERS, route: "/users" },
  },
  {
    target: '[data-tour="users-convidar"]',
    placement: "bottom-end",
    title: "📨 Convidar alguém é simples",
    content:
      "Informe o e-mail, escolha o papel e selecione os produtos. A pessoa recebe um e-mail com um link direto para ativar a conta no Aegis — sem precisar configurar nada manualmente.",
    data: { roles: CAN_MANAGE_USERS, route: "/users" },
  },

  // ─── 6. Configurações ────────────────────────────────────────────────────
  {
    target: '[data-tour="settings-geral"]',
    placement: "right",
    title: "⚙️ Configurações do workspace",
    content:
      "Em Configurações você ajusta o nome, identidade visual e políticas gerais do seu workspace. Alterações aqui afetam todos os produtos e membros deste tenant.",
    data: { roles: CAN_SEE_SETTINGS, route: "/settings" },
  },
  {
    target: '[data-tour="settings-perigo"]',
    placement: "top",
    title: "⚠️ Zona de perigo",
    content:
      "Ações como excluir o workspace são irreversíveis. O Aegis sempre pede confirmação explícita — digitar o nome do tenant — antes de aplicar uma exclusão.",
    data: { roles: CAN_SEE_DANGER_ZONE, route: "/settings/tenant" },
  },

  // ─── 7. Sidebar ──────────────────────────────────────────────────────────
  {
    target: '[data-tour="sidebar-nav"]',
    placement: "right",
    title: "🗺️ Navegação principal",
    content:
      "A barra lateral é o seu ponto de partida para qualquer área do produto selecionado. Os itens visíveis dependem dos módulos habilitados — se um módulo não aparece aqui, verifique as configurações do produto.",
  },

  // ─── 8. Conclusão ────────────────────────────────────────────────────────
  {
    target: "body",
    placement: "center",
    title: "🎉 Você está pronto!",
    content:
      'Este foi um tour rápido pelo Aegis. Explore, crie e publique com confiança — se tiver dúvidas, a equipe de suporte está a um clique em "Reportar problema" no rodapé de qualquer tela. Bom trabalho!',
  },
];

/**
 * Filtra os steps pelo papel efetivo (`useViewAsRole`, não `authUser.role`
 * direto — mesma fonte que o resto do app usa para simular permissão) e
 * pelos módulos habilitados no produto selecionado (`resolveEnabledModules`).
 * Sem produto selecionado (ainda no `/select-product`/primeiro acesso),
 * nenhum módulo está "habilitado" — steps com `requiredModule` ficam de
 * fora até o usuário entrar em um produto.
 */
export function getVisibleTutorialSteps(role: UserRole, enabledModules: string[]): TutorialStep[] {
  return TUTORIAL_STEPS.filter((step) => {
    const gate = step.data;
    if (!gate) return true;
    if (gate.roles && !gate.roles.includes(role)) return false;
    if (gate.requiredModule && !enabledModules.includes(gate.requiredModule)) return false;
    return true;
  });
}
