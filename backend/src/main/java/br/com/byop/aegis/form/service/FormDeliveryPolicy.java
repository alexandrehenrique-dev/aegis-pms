package br.com.byop.aegis.form.service;

import br.com.byop.aegis.form.exception.InvalidFormDeliveryException;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class FormDeliveryPolicy {

    public void assertValid(Map<String, Object> channel) {
        String type = stringValue(channel.get("type"));
        if (!enabled(channel)) {
            return;
        }
        Map<String, String> config = config(channel);
        switch (type) {
            case "email" -> require(config.get("to"));
            case "whatsapp" -> require(config.get("number"));
            case "telegram" -> {
                require(config.get("chatId"));
                require(config.get("botToken"));
            }
            case "webhook" -> assertWebhook(config);
            default -> throw new InvalidFormDeliveryException("Unknown delivery channel: " + type);
        }
    }

    private void assertWebhook(Map<String, String> config) {
        String url = config.get("url");
        require(url);
        URI uri = URI.create(url);
        if (uri.getScheme() == null || uri.getHost() == null
                || (!"http".equals(uri.getScheme()) && !"https".equals(uri.getScheme()))) {
            throw new InvalidFormDeliveryException("Webhook URL must be absolute HTTP(S)");
        }
    }

    private boolean enabled(Map<String, Object> channel) {
        return Boolean.TRUE.equals(channel.get("enabled"));
    }

    private Map<String, String> config(Map<String, Object> channel) {
        Object value = channel.get("config");
        if (value instanceof Map<?, ?> map && map.keySet().stream().allMatch(item -> item instanceof String _)
                && map.values().stream().allMatch(item -> item instanceof String _)) {
            Map<String, String> config = new LinkedHashMap<>();
            map.forEach((key, configValue) -> config.put((String) key, (String) configValue));
            return config;
        }
        throw new InvalidFormDeliveryException("Delivery channel config is required");
    }

    private void require(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidFormDeliveryException("Delivery channel config is incomplete");
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }
}
