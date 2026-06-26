package br.com.byop.aegis.system.controller;

import br.com.byop.aegis.security.SecurityConfig;
import br.com.byop.aegis.system.dto.SystemStatusResponse;
import br.com.byop.aegis.system.service.SystemStatusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SystemStatusController.class)
@Import(SecurityConfig.class)
class SystemStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SystemStatusService systemStatusService;

    @Test
    void shouldRejectStatusWithoutBearerToken() throws Exception {
        mockMvc.perform(get("/api/v1/system/status"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnSystemStatusWithValidJwt() throws Exception {
        when(systemStatusService.currentStatus()).thenReturn(new SystemStatusResponse(
                "UP",
                "UP",
                "Banco conectado.",
                true,
                "http://localhost:8282/realms/aegis",
                "UP",
                List.of("local"),
                "0.0.1-SNAPSHOT"
        ));

        mockMvc.perform(get("/api/v1/system/status")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationStatus").value("UP"))
                .andExpect(jsonPath("$.databaseStatus").value("UP"))
                .andExpect(jsonPath("$.databaseMessage").value("Banco conectado."))
                .andExpect(jsonPath("$.keycloakIssuerConfigured").value(true))
                .andExpect(jsonPath("$.keycloakIssuer").value("http://localhost:8282/realms/aegis"))
                .andExpect(jsonPath("$.knowledgeGraphStatus").value("UP"))
                .andExpect(jsonPath("$.activeProfiles[0]").value("local"))
                .andExpect(jsonPath("$.buildVersion").value("0.0.1-SNAPSHOT"));
    }
}
