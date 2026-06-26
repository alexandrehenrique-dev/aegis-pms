package br.com.byop.aegis.tenant.controller;

import br.com.byop.aegis.core.CoreExceptionHandler;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.security.AuthenticatedUserProvider;
import br.com.byop.aegis.security.SecurityConfig;
import br.com.byop.aegis.tenant.dto.TenantSummary;
import br.com.byop.aegis.tenant.exception.InvalidTenantConfirmationException;
import br.com.byop.aegis.tenant.exception.TenantAlreadyExistsException;
import br.com.byop.aegis.tenant.exception.TenantExceptionHandler;
import br.com.byop.aegis.tenant.exception.TenantNotFoundException;
import br.com.byop.aegis.tenant.service.TenantService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TenantController.class)
@Import({
        SecurityConfig.class,
        CoreExceptionHandler.class,
        TenantExceptionHandler.class
})
class TenantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantService tenantService;

    @MockitoBean
    private AuthenticatedUserProvider authenticatedUserProvider;

    @Test
    void shouldCreateTenant() throws Exception {
        AuthenticatedUser caller = user();
        TenantSummary summary = tenantSummary();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(tenantService.createTenant(any(AuthenticatedUser.class), any())).thenReturn(summary);

        mockMvc.perform(post("/api/v1/tenants")
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "key": "byop",
                                  "name": "BYOP",
                                  "plan": "PRO",
                                  "initialAdminEmail": "admin@byop.dev"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(summary.id().toString()))
                .andExpect(jsonPath("$.key").value("byop"))
                .andExpect(jsonPath("$.name").value("BYOP"))
                .andExpect(jsonPath("$.status").value("ativo"))
                .andExpect(jsonPath("$.plan").value("FREE"));
    }

    @Test
    void shouldCreateTenantWithPlanAndInitialAdminEmail() throws Exception {
        AuthenticatedUser caller = user();
        TenantSummary summary = tenantSummary();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(tenantService.createTenant(any(AuthenticatedUser.class), any())).thenReturn(summary);

        mockMvc.perform(post("/api/v1/tenants")
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "key": "byop",
                                  "name": "BYOP",
                                  "plan": "PRO",
                                  "initialAdminEmail": "admin@byop.dev"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(summary.id().toString()))
                .andExpect(jsonPath("$.plan").value("FREE"));
    }

    @Test
    void shouldListTenants() throws Exception {
        AuthenticatedUser caller = user();
        TenantSummary summary = tenantSummary();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(tenantService.listTenants(caller)).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/v1/tenants")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(summary.id().toString()))
                .andExpect(jsonPath("$[0].key").value("byop"));
    }

    @Test
    void shouldDelegateTenantListScopeToService() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(tenantService.listTenants(caller)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/tenants")
                        .with(jwt()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldUpdateTenant() throws Exception {
        UUID tenantId = UUID.fromString("12121212-1212-1212-1212-121212121212");
        TenantSummary summary = tenantSummary();
        when(tenantService.updateTenant(any(UUID.class), any())).thenReturn(summary);

        mockMvc.perform(put("/api/v1/tenants/{tenantId}", tenantId)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "BYOP Updated",
                                  "plan": "PRO",
                                  "status": "ativo"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(summary.id().toString()))
                .andExpect(jsonPath("$.key").value("byop"))
                .andExpect(jsonPath("$.status").value("ativo"));
    }

    @Test
    void shouldDeleteTenant() throws Exception {
        AuthenticatedUser caller = user();
        UUID tenantId = UUID.fromString("12121212-1212-1212-1212-121212121212");
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);

        mockMvc.perform(delete("/api/v1/tenants/{tenantId}", tenantId)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "confirmationText": "BYOP"
                                }
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturnBadRequestWhenTenantDeleteConfirmationIsWrong() throws Exception {
        AuthenticatedUser caller = user();
        UUID tenantId = UUID.fromString("12121212-1212-1212-1212-121212121212");
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        org.mockito.Mockito.doThrow(new InvalidTenantConfirmationException())
                .when(tenantService)
                .deleteTenant(any(AuthenticatedUser.class), any(UUID.class), any());

        mockMvc.perform(delete("/api/v1/tenants/{tenantId}", tenantId)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "confirmationText": "Wrong"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_TENANT_CONFIRMATION"));
    }

    @Test
    void shouldRejectInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/v1/tenants")
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "key": "",
                                  "name": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnConflictWhenTenantKeyAlreadyExists() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(tenantService.createTenant(any(AuthenticatedUser.class), any()))
                .thenThrow(new TenantAlreadyExistsException("byop"));

        mockMvc.perform(post("/api/v1/tenants")
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "key": "byop",
                                  "name": "BYOP"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("TENANT_ALREADY_EXISTS"));
    }

    @Test
    void shouldReturnNotFoundWhenTenantDoesNotExist() throws Exception {
        AuthenticatedUser caller = user();
        UUID tenantId = UUID.fromString("12121212-1212-1212-1212-121212121212");
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(tenantService.listTenants(caller)).thenThrow(new TenantNotFoundException(tenantId));

        mockMvc.perform(get("/api/v1/tenants")
                        .with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("TENANT_NOT_FOUND"));
    }

    private TenantSummary tenantSummary() {
        return new TenantSummary(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "byop",
                "BYOP",
                "ativo",
                "FREE",
                OffsetDateTime.parse("2026-06-25T10:00:00-03:00"),
                OffsetDateTime.parse("2026-06-25T10:10:00-03:00")
        );
    }

    private AuthenticatedUser user() {
        return new AuthenticatedUser(
                "subject-123",
                "subject@byop.dev",
                "subject",
                "Subject",
                Set.of("ROLE_SUPER_ADMIN")
        );
    }
}
