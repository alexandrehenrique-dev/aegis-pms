package br.com.byop.aegis.security;

import br.com.byop.aegis.identity.auth.controller.AuthController;
import br.com.byop.aegis.identity.auth.dto.AuthTokenResponse;
import br.com.byop.aegis.identity.auth.service.AuthService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

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

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "username": "loki",
                                      "password": "123456"
                                    }
                                    """))
                    .andExpect(status().isOk());
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
