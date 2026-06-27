package br.com.byop.aegis.audit.api;

import java.util.Map;
import java.util.UUID;

/**
 * Comando de gravacao de um evento de auditoria, recebido por
 * {@link AuditService#record(AuditRecordCommand)}. Campos opcionais
 * ({@code productId}, {@code targetType}/{@code targetId}/{@code targetLabel},
 * {@code module}, {@code before}/{@code after}) podem ser {@code null} — o
 * risco nunca e informado pelo chamador, e sempre calculado internamente a
 * partir de {@code action}.
 */
public record AuditRecordCommand(
        UUID tenantId,
        UUID productId,
        String actorSubject,
        String action,
        String targetType,
        String targetId,
        String targetLabel,
        String module,
        Map<String, Object> before,
        Map<String, Object> after
) {
}
