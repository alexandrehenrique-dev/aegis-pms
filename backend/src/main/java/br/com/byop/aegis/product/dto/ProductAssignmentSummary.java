package br.com.byop.aegis.product.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProductAssignmentSummary(
        UUID id,
        UUID tenantId,
        UUID productId,
        String productName,
        String userSubject,
        String userName,
        String userEmail,
        String role,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
