package br.com.byop.aegis.tenant.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TenantMembershipReference(
        UUID id,
        UUID tenantId,
        String tenantName,
        String userSubject,
        String role,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
