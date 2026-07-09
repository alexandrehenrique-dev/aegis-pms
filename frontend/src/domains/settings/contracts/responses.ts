export type SettingCard = {
  name: string; description: string; status: string;
  lastUpdated: string; owner: string; risk: string;
};

export type ListSettingCardsResponse = SettingCard[];

export type RoleMatrixEntry = {
  role: string;
  permissions: Record<string, boolean>;
};

/**
 * Espelha `ProductSecuritySettingsResponse` do backend (etapa 19 +
 * etapa 30 §D.4.2 — `br.com.byop.aegis.settings.dto`). `telegramAlert` é
 * `null` sempre que nenhum chatId/botToken foi configurado ainda.
 */
export type TelegramAlert = { chatId: string; botTokenMasked: string };

export type ProductSecuritySettings = {
  webhookUrl: string | null;
  analyticsEnabled: boolean;
  analyticsProviderKey: string | null;
  emailDeliveryEnabled: boolean;
  updatedAt: string | null;
  webhookStatus: "connected" | "disconnected";
  analyticsStatus: "connected" | "disconnected" | "attention";
  emailStatus: "connected" | "disconnected";
  telegramAlert: TelegramAlert | null;
};

/**
 * Espelha `UpdateProductSecuritySettingsRequest`. Importante: o backend trata
 * `telegramAlert: null` como "não alterar" — para limpar de fato é preciso
 * enviar `{ chatId: "", botToken: "" }` (strings vazias viram `null` no
 * backend via `blankToNull()`). Nunca omitir o campo esperando limpeza.
 */
export type UpdateProductSecuritySettingsRequest = {
  webhookUrl?: string;
  webhookSecret?: string;
  analyticsEnabled: boolean;
  analyticsProviderKey?: string;
  emailDeliveryEnabled: boolean;
  telegramAlert?: { chatId: string; botToken: string } | null;
};
