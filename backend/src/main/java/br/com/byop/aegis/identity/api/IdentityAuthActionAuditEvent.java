package br.com.byop.aegis.identity.api;

import java.util.Map;
import java.util.UUID;

public record IdentityAuthActionAuditEvent(
        UUID tenantId,
        String actorSubject,
        String action,
        String targetId,
        String targetLabel,
        Map<String, Object> metadata
) {
}
