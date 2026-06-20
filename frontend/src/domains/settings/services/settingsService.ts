import { settingCards } from "../mocks/settings.mocks";
import type { ListSettingCardsResponse, SettingCard } from "../contracts/responses";

const settingCardsStore: SettingCard[] = settingCards.map(([name, description, status, lastUpdated, owner, risk]) => ({
  name, description, status, lastUpdated, owner, risk,
}));

export const settingsService = {
  async listSettingCards(): Promise<ListSettingCardsResponse> {
    return settingCardsStore;
  },
  async restoreDefaultRoles(): Promise<void> {},
  async createRole(_payload: { name: string; description: string }): Promise<void> {
    void _payload;
  },
  async restoreDefaultPermissions(): Promise<void> {},
  async savePermissions(): Promise<void> {},
  async saveSecurity(): Promise<void> {},
  async generateAccessPreview(_payload: { subject: string; product: string; module: string }): Promise<void> {
    void _payload;
  },
};
