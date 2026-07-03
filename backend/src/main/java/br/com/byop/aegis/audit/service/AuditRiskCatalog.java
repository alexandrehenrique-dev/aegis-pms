package br.com.byop.aegis.audit.service;

import br.com.byop.aegis.audit.domain.AuditRisk;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Tabela simples de mapeamento {@code acao -> risco}, conforme exigido pela
 * Sprint 16 ("manter uma tabela de mapeamento simples, nao um calculo
 * complexo"). Acao sem entrada na tabela recebe o risco neutro {@link
 * AuditRisk#MEDIO} por seguranca, nunca {@link AuditRisk#BAIXO}.
 */
@Component
public class AuditRiskCatalog {

    private static final Map<String, AuditRisk> RISK_BY_ACTION = Map.ofEntries(
            Map.entry("TENANT_CREATED", AuditRisk.BAIXO),
            Map.entry("TENANT_UPDATED", AuditRisk.MEDIO),
            Map.entry("TENANT_DELETED", AuditRisk.ALTO),
            Map.entry("PRODUCT_ASSIGNMENT_CREATED", AuditRisk.BAIXO),
            Map.entry("PRODUCT_ASSIGNMENT_REMOVED", AuditRisk.MEDIO),
            Map.entry("MODULE_ENABLED", AuditRisk.MEDIO),
            Map.entry("MODULE_DISABLED", AuditRisk.MEDIO),
            Map.entry("USER_INVITED_TO_TENANT", AuditRisk.BAIXO),
            Map.entry("USER_BLOCKED", AuditRisk.MEDIO),
            Map.entry("USER_REMOVED_FROM_TENANT", AuditRisk.ALTO),
            Map.entry("USER_RESTORED_TO_TENANT", AuditRisk.MEDIO),
            Map.entry("CONTENT_CREATED", AuditRisk.BAIXO),
            Map.entry("CONTENT_PUBLISHED", AuditRisk.MEDIO),
            Map.entry("FORM_SUBMISSION_RECEIVED", AuditRisk.BAIXO),
            Map.entry("ASSET_DELETED", AuditRisk.ALTO)
    );

    private static final AuditRisk DEFAULT_RISK = AuditRisk.MEDIO;

    /**
     * Resolve o risco de uma acao auditada.
     *
     * @param action codigo da acao (ex.: {@code TENANT_DELETED})
     * @return o risco cadastrado para a acao, ou {@link #DEFAULT_RISK} se a
     *         acao nao estiver na tabela
     */
    public AuditRisk resolve(String action) {
        return RISK_BY_ACTION.getOrDefault(action, DEFAULT_RISK);
    }
}
