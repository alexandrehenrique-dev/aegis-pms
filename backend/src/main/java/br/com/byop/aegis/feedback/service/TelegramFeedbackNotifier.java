package br.com.byop.aegis.feedback.service;

import br.com.byop.aegis.feedback.domain.Feedback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;

import java.util.Map;

@Slf4j
@Component
public class TelegramFeedbackNotifier {

    private static final String TELEGRAM_SEND_MESSAGE_URL = "https://api.telegram.org/bot{botToken}/sendMessage";
    private static final int DESCRIPTION_PREVIEW_LIMIT = 200;

    private final RestClient restClient;
    private final boolean aegisTelegramEnabled;
    private final String aegisTelegramChatId;
    private final String aegisTelegramBotToken;

    public TelegramFeedbackNotifier(RestClient restClient,
                                    @Value("${aegis.telegram.alert.enabled:false}") boolean aegisTelegramEnabled,
                                    @Value("${aegis.telegram.alert.chat-id:}") String aegisTelegramChatId,
                                    @Value("${aegis.telegram.alert.bot-token:}") String aegisTelegramBotToken) {
        this.restClient = restClient;
        this.aegisTelegramEnabled = aegisTelegramEnabled;
        this.aegisTelegramChatId = aegisTelegramChatId;
        this.aegisTelegramBotToken = aegisTelegramBotToken;
    }

    public void notify(Feedback feedback) {
        if (!aegisTelegramEnabled || !hasText(aegisTelegramChatId) || !hasText(aegisTelegramBotToken)) {
            log.debug("notify: Telegram desabilitado ou nao configurado — feedback publicId='{}' nao notificado", feedback.getPublicId());
            return;
        }
        sendBestEffort(feedback);
    }

    private void sendBestEffort(Feedback feedback) {
        log.debug("sendBestEffort: tentando envio Telegram para feedback publicId='{}'", feedback.getPublicId());
        try {
            restClient.post()
                    .uri(TELEGRAM_SEND_MESSAGE_URL, aegisTelegramBotToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "chat_id", aegisTelegramChatId,
                            "text", buildMessage(feedback),
                            "parse_mode", "HTML"
                    ))
                    .retrieve()
                    .toBodilessEntity();
            log.info("sendBestEffort: notificacao Telegram enviada para feedback publicId='{}'", feedback.getPublicId());
        } catch (RuntimeException exception) {
            log.warn("Telegram dispatch failed for feedback {}", feedback.getPublicId(), exception);
        }
    }

    private String buildMessage(Feedback feedback) {
        StringBuilder message = new StringBuilder()
                .append("<b>Aegis feedback - ").append(escape(feedback.getPublicId())).append("</b>\n")
                .append("Categoria: ").append(escape(feedback.getCategory().contractValue())).append("\n")
                .append("Prioridade: <b>").append(escape(feedback.getPriority().contractValue())).append("</b>\n")
                .append("Tenant: ").append(escape(String.valueOf(feedback.getTenantId()))).append("\n")
                .append("Produto: ").append(escape(String.valueOf(feedback.getProductId()))).append("\n")
                .append("Tela: ").append(escape(valueOrDash(feedback.getScreenName()))).append("\n")
                .append("Usuario: ").append(escape(feedback.getCreatedBySubject())).append("\n\n")
                .append(escape(preview(feedback.getDescription())));

        if (feedback.getAttachmentAssetId() != null) {
            message.append("\nAnexo: <code>/api/v1/assets/")
                    .append(feedback.getAttachmentAssetId())
                    .append("/download</code>");
        }
        return message.toString();
    }

    private String preview(String description) {
        if (description.length() <= DESCRIPTION_PREVIEW_LIMIT) {
            return description;
        }
        return description.substring(0, DESCRIPTION_PREVIEW_LIMIT) + "...";
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
