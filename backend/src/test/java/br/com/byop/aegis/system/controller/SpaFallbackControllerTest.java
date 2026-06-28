package br.com.byop.aegis.system.controller;

import br.com.byop.aegis.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SpaFallbackController.class)
@Import(SecurityConfig.class)
class SpaFallbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldForwardRootToIndexHtml() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void shouldForwardNestedSpaRouteToIndexHtml() throws Exception {
        mockMvc.perform(get("/products/maestro-beton/detail"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void shouldForwardDeeplyNestedSpaRouteToIndexHtml() throws Exception {
        mockMvc.perform(get("/settings/security"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void shouldNotInterceptApiRoutes() throws Exception {
        mockMvc.perform(get("/api/v1/system/status"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldNotInterceptActuatorRoutes() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDenyRouteOutsideKnownSpaPrefixes() throws Exception {
        // Requisição anônima a uma rota fora da allowlist: o filtro de exceção do
        // Spring Security trata `denyAll()` para usuário anônimo como "precisa
        // autenticar" (delega ao AuthenticationEntryPoint -> 401), o mesmo
        // comportamento de `.authenticated()` sem token — só um usuário já
        // autenticado receberia 403 nesta rota.
        mockMvc.perform(get("/totally-unknown-route"))
                .andExpect(status().isUnauthorized());
    }
}
