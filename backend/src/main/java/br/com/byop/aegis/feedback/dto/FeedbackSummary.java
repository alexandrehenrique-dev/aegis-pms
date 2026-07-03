package br.com.byop.aegis.feedback.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FeedbackSummary(
        String id,
        String category,
        String priority,
        String description,
        String status,
        String createdBySubject,
        UUID tenantId,
        UUID productId,
        OffsetDateTime createdAt,
        UUID attachmentAssetId
) {
}
