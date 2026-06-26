package br.com.byop.aegis.identity.auth.client;

import br.com.byop.aegis.identity.auth.config.KeycloakProperties;
import br.com.byop.aegis.identity.auth.exception.KeycloakAuthenticationException;
import br.com.byop.aegis.identity.user.dto.UserResponse;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.*;
import org.springframework.web.client.RestClient;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.junit.jupiter.api.Assertions.*;

class KeycloakAdminClientTest {

    private static WireMockServer wireMockServer;

    private KeycloakAdminClient client;

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
        wireMockServer.resetAll();

        KeycloakProperties properties = new KeycloakProperties(
                "http://localhost:8282/realms/aegis",
                wireMockServer.baseUrl(),
                "/admin/realms/",
                "aegis",
                "aegis-web",
                "admin-cli",
                "admin",
                "admin"
        );

        client = new KeycloakAdminClient(
                RestClient.builder().build(),
                properties
        );
    }

    @Test
    void shouldSendResetPasswordEmail() {
        stubAdminToken();
        stubUserLookup("""
                [
                  {
                    "id": "user-id",
                    "username": "loki",
                    "email": "loki@teste.com",
                    "firstName": "loki",
                    "lastName": "de asgard",
                    "enabled": true
                  }
                ]
                """);
        stubExecuteActionsEmail();

        assertDoesNotThrow(() -> client.sendResetPasswordEmail("loki@teste.com"));

        wireMockServer.verify(postRequestedFor(urlEqualTo("/realms/master/protocol/openid-connect/token")));
        wireMockServer.verify(getRequestedFor(urlPathEqualTo("/admin/realms/aegis/users"))
                .withQueryParam("email", equalTo("loki@teste.com"))
                .withQueryParam("exact", equalTo("true")));
        wireMockServer.verify(putRequestedFor(urlEqualTo(
                "/admin/realms/aegis/users/user-id/execute-actions-email?client_id=aegis-web"
        )));
    }

    @Test
    void shouldDoNothingWhenUserDoesNotExist() {
        stubAdminToken();
        stubUserLookup("[]");

        assertDoesNotThrow(() -> client.sendResetPasswordEmail("missing@teste.com"));

        wireMockServer.verify(0, putRequestedFor(urlPathMatching("/execute-actions-email")));
    }

    @Test
    void shouldDoNothingWhenUserLookupReturnsNullBody() {
        stubAdminToken();

        wireMockServer.stubFor(get(urlPathEqualTo("/admin/realms/aegis/users"))
                .willReturn(ok()));

        assertDoesNotThrow(() -> client.sendResetPasswordEmail("missing@teste.com"));

        wireMockServer.verify(0, putRequestedFor(urlPathMatching("/execute-actions-email")));
    }

    @Test
    void shouldInviteExistingUserByEmail() {
        stubAdminToken();
        stubUserLookup("""
                [
                  {
                    "id": "user-id",
                    "username": "guest",
                    "email": "guest@byop.dev",
                    "firstName": "Guest",
                    "lastName": "User",
                    "enabled": true
                  }
                ]
                """);
        stubExecuteActionsEmail();

        UserResponse user = client.inviteUserByEmail("guest@byop.dev");

        assertEquals("user-id", user.id());
        assertEquals("guest@byop.dev", user.email());
        wireMockServer.verify(0, postRequestedFor(urlEqualTo("/admin/realms/aegis/users")));
        wireMockServer.verify(putRequestedFor(urlEqualTo(
                "/admin/realms/aegis/users/user-id/execute-actions-email?client_id=aegis-web"
        )).withRequestBody(equalToJson("[\"UPDATE_PASSWORD\"]")));
    }

    @Test
    void shouldCreateUserAndInviteWhenEmailDoesNotExist() {
        stubAdminToken();
        wireMockServer.stubFor(get(urlPathEqualTo("/admin/realms/aegis/users"))
                .withQueryParam("email", equalTo("guest@byop.dev"))
                .withQueryParam("exact", equalTo("true"))
                .inScenario("invite")
                .whenScenarioStateIs(STARTED)
                .willReturn(okJson("[]"))
                .willSetStateTo("created"));
        wireMockServer.stubFor(post(urlEqualTo("/admin/realms/aegis/users"))
                .withHeader("Authorization", equalTo("Bearer admin-token"))
                .withRequestBody(matchingJsonPath("$.username", equalTo("guest@byop.dev")))
                .withRequestBody(matchingJsonPath("$.email", equalTo("guest@byop.dev")))
                .withRequestBody(matchingJsonPath("$.enabled", equalTo("true")))
                .withRequestBody(containing("UPDATE_PASSWORD"))
                .willReturn(created()));
        wireMockServer.stubFor(get(urlPathEqualTo("/admin/realms/aegis/users"))
                .withQueryParam("email", equalTo("guest@byop.dev"))
                .withQueryParam("exact", equalTo("true"))
                .inScenario("invite")
                .whenScenarioStateIs("created")
                .willReturn(okJson("""
                        [
                          {
                            "id": "created-user-id",
                            "username": "guest@byop.dev",
                            "email": "guest@byop.dev",
                            "enabled": true
                          }
                        ]
                        """)));
        wireMockServer.stubFor(put(urlEqualTo(
                "/admin/realms/aegis/users/created-user-id/execute-actions-email?client_id=aegis-web"
        )).willReturn(noContent()));

        UserResponse user = client.inviteUserByEmail("guest@byop.dev");

        assertEquals("created-user-id", user.id());
        assertEquals("guest@byop.dev", user.username());
        wireMockServer.verify(postRequestedFor(urlEqualTo("/admin/realms/aegis/users"))
                .withRequestBody(containing("UPDATE_PASSWORD")));
        wireMockServer.verify(putRequestedFor(urlEqualTo(
                "/admin/realms/aegis/users/created-user-id/execute-actions-email?client_id=aegis-web"
        )));
    }

    @Test
    void shouldThrowWhenCreatedUserCannotBeResolved() {
        stubAdminToken();
        stubUserLookup("[]");
        wireMockServer.stubFor(post(urlEqualTo("/admin/realms/aegis/users")).willReturn(created()));

        assertThrows(
                KeycloakAuthenticationException.class,
                () -> client.inviteUserByEmail("guest@byop.dev")
        );
    }

    @Test
    void shouldThrowWhenInviteExecuteActionsEmailFails() {
        stubAdminToken();
        stubUserLookup("""
                [
                  {
                    "id": "user-id",
                    "username": "guest",
                    "email": "guest@byop.dev",
                    "enabled": true
                  }
                ]
                """);
        wireMockServer.stubFor(put(urlEqualTo(
                "/admin/realms/aegis/users/user-id/execute-actions-email?client_id=aegis-web"
        )).willReturn(serverError()));

        assertThrows(
                KeycloakAuthenticationException.class,
                () -> client.inviteUserByEmail("guest@byop.dev")
        );
    }

    @Test
    void shouldThrowWhenAdminTokenFails() {
        wireMockServer.stubFor(post(urlEqualTo("/realms/master/protocol/openid-connect/token"))
                .willReturn(serverError()));

        assertThrows(
                KeycloakAuthenticationException.class,
                () -> client.sendResetPasswordEmail("loki@teste.com")
        );
    }

    @Test
    void shouldThrowWhenAdminTokenBodyIsNull() {
        wireMockServer.stubFor(post(urlEqualTo("/realms/master/protocol/openid-connect/token"))
                .willReturn(ok()));

        assertThrows(
                KeycloakAuthenticationException.class,
                () -> client.findUsers()
        );
    }

    @Test
    void shouldThrowWhenAdminTokenDoesNotContainAccessToken() {
        wireMockServer.stubFor(post(urlEqualTo("/realms/master/protocol/openid-connect/token"))
                .willReturn(okJson("{}")));

        assertThrows(
                KeycloakAuthenticationException.class,
                () -> client.findUsers()
        );
    }

    @Test
    void shouldThrowWhenUserLookupFails() {
        stubAdminToken();

        wireMockServer.stubFor(get(urlPathEqualTo("/admin/realms/aegis/users"))
                .willReturn(serverError()));

        assertThrows(
                KeycloakAuthenticationException.class,
                () -> client.sendResetPasswordEmail("loki@teste.com")
        );
    }

    @Test
    void shouldThrowWhenExecuteActionsEmailFails() {
        stubAdminToken();
        stubUserLookup("""
                [
                  {
                    "id": "user-id",
                    "username": "loki",
                    "email": "loki@teste.com",
                    "enabled": true
                  }
                ]
                """);

        wireMockServer.stubFor(put(urlEqualTo(
                "/admin/realms/aegis/users/user-id/execute-actions-email?client_id=aegis-web"
        )).willReturn(serverError()));

        assertThrows(
                KeycloakAuthenticationException.class,
                () -> client.sendResetPasswordEmail("loki@teste.com")
        );
    }

    @Test
    void shouldFindUsers() {
        stubAdminToken();

        wireMockServer.stubFor(
                get(urlEqualTo("/admin/realms/aegis/users"))
                        .withHeader("Authorization", equalTo("Bearer admin-token"))
                        .willReturn(okJson("""
                            [
                              {
                                "id": "user-1",
                                "username": "loki",
                                "email": "loki@teste.com",
                                "firstName": "loki",
                                "lastName": "de asgard",
                                "enabled": true
                              }
                            ]
                            """))
        );

        List<UserResponse> users = client.findUsers();

        assertEquals(1, users.size());
        assertEquals("user-1", users.getFirst().id());
        assertEquals("loki", users.getFirst().username());
        assertEquals("loki@teste.com", users.getFirst().email());
        assertEquals("loki", users.getFirst().firstName());
        assertEquals("de asgard", users.getFirst().lastName());
        assertTrue(users.getFirst().enabled());
    }

    @Test
    void shouldNormalizeConfiguredAdminRealmsPath() {
        KeycloakAdminClient customClient = new KeycloakAdminClient(
                RestClient.builder().build(),
                new KeycloakProperties(
                        "http://localhost:8282/realms/aegis",
                        wireMockServer.baseUrl(),
                        "admin/realms",
                        "aegis",
                        "aegis-web",
                        "admin-cli",
                        "admin",
                        "admin"
                )
        );
        stubAdminToken();

        wireMockServer.stubFor(
                get(urlEqualTo("/admin/realms/aegis/users"))
                        .withHeader("Authorization", equalTo("Bearer admin-token"))
                        .willReturn(okJson("[]"))
        );

        List<UserResponse> users = customClient.findUsers();

        assertTrue(users.isEmpty());
        wireMockServer.verify(getRequestedFor(urlEqualTo("/admin/realms/aegis/users")));
    }

    @Test
    void shouldReturnEmptyListWhenFindUsersBodyIsNull() {
        stubAdminToken();

        wireMockServer.stubFor(
                get(urlEqualTo("/admin/realms/aegis/users"))
                        .withHeader("Authorization", equalTo("Bearer admin-token"))
                        .willReturn(ok())
        );

        List<UserResponse> users = client.findUsers();

        assertTrue(users.isEmpty());
    }

    @Test
    void shouldIgnoreNullUsersWhenFindingUsers() {
        stubAdminToken();

        wireMockServer.stubFor(
                get(urlEqualTo("/admin/realms/aegis/users"))
                        .withHeader("Authorization", equalTo("Bearer admin-token"))
                        .willReturn(okJson("""
                            [
                              null,
                              {
                                "id": "user-1",
                                "username": "loki",
                                "email": "loki@teste.com",
                                "firstName": "loki",
                                "lastName": "de asgard",
                                "enabled": true
                              }
                            ]
                            """))
        );

        List<UserResponse> users = client.findUsers();

        assertEquals(1, users.size());
        assertEquals("user-1", users.getFirst().id());
    }

    @Test
    void shouldFindUserById() {
        stubAdminToken();

        wireMockServer.stubFor(
                get(urlEqualTo("/admin/realms/aegis/users/user-1"))
                        .withHeader("Authorization", equalTo("Bearer admin-token"))
                        .willReturn(okJson("""
                            {
                              "id": "user-1",
                              "username": "loki",
                              "email": "loki@teste.com",
                              "firstName": "loki",
                              "lastName": "de asgard",
                              "enabled": true
                            }
                            """))
        );

        UserResponse user = client.findUserById("user-1");

        assertEquals("user-1", user.id());
        assertEquals("loki", user.username());
        assertEquals("loki@teste.com", user.email());
        assertEquals("loki", user.firstName());
        assertEquals("de asgard", user.lastName());
        assertTrue(user.enabled());
    }

    @Test
    void shouldThrowWhenFindUsersFails() {
        stubAdminToken();

        wireMockServer.stubFor(
                get(urlEqualTo("/admin/realms/aegis/users"))
                        .willReturn(serverError())
        );

        assertThrows(
                KeycloakAuthenticationException.class,
                () -> client.findUsers()
        );
    }

    @Test
    void shouldThrowWhenFindUserByIdFails() {
        stubAdminToken();

        wireMockServer.stubFor(
                get(urlEqualTo("/admin/realms/aegis/users/user-1"))
                        .willReturn(serverError())
        );

        assertThrows(
                KeycloakAuthenticationException.class,
                () -> client.findUserById("user-1")
        );
    }

    @Test
    void shouldThrowWhenFindUserByIdBodyIsNull() {
        stubAdminToken();

        wireMockServer.stubFor(
                get(urlEqualTo("/admin/realms/aegis/users/user-1"))
                        .willReturn(ok())
        );

        assertThrows(
                KeycloakAuthenticationException.class,
                () -> client.findUserById("user-1")
        );
    }

    private void stubAdminToken() {
        wireMockServer.stubFor(
                post(urlEqualTo("/realms/master/protocol/openid-connect/token"))
                        .willReturn(okJson("""
                            {
                              "access_token": "admin-token"
                            }
                            """))
        );
    }

    private void stubUserLookup(String body) {
        wireMockServer.stubFor(get(urlPathEqualTo("/admin/realms/aegis/users"))
                .withQueryParam("email", matching(".*"))
                .withQueryParam("exact", equalTo("true"))
                .willReturn(okJson(body)));
    }

    private void stubExecuteActionsEmail() {
        wireMockServer.stubFor(put(urlEqualTo(
                "/admin/realms/aegis/users/user-id/execute-actions-email?client_id=aegis-web"
        )).willReturn(noContent()));
    }
}
