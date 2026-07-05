package br.com.byop.aegis.identity.auth.client;

import br.com.byop.aegis.identity.auth.config.KeycloakProperties;
import br.com.byop.aegis.identity.auth.exception.KeycloakAuthenticationException;
import br.com.byop.aegis.identity.user.dto.UserResponse;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

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
        UserResponse user = client.inviteUserByEmail("guest@byop.dev");

        assertEquals("user-id", user.id());
        assertEquals("guest@byop.dev", user.email());
        wireMockServer.verify(0, postRequestedFor(urlEqualTo("/admin/realms/aegis/users")));
        wireMockServer.verify(0, putRequestedFor(urlPathMatching(".*/execute-actions-email.*")));
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
                .withRequestBody(matchingJsonPath("$.requiredActions", equalToJson("[]")))
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
        UserResponse user = client.inviteUserByEmail("guest@byop.dev");

        assertEquals("created-user-id", user.id());
        assertEquals("guest@byop.dev", user.username());
        wireMockServer.verify(postRequestedFor(urlEqualTo("/admin/realms/aegis/users"))
                .withRequestBody(matchingJsonPath("$.requiredActions", equalToJson("[]"))));
        wireMockServer.verify(0, putRequestedFor(urlPathMatching(".*/execute-actions-email.*")));
    }

    @Test
    void shouldCreateUserWithSplitNameWhenInvitingWithName() {
        stubAdminToken();
        wireMockServer.stubFor(get(urlPathEqualTo("/admin/realms/aegis/users"))
                .withQueryParam("email", equalTo("guest@byop.dev"))
                .withQueryParam("exact", equalTo("true"))
                .inScenario("invite-with-name")
                .whenScenarioStateIs(STARTED)
                .willReturn(okJson("[]"))
                .willSetStateTo("created"));
        wireMockServer.stubFor(post(urlEqualTo("/admin/realms/aegis/users"))
                .withRequestBody(matchingJsonPath("$.firstName", equalTo("Guest")))
                .withRequestBody(matchingJsonPath("$.lastName", equalTo("User")))
                .willReturn(created()));
        wireMockServer.stubFor(get(urlPathEqualTo("/admin/realms/aegis/users"))
                .withQueryParam("email", equalTo("guest@byop.dev"))
                .withQueryParam("exact", equalTo("true"))
                .inScenario("invite-with-name")
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

        UserResponse user = client.inviteUser("guest@byop.dev", "Guest User");

        assertEquals("created-user-id", user.id());
    }

    @Test
    void shouldCreateUserWithBlankNameWhenInvitingWithBlankName() {
        stubAdminToken();
        wireMockServer.stubFor(get(urlPathEqualTo("/admin/realms/aegis/users"))
                .withQueryParam("email", equalTo("guest@byop.dev"))
                .withQueryParam("exact", equalTo("true"))
                .inScenario("invite-with-blank-name")
                .whenScenarioStateIs(STARTED)
                .willReturn(okJson("[]"))
                .willSetStateTo("created"));
        wireMockServer.stubFor(post(urlEqualTo("/admin/realms/aegis/users"))
                .withRequestBody(matchingJsonPath("$.firstName", absent()))
                .withRequestBody(matchingJsonPath("$.lastName", absent()))
                .willReturn(created()));
        wireMockServer.stubFor(get(urlPathEqualTo("/admin/realms/aegis/users"))
                .withQueryParam("email", equalTo("guest@byop.dev"))
                .withQueryParam("exact", equalTo("true"))
                .inScenario("invite-with-blank-name")
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

        UserResponse user = client.inviteUser("guest@byop.dev", " ");

        assertEquals("created-user-id", user.id());
    }

    @Test
    void shouldThrowWhenInviteUserFails() {
        wireMockServer.stubFor(post(urlEqualTo("/realms/master/protocol/openid-connect/token"))
                .willReturn(serverError()));

        assertThrows(
                KeycloakAuthenticationException.class,
                () -> client.inviteUserByEmail("guest@byop.dev")
        );
    }

    @Test
    void shouldEnsureExistingDemoUserWithPasswordAndRealmRole() {
        stubAdminToken();
        stubUserLookup("""
                [
                  {
                    "id": "demo-user-id",
                    "username": "super-admin@byop.io",
                    "email": "super-admin@byop.io",
                    "firstName": "Super",
                    "lastName": "Admin",
                    "enabled": true
                  }
                ]
                """);
        stubDemoUserUpdates("demo-user-id", "AEGIS_SUPER_ADMIN");

        UserResponse user = client.ensureDemoUser(
                "super-admin@byop.io",
                "Super Admin",
                "senha123",
                "AEGIS_SUPER_ADMIN"
        );

        assertEquals("demo-user-id", user.id());
        wireMockServer.verify(0, postRequestedFor(urlEqualTo("/admin/realms/aegis/users")));
        wireMockServer.verify(putRequestedFor(urlEqualTo("/admin/realms/aegis/users/demo-user-id/reset-password"))
                .withRequestBody(matchingJsonPath("$.value", equalTo("senha123")))
                .withRequestBody(matchingJsonPath("$.temporary", equalTo("false"))));
        wireMockServer.verify(postRequestedFor(urlEqualTo("/admin/realms/aegis/users/demo-user-id/role-mappings/realm"))
                .withRequestBody(containing("AEGIS_SUPER_ADMIN")));
    }

    @Test
    void shouldCreateDemoUserWhenMissing() {
        stubAdminToken();
        wireMockServer.stubFor(get(urlPathEqualTo("/admin/realms/aegis/users"))
                .withQueryParam("email", equalTo("pm@byop.io"))
                .withQueryParam("exact", equalTo("true"))
                .inScenario("ensure-demo")
                .whenScenarioStateIs(STARTED)
                .willReturn(okJson("[]"))
                .willSetStateTo("created"));
        wireMockServer.stubFor(post(urlEqualTo("/admin/realms/aegis/users"))
                .withRequestBody(matchingJsonPath("$.username", equalTo("pm@byop.io")))
                .withRequestBody(matchingJsonPath("$.credentials[0].value", equalTo("senha123")))
                .withRequestBody(matchingJsonPath("$.credentials[0].temporary", equalTo("false")))
                .willReturn(created()));
        wireMockServer.stubFor(get(urlPathEqualTo("/admin/realms/aegis/users"))
                .withQueryParam("email", equalTo("pm@byop.io"))
                .withQueryParam("exact", equalTo("true"))
                .inScenario("ensure-demo")
                .whenScenarioStateIs("created")
                .willReturn(okJson("""
                        [
                          {
                            "id": "pm-user-id",
                            "username": "pm@byop.io",
                            "email": "pm@byop.io",
                            "firstName": "Marina",
                            "lastName": "Costa",
                            "enabled": true
                          }
                        ]
                        """)));
        stubDemoUserUpdates("pm-user-id", "AEGIS_PRODUCT_MANAGER");

        UserResponse user = client.ensureDemoUser(
                "pm@byop.io",
                "Marina Costa",
                "senha123",
                "AEGIS_PRODUCT_MANAGER"
        );

        assertEquals("pm-user-id", user.id());
        wireMockServer.verify(postRequestedFor(urlEqualTo("/admin/realms/aegis/users"))
                .withRequestBody(matchingJsonPath("$.requiredActions", equalToJson("[]"))));
    }

    @Test
    void shouldResetPasswordWithPublicMethod() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KeycloakAdminClient localClient = new KeycloakAdminClient(
                builder.build(),
                new KeycloakProperties(
                        "http://localhost:8282/realms/aegis",
                        "http://keycloak.test",
                        "/admin/realms/",
                        "aegis",
                        "aegis-web",
                        "admin-cli",
                        "admin",
                        "admin"
                )
        );
        server.expect(requestTo("http://keycloak.test/realms/master/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "access_token": "admin-token"
                        }
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://keycloak.test/admin/realms/aegis/users/user-id/reset-password"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withNoContent());

        assertDoesNotThrow(() -> localClient.resetPassword("user-id", "Senha123"));

        server.verify();
    }

    @Test
    void shouldThrowWhenPublicResetPasswordFails() {
        stubAdminToken();
        wireMockServer.stubFor(put(urlEqualTo("/admin/realms/aegis/users/user-id/reset-password"))
                .willReturn(serverError()));

        assertThrows(
                KeycloakAuthenticationException.class,
                () -> client.resetPassword("user-id", "Senha123")
        );
    }

    @Test
    void shouldClearRequiredActionsWithPublicMethod() {
        stubAdminToken();
        wireMockServer.stubFor(put(urlEqualTo("/admin/realms/aegis/users/user-id"))
                .willReturn(noContent()));

        assertDoesNotThrow(() -> client.clearRequiredActions("user-id"));

        wireMockServer.verify(putRequestedFor(urlEqualTo("/admin/realms/aegis/users/user-id"))
                .withRequestBody(equalToJson("{\"requiredActions\":[]}")));
    }

    @Test
    void shouldThrowWhenPublicClearRequiredActionsFails() {
        stubAdminToken();

        assertThrows(
                KeycloakAuthenticationException.class,
                () -> client.clearRequiredActions("user-id")
        );
    }

    @Test
    void shouldThrowWhenEnsuringDemoUserFails() {
        stubAdminToken();
        stubUserLookup("""
                [
                  {
                    "id": "demo-user-id",
                    "username": "super-admin@byop.io",
                    "email": "super-admin@byop.io",
                    "enabled": true
                  }
                ]
                """);
        wireMockServer.stubFor(put(urlEqualTo("/admin/realms/aegis/users/demo-user-id"))
                .willReturn(serverError()));

        assertThrows(
                KeycloakAuthenticationException.class,
                () -> client.ensureDemoUser(
                        "super-admin@byop.io",
                        "Super Admin",
                        "senha123",
                        "AEGIS_SUPER_ADMIN"
                )
        );
    }

    @Test
    void shouldThrowWhenCreatedDemoUserCannotBeResolved() {
        stubAdminToken();
        stubUserLookup("[]");
        wireMockServer.stubFor(post(urlEqualTo("/admin/realms/aegis/users")).willReturn(created()));

        assertThrows(
                KeycloakAuthenticationException.class,
                () -> client.ensureDemoUser(
                        "pm@byop.io",
                        "Marina Costa",
                        "senha123",
                        "AEGIS_PRODUCT_MANAGER"
                )
        );
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
    void shouldNotCallExecuteActionsEmailWhenInvitingExistingUser() {
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
        UserResponse user = client.inviteUserByEmail("guest@byop.dev");

        assertEquals("user-id", user.id());
        wireMockServer.verify(0, putRequestedFor(urlPathMatching(".*/execute-actions-email.*")));
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
    void shouldReturnTrueWhenUserHasRequiredAction() {
        stubAdminToken();
        wireMockServer.stubFor(
                get(urlEqualTo("/admin/realms/aegis/users/user-1"))
                        .willReturn(okJson("""
                            {
                              "id": "user-1",
                              "username": "loki",
                              "email": "loki@teste.com",
                              "enabled": true,
                              "requiredActions": ["UPDATE_PASSWORD"]
                            }
                            """))
        );

        assertTrue(client.hasRequiredAction("user-1", "UPDATE_PASSWORD"));
    }

    @Test
    void shouldReturnFalseWhenUserHasNoRequiredActionsField() {
        stubAdminToken();
        wireMockServer.stubFor(
                get(urlEqualTo("/admin/realms/aegis/users/user-1"))
                        .willReturn(okJson("""
                            {
                              "id": "user-1",
                              "username": "loki",
                              "email": "loki@teste.com",
                              "enabled": true
                            }
                            """))
        );

        assertFalse(client.hasRequiredAction("user-1", "UPDATE_PASSWORD"));
    }

    @Test
    void shouldReturnFalseWhenUserDoesNotHaveRequiredAction() {
        stubAdminToken();
        wireMockServer.stubFor(
                get(urlEqualTo("/admin/realms/aegis/users/user-1"))
                        .willReturn(okJson("""
                            {
                              "id": "user-1",
                              "username": "loki",
                              "email": "loki@teste.com",
                              "enabled": true,
                              "requiredActions": []
                            }
                            """))
        );

        assertFalse(client.hasRequiredAction("user-1", "UPDATE_PASSWORD"));
    }

    @Test
    void shouldThrowWhenHasRequiredActionFails() {
        stubAdminToken();
        wireMockServer.stubFor(
                get(urlEqualTo("/admin/realms/aegis/users/user-1"))
                        .willReturn(serverError())
        );

        assertThrows(
                KeycloakAuthenticationException.class,
                () -> client.hasRequiredAction("user-1", "UPDATE_PASSWORD")
        );
    }

    @Test
    void shouldFindUserByEmail() {
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

        UserResponse user = client.findUserByEmail("guest@byop.dev").orElseThrow();

        assertEquals("user-id", user.id());
        assertEquals("guest@byop.dev", user.email());
    }

    @Test
    void shouldThrowWhenFindUserByEmailFails() {
        stubAdminToken();
        wireMockServer.stubFor(get(urlPathEqualTo("/admin/realms/aegis/users"))
                .withQueryParam("email", equalTo("guest@byop.dev"))
                .withQueryParam("exact", equalTo("true"))
                .willReturn(serverError()));

        assertThrows(
                KeycloakAuthenticationException.class,
                () -> client.findUserByEmail("guest@byop.dev")
        );
    }

    @Test
    void shouldReturnEmptyWhenFindUserByEmailHasNoResult() {
        stubAdminToken();
        stubUserLookup("[]");

        assertTrue(client.findUserByEmail("missing@byop.dev").isEmpty());
    }

    @Test
    void shouldExecuteCustomActionsEmail() {
        stubAdminToken();
        wireMockServer.stubFor(put(urlPathEqualTo(
                "/admin/realms/aegis/users/user-id/execute-actions-email"
        )).willReturn(noContent()));

        client.executeActionsEmail("user-id", List.of("UPDATE_PASSWORD"));

        wireMockServer.verify(putRequestedFor(urlPathEqualTo(
                "/admin/realms/aegis/users/user-id/execute-actions-email"
        )).withRequestBody(equalToJson("[\"UPDATE_PASSWORD\"]")));
    }

    @Test
    void shouldExecuteMultipleCustomActionsEmail() {
        stubAdminToken();
        wireMockServer.stubFor(put(urlPathEqualTo(
                "/admin/realms/aegis/users/user-id/execute-actions-email"
        )).willReturn(noContent()));

        client.executeActionsEmail("user-id", List.of("UPDATE_PASSWORD", "VERIFY_EMAIL"));

        wireMockServer.verify(putRequestedFor(urlPathEqualTo(
                "/admin/realms/aegis/users/user-id/execute-actions-email"
        )).withRequestBody(equalToJson("[\"UPDATE_PASSWORD\",\"VERIFY_EMAIL\"]")));
    }

    @Test
    void shouldThrowWhenPublicExecuteActionsEmailFails() {
        stubAdminToken();
        wireMockServer.stubFor(put(urlPathEqualTo(
                "/admin/realms/aegis/users/user-id/execute-actions-email"
        )).willReturn(serverError()));
        List<String> requiredActions = List.of("UPDATE_PASSWORD");

        assertThrows(
                KeycloakAuthenticationException.class,
                () -> client.executeActionsEmail("user-id", requiredActions)
        );
    }

    @Test
    void shouldSetUserEnabled() {
        stubAdminToken();
        wireMockServer.stubFor(put(urlEqualTo("/admin/realms/aegis/users/user-id"))
                .willReturn(noContent()));

        client.setUserEnabled("user-id", false);

        wireMockServer.verify(putRequestedFor(urlEqualTo("/admin/realms/aegis/users/user-id"))
                .withRequestBody(equalToJson("{\"enabled\":false}")));
    }

    @Test
    void shouldThrowWhenSetUserEnabledFails() {
        stubAdminToken();
        wireMockServer.stubFor(put(urlEqualTo("/admin/realms/aegis/users/user-id"))
                .willReturn(serverError()));

        assertThrows(
                KeycloakAuthenticationException.class,
                () -> client.setUserEnabled("user-id", true)
        );
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

    private void stubDemoUserUpdates(String userId, String roleName) {
        wireMockServer.stubFor(put(urlEqualTo("/admin/realms/aegis/users/" + userId))
                .willReturn(noContent()));
        wireMockServer.stubFor(put(urlEqualTo("/admin/realms/aegis/users/" + userId + "/reset-password"))
                .willReturn(noContent()));
        wireMockServer.stubFor(get(urlEqualTo("/admin/realms/aegis/roles/" + roleName))
                .willReturn(okJson("""
                    {
                      "id": "role-id",
                      "name": "%s"
                    }
                    """.formatted(roleName))));
        wireMockServer.stubFor(post(urlEqualTo("/admin/realms/aegis/users/" + userId + "/role-mappings/realm"))
                .willReturn(noContent()));
    }
}
