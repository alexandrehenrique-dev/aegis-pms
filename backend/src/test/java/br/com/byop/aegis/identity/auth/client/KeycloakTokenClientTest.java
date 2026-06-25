package br.com.byop.aegis.identity.auth.client;

import br.com.byop.aegis.identity.auth.config.KeycloakProperties;
import br.com.byop.aegis.identity.auth.exception.AccountDisabledException;
import br.com.byop.aegis.identity.auth.exception.InvalidCredentialsException;
import br.com.byop.aegis.identity.auth.exception.KeycloakAuthenticationException;
import br.com.byop.aegis.identity.auth.exception.RefreshTokenExpiredException;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.client.RestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

class KeycloakTokenClientTest {

    private static WireMockServer wireMockServer;

    private KeycloakTokenClient client;

    @BeforeAll
    static void beforeAll() {
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
    }

    @AfterAll
    static void afterAll() {
        wireMockServer.stop();
    }

    @BeforeEach
    void setup() {

        KeycloakProperties properties =
                new KeycloakProperties(
                        "issuer",
                        wireMockServer.baseUrl(),
                        "/admin/realms/",
                        "aegis",
                        "aegis-web",
                        "admin-cli",
                        "admin",
                        "admin"
                );

        client = new KeycloakTokenClient(
                RestClient.builder().build(),
                properties
        );
    }

    @Test
    void shouldLoginSuccessfully() {

        wireMockServer.stubFor(
                post(urlEqualTo("/realms/aegis/protocol/openid-connect/token"))
                        .willReturn(okJson("""
                                {
                                  "access_token":"access",
                                  "refresh_token":"refresh",
                                  "expires_in":300,
                                  "token_type":"Bearer"
                                }
                                """))
        );

        KeycloakTokenResponse response =
                client.login("loki", "123");

        assertEquals("access", response.accessToken());
        assertEquals("refresh", response.refreshToken());
        assertEquals(300L, response.expiresIn());
    }

    @Test
    void shouldThrowInvalidCredentials() {
        stubUnauthorizedTokenEndpoint("""
                {
                  "error": "invalid_grant",
                  "error_description": "Invalid user credentials"
                }
                """);

        assertLoginThrows(InvalidCredentialsException.class);
    }

    @Test
    void shouldRefreshSuccessfully() {

        wireMockServer.stubFor(
                post(urlEqualTo("/realms/aegis/protocol/openid-connect/token"))
                        .willReturn(okJson("""
                                {
                                  "access_token":"new-access",
                                  "refresh_token":"new-refresh",
                                  "expires_in":300,
                                  "token_type":"Bearer"
                                }
                                """))
        );

        KeycloakTokenResponse response =
                client.refresh("refresh");

        assertEquals(
                "new-access",
                response.accessToken()
        );
    }

    @Test
    void shouldThrowRefreshExpired() {

        stubUnauthorizedTokenEndpoint();

        assertThrows(
                RefreshTokenExpiredException.class,
                () -> client.refresh("expired")
        );
    }

    @Test
    void shouldLogoutSuccessfully() {

        wireMockServer.stubFor(
                post(urlEqualTo("/realms/aegis/protocol/openid-connect/logout"))
                        .willReturn(noContent())
        );

        assertDoesNotThrow(
                () -> client.logout("refresh")
        );
    }

    @Test
    void shouldThrowServerError() {

        wireMockServer.stubFor(
                post(urlEqualTo("/realms/aegis/protocol/openid-connect/token"))
                        .willReturn(serverError())
        );

        assertThrows(
                KeycloakAuthenticationException.class,
                () -> client.login("loki", "123")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Account disabled",
            "User disabled",
            "disabled"
    })
    void shouldThrowAccountDisabled(String description) {
        wireMockServer.stubFor(
                post(urlEqualTo("/realms/aegis/protocol/openid-connect/token"))
                        .willReturn(unauthorized()
                                .withHeader("Content-Type", "application/json")
                                .withBody("""
                                    {
                                      "error": "invalid_grant",
                                      "error_description": "%s"
                                    }
                                    """.formatted(description)))
        );

        assertThrows(
                AccountDisabledException.class,
                () -> client.login("loki", "credential-value")
        );
    }

    @Test
    void shouldThrowInvalidCredentialsWhenUnauthorizedBodyIsBlank() {
        stubUnauthorizedTokenEndpoint("");

        assertLoginThrows(InvalidCredentialsException.class);
    }

    @Test
    void shouldThrowKeycloakAuthenticationExceptionOnRefreshServerError() {

        wireMockServer.stubFor(
                post(urlEqualTo("/realms/aegis/protocol/openid-connect/token"))
                        .willReturn(serverError())
        );

        assertThrows(
                KeycloakAuthenticationException.class,
                () -> client.refresh("refresh-token")
        );
    }

    @Test
    void shouldThrowKeycloakAuthenticationExceptionOnLogoutServerError() {

        wireMockServer.stubFor(
                post(urlEqualTo("/realms/aegis/protocol/openid-connect/logout"))
                        .willReturn(serverError())
        );

        assertThrows(
                KeycloakAuthenticationException.class,
                () -> client.logout("refresh-token")
        );
    }

    @Test
    void shouldThrowInvalidCredentialsWhenUnauthorizedBodyIsNull() {
        stubUnauthorizedTokenEndpoint();

        assertLoginThrows(InvalidCredentialsException.class);
    }

    private void stubUnauthorizedTokenEndpoint() {
        wireMockServer.stubFor(
                post(urlEqualTo("/realms/aegis/protocol/openid-connect/token"))
                        .willReturn(unauthorized())
        );
    }

    private void stubUnauthorizedTokenEndpoint(String body) {
        wireMockServer.stubFor(
                post(urlEqualTo("/realms/aegis/protocol/openid-connect/token"))
                        .willReturn(unauthorized()
                                .withHeader("Content-Type", "application/json")
                                .withBody(body))
        );
    }

    private <T extends Throwable> void assertLoginThrows(Class<T> exceptionType) {
        assertThrows(
                exceptionType,
                () -> client.login("loki", "wrong")
        );
    }
}
