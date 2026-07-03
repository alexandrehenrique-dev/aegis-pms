package br.com.byop.aegis.settings.controller;

import br.com.byop.aegis.core.CoreExceptionHandler;
import br.com.byop.aegis.security.AuthenticatedUserProvider;
import br.com.byop.aegis.security.SecurityConfig;
import br.com.byop.aegis.settings.dto.ProductSecuritySettingsResponse;
import br.com.byop.aegis.settings.exception.InsufficientSettingsRoleException;
import br.com.byop.aegis.settings.exception.SettingsExceptionHandler;
import br.com.byop.aegis.settings.exception.SettingsNotFoundException;
import br.com.byop.aegis.settings.service.ProductSecuritySettingsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProductSecuritySettingsController.class)
@Import({
        SecurityConfig.class,
        CoreExceptionHandler.class,
        SettingsExceptionHandler.class,
        AuthenticatedUserProvider.class
})
class ProductSecuritySettingsControllerTest {

    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductSecuritySettingsService settingsService;

    @Test
    void shouldGetSecuritySettingsWithoutWebhookSecret() throws Exception {
        when(settingsService.getSecuritySettings(any(), eq(PRODUCT_ID))).thenReturn(response());

        mockMvc.perform(get("/api/v1/products/{productId}/settings/security", PRODUCT_ID).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.webhookUrl").value("https://example.com/hook"))
                .andExpect(jsonPath("$.webhookStatus").value("connected"))
                .andExpect(jsonPath("$.webhookSecret").doesNotExist())
                .andExpect(jsonPath("$.*", not(org.hamcrest.Matchers.hasItem("secret"))));
    }

    @Test
    void shouldUpdateSecuritySettings() throws Exception {
        when(settingsService.updateSecuritySettings(any(), eq(PRODUCT_ID), any())).thenReturn(response());

        mockMvc.perform(put("/api/v1/products/{productId}/settings/security", PRODUCT_ID)
                        .with(jwt())
                        .contentType("application/json")
                        .content("""
                                {
                                  "webhookUrl": "https://example.com/hook",
                                  "webhookSecret": "secret",
                                  "analyticsEnabled": true,
                                  "analyticsProviderKey": "ga-key",
                                  "emailDeliveryEnabled": true,
                                  "telegramAlert": {
                                    "chatId": "123",
                                    "botToken": "secret-token"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analyticsEnabled").value(true))
                .andExpect(jsonPath("$.telegramAlert.chatId").value("123"))
                .andExpect(jsonPath("$.telegramAlert.botTokenMasked").value("****1234"))
                .andExpect(jsonPath("$.*", not(org.hamcrest.Matchers.hasItem("secret-token"))))
                .andExpect(jsonPath("$.webhookSecret").doesNotExist());
    }

    @Test
    void shouldReturnForbiddenForEditor() throws Exception {
        when(settingsService.getSecuritySettings(any(), eq(PRODUCT_ID)))
                .thenThrow(new InsufficientSettingsRoleException());

        mockMvc.perform(get("/api/v1/products/{productId}/settings/security", PRODUCT_ID).with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("SETTINGS_FORBIDDEN"));
    }

    @Test
    void shouldReturnNotFoundWhenProductOutOfScope() throws Exception {
        when(settingsService.getSecuritySettings(any(), eq(PRODUCT_ID))).thenThrow(new SettingsNotFoundException());

        mockMvc.perform(get("/api/v1/products/{productId}/settings/security", PRODUCT_ID).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("SETTINGS_NOT_FOUND"));
    }

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/products/{productId}/settings/security", PRODUCT_ID))
                .andExpect(status().isUnauthorized());
    }

    private ProductSecuritySettingsResponse response() {
        return new ProductSecuritySettingsResponse("https://example.com/hook", true, "ga-key", true,
                OffsetDateTime.parse("2026-06-29T12:00:00Z"), "connected", "connected", "connected",
                new ProductSecuritySettingsResponse.TelegramAlertResponse("123", "****1234"));
    }
}
