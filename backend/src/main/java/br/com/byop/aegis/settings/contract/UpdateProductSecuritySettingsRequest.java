package br.com.byop.aegis.settings.contract;

public record UpdateProductSecuritySettingsRequest(
        String webhookUrl,
        String webhookSecret,
        boolean analyticsEnabled,
        String analyticsProviderKey,
        boolean emailDeliveryEnabled,
        TelegramAlertRequest telegramAlert
) {
    public record TelegramAlertRequest(
            String chatId,
            String botToken
    ) {
    }
}
