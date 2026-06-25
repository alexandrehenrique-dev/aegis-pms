package br.com.byop.aegis.core.tenant.dto;

import br.com.byop.aegis.core.tenant.TenantStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TenantSummary(
        UUID id,
        String key,
        String name,
        TenantStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
