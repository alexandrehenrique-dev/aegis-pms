package br.com.byop.aegis.notification.controller;

import br.com.byop.aegis.core.CoreExceptionHandler;
import br.com.byop.aegis.notification.dto.NotificationResponse;
import br.com.byop.aegis.notification.dto.NotificationWithStatus;
import br.com.byop.aegis.notification.exception.InsufficientNotificationRoleException;
import br.com.byop.aegis.notification.exception.NotificationExceptionHandler;
import br.com.byop.aegis.notification.exception.NotificationNotFoundException;
import br.com.byop.aegis.notification.service.NotificationService;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NotificationController.class)
@Import({
        SecurityConfig.class,
        CoreExceptionHandler.class,
        NotificationExceptionHandler.class
})
class NotificationControllerTest {

    private static final UUID NOTIFICATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-07-02T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private AuthenticatedUserProvider authenticatedUserProvider;

    @Test
    void shouldListMine() throws Exception {
        AuthenticatedUser caller = user("user-1", "ROLE_EDITOR");
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(notificationService.listMine(caller)).thenReturn(List.of(withStatus()));

        mockMvc.perform(get("/api/v1/notifications/mine").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(NOTIFICATION_ID.toString()))
                .andExpect(jsonPath("$[0].type").value("FEATURE"));
    }

    @Test
    void shouldReturnPendingModal() throws Exception {
        AuthenticatedUser caller = user("user-1", "ROLE_EDITOR");
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(notificationService.getPendingModal(caller)).thenReturn(withStatus());

        mockMvc.perform(get("/api/v1/notifications/mine/pending-modal").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.presentationMode").value("MODAL_ONCE"));
    }

    @Test
    void shouldReturnNoContentWhenNoPendingModalExists() throws Exception {
        AuthenticatedUser caller = user("user-1", "ROLE_EDITOR");
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(notificationService.getPendingModal(caller)).thenReturn(null);

        mockMvc.perform(get("/api/v1/notifications/mine/pending-modal").with(jwt()))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldMarkShownAndRead() throws Exception {
        AuthenticatedUser caller = user("user-1", "ROLE_EDITOR");
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);

        mockMvc.perform(post("/api/v1/notifications/{notificationId}/mark-shown", NOTIFICATION_ID).with(jwt()))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/notifications/{notificationId}/mark-read", NOTIFICATION_ID).with(jwt()))
                .andExpect(status().isNoContent());

        verify(notificationService).markShown(caller, NOTIFICATION_ID);
        verify(notificationService).markRead(caller, NOTIFICATION_ID);
    }

    @Test
    void shouldReturnNotFoundWhenNotificationDoesNotBelongToCaller() throws Exception {
        AuthenticatedUser caller = user("user-1", "ROLE_EDITOR");
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        doThrow(new NotificationNotFoundException())
                .when(notificationService).markRead(caller, NOTIFICATION_ID);

        mockMvc.perform(post("/api/v1/notifications/{notificationId}/mark-read", NOTIFICATION_ID).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOTIFICATION_NOT_FOUND"));
    }

    @Test
    void shouldCreateNotificationAsSuperAdmin() throws Exception {
        AuthenticatedUser caller = user("admin", "ROLE_SUPER_ADMIN");
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(notificationService.create(eq(caller), any())).thenReturn(response());

        mockMvc.perform(post("/api/v1/notifications")
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "FEATURE",
                                  "title": "Nova feature",
                                  "bodyMarkdown": "<p>body</p>",
                                  "presentationMode": "MODAL_ONCE",
                                  "target": { "type": "ALL" }
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(NOTIFICATION_ID.toString()));
    }

    @Test
    void shouldRejectCreateAsNonSuperAdmin() throws Exception {
        AuthenticatedUser caller = user("editor", "ROLE_EDITOR");
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(notificationService.create(eq(caller), any())).thenThrow(new InsufficientNotificationRoleException());

        mockMvc.perform(post("/api/v1/notifications")
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "GENERAL",
                                  "title": "x",
                                  "bodyMarkdown": "x",
                                  "presentationMode": "BELL_ONLY",
                                  "target": { "type": "ALL" }
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("NOTIFICATION_ADMIN_FORBIDDEN"));
    }

    @Test
    void shouldListAdminNotificationsAndRejectNonSuperAdmin() throws Exception {
        AuthenticatedUser admin = user("admin", "ROLE_SUPER_ADMIN");
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(admin);
        when(notificationService.listAll(admin)).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/v1/notifications").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].createdBySubject").value("admin"));

        AuthenticatedUser editor = user("editor", "ROLE_EDITOR");
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(editor);
        when(notificationService.listAll(editor)).thenThrow(new InsufficientNotificationRoleException());

        mockMvc.perform(get("/api/v1/notifications").with(jwt()))
                .andExpect(status().isForbidden());
    }

    private NotificationWithStatus withStatus() {
        return new NotificationWithStatus(NOTIFICATION_ID, "FEATURE", "Nova feature", "<p>body</p>",
                "MODAL_ONCE", CREATED_AT, false, false, null);
    }

    private NotificationResponse response() {
        return new NotificationResponse(NOTIFICATION_ID, "FEATURE", "Nova feature", "<p>body</p>",
                "MODAL_ONCE", "admin", CREATED_AT, CREATED_AT);
    }

    private AuthenticatedUser user(String subject, String role) {
        return new AuthenticatedUser(subject, subject + "@byop.dev", subject, subject, Set.of(role));
    }
}
