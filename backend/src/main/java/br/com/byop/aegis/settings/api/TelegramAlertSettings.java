package br.com.byop.aegis.settings.api;

/**
 * Configuracao interna de dispatch Telegram de um produto.
 */
public record TelegramAlertSettings(
        String chatId,
        String botToken
) {
}
