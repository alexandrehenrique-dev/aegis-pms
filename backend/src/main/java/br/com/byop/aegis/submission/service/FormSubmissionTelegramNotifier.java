package br.com.byop.aegis.submission.service;

import br.com.byop.aegis.form.api.FormReference;
import br.com.byop.aegis.submission.domain.Submission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Component
public class FormSubmissionTelegramNotifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(FormSubmissionTelegramNotifier.class);
    private static final String TELEGRAM_TYPE = "telegram";
    private static final String TELEGRAM_SEND_MESSAGE_URL = "https://api.telegram.org/bot{botToken}/sendMessage";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public FormSubmissionTelegramNotifier(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public void notify(FormReference form, Submission submission) {
        for (Map<String, Object> channel : readChannels(form.deliveryChannelsJson())) {
            if (telegramEnabled(channel)) {
                sendBestEffort(form, submission, config(channel));
            }
        }
    }

    private void sendBestEffort(FormReference form, Submission submission, Map<String, String> config) {
        String botToken = config.get("botToken");
        String chatId = config.get("chatId");
        if (!hasText(botToken) || !hasText(chatId)) {
            return;
        }
        try {
            restClient.post()
                    .uri(TELEGRAM_SEND_MESSAGE_URL, botToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "chat_id", chatId,
                            "text", buildMessage(form, submission),
                            "parse_mode", "HTML"
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException exception) {
            LOGGER.warn("Telegram form delivery failed for submission {}", submission.getId(), exception);
        }
    }

    private String buildMessage(FormReference form, Submission submission) {
        return new StringBuilder()
                .append("<b>Nova submissao de formulario</b>\n")
                .append("Produto: ").append(escape(String.valueOf(form.productId()))).append("\n")
                .append("Formulario: ").append(escape(String.valueOf(form.id()))).append("\n")
                .append("Submissao: ").append(escape(String.valueOf(submission.getId()))).append("\n")
                .append("Nome: ").append(escape(valueOrDash(submission.getName()))).append("\n")
                .append("Email: ").append(escape(valueOrDash(submission.getEmail()))).append("\n")
                .append("Origem: ").append(escape(valueOrDash(submission.getSource())))
                .toString();
    }

    private List<Map<String, Object>> readChannels(String deliveryChannelsJson) {
        if (!hasText(deliveryChannelsJson)) {
            return List.of();
        }
        try {
            List<?> value = objectMapper.readValue(deliveryChannelsJson, List.class);
            if (!value.stream().allMatch(item -> item instanceof Map<?, ?>)) {
                return List.of();
            }
            return value.stream()
                    .map(item -> toObjectMap((Map<?, ?>) item))
                    .toList();
        } catch (JacksonException exception) {
            LOGGER.warn("Ignoring invalid form delivery JSON while notifying Telegram", exception);
            return List.of();
        }
    }

    private boolean telegramEnabled(Map<String, Object> channel) {
        return TELEGRAM_TYPE.equals(String.valueOf(channel.get("type"))) && Boolean.TRUE.equals(channel.get("enabled"));
    }

    private Map<String, String> config(Map<String, Object> channel) {
        Object value = channel.get("config");
        if (value instanceof Map<?, ?> map) {
            java.util.LinkedHashMap<String, String> config = new java.util.LinkedHashMap<>();
            map.forEach((key, configValue) -> config.put(String.valueOf(key), String.valueOf(configValue)));
            return config;
        }
        return Map.of();
    }

    private Map<String, Object> toObjectMap(Map<?, ?> map) {
        java.util.LinkedHashMap<String, Object> objectMap = new java.util.LinkedHashMap<>();
        map.forEach((key, mapValue) -> objectMap.put(key.toString(), mapValue));
        return objectMap;
    }

    private String valueOrDash(String value) {
        return hasText(value) ? value : "-";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String escape(String value) {
        return HtmlUtils.htmlEscape(value);
    }
}
