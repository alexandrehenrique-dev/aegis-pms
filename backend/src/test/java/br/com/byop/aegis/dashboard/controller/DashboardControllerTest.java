package br.com.byop.aegis.dashboard.controller;

import br.com.byop.aegis.core.CoreExceptionHandler;
import br.com.byop.aegis.dashboard.dto.DashboardSummaryResponse;
import br.com.byop.aegis.dashboard.service.DashboardService;
import br.com.byop.aegis.security.AuthenticatedUserProvider;
import br.com.byop.aegis.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DashboardController.class)
@Import({
        SecurityConfig.class,
        CoreExceptionHandler.class,
        AuthenticatedUserProvider.class
})
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @Test
    void shouldGetSummary() throws Exception {
        when(dashboardService.getSummary(any())).thenReturn(new DashboardSummaryResponse(
                3, 1, 4, 2, 2, 1, 12, 3, 5, 7, 2, "0%"
        ));

        mockMvc.perform(get("/api/v1/dashboard/summary").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeProducts").value(3))
                .andExpect(jsonPath("$.archivedProducts").value(1))
                .andExpect(jsonPath("$.formsReceived").value(12))
                .andExpect(jsonPath("$.conversionRate").value("0%"));
    }

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/summary"))
                .andExpect(status().isUnauthorized());
    }
}
