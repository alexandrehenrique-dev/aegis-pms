package br.com.byop.aegis.identity.auth.controller;

import br.com.byop.aegis.identity.auth.dto.AuthMessageResponse;
import br.com.byop.aegis.identity.auth.dto.AuthTokenResponse;
import br.com.byop.aegis.identity.auth.dto.AuthInviteValidationResponse;
import br.com.byop.aegis.identity.auth.exception.AuthActionTokenExpiredException;
import br.com.byop.aegis.identity.auth.exception.AuthActionTokenNotFoundException;
import br.com.byop.aegis.identity.auth.exception.AuthActionTokenUsedException;
import br.com.byop.aegis.identity.auth.exception.AuthRateLimitExceededException;
import br.com.byop.aegis.identity.auth.exception.WeakPasswordException;
import br.com.byop.aegis.identity.auth.service.AuthActivationService;
import br.com.byop.aegis.identity.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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

    @MockitoBean
    private AuthActivationService authActivationService;

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

    @Test
    void shouldValidateInvite() throws Exception {
        UUID token = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        when(authActivationService.validateInvite(token))
                .thenReturn(new AuthInviteValidationResponse(
                        "Guest",
                        "guest@byop.dev",
                        "BYOP",
                        List.of("Aegis"),
                        null,
                        "EDITOR",
                        "Admin",
                        Instant.parse("2026-07-05T12:00:00Z"),
                        true
                ));

        mockMvc.perform(post("/api/v1/auth/invite/validate")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("Guest"))
                .andExpect(jsonPath("$.productNames[0]").value("Aegis"));
    }

    @Test
    void shouldActivateInvite() throws Exception {
        UUID token = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        when(authActivationService.activate(token, "Senha123", "Alexandre", "Henrique"))
                .thenReturn(new AuthMessageResponse("Conta ativada. Faça login para continuar."));

        mockMvc.perform(post("/api/v1/auth/activate")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                                  "password": "Senha123",
                                  "firstName": "Alexandre",
                                  "lastName": "Henrique"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Conta ativada. Faça login para continuar."));
    }

    @Test
    void shouldRejectActivateWithoutFirstNameOrLastName() throws Exception {
        mockMvc.perform(post("/api/v1/auth/activate")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                                  "password": "Senha123",
                                  "firstName": "",
                                  "lastName": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAcceptExistingUserInvite() throws Exception {
        UUID token = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        when(authActivationService.acceptExistingUser(token))
                .thenReturn(new AuthMessageResponse("Convite aceito. Faça login para acessar o produto."));

        mockMvc.perform(post("/api/v1/auth/invite/accept-existing")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Convite aceito. Faça login para acessar o produto."));
    }

    @Test
    void shouldRequestPasswordReset() throws Exception {
        when(authActivationService.requestPasswordReset("user@byop.dev"))
                .thenReturn(new AuthMessageResponse(
                        "Se este e-mail existe na plataforma, um link de recuperação será enviado."
                ));

        mockMvc.perform(post("/api/v1/auth/reset-password/request")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@byop.dev"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                        "Se este e-mail existe na plataforma, um link de recuperação será enviado."
                ));
    }

    @Test
    void shouldConfirmPasswordReset() throws Exception {
        UUID token = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        when(authActivationService.confirmPasswordReset(token, "NovaSenha456"))
                .thenReturn(new AuthMessageResponse("Senha redefinida. Faça login para continuar."));

        mockMvc.perform(post("/api/v1/auth/reset-password/confirm")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                                  "password": "NovaSenha456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Senha redefinida. Faça login para continuar."));
    }

    @Test
    void shouldMapActionErrors() throws Exception {
        UUID token = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID usedToken = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        UUID missingToken = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        when(authActivationService.validateInvite(token)).thenThrow(new AuthActionTokenExpiredException());
        when(authActivationService.validateInvite(usedToken)).thenThrow(new AuthActionTokenUsedException());
        when(authActivationService.validateInvite(missingToken)).thenThrow(new AuthActionTokenNotFoundException());
        when(authActivationService.activate(token, "fraca", "Alexandre", "Henrique"))
                .thenThrow(new WeakPasswordException("A senha deve ter ao menos 8 caracteres, incluindo letras e números."));
        when(authActivationService.requestPasswordReset("user@byop.dev"))
                .thenThrow(new AuthRateLimitExceededException(900));

        mockMvc.perform(post("/api/v1/auth/invite/validate")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
                                }
                                """))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.error").value("TOKEN_EXPIRED"));

        mockMvc.perform(post("/api/v1/auth/invite/validate")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("TOKEN_ALREADY_USED"));

        mockMvc.perform(post("/api/v1/auth/invite/validate")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "cccccccc-cccc-cccc-cccc-cccccccccccc"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("TOKEN_NOT_FOUND"));

        mockMvc.perform(post("/api/v1/auth/activate")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                                  "password": "fraca",
                                  "firstName": "Alexandre",
                                  "lastName": "Henrique"
                                }
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error").value("WEAK_PASSWORD"));

        mockMvc.perform(post("/api/v1/auth/reset-password/request")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@byop.dev"
                                }
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "900"))
                .andExpect(jsonPath("$.error").value("TOO_MANY_REQUESTS"));
    }
}
