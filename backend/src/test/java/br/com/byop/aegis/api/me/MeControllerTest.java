package br.com.byop.aegis.api.me;

import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.security.AuthenticatedUserProvider;
import br.com.byop.aegis.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MeController.class)
@Import({

        SecurityConfig.class,

        MeResponseMapper.class

})
class MeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticatedUserProvider authenticatedUserProvider;

    @Test
    void shouldReturnUnauthorizedWhenTokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnUnauthorizedWhenTokenIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/me")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnAuthenticatedUserWhenJwtIsValid() throws Exception {
        AuthenticatedUser user = new AuthenticatedUser(
                "subject-123",
                "loki@byop.dev",
                "loki",
                "Loki",
                Set.of("ROLE_SUPER_ADMIN")
        );

        when(authenticatedUserProvider.from(any(Authentication.class)))
                .thenReturn(user);

        mockMvc.perform(get("/api/v1/me")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("subject-123"))
                .andExpect(jsonPath("$.email").value("loki@byop.dev"))
                .andExpect(jsonPath("$.username").value("loki"))
                .andExpect(jsonPath("$.name").value("Loki"))
                .andExpect(jsonPath("$.role").value("super_admin"));
    }
}