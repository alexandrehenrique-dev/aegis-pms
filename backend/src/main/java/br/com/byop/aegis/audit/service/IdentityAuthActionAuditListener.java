package br.com.byop.aegis.audit.service;

import br.com.byop.aegis.audit.api.AuditRecordCommand;
import br.com.byop.aegis.audit.api.AuditService;
import br.com.byop.aegis.identity.api.IdentityAuthActionAuditEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class IdentityAuthActionAuditListener {

    private static final String TARGET_TYPE_USER = "User";

    private final AuditService auditService;

    IdentityAuthActionAuditListener(AuditService auditService) {
        this.auditService = auditService;
    }

    @EventListener
    void onIdentityAuthAction(IdentityAuthActionAuditEvent event) {
        auditService.recordEvent(new AuditRecordCommand(
                event.tenantId(),
                null,
                event.actorSubject(),
                event.action(),
                TARGET_TYPE_USER,
                event.targetId(),
                event.targetLabel(),
                null,
                null,
                event.metadata()
        ));
    }
}
