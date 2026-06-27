package br.com.byop.aegis.audit.dto;

import java.util.UUID;

/**
 * Linha da listagem de eventos de auditoria de um tenant. Campos com nomes
 * resolvidos para exibicao (ator, tenant, recurso), nunca identificadores
 * crus, conforme contrato consumido por {@code AuditTimeline.tsx}.
 */
public record AuditEventSummary(
        UUID id,
        String actor,
        String action,
        String target,
        String tenant,
        String module,
        String time,
        String risk
) {
}
