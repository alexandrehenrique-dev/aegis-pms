import { settingCards } from "../mocks/settings.mocks";
import { logApiCall } from "../../../shared/services/devLog";
import { IS_API_MODE } from "../../../infra/apiMode";
import { apiClient } from "../../../shared/services/apiClient";
import type { ListSettingCardsResponse, ProductSecuritySettings, RoleMatrixEntry, SettingCard, UpdateProductSecuritySettingsRequest } from "../contracts/responses";

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

function connectionStatus(value: string | null | undefined): "connected" | "disconnected" {
  return value && value.trim() ? "connected" : "disconnected";
}

function analyticsStatus(enabled: boolean, providerKey: string | null | undefined): "connected" | "disconnected" | "attention" {
  if (!enabled) return "disconnected";
  return connectionStatus(providerKey) === "connected" ? "connected" : "attention";
}

export const settingsService = {
  async listSettingCards(productId?: string): Promise<ListSettingCardsResponse> {
    if (IS_API_MODE) {
      if (!productId) {
        console.warn("[settingsService] overview de settings requer produto efetivo — usando mock em modo API.");
        return settingCardsStore;
      }
      return apiClient.get<ListSettingCardsResponse>(`/products/${productId}/settings/overview`);
    }
    return settingCardsStore;
  },
  async restoreDefaultRoles(tenantId?: string): Promise<void> {
    if (IS_API_MODE) {
      if (!tenantId) {
        console.warn("[settingsService] restaurar roles requer tenant efetivo — ignorando em modo API.");
        return;
      }
      await apiClient.post(`/tenants/${tenantId}/roles/restore-defaults`);
      return;
    }
    logApiCall("POST", "/api/v1/admin/roles/restore-defaults");
  },
  async createRole(payload: { name: string; description: string }): Promise<void> {
    if (IS_API_MODE) {
      console.warn("[settingsService] endpoint para criar role individual não existe no backend — ignorando em modo API.");
      return;
    }
    logApiCall("POST", "/api/v1/admin/roles", payload);
  },
  async restoreDefaultPermissions(tenantId?: string): Promise<void> {
    if (IS_API_MODE) {
      if (!tenantId) {
        console.warn("[settingsService] restaurar matriz de permissoes requer tenant efetivo — ignorando em modo API.");
        return;
      }
      await apiClient.post(`/tenants/${tenantId}/permission-matrix/restore-defaults`);
      return;
    }
    logApiCall("POST", "/api/v1/admin/permissions/restore-defaults");
  },
  async getPermissionMatrix(tenantId?: string): Promise<RoleMatrixEntry[]> {
    if (IS_API_MODE) {
      if (!tenantId) {
        console.warn("[settingsService] carregar matriz de permissoes requer tenant efetivo — usando lista vazia em modo API.");
        return [];
      }
      return apiClient.get<RoleMatrixEntry[]>(`/tenants/${tenantId}/permission-matrix`);
    }
    logApiCall("GET", "/api/v1/admin/permissions");
    return [];
  },
  async savePermissions(tenantId?: string, entries: RoleMatrixEntry[] = []): Promise<RoleMatrixEntry[]> {
    if (IS_API_MODE) {
      if (!tenantId) {
        console.warn("[settingsService] salvar matriz de permissoes requer tenant efetivo — ignorando em modo API.");
        return [];
      }
      return apiClient.put<RoleMatrixEntry[]>(`/tenants/${tenantId}/roles`, entries);
    }
    logApiCall("PUT", "/api/v1/admin/permissions", entries);
    return entries;
  },
  /** Path corrigido: `/products/{productId}/settings/security` (etapa 19, Seção C) — `productId` agora obrigatório. */
  async saveSecurity(productId: string, payload?: unknown): Promise<void> {
    if (IS_API_MODE) return apiClient.put(`/products/${productId}/settings/security`, payload);
    logApiCall("PUT", `/api/v1/products/${productId}/settings/security`, payload);
  },
  async generateAccessPreview(payload: { subject: string; product: string; module: string }, tenantId?: string): Promise<void> {
    if (IS_API_MODE) {
      if (!tenantId) {
        console.warn("[settingsService] preview de permissoes requer tenant efetivo — ignorando em modo API.");
        return;
      }
      await apiClient.post(`/tenants/${tenantId}/permission-matrix/preview`, { role: payload.subject });
      return;
    }
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
    updated.webhookStatus = connectionStatus(updated.webhookUrl);
    updated.analyticsStatus = analyticsStatus(updated.analyticsEnabled, updated.analyticsProviderKey);
    updated.emailStatus = updated.emailDeliveryEnabled ? "connected" : "disconnected";
    securitySettingsStore.set(productId, updated);
    return updated;
  },
};
