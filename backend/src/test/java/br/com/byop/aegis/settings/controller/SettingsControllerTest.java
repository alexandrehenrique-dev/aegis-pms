package br.com.byop.aegis.settings.controller;

import br.com.byop.aegis.core.CoreExceptionHandler;
import br.com.byop.aegis.security.AuthenticatedUserProvider;
import br.com.byop.aegis.security.SecurityConfig;
import br.com.byop.aegis.settings.dto.RoleMatrixEntry;
import br.com.byop.aegis.settings.dto.SettingCard;
import br.com.byop.aegis.settings.exception.InsufficientSettingsRoleException;
import br.com.byop.aegis.settings.exception.InvalidSettingsRoleException;
import br.com.byop.aegis.settings.exception.SettingsExceptionHandler;
import br.com.byop.aegis.settings.exception.SettingsNotFoundException;
import br.com.byop.aegis.settings.service.SettingsService;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SettingsController.class)
@Import({
        SecurityConfig.class,
        CoreExceptionHandler.class,
        SettingsExceptionHandler.class,
        AuthenticatedUserProvider.class
})
class SettingsControllerTest {

    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TENANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SettingsService settingsService;

    @Test
    void shouldGetOverview() throws Exception {
        when(settingsService.getOverview(any(), eq(PRODUCT_ID))).thenReturn(List.of(
                new SettingCard("Produto", "desc", "configurado", "—", "—", "medio")
        ));

        mockMvc.perform(get("/api/v1/products/{productId}/settings/overview", PRODUCT_ID).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Produto"));
    }

    @Test
    void shouldReturnNotFoundWhenProductOutOfScopeForOverview() throws Exception {
        when(settingsService.getOverview(any(), eq(PRODUCT_ID))).thenThrow(new SettingsNotFoundException());

        mockMvc.perform(get("/api/v1/products/{productId}/settings/overview", PRODUCT_ID).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("SETTINGS_NOT_FOUND"));
    }

    @Test
    void shouldReturnForbiddenWhenRoleCannotSeeSettings() throws Exception {
        when(settingsService.getOverview(any(), eq(PRODUCT_ID))).thenThrow(new InsufficientSettingsRoleException());

        mockMvc.perform(get("/api/v1/products/{productId}/settings/overview", PRODUCT_ID).with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("SETTINGS_FORBIDDEN"));
    }

    @Test
    void shouldUpdateProductSettings() throws Exception {
        when(settingsService.updateProductSettings(any(), eq(PRODUCT_ID), any())).thenReturn(List.of());

        mockMvc.perform(put("/api/v1/products/{productId}/settings", PRODUCT_ID)
                        .with(jwt())
                        .contentType("application/json")
                        .content("{\"name\":\"Novo Nome\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectUpdateProductSettingsWithBlankName() throws Exception {
        mockMvc.perform(put("/api/v1/products/{productId}/settings", PRODUCT_ID)
                        .with(jwt())
                        .contentType("application/json")
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("REQUEST_VALIDATION_FAILED"));
    }

    @Test
    void shouldGetRoles() throws Exception {
        when(settingsService.getRoleMatrix(any(), eq(TENANT_ID))).thenReturn(List.of(
                new RoleMatrixEntry("TENANT_ADMIN", Map.of("/settings/roles", true))
        ));

        mockMvc.perform(get("/api/v1/tenants/{tenantId}/roles", TENANT_ID).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("TENANT_ADMIN"))
                .andExpect(jsonPath("$[0].permissions.['/settings/roles']").value(true));
    }

    @Test
    void shouldUpdateRoles() throws Exception {
        when(settingsService.updateRoleMatrix(any(), eq(TENANT_ID), any())).thenReturn(List.of());

        mockMvc.perform(put("/api/v1/tenants/{tenantId}/roles", TENANT_ID)
                        .with(jwt())
                        .contentType("application/json")
                        .content("[{\"role\":\"EDITOR\",\"permissions\":{\"/content\":true}}]"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRestoreDefaultRoles() throws Exception {
        when(settingsService.restoreDefaultPermissions(any(), eq(TENANT_ID))).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/tenants/{tenantId}/roles/restore-defaults", TENANT_ID).with(jwt()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldGetPermissionMatrix() throws Exception {
        when(settingsService.getRoleMatrix(any(), eq(TENANT_ID))).thenReturn(List.of(
                new RoleMatrixEntry("VIEWER", Map.of("/content", true))
        ));

        mockMvc.perform(get("/api/v1/tenants/{tenantId}/permission-matrix", TENANT_ID).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("VIEWER"));
    }

    @Test
    void shouldPreviewPermissionMatrix() throws Exception {
        when(settingsService.previewPermissionMatrix(any(), eq(TENANT_ID), any()))
                .thenReturn(new RoleMatrixEntry("EDITOR", Map.of("/content", true)));

        mockMvc.perform(post("/api/v1/tenants/{tenantId}/permission-matrix/preview", TENANT_ID)
                        .with(jwt())
                        .contentType("application/json")
                        .content("{\"role\":\"EDITOR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("EDITOR"));
    }

    @Test
    void shouldReturnBadRequestForInvalidRoleOnPreview() throws Exception {
        when(settingsService.previewPermissionMatrix(any(), eq(TENANT_ID), any()))
                .thenThrow(new InvalidSettingsRoleException("INTERN"));

        mockMvc.perform(post("/api/v1/tenants/{tenantId}/permission-matrix/preview", TENANT_ID)
                        .with(jwt())
                        .contentType("application/json")
                        .content("{\"role\":\"INTERN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_SETTINGS_ROLE"));
    }

    @Test
    void shouldRestoreDefaultPermissionMatrix() throws Exception {
        when(settingsService.restoreDefaultPermissions(any(), eq(TENANT_ID))).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/tenants/{tenantId}/permission-matrix/restore-defaults", TENANT_ID).with(jwt()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/products/{productId}/settings/overview", PRODUCT_ID))
                .andExpect(status().isUnauthorized());
    }
}
