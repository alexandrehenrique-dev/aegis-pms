package br.com.byop.aegis.identity.auth.controller;

import br.com.byop.aegis.identity.auth.dto.AuthMessageResponse;
import br.com.byop.aegis.identity.auth.dto.AuthTokenResponse;
import br.com.byop.aegis.identity.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = OAuth2ResourceServerWebSecurityAutoConfiguration.class
)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void shouldLogin() throws Exception {

        when(authService.login("loki", "123456"))
                .thenReturn(new AuthTokenResponse(
                        "access-token",
                        "refresh-token",
                        300L,
                        "Bearer"
                ));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "loki",
                                  "password": "123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.expiresIn").value(300))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));

        verify(authService).login("loki", "123456");
        verifyNoMoreInteractions(authService);
    }

    @Test
    void shouldRejectLoginWithoutUsername() throws Exception {

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "",
                                  "password": "123456"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRefresh() throws Exception {

        when(authService.refresh("refresh-token"))
                .thenReturn(new AuthTokenResponse(
                        "new-access-token",
                        "new-refresh-token",
                        300L,
                        "Bearer"
                ));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "refresh-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
                .andExpect(jsonPath("$.expiresIn").value(300))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));

        verify(authService).refresh("refresh-token");
        verifyNoMoreInteractions(authService);
    }

    @Test
    void shouldRejectRefreshWithoutRefreshToken() throws Exception {

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldLogout() throws Exception {

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "refresh-token"
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(authService).logout("refresh-token");
        verifyNoMoreInteractions(authService);
    }

    @Test
    void shouldRejectLogoutWithoutRefreshToken() throws Exception {

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldForgotPassword() throws Exception {

        when(authService.forgotPassword("loki@byop.dev"))
                .thenReturn(new AuthMessageResponse(
                        "Se o e-mail estiver cadastrado, você receberá as instruções em breve."
                ));

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "loki@byop.dev"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                        "Se o e-mail estiver cadastrado, você receberá as instruções em breve."
                ));

        verify(authService).forgotPassword("loki@byop.dev");
        verifyNoMoreInteractions(authService);
    }

    @Test
    void shouldRejectForgotPasswordWithInvalidEmail() throws Exception {

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "invalid-email"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}