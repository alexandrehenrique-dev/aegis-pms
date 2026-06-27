package br.com.byop.aegis.audit.dto;

import java.util.Map;
import java.util.UUID;

/**
 * Detalhe de um evento de auditoria — mesmo shape de {@link AuditEventSummary}
 * acrescido dos campos tecnicos consumidos por {@code AuditEventDetail.tsx}
 * (diff antes/depois, trace id, IP, user agent).
 */
public record AuditEventDetail(
        UUID id,
        String actor,
        String action,
        String target,
        String tenant,
        String module,
        String time,
        String risk,
        Map<String, Object> diffJson,
        String traceId,
        String ip,
        String userAgent
) {
}
