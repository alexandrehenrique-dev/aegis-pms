package br.com.byop.aegis.audit.service;

import br.com.byop.aegis.audit.domain.AuditEvent;
import br.com.byop.aegis.audit.domain.AuditRisk;
import br.com.byop.aegis.audit.dto.AuditEventDetail;
import br.com.byop.aegis.audit.dto.AuditEventPage;
import br.com.byop.aegis.audit.dto.AuditEventPageQuery;
import br.com.byop.aegis.audit.dto.AuditEventSummary;
import br.com.byop.aegis.audit.exception.AuditEventNotFoundException;
import br.com.byop.aegis.audit.exception.InvalidAuditRiskFilterException;
import br.com.byop.aegis.audit.mapper.AuditEventMapper;
import br.com.byop.aegis.audit.repository.AuditEventRepository;
import br.com.byop.aegis.audit.api.TenantVisibilityPort;
import br.com.byop.aegis.identity.api.IdentityUserDirectory;
import br.com.byop.aegis.security.AuthenticatedUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Leitura da trilha de auditoria, sempre escopada por tenant. Dominio de
 * infraestrutura (Secao 10.6 do padrao de qualidade) — nao usa
 * {@code ProductAccessResolver}, apenas a regra geral de isolamento por
 * tenant (Secao 10.1-10.4).
 */
@Slf4j
@Service
public class AuditEventQueryService {

    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String TARGET_TYPE_TENANT = "Tenant";
    private static final int MAX_PAGE_SIZE = 100;

    private final AuditEventRepository auditEventRepository;
    private final AuditEventMapper auditEventMapper;
    private final TenantVisibilityPort tenantVisibilityPort;
    private final IdentityUserDirectory identityUserDirectory;
    private final ObjectMapper objectMapper;

