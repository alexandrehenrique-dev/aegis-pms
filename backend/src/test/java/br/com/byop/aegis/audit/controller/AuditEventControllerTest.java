package br.com.byop.aegis.audit.controller;

import br.com.byop.aegis.audit.dto.AuditEventDetail;
import br.com.byop.aegis.audit.dto.AuditEventPageQuery;
import br.com.byop.aegis.audit.dto.AuditEventSummary;
import br.com.byop.aegis.audit.exception.AuditEventExceptionHandler;
import br.com.byop.aegis.audit.exception.AuditEventNotFoundException;
import br.com.byop.aegis.audit.exception.InvalidAuditRiskFilterException;
import br.com.byop.aegis.audit.service.AuditEventQueryService;
import br.com.byop.aegis.core.CoreExceptionHandler;
import br.com.byop.aegis.security.AuthenticatedUserProvider;
import br.com.byop.aegis.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuditEventController.class)
@Import({
        SecurityConfig.class,
        CoreExceptionHandler.class,
        AuditEventExceptionHandler.class,
        AuthenticatedUserProvider.class
})
class AuditEventControllerTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID EVENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditEventQueryService auditEventQueryService;

    @Test
    void shouldListAuditEvents() throws Exception {
        AuditEventSummary summary = summary();
        when(auditEventQueryService.listEvents(any(), eq(TENANT_ID), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(summary));

        mockMvc.perform(get("/api/v1/tenants/{tenantId}/audit-events", TENANT_ID).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(EVENT_ID.toString()))
                .andExpect(jsonPath("$[0].actor").value("Ana Martins"))
                .andExpect(jsonPath("$[0].action").value("TENANT_DELETED"))
                .andExpect(jsonPath("$[0].risk").value("alto"));
    }

    @Test
    void shouldListAuditEventsWithFilters() throws Exception {
        UUID productId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        when(auditEventQueryService.listEvents(any(), eq(TENANT_ID), eq("subject-1"), eq(productId),
                eq("CONTENT"), eq("baixo"))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/tenants/{tenantId}/audit-events", TENANT_ID)
                        .param("actorSubject", "subject-1")
                        .param("productId", productId.toString())
                        .param("module", "CONTENT")
                        .param("risk", "baixo")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldListPagedAuditEventsWithFilters() throws Exception {
        UUID productId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        AuditEventSummary summary = summary();
        AuditEventPageQuery pageQuery = new AuditEventPageQuery("subject-1", productId, "CONTENT", "baixo", "tenant", 2, 10);
        when(auditEventQueryService.listEventsPage(any(), eq(TENANT_ID), eq(pageQuery)))
                .thenReturn(new br.com.byop.aegis.audit.dto.AuditEventPage(List.of(summary), 2, 10, 21, 3));

        mockMvc.perform(get("/api/v1/tenants/{tenantId}/audit-events/page", TENANT_ID)
                        .param("actorSubject", "subject-1")
                        .param("productId", productId.toString())
                        .param("module", "CONTENT")
                        .param("risk", "baixo")
                        .param("q", "tenant")
                        .param("page", "2")
                        .param("size", "10")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(EVENT_ID.toString()))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(21))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void shouldReturnNotFoundWhenTenantNotVisible() throws Exception {
        when(auditEventQueryService.listEvents(any(), eq(TENANT_ID), isNull(), isNull(), isNull(), isNull()))
                .thenThrow(new AuditEventNotFoundException());

        mockMvc.perform(get("/api/v1/tenants/{tenantId}/audit-events", TENANT_ID).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("AUDIT_EVENT_NOT_FOUND"));
    }

    @Test
    void shouldReturnBadRequestForInvalidRiskFilter() throws Exception {
        when(auditEventQueryService.listEvents(any(), eq(TENANT_ID), isNull(), isNull(), isNull(), eq("inexistente")))
                .thenThrow(new InvalidAuditRiskFilterException("inexistente"));

        mockMvc.perform(get("/api/v1/tenants/{tenantId}/audit-events", TENANT_ID)
                        .param("risk", "inexistente")
                        .with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_AUDIT_RISK_FILTER"));
    }

    @Test
    void shouldGetAuditEventDetail() throws Exception {
        AuditEventDetail detail = detail();
        when(auditEventQueryService.getEvent(any(), eq(TENANT_ID), eq(EVENT_ID))).thenReturn(detail);

        mockMvc.perform(get("/api/v1/tenants/{tenantId}/audit-events/{eventId}", TENANT_ID, EVENT_ID).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(EVENT_ID.toString()))
                .andExpect(jsonPath("$.traceId").value("trace-1"))
                .andExpect(jsonPath("$.ip").value("127.0.0.1"))
                .andExpect(jsonPath("$.userAgent").value("JUnit"))
                .andExpect(jsonPath("$.diffJson.after.status").value("PUBLISHED"));
    }

    @Test
    void shouldReturnNotFoundWhenEventDoesNotExist() throws Exception {
        when(auditEventQueryService.getEvent(any(), eq(TENANT_ID), eq(EVENT_ID)))
                .thenThrow(new AuditEventNotFoundException());

        mockMvc.perform(get("/api/v1/tenants/{tenantId}/audit-events/{eventId}", TENANT_ID, EVENT_ID).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("AUDIT_EVENT_NOT_FOUND"));
    }

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/tenants/{tenantId}/audit-events", TENANT_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldNotExposeAnyWriteEndpointForListPath() throws Exception {
        mockMvc.perform(post("/api/v1/tenants/{tenantId}/audit-events", TENANT_ID).with(jwt()))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(put("/api/v1/tenants/{tenantId}/audit-events", TENANT_ID).with(jwt()))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete("/api/v1/tenants/{tenantId}/audit-events", TENANT_ID).with(jwt()))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void shouldNotExposeAnyWriteEndpointForDetailPath() throws Exception {
        mockMvc.perform(post("/api/v1/tenants/{tenantId}/audit-events/{eventId}", TENANT_ID, EVENT_ID).with(jwt()))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(put("/api/v1/tenants/{tenantId}/audit-events/{eventId}", TENANT_ID, EVENT_ID).with(jwt()))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete("/api/v1/tenants/{tenantId}/audit-events/{eventId}", TENANT_ID, EVENT_ID).with(jwt()))
                .andExpect(status().isMethodNotAllowed());
    }

    private AuditEventSummary summary() {
        return new AuditEventSummary(EVENT_ID, "Ana Martins", "TENANT_DELETED", "ACME", "ACME", null,
                "2026-06-27T10:00-03:00", "alto");
    }

    private AuditEventDetail detail() {
        return new AuditEventDetail(EVENT_ID, "Ana Martins", "CONTENT_PUBLISHED", "Pagina inicial", "ACME", "CONTENT",
                "2026-06-27T10:00-03:00", "medio",
                Map.of("before", Map.of("status", "DRAFT"), "after", Map.of("status", "PUBLISHED")),
                "trace-1", "127.0.0.1", "JUnit");
    }
}
