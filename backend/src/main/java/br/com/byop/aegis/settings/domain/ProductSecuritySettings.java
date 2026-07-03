package br.com.byop.aegis.settings.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "product_security_settings")
public class ProductSecuritySettings {

    @Id
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "webhook_url", length = 1000)
    private String webhookUrl;

    @Column(name = "webhook_secret", length = 500)
    private String webhookSecret;

    @Column(name = "analytics_enabled", nullable = false)
    private boolean analyticsEnabled;

    @Column(name = "analytics_provider_key", length = 500)
    private String analyticsProviderKey;

    @Column(name = "email_delivery_enabled", nullable = false)
    private boolean emailDeliveryEnabled;

    @Column(name = "telegram_alert_bot_token", length = 128)
    private String telegramAlertBotToken;

    @Column(name = "telegram_alert_chat_id", length = 64)
    private String telegramAlertChatId;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    protected ProductSecuritySettings() {
    }

    public ProductSecuritySettings(UUID productId) {
        this.productId = productId;
    }

    public void update(Update update) {
        webhookUrl = blankToNull(update.webhookUrl());
        if (update.webhookSecret() != null && !update.webhookSecret().isBlank()) {
            webhookSecret = update.webhookSecret();
        }
        analyticsEnabled = update.analyticsEnabled();
        analyticsProviderKey = blankToNull(update.analyticsProviderKey());
        emailDeliveryEnabled = update.emailDeliveryEnabled();
        if (update.telegramAlertChatId() != null) {
            telegramAlertChatId = blankToNull(update.telegramAlertChatId());
        }
        if (update.telegramAlertBotToken() != null) {
            telegramAlertBotToken = blankToNull(update.telegramAlertBotToken());
        }
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public boolean isAnalyticsEnabled() {
        return analyticsEnabled;
    }

    public String getAnalyticsProviderKey() {
        return analyticsProviderKey;
    }

    public boolean isEmailDeliveryEnabled() {
        return emailDeliveryEnabled;
    }

    public String getTelegramAlertBotToken() {
        return telegramAlertBotToken;
    }

    public String getTelegramAlertChatId() {
        return telegramAlertChatId;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public record Update(
            String webhookUrl,
            String webhookSecret,
            boolean analyticsEnabled,
            String analyticsProviderKey,
            boolean emailDeliveryEnabled,
            String telegramAlertChatId,
            String telegramAlertBotToken
    ) {
    }
}
