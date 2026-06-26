package br.com.byop.aegis.tenant.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TenantSummary(
        UUID id,
        String key,
        String name,
        String status,
        String plan,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
