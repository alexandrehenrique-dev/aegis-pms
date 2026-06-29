package br.com.byop.aegis.system.controller;

import br.com.byop.aegis.security.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        SpaFallbackController.class
})
@Import({
        SecurityConfig.class,
        SpaFallbackControllerTest.ActuatorHealthController.class
})
class SpaFallbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {"/", "/dashboard", "/products", "/qualquer-rota", "/qualquer/rota/client-side"})
    void shouldForwardSpaRoutesToIndexHtml(String route) throws Exception {
        mockMvc.perform(get(route))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void shouldNotInterceptApiRoutes() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(forwardedUrl(null));
    }

    @Test
    void shouldNotInterceptActuatorRoutes() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(forwardedUrl(null));
    }

    @Test
    void shouldNotInterceptStaticAssets() throws Exception {
        mockMvc.perform(get("/assets/index.js"))
                .andExpect(status().isNotFound())
                .andExpect(forwardedUrl(null));
    }

    @RestController
    static class ActuatorHealthController {

        @GetMapping("/actuator/health")
        Map<String, String> health() {
            return Map.of("status", "UP");
        }
    }
}
