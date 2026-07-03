package br.com.byop.aegis.audit.context;

/**
 * Contexto tecnico da request usado para enriquecer eventos de auditoria.
 */
public record AuditContext(
        String traceId,
        String ip,
        String userAgent
) {
}
