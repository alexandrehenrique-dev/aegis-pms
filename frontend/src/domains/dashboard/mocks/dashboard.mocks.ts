import { FileText, Image, Boxes, BarChart3, Globe2, Sparkles, Workflow, Clock3, ShieldCheck, PanelRight } from "lucide-react";

// KPIs do hub do tenant. Vira GET /api/v1/dashboard/summary quando o backend
// existir (ver docs/trace/00_endpoints_esperados.md, Seção B) — provavelmente
// um agregador sobre os endpoints de tenants/produtos/módulos já definidos.
export const dashboardSummary = {
  activeProducts: 5,
  archivedProducts: 1,
  pendingContent: 18,
  pendingContentNeedingReview: 7,
  openApprovals: 6,
  criticalApprovals: 2,
  formsReceived: 143,
  formsReceivedToday: 12,
  recentAssets: 32,
  activeUsers: 21,
  productManagers: 9,
  conversionRate: "4.8%",
};

// Cross-domain reuse: o catálogo de módulos pertence conceitualmente a
// products, mas a lista canônica ainda vive aqui (movida de App.tsx na
// Sprint 01). productsService.listModuleCatalog() importa daqui.
export const modules = [
  [FileText, "Conteúdo", "Operar páginas, artigos e workflow editorial.", "habilitado", "MVP", "Sem dependências", "Publicação e governança"],
  [Image, "Assets", "Centralizar mídia, documentos e metadados.", "habilitado", "MVP", "Storage", "Organização de acervo"],
  [Boxes, "Forms", "Capturar respostas e oportunidades do produto.", "habilitado", "MVP", "Conteúdo", "Entrada operacional"],
  [BarChart3, "Analytics", "Acompanhar sinais de performance.", "habilitado", "V1", "Eventos", "Decisão baseada em dados"],
  [Globe2, "SEO", "Controlar presença e preview público.", "desabilitado", "MVP", "Conteúdo", "Descoberta orgânica"],
  [Sparkles, "Knowledge Graph", "Conectar entidades do produto.", "dependência", "V1", "Conteúdo + Assets", "Conhecimento navegável"],
  [Workflow, "Workflow", "Revisar, aprovar e publicar com rastreio.", "habilitado", "MVP", "Usuários", "Controle editorial"],
  [Clock3, "Versionamento", "Comparar e restaurar versões.", "futuro", "Futuro", "Workflow", "Memória operacional"],
  [ShieldCheck, "Auditoria", "Rastrear decisões e mudanças relevantes.", "sem permissão", "MVP", "Permissões", "Governança"],
  [PanelRight, "Integrações", "Conectar webhooks e provedores externos.", "desabilitado", "V1", "Configurações", "Automação"],
] as const;
