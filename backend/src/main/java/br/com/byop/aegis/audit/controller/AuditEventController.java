package br.com.byop.aegis.audit.controller;

import br.com.byop.aegis.audit.dto.AuditEventDetail;
import br.com.byop.aegis.audit.dto.AuditEventPage;
import br.com.byop.aegis.audit.dto.AuditEventPageQuery;
import br.com.byop.aegis.audit.dto.AuditEventSummary;
import br.com.byop.aegis.audit.service.AuditEventQueryService;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.security.AuthenticatedUserProvider;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Leitura da trilha de auditoria de um tenant — somente leitura, nenhum
 * endpoint de escrita direta. Eventos sao gravados exclusivamente pelos
 * proprios servicos de dominio via {@code AuditService.recordEvent(...)}.
 */
@RestController
public class AuditEventController {

    private final AuditEventQueryService auditEventQueryService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public AuditEventController(AuditEventQueryService auditEventQueryService,
                                AuthenticatedUserProvider authenticatedUserProvider) {
        this.auditEventQueryService = auditEventQueryService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping("/api/v1/tenants/{tenantId}/audit-events")
    public List<AuditEventSummary> listAuditEvents(@PathVariable("tenantId") UUID tenantId,
                                                   @RequestParam(name = "actorSubject", required = false) String actorSubject,
                                                   @RequestParam(name = "productId", required = false) UUID productId,
                                                   @RequestParam(name = "module", required = false) String module,
                                                   @RequestParam(name = "risk", required = false) String risk,
                                                   Authentication authentication) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        return auditEventQueryService.listEvents(caller, tenantId, actorSubject, productId, module, risk);
    }

    @GetMapping("/api/v1/tenants/{tenantId}/audit-events/page")
    public AuditEventPage listAuditEventsPage(@PathVariable("tenantId") UUID tenantId,
                                              @RequestParam(name = "actorSubject", required = false) String actorSubject,
                                              @RequestParam(name = "productId", required = false) UUID productId,
                                              @RequestParam(name = "module", required = false) String module,
                                              @RequestParam(name = "risk", required = false) String risk,
                                              @RequestParam(name = "q", required = false) String query,
                                              @RequestParam(name = "page", defaultValue = "0") int page,
                                              @RequestParam(name = "size", defaultValue = "25") int size,
                                              Authentication authentication) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        AuditEventPageQuery pageQuery = new AuditEventPageQuery(actorSubject, productId, module, risk, query, page, size);
        return auditEventQueryService.listEventsPage(caller, tenantId, pageQuery);
    }

    @GetMapping("/api/v1/tenants/{tenantId}/audit-events/{eventId}")
    public AuditEventDetail getAuditEvent(@PathVariable("tenantId") UUID tenantId,
                                          @PathVariable("eventId") UUID eventId,
                                          Authentication authentication) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        return auditEventQueryService.getEvent(caller, tenantId, eventId);
    }
}
