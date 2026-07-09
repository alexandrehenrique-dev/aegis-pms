package br.com.byop.aegis.audit.dto;

import java.util.UUID;

public record AuditEventPageQuery(
        String actorSubject,
        UUID productId,
        String module,
        String risk,
        String query,
        int page,
        int size
) {
}
