package br.com.byop.aegis.notification.api;

import java.util.UUID;

/**
 * Contrato publico para notificar a plataforma sobre feedback critico.
 */
public record CriticalFeedbackNotificationRequest(
        String publicId,
        String category,
        String priority,
        String description,
        UUID tenantId,
        UUID productId,
        String createdBySubject,
        String screenName,
        UUID attachmentAssetId
) {
}
