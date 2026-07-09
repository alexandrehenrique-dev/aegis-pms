package br.com.byop.aegis.audit.dto;

import java.util.List;

public record AuditEventPage(
        List<AuditEventSummary> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