    public AuditEventQueryService(AuditEventRepository auditEventRepository, AuditEventMapper auditEventMapper,
                                  TenantVisibilityPort tenantVisibilityPort, IdentityUserDirectory identityUserDirectory,
                                  ObjectMapper objectMapper) {
        this.auditEventRepository = auditEventRepository;
        this.auditEventMapper = auditEventMapper;
        this.tenantVisibilityPort = tenantVisibilityPort;
        this.identityUserDirectory = identityUserDirectory;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<AuditEventSummary> listEvents(AuthenticatedUser caller, UUID tenantId, String actorSubject,
                                              UUID productId, String module, String risk) {
        log.debug("listEvents: tenantId='{}', productId='{}', module='{}', risk='{}'", tenantId, productId, module, risk);
        assertTenantVisible(caller, tenantId);
        AuditRisk riskFilter = parseRiskFilter(risk);
        return auditEventRepository.findAllByFilters(tenantId, actorSubject, productId, module, riskFilter)
                .stream()
                .map(event -> auditEventMapper.toSummary(event, resolveActor(event.getActorSubject()),
                        resolveTenantName(tenantId, event), resolveTarget(event)))
                .toList();
    }

    @Transactional(readOnly = true)
    public AuditEventPage listEventsPage(AuthenticatedUser caller, UUID tenantId, AuditEventPageQuery pageQuery) {
        log.debug("listEventsPage: tenantId='{}', productId='{}', module='{}', risk='{}', query='{}', page='{}', size='{}'",
                tenantId, pageQuery.productId(), pageQuery.module(), pageQuery.risk(), pageQuery.query(), pageQuery.page(),
                pageQuery.size());
        assertTenantVisible(caller, tenantId);
        AuditRisk riskFilter = parseRiskFilter(pageQuery.risk());
        String searchQuery = normalizeQuery(pageQuery.query());
        int safePage = Math.max(pageQuery.page(), 0);
        int safeSize = Math.clamp(pageQuery.size(), 1, MAX_PAGE_SIZE);
        PageRequest pageRequest = PageRequest.of(safePage, safeSize);
        Page<AuditEvent> result = searchQuery == null
                ? auditEventRepository.findPageByFilters(tenantId, pageQuery.actorSubject(), pageQuery.productId(),
                        pageQuery.module(), riskFilter, pageRequest)
                : auditEventRepository.findPageByFiltersAndQuery(tenantId, pageQuery.actorSubject(), pageQuery.productId(),
                        pageQuery.module(), riskFilter,
                        toQueryPattern(searchQuery), pageRequest);
        List<AuditEventSummary> items = result.stream()
                .map(event -> auditEventMapper.toSummary(event, resolveActor(event.getActorSubject()),
                        resolveTenantName(tenantId, event), resolveTarget(event)))
                .toList();
        return new AuditEventPage(items, result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AuditEventDetail getEvent(AuthenticatedUser caller, UUID tenantId, UUID eventId) {
        log.debug("getEvent: tenantId='{}', eventId='{}'", tenantId, eventId);
        assertTenantVisible(caller, tenantId);
        AuditEvent event = auditEventRepository.findByIdAndTenantId(eventId, tenantId)
                .orElseThrow(AuditEventNotFoundException::new);
        return auditEventMapper.toDetail(event, resolveActor(event.getActorSubject()),
                resolveTenantName(tenantId, event), resolveTarget(event), readDiffJson(event.getDiffJson()));
    }

    /**
     * SUPER_ADMIN sempre tem acesso, mesmo a um tenant ja excluido (cenario
     * forense). Qualquer outro papel precisa de membership ativa — a
     * existencia do tenant na tabela {@code tenants} nunca e verificada
     * aqui, porque a trilha de auditoria deve sobreviver a exclusao do
     * tenant que ela audita.
     */
    private void assertTenantVisible(AuthenticatedUser caller, UUID tenantId) {
        if (isSuperAdmin(caller) || tenantVisibilityPort.hasActiveMembership(tenantId, caller.subject())) {
            return;
        }
        log.warn("assertTenantVisible: acesso negado a tenantId='{}' para caller='{}'", tenantId, caller.subject());
        throw new AuditEventNotFoundException();
    }

    private boolean isSuperAdmin(AuthenticatedUser caller) {
        return caller.authorities().contains(ROLE_SUPER_ADMIN);
    }

    private AuditRisk parseRiskFilter(String risk) {
        if (risk == null) {
            return null;
        }
        try {
            return AuditRisk.fromContractValue(risk);
        } catch (IllegalArgumentException _) {
            log.warn("parseRiskFilter: filtro de risco invalido rejeitado risk='{}'", risk);
            throw new InvalidAuditRiskFilterException(risk);
        }
    }

    private String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        return query.trim().toLowerCase();
    }

    private String toQueryPattern(String query) {
        return "%" + query + "%";
    }

    private String resolveActor(String actorSubject) {
        try {
            return identityUserDirectory.getRequiredUser(actorSubject).displayName();
        } catch (RuntimeException _) {
            return actorSubject;
        }
    }

    /**
     * Nome do tenant resolvido ao vivo via {@link TenantVisibilityPort}.
     * Quando o tenant ja foi excluido (FK removida na migration V8,
     * exatamente para permitir esta consulta), recorre ao snapshot gravado
     * em {@code targetLabel} apenas quando o proprio evento audita o tenant
     * ({@code targetType == "Tenant"}) — para qualquer outro evento do
     * mesmo tenant excluido, o id bruto e o melhor dado disponivel.
     */
    private String resolveTenantName(UUID tenantId, AuditEvent event) {
        String tenantName = tenantVisibilityPort.findTenantName(tenantId);
        if (tenantName != null) {
            return tenantName;
        }
        if (TARGET_TYPE_TENANT.equals(event.getTargetType()) && event.getTargetLabel() != null) {
            return event.getTargetLabel();
        }
        return String.valueOf(tenantId);
    }

    private String resolveTarget(AuditEvent event) {
        if (event.getTargetLabel() != null) {
            return event.getTargetLabel();
        }
        if (event.getTargetType() != null) {
            return event.getTargetId() == null ? event.getTargetType() : event.getTargetType() + " " + event.getTargetId();
        }
        return event.getTargetId();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readDiffJson(String diffJson) {
        if (diffJson == null || diffJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(diffJson, Map.class);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Unable to deserialize audit diff", ex);
        }
    }
}
