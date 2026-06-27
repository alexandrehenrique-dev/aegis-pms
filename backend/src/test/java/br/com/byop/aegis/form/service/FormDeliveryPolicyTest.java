package br.com.byop.aegis.form.service;

import br.com.byop.aegis.form.exception.InvalidFormDeliveryException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FormDeliveryPolicyTest {

    private final FormDeliveryPolicy policy = new FormDeliveryPolicy();

    @Test
    void shouldAcceptValidEnabledChannels() {
        policy.assertValid(Map.of("type", "email", "enabled", true, "config", Map.of("to", "ops@byop.com")));
        policy.assertValid(Map.of("type", "whatsapp", "enabled", true, "config", Map.of("number", "+5511999999999")));
        policy.assertValid(Map.of("type", "telegram", "enabled", true,
                "config", Map.of("chatId", "123", "botToken", "token")));
        policy.assertValid(Map.of("type", "webhook", "enabled", true,
                "config", Map.of("url", "https://example.com/hook", "method", "POST")));
        policy.assertValid(Map.of("type", "webhook", "enabled", true,
                "config", Map.of("url", "http://example.com/hook", "method", "POST")));
    }

    @Test
    void shouldIgnoreDisabledChannel() {
        policy.assertValid(Map.of("type", "telegram", "enabled", false));
    }

    @Test
    void shouldRejectUnknownChannel() {
        Map<String, Object> channel = Map.of("type", "sms", "enabled", true, "config", Map.of());

        assertThatThrownBy(() -> policy.assertValid(channel))
                .isInstanceOf(InvalidFormDeliveryException.class);
    }

    @Test
    void shouldRejectMissingConfig() {
        Map<String, Object> channel = Map.of("type", "email", "enabled", true);

        assertThatThrownBy(() -> policy.assertValid(channel))
                .isInstanceOf(InvalidFormDeliveryException.class);
    }

    @Test
    void shouldRejectTelegramWithoutChatIdOrBotToken() {
        Map<String, Object> channel = Map.of("type", "telegram", "enabled", true,
                "config", Map.of("chatId", "123"));

        assertThatThrownBy(() -> policy.assertValid(channel))
                .isInstanceOf(InvalidFormDeliveryException.class);
    }

    @Test
    void shouldRejectWebhookWithoutValidUrl() {
        Map<String, Object> channel = Map.of("type", "webhook", "enabled", true,
                "config", Map.of("url", "not-a-url"));

        assertThatThrownBy(() -> policy.assertValid(channel))
                .isInstanceOf(InvalidFormDeliveryException.class);
    }

    @Test
    void shouldRejectWebhookWithUnsupportedScheme() {
        Map<String, Object> channel = Map.of("type", "webhook", "enabled", true,
                "config", Map.of("url", "ftp://example.com/hook"));

        assertThatThrownBy(() -> policy.assertValid(channel))
                .isInstanceOf(InvalidFormDeliveryException.class);
    }

    @Test
    void shouldRejectWebhookWithoutHost() {
        Map<String, Object> channel = Map.of("type", "webhook", "enabled", true,
                "config", Map.of("url", "https:/hook"));

        assertThatThrownBy(() -> policy.assertValid(channel))
                .isInstanceOf(InvalidFormDeliveryException.class);
    }

    @Test
    void shouldRejectBlankRequiredConfigValue() {
        Map<String, Object> channel = Map.of("type", "email", "enabled", true,
                "config", Map.of("to", " "));

        assertThatThrownBy(() -> policy.assertValid(channel))
                .isInstanceOf(InvalidFormDeliveryException.class);
    }

    @Test
    void shouldRejectChannelWithoutTypeWhenEnabled() {
        Map<String, Object> channel = Map.of("enabled", true, "config", Map.of());

        assertThatThrownBy(() -> policy.assertValid(channel))
                .isInstanceOf(InvalidFormDeliveryException.class);
    }

    @Test
    void shouldRejectConfigWithNonStringValues() {
        Map<String, Object> channel = Map.of("type", "email", "enabled", true,
                "config", Map.of("to", 123));

        assertThatThrownBy(() -> policy.assertValid(channel))
                .isInstanceOf(InvalidFormDeliveryException.class);
    }

    @Test
    void shouldRejectConfigWithNonStringKeys() {
        Map<String, Object> channel = Map.of("type", "email", "enabled", true,
                "config", Map.of(123, "ops@byop.com"));

        assertThatThrownBy(() -> policy.assertValid(channel))
                .isInstanceOf(InvalidFormDeliveryException.class);
    }
}
