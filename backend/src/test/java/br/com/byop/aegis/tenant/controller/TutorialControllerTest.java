package br.com.byop.aegis.tenant.controller;

import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.security.AuthenticatedUserProvider;
import br.com.byop.aegis.security.SecurityConfig;
import br.com.byop.aegis.tenant.api.TenantAccessService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TutorialController.class)
@Import(SecurityConfig.class)
class TutorialControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticatedUserProvider authenticatedUserProvider;

    @MockitoBean
    private TenantAccessService tenantAccessService;

    @Test
    void shouldReturnUnauthorizedWhenTokenIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/tutorial/complete"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldCompleteTutorialForAuthenticatedUser() throws Exception {
        AuthenticatedUser user = new AuthenticatedUser(
                "subject-123",
                "loki@byop.dev",
                "loki",
                "Loki",
                Set.of("ROLE_EDITOR")
        );
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(user);

        mockMvc.perform(post("/api/v1/tutorial/complete")
                        .with(jwt()))
                .andExpect(status().isNoContent());

        verify(tenantAccessService).markTutorialCompleted("subject-123");
    }
}
