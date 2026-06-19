export type SettingCard = {
  name: string; description: string; status: string;
  lastUpdated: string; owner: string; risk: string;
};

export type ListSettingCardsResponse = SettingCard[];
