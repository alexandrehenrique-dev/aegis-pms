package br.com.byop.aegis.product.user.controller;

import br.com.byop.aegis.core.CoreExceptionHandler;
import br.com.byop.aegis.product.user.dto.TenantUserSummary;
import br.com.byop.aegis.product.user.exception.LastTenantAdminException;
import br.com.byop.aegis.product.user.exception.TenantUserExceptionHandler;
import br.com.byop.aegis.product.user.service.TenantUserService;
import br.com.byop.aegis.security.AuthenticatedUserProvider;
import br.com.byop.aegis.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TenantUserController.class)
@Import({
        SecurityConfig.class,
        CoreExceptionHandler.class,
        TenantUserExceptionHandler.class,
        AuthenticatedUserProvider.class
})
class TenantUserControllerTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantUserService userService;

    @Test
    void shouldListTenantUsers() throws Exception {
        when(userService.listUsers(any(), eq(TENANT_ID))).thenReturn(List.of(summary("user-1", "ativo")));

        mockMvc.perform(get("/api/v1/tenants/{tenantId}/users", TENANT_ID).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value("user-1"));
    }

    @Test
    void shouldInviteTenantUser() throws Exception {
        when(userService.inviteUser(any(), eq(TENANT_ID), any())).thenReturn(summary("user-1", "convidado"));

        mockMvc.perform(post("/api/v1/tenants/{tenantId}/users/invite", TENANT_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "User One",
                                  "email": "user-1@byop.dev",
                                  "role": "EDITOR",
                                  "allowedProducts": "Aegis"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("convidado"));
    }

    @Test
    void shouldRejectInvalidInviteRequest() throws Exception {
        mockMvc.perform(post("/api/v1/tenants/{tenantId}/users/invite", TENANT_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "email": "invalid",
                                  "role": "",
                                  "allowedProducts": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("REQUEST_VALIDATION_FAILED"));
    }

    @Test
    void shouldGetUpdateResendBlockRemoveAndRestoreTenantUser() throws Exception {
        when(userService.getUser(any(), eq(TENANT_ID), eq("user-1"))).thenReturn(summary("user-1", "ativo"));
        when(userService.updateUser(any(), eq(TENANT_ID), eq("user-1"), any())).thenReturn(summary("user-1", "bloqueado"));
        when(userService.resendInvite(any(), eq(TENANT_ID), eq("user-1"))).thenReturn(summary("user-1", "convidado"));
        when(userService.blockUser(any(), eq(TENANT_ID), eq("user-1"))).thenReturn(summary("user-1", "bloqueado"));
        when(userService.restoreUser(any(), eq(TENANT_ID), eq("user-1"))).thenReturn(summary("user-1", "ativo"));

        mockMvc.perform(get("/api/v1/tenants/{tenantId}/users/{userId}", TENANT_ID, "user-1").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-1"));
        mockMvc.perform(put("/api/v1/tenants/{tenantId}/users/{userId}", TENANT_ID, "user-1")
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"role":"VIEWER","allowedProducts":"Aegis","status":"bloqueado"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("bloqueado"));
        mockMvc.perform(post("/api/v1/tenants/{tenantId}/users/{userId}/resend-invite", TENANT_ID, "user-1").with(jwt()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/tenants/{tenantId}/users/{userId}/block", TENANT_ID, "user-1").with(jwt()))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/tenants/{tenantId}/users/{userId}", TENANT_ID, "user-1").with(jwt()))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/tenants/{tenantId}/users/{userId}/restore", TENANT_ID, "user-1").with(jwt()))
                .andExpect(status().isOk());

        verify(userService).removeUser(any(), eq(TENANT_ID), eq("user-1"));
    }

    @Test
    void shouldMapLastAdminError() throws Exception {
        doThrow(new LastTenantAdminException())
                .when(userService)
                .removeUser(any(), eq(TENANT_ID), eq("user-1"));

        mockMvc.perform(delete("/api/v1/tenants/{tenantId}/users/{userId}", TENANT_ID, "user-1").with(jwt()))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.error").value("LAST_TENANT_ADMIN"));
    }

    private TenantUserSummary summary(String userId, String status) {
        return new TenantUserSummary(userId, "User One", userId + "@byop.dev", "editor", "Aegis", status, "nunca", "ativo");
    }
}
