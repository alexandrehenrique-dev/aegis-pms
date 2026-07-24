package br.com.byop.aegis.security;

import br.com.byop.aegis.identity.auth.controller.AuthController;
import br.com.byop.aegis.identity.auth.dto.AuthTokenResponse;
import br.com.byop.aegis.identity.auth.service.AuthActivationService;
import br.com.byop.aegis.identity.auth.service.AuthService;
import br.com.byop.aegis.shared.config.AegisApplicationConfig;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD;
import static org.springframework.http.HttpHeaders.ORIGIN;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, AegisApplicationConfig.class})
@TestPropertySource(properties = {
        "aegis.app.base-url=https://aegis.byop.dev",
        "aegis.app.cors-allowed-origins=https://aegis.byop.dev"
})
class SecurityConfigTest {

    private static final String PROD_ORIGIN = "https://aegis.byop.dev";
    private static final String LOGIN_ENDPOINT = "/api/v1/auth/login";
    private static final String LOGIN_PAYLOAD = """
            {
              "username": "loki",
              "password": "123456"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private AuthActivationService authActivationService;

    @Nested
    class PublicAuthEndpoints {

        @Test
        void shouldAllowLoginWithoutBearerToken() throws Exception {

            when(authService.login("loki", "123456"))
                    .thenReturn(new AuthTokenResponse(
                            "access-token",
                            "refresh-token",
                            300L,
                            "Bearer"
                    ));

            mockMvc.perform(post(LOGIN_ENDPOINT)
                            .contentType(APPLICATION_JSON)
                            .content(LOGIN_PAYLOAD))
                    .andExpect(status().isOk());
        }

        @Test
        void shouldAllowConfiguredProductionOriginToReachLoginController() throws Exception {

            when(authService.login("loki", "123456"))
                    .thenReturn(new AuthTokenResponse(
                            "access-token",
                            "refresh-token",
                            300L,
                            "Bearer"
                    ));

            mockMvc.perform(post(LOGIN_ENDPOINT)
                            .header(ORIGIN, PROD_ORIGIN)
                            .contentType(APPLICATION_JSON)
                            .content(LOGIN_PAYLOAD))
                    .andExpect(status().isOk())
                    .andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, PROD_ORIGIN));
        }

        @Test
        void shouldRejectUnconfiguredOriginBeforeLoginController() throws Exception {

            mockMvc.perform(post(LOGIN_ENDPOINT)
                            .header(ORIGIN, "https://malicious.example")
                            .contentType(APPLICATION_JSON)
                            .content(LOGIN_PAYLOAD))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(authService);
        }

        @Test
        void shouldAllowPreflightForConfiguredProductionOrigin() throws Exception {

            mockMvc.perform(options(LOGIN_ENDPOINT)
                            .header(ORIGIN, PROD_ORIGIN)
                            .header(ACCESS_CONTROL_REQUEST_METHOD, "POST")
                            .header(ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, PROD_ORIGIN))
                    .andExpect(header().string(ACCESS_CONTROL_ALLOW_METHODS, org.hamcrest.Matchers.containsString("POST")))
                    .andExpect(header().string(
                            ACCESS_CONTROL_ALLOW_HEADERS,
                            org.hamcrest.Matchers.containsStringIgnoringCase("content-type")
                    ));
        }

        @Test
        void shouldAllowActuatorHealthWithoutBearerToken() throws Exception {

            mockMvc.perform(get("/actuator/health"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void shouldAllowActuatorInfoWithoutBearerToken() throws Exception {

            mockMvc.perform(get("/actuator/info"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void shouldAllowPublicFormSubmitWithoutBearerToken() throws Exception {

            mockMvc.perform(post("/api/v1/products/11111111-1111-1111-1111-111111111111/forms/"
                            + "22222222-2222-2222-2222-222222222222/submit")
                            .contentType(APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void shouldAllowExportDownloadWithoutBearerToken() throws Exception {

            mockMvc.perform(get("/api/v1/exports/11111111-1111-1111-1111-111111111111/download"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class ProtectedApiEndpoints {

        @Test
        void shouldRejectMeWithoutBearerToken() throws Exception {

            mockMvc.perform(get("/api/v1/me"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldAllowMeWithBearerToken() throws Exception {

            mockMvc.perform(get("/api/v1/me")
                            .with(jwt()))
                    .andExpect(status().isNotFound());
        }
    }
}
