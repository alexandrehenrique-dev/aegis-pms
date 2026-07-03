import { settingCards } from "../mocks/settings.mocks";
import { logApiCall } from "../../../shared/services/devLog";
import { IS_API_MODE } from "../../../infra/apiMode";
import { apiClient } from "../../../shared/services/apiClient";
import type { ListSettingCardsResponse, ProductSecuritySettings, SettingCard, UpdateProductSecuritySettingsRequest } from "../contracts/responses";

const settingCardsStore: SettingCard[] = settingCards.map(([name, description, status, lastUpdated, owner, risk]) => ({
  name, description, status, lastUpdated, owner, risk,
}));

// Store em memória por produto — mesmo padrão dos demais services em modo
// mock (feedbackService, auditService). `telegramAlert` começa `null`: nenhum
// produto tem Telegram configurado até o usuário salvar pela primeira vez.
const securitySettingsStore = new Map<string, ProductSecuritySettings>();

function defaultSecuritySettings(): ProductSecuritySettings {
  return {
    webhookUrl: null,
    analyticsEnabled: false,
    analyticsProviderKey: null,
    emailDeliveryEnabled: false,
    updatedAt: null,
    webhookStatus: "disconnected",
    analyticsStatus: "disconnected",
    emailStatus: "disconnected",
    telegramAlert: null,
  };
}

function maskToken(token: string): string {
  return `****${token.slice(-4)}`;
}

export const settingsService = {
  async listSettingCards(): Promise<ListSettingCardsResponse> {
    if (IS_API_MODE) return apiClient.get<ListSettingCardsResponse>("/settings/cards");
    return settingCardsStore;
  },
  async restoreDefaultRoles(): Promise<void> {
    if (IS_API_MODE) return apiClient.post("/settings/roles/restore-defaults");
    logApiCall("POST", "/api/v1/admin/roles/restore-defaults");
  },
  async createRole(payload: { name: string; description: string }): Promise<void> {
    if (IS_API_MODE) return apiClient.post("/settings/roles", payload);
    logApiCall("POST", "/api/v1/admin/roles", payload);
  },
  async restoreDefaultPermissions(): Promise<void> {
    if (IS_API_MODE) return apiClient.post("/settings/permissions/restore-defaults");
    logApiCall("POST", "/api/v1/admin/permissions/restore-defaults");
  },
  async savePermissions(): Promise<void> {
    if (IS_API_MODE) return apiClient.put("/settings/permissions");
    logApiCall("PUT", "/api/v1/admin/permissions");
  },
  /** Path corrigido: `/products/{productId}/settings/security` (etapa 19, Seção C) — `productId` agora obrigatório. */
  async saveSecurity(productId: string, payload?: unknown): Promise<void> {
    if (IS_API_MODE) return apiClient.put(`/products/${productId}/settings/security`, payload);
    logApiCall("PUT", `/api/v1/products/${productId}/settings/security`, payload);
  },
  async generateAccessPreview(payload: { subject: string; product: string; module: string }): Promise<void> {
    if (IS_API_MODE) return apiClient.post("/settings/permissions/preview", payload);
    logApiCall("POST", "/api/v1/admin/permissions/preview", payload);
  },

  /** Espelha `GET /api/v1/products/{productId}/settings/security` (Sprint 23). */
  async getProductSettings(productId: string): Promise<ProductSecuritySettings> {
    if (IS_API_MODE) return apiClient.get<ProductSecuritySettings>(`/products/${productId}/settings/security`);
    return securitySettingsStore.get(productId) ?? defaultSecuritySettings();
  },

  /**
   * Espelha `PUT /api/v1/products/{productId}/settings/security` (Sprint 23).
   * Em modo mock, `botToken` vazio limpa o Telegram (mesma semântica do
   * backend real: string vazia vira `null`), nunca `telegramAlert: null`.
   */
  async updateProductSettings(productId: string, payload: UpdateProductSecuritySettingsRequest): Promise<ProductSecuritySettings> {
    if (IS_API_MODE) return apiClient.put<ProductSecuritySettings>(`/products/${productId}/settings/security`, payload);
    logApiCall("PUT", `/api/v1/products/${productId}/settings/security`, payload);
    const current = securitySettingsStore.get(productId) ?? defaultSecuritySettings();
    const telegramAlert = payload.telegramAlert && payload.telegramAlert.chatId.trim() && payload.telegramAlert.botToken.trim()
      ? { chatId: payload.telegramAlert.chatId.trim(), botTokenMasked: maskToken(payload.telegramAlert.botToken.trim()) }
      : payload.telegramAlert
        ? null
        : current.telegramAlert;
    const updated: ProductSecuritySettings = {
      ...current,
      webhookUrl: payload.webhookUrl ?? current.webhookUrl,
      analyticsEnabled: payload.analyticsEnabled,
      analyticsProviderKey: payload.analyticsProviderKey ?? current.analyticsProviderKey,
      emailDeliveryEnabled: payload.emailDeliveryEnabled,
      updatedAt: new Date().toISOString(),
      telegramAlert,
    };
    securitySettingsStore.set(productId, updated);
    return updated;
  },
};
