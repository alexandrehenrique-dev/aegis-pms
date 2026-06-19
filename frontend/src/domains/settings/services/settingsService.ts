import { settingCards } from "../mocks/settings.mocks";
import type { ListSettingCardsResponse, SettingCard } from "../contracts/responses";

const settingCardsStore: SettingCard[] = settingCards.map(([name, description, status, lastUpdated, owner, risk]) => ({
  name, description, status, lastUpdated, owner, risk,
}));

export const settingsService = {
  async listSettingCards(): Promise<ListSettingCardsResponse> {
    return settingCardsStore;
  },
};
