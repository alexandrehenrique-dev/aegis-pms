// Legacy "screen" union from the original Figma Make monolith (App.tsx state machine).
// Kept as a reference map between the old fake-navigation model and the new
// React Router routes — used by shared widgets that still reason about
// "which screen/section" something belongs to (labels, parent module, tabs).
export type Screen =
  | "global" | "products" | "create" | "product" | "detail" | "empty" | "modules"
  | "content" | "contentList" | "workflow" | "editor" | "preview" | "publish" | "versions" | "compare"
  | "assets" | "assetUpload" | "assetDetail" | "assetMeta" | "assetTags" | "assetUsage" | "assetPicker"
  | "forms" | "formList" | "formBuilder" | "formPreview" | "submissions" | "submissionDetail" | "formAnalytics" | "formPublication"
  | "analytics" | "productHealth" | "contentAnalytics" | "formAnalyticsView" | "trafficChannels" | "reports" | "trends" | "analyticsStates"
  | "knowledge" | "graphCanvas" | "relationships" | "entityDetails" | "entitySearch" | "orphans" | "knowledgeInsights"
  | "settings" | "productSettings" | "tenantSettings" | "usersMgmt" | "inviteUser" | "userDetail" | "permissionMatrix" | "roles" | "accessPreview"
  | "auditTimeline" | "auditDetail" | "securityIntegrations";

export type ProductStatus = "Ativo" | "Pendente" | "Arquivado" | "Sem módulos";
export type ModuleState = "habilitado" | "desabilitado" | "dependência" | "futuro" | "sem permissão";
