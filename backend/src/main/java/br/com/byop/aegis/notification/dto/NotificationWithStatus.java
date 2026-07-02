package br.com.byop.aegis.notification.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationWithStatus(
        UUID id,
        String type,
        String title,
        String bodyMarkdown,
        String presentationMode,
        OffsetDateTime createdAt,
        boolean autoShown,
        boolean read,
        OffsetDateTime readAt
) {
}
