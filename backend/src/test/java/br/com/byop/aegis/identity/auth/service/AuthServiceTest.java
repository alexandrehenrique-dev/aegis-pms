package br.com.byop.aegis.identity.auth.service;

import br.com.byop.aegis.identity.auth.client.KeycloakTokenClient;
import br.com.byop.aegis.identity.auth.client.KeycloakTokenResponse;
import br.com.byop.aegis.identity.auth.dto.AuthMessageResponse;
import br.com.byop.aegis.identity.auth.dto.AuthTokenResponse;
import br.com.byop.aegis.identity.auth.exception.InvalidCredentialsException;
import br.com.byop.aegis.identity.auth.exception.RefreshTokenExpiredException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private final AuthActivationService authActivationService =
            mock(AuthActivationService.class);
    private final KeycloakTokenClient keycloakTokenClient =
            mock(KeycloakTokenClient.class);

    private final AuthService service =
            new AuthService(keycloakTokenClient, authActivationService);

    @Test
    void shouldLogin() {

        when(keycloakTokenClient.login("loki", "123456"))
                .thenReturn(new KeycloakTokenResponse(
                        "access-token",
                        "refresh-token",
                        300L,
                        "Bearer"
                ));

        AuthTokenResponse response =
                service.login("loki", "123456");

        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals(300L, response.expiresIn());
        assertEquals("Bearer", response.tokenType());

        verify(keycloakTokenClient).login("loki", "123456");
        verifyNoMoreInteractions(keycloakTokenClient);
    }

    @Test
    void shouldLogWarnAndPropagateLoginFailure() {

        InvalidCredentialsException expected = new InvalidCredentialsException();
        when(keycloakTokenClient.login("loki", "wrong")).thenThrow(expected);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> service.login("loki", "wrong"));

        assertEquals(expected, thrown);
        verify(keycloakTokenClient).login("loki", "wrong");
    }

    @Test
    void shouldRefresh() {

        when(keycloakTokenClient.refresh("refresh-token"))
                .thenReturn(new KeycloakTokenResponse(
                        "new-access-token",
                        "new-refresh-token",
                        300L,
                        "Bearer"
                ));

        AuthTokenResponse response =
                service.refresh("refresh-token");

        assertEquals("new-access-token", response.accessToken());
        assertEquals("new-refresh-token", response.refreshToken());
        assertEquals(300L, response.expiresIn());
        assertEquals("Bearer", response.tokenType());

        verify(keycloakTokenClient).refresh("refresh-token");
        verifyNoMoreInteractions(keycloakTokenClient);
    }

    @Test
    void shouldLogWarnAndPropagateRefreshFailure() {

        RefreshTokenExpiredException expected = new RefreshTokenExpiredException();
        when(keycloakTokenClient.refresh("refresh-token")).thenThrow(expected);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> service.refresh("refresh-token"));

        assertEquals(expected, thrown);
        verify(keycloakTokenClient).refresh("refresh-token");
    }

    @Test
    void shouldLogout() {

        service.logout("refresh-token");

        verify(keycloakTokenClient).logout("refresh-token");
        verifyNoMoreInteractions(keycloakTokenClient);
    }

    @Test
    void shouldReturnGenericForgotPasswordMessage() {

        AuthMessageResponse response =
                service.forgotPassword("loki@byop.dev");

        assertEquals(
                "Se o e-mail estiver cadastrado, você receberá as instruções em breve.",
                response.message()
        );

        verify(authActivationService).requestPasswordReset("loki@byop.dev");
        verifyNoMoreInteractions(authActivationService);
        verifyNoInteractions(keycloakTokenClient);
    }
}
