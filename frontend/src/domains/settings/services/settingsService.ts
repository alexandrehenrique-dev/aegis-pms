import { settingCards } from "../mocks/settings.mocks";
import { logApiCall } from "../../../shared/services/devLog";
import { IS_API_MODE } from "../../../infra/apiMode";
import { apiClient } from "../../../shared/services/apiClient";
import type { ListSettingCardsResponse, SettingCard } from "../contracts/responses";

const settingCardsStore: SettingCard[] = settingCards.map(([name, description, status, lastUpdated, owner, risk]) => ({
  name, description, status, lastUpdated, owner, risk,
}));

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
};
