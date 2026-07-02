package br.com.byop.aegis.notification.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String type,
        String title,
        String bodyMarkdown,
        String presentationMode,
        String createdBySubject,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
