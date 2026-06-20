import { settingCards } from "../mocks/settings.mocks";
import { logApiCall } from "../../../shared/services/devLog";
import type { ListSettingCardsResponse, SettingCard } from "../contracts/responses";

const settingCardsStore: SettingCard[] = settingCards.map(([name, description, status, lastUpdated, owner, risk]) => ({
  name, description, status, lastUpdated, owner, risk,
}));

export const settingsService = {
  async listSettingCards(): Promise<ListSettingCardsResponse> {
    return settingCardsStore;
  },
  async restoreDefaultRoles(): Promise<void> {
    logApiCall("POST", "/api/v1/admin/roles/restore-defaults");
  },
  async createRole(payload: { name: string; description: string }): Promise<void> {
    logApiCall("POST", "/api/v1/admin/roles", payload);
  },
  async restoreDefaultPermissions(): Promise<void> {
    logApiCall("POST", "/api/v1/admin/permissions/restore-defaults");
  },
  async savePermissions(): Promise<void> {
    logApiCall("PUT", "/api/v1/admin/permissions");
  },
  async saveSecurity(): Promise<void> {
    logApiCall("PUT", "/api/v1/admin/settings/security");
  },
  async generateAccessPreview(payload: { subject: string; product: string; module: string }): Promise<void> {
    logApiCall("POST", "/api/v1/admin/permissions/preview", payload);
  },
};
