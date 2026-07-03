package br.com.byop.aegis.feedback.controller;

import br.com.byop.aegis.core.CoreExceptionHandler;
import br.com.byop.aegis.feedback.dto.FeedbackSummary;
import br.com.byop.aegis.feedback.exception.FeedbackAttachmentNotFoundException;
import br.com.byop.aegis.feedback.exception.FeedbackContextRequiredException;
import br.com.byop.aegis.feedback.exception.FeedbackExceptionHandler;
import br.com.byop.aegis.feedback.exception.FeedbackForbiddenException;
import br.com.byop.aegis.feedback.exception.FeedbackNotFoundException;
import br.com.byop.aegis.feedback.service.FeedbackService;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.security.AuthenticatedUserProvider;
import br.com.byop.aegis.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FeedbackController.class)
@Import({
        SecurityConfig.class,
        CoreExceptionHandler.class,
        FeedbackExceptionHandler.class
})
class FeedbackControllerTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PRODUCT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ASSET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-07-03T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeedbackService feedbackService;

    @MockitoBean
    private AuthenticatedUserProvider authenticatedUserProvider;

    @Test
    void shouldCreateFeedback() throws Exception {
        AuthenticatedUser caller = user("editor", "ROLE_EDITOR");
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(feedbackService.create(eq(caller), any())).thenReturn(summary("AGS-0042"));

        mockMvc.perform(post("/api/v1/feedback")
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "22222222-2222-2222-2222-222222222222",
                                  "category": "Bug",
                                  "priority": "alta",
                                  "description": "Botao X nao responde ao clicar.",
                                  "screenName": "/content/list",
                                  "attachmentAssetId": "33333333-3333-3333-3333-333333333333"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("AGS-0042"))
                .andExpect(jsonPath("$.productId").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.attachmentAssetId").value(ASSET_ID.toString()));
    }

    @Test
    void shouldRejectCreateFeedbackWithInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/v1/feedback")
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"category":"","priority":"alta","description":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectCreateFeedbackWhenContextIsAmbiguous() throws Exception {
        AuthenticatedUser caller = user("editor", "ROLE_EDITOR");
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(feedbackService.create(eq(caller), any())).thenThrow(new FeedbackContextRequiredException());

        mockMvc.perform(post("/api/v1/feedback")
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"category":"Bug","priority":"alta","description":"Descricao"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("FEEDBACK_CONTEXT_REQUIRED"));
    }

    @Test
    void shouldRejectCreateFeedbackWithCrossTenantAttachment() throws Exception {
        AuthenticatedUser caller = user("editor", "ROLE_EDITOR");
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(feedbackService.create(eq(caller), any())).thenThrow(new FeedbackAttachmentNotFoundException(ASSET_ID));

        mockMvc.perform(post("/api/v1/feedback")
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "22222222-2222-2222-2222-222222222222",
                                  "category": "Bug",
                                  "priority": "alta",
                                  "description": "Descricao",
                                  "attachmentAssetId": "33333333-3333-3333-3333-333333333333"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("FEEDBACK_ATTACHMENT_NOT_FOUND"));
    }

    @Test
    void shouldListGlobalFeedback() throws Exception {
        AuthenticatedUser caller = user("admin", "ROLE_SUPER_ADMIN");
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(feedbackService.listAll(caller)).thenReturn(List.of(summary("AGS-0001")));

        mockMvc.perform(get("/api/v1/feedback").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("AGS-0001"));
    }

    @Test
    void shouldBlockGlobalFeedbackForNonSuperAdmin() throws Exception {
        AuthenticatedUser caller = user("editor", "ROLE_EDITOR");
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(feedbackService.listAll(caller)).thenThrow(new FeedbackForbiddenException());

        mockMvc.perform(get("/api/v1/feedback").with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FEEDBACK_FORBIDDEN"));
    }

    @Test
    void shouldListTenantFeedback() throws Exception {
        AuthenticatedUser caller = user("tenant-admin", "ROLE_TENANT_ADMIN");
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(feedbackService.listByTenant(caller, TENANT_ID)).thenReturn(List.of(summary("AGS-0002")));

        mockMvc.perform(get("/api/v1/tenants/{tenantId}/feedback", TENANT_ID).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tenantId").value(TENANT_ID.toString()));
    }

    @Test
    void shouldReturnNotFoundForTenantOutsideScope() throws Exception {
        AuthenticatedUser caller = user("tenant-admin", "ROLE_TENANT_ADMIN");
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(feedbackService.listByTenant(caller, TENANT_ID)).thenThrow(new FeedbackNotFoundException(TENANT_ID.toString()));

        mockMvc.perform(get("/api/v1/tenants/{tenantId}/feedback", TENANT_ID).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("FEEDBACK_NOT_FOUND"));
    }

    @Test
    void shouldUpdateStatus() throws Exception {
        AuthenticatedUser caller = user("admin", "ROLE_SUPER_ADMIN");
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(feedbackService.updateStatus(caller, "AGS-0042", "resolvido")).thenReturn(summary("AGS-0042"));

        mockMvc.perform(put("/api/v1/feedback/{feedbackId}/status", "AGS-0042")
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"status":"resolvido"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("AGS-0042"));

        verify(feedbackService).updateStatus(caller, "AGS-0042", "resolvido");
    }

    @Test
    void shouldRejectUpdateStatusForNonSuperAdminAndInvalidPayload() throws Exception {
        AuthenticatedUser caller = user("editor", "ROLE_EDITOR");
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(feedbackService.updateStatus(caller, "AGS-0042", "resolvido"))
                .thenThrow(new FeedbackForbiddenException());

        mockMvc.perform(put("/api/v1/feedback/{feedbackId}/status", "AGS-0042")
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"status":"resolvido"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/feedback/{feedbackId}/status", "AGS-0042")
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"status":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidStatusValue() throws Exception {
        AuthenticatedUser caller = user("admin", "ROLE_SUPER_ADMIN");
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(feedbackService.updateStatus(caller, "AGS-0042", "invalid"))
                .thenThrow(new IllegalArgumentException("invalid"));

        mockMvc.perform(put("/api/v1/feedback/{feedbackId}/status", "AGS-0042")
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"status":"invalid"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_FEEDBACK_REQUEST"));
    }

    private FeedbackSummary summary(String id) {
        return new FeedbackSummary(id, "Bug", "alta", "Descricao", "aberto", "editor", TENANT_ID, PRODUCT_ID,
                CREATED_AT, ASSET_ID);
    }

    private AuthenticatedUser user(String subject, String role) {
        return new AuthenticatedUser(subject, subject + "@byop.dev", subject, subject, Set.of(role));
    }
}
