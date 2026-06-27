package br.com.byop.aegis.analytics.controller;

import br.com.byop.aegis.analytics.dto.AnalyticsKpiResponse;
import br.com.byop.aegis.analytics.dto.AnalyticsReportResponse;
import br.com.byop.aegis.analytics.dto.ChannelRowResponse;
import br.com.byop.aegis.analytics.dto.HealthSignalResponse;
import br.com.byop.aegis.analytics.dto.TrendCardResponse;
import br.com.byop.aegis.analytics.service.AnalyticsService;
import br.com.byop.aegis.core.CoreExceptionHandler;
import br.com.byop.aegis.product.api.ModuleKey;
import br.com.byop.aegis.product.api.ProductAccessPort;
import br.com.byop.aegis.product.exception.ProductExceptionHandler;
import br.com.byop.aegis.product.exception.ProductNotFoundException;
import br.com.byop.aegis.product.module.ModuleAccessAspect;
import br.com.byop.aegis.product.repository.ProductModuleRepository;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.security.AuthenticatedUserProvider;
import br.com.byop.aegis.security.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AnalyticsController.class)
@Import({
        SecurityConfig.class,
        CoreExceptionHandler.class,
        ProductExceptionHandler.class,
        ModuleAccessAspect.class
})
class AnalyticsControllerTest {

    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalyticsService analyticsService;

    @MockitoBean
    private ProductAccessPort productAccessPort;

    @MockitoBean
    private AuthenticatedUserProvider authenticatedUserProvider;

    @MockitoBean
    private ProductModuleRepository productModuleRepository;

    @BeforeEach
    void setUp() {
        when(productModuleRepository.existsByProductIdAndModuleKeyAndEnabledTrue(PRODUCT_ID, ModuleKey.ANALYTICS))
                .thenReturn(true);
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(user());
    }

    @Test
    void shouldListKpis() throws Exception {
        when(analyticsService.listKpis(PRODUCT_ID)).thenReturn(List.of(
                new AnalyticsKpiResponse("Conteúdos publicados", "5", "sem pendências", "ok", "positivo")
        ));

        mockMvc.perform(get("/api/v1/products/{productId}/analytics/kpis", PRODUCT_ID).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].label").value("Conteúdos publicados"))
                .andExpect(jsonPath("$[0].value").value("5"))
                .andExpect(jsonPath("$[0].comparison").value("sem pendências"))
                .andExpect(jsonPath("$[0].note").value("ok"))
                .andExpect(jsonPath("$[0].tone").value("positivo"));

        verify(productAccessPort).assertAccessible(PRODUCT_ID, user());
    }

    @Test
    void shouldListHealth() throws Exception {
        when(analyticsService.listHealth(PRODUCT_ID)).thenReturn(List.of(
                new HealthSignalResponse("Saúde geral", "Produto saudável", "94", "positivo")
        ));

        mockMvc.perform(get("/api/v1/products/{productId}/analytics/health", PRODUCT_ID).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].label").value("Saúde geral"))
                .andExpect(jsonPath("$[0].status").value("Produto saudável"))
                .andExpect(jsonPath("$[0].score").value("94"))
                .andExpect(jsonPath("$[0].tone").value("positivo"));
    }

    @Test
    void shouldListChannels() throws Exception {
        when(analyticsService.listChannels(PRODUCT_ID)).thenReturn(List.of(
                new ChannelRowResponse("Direto", "0", "0%", "simulado")
        ));

        mockMvc.perform(get("/api/v1/products/{productId}/analytics/channels", PRODUCT_ID).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Direto"))
                .andExpect(jsonPath("$[0].visits").value("0"))
                .andExpect(jsonPath("$[0].conversion").value("0%"))
                .andExpect(jsonPath("$[0].trend").value("simulado"));
    }

    @Test
    void shouldListTrends() throws Exception {
        when(analyticsService.listTrends(PRODUCT_ID)).thenReturn(List.of(
                new TrendCardResponse("Atenção", "Conteúdo antigo.", "1 pendente", "alta", false)
        ));

        mockMvc.perform(get("/api/v1/products/{productId}/analytics/trends", PRODUCT_ID).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("Atenção"))
                .andExpect(jsonPath("$[0].text").value("Conteúdo antigo."))
                .andExpect(jsonPath("$[0].metric").value("1 pendente"))
                .andExpect(jsonPath("$[0].severity").value("alta"))
                .andExpect(jsonPath("$[0].reviewed").value(false));
    }

    @Test
    void shouldListReports() throws Exception {
        when(analyticsService.listReports(PRODUCT_ID)).thenReturn(List.of(
                new AnalyticsReportResponse("Relatório mensal do produto", "Resumo", "mês atual", "PDF", "pronto", "—")
        ));

        mockMvc.perform(get("/api/v1/products/{productId}/analytics/reports", PRODUCT_ID).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Relatório mensal do produto"))
                .andExpect(jsonPath("$[0].description").value("Resumo"))
                .andExpect(jsonPath("$[0].period").value("mês atual"))
                .andExpect(jsonPath("$[0].format").value("PDF"))
                .andExpect(jsonPath("$[0].status").value("pronto"))
                .andExpect(jsonPath("$[0].lastGenerated").value("—"));
    }

    @Test
    void shouldReturnNotFoundForProductOutsideUserScope() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        doThrow(new ProductNotFoundException(PRODUCT_ID)).when(productAccessPort).assertAccessible(PRODUCT_ID, caller);

        mockMvc.perform(get("/api/v1/products/{productId}/analytics/kpis", PRODUCT_ID).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void shouldReturnModuleDisabledWhenAnalyticsModuleIsDisabled() throws Exception {
        when(productModuleRepository.existsByProductIdAndModuleKeyAndEnabledTrue(PRODUCT_ID, ModuleKey.ANALYTICS))
                .thenReturn(false);

        mockMvc.perform(get("/api/v1/products/{productId}/analytics/kpis", PRODUCT_ID).with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("MODULE_DISABLED"))
                .andExpect(jsonPath("$.moduleKey").value("ANALYTICS"));
    }

    private AuthenticatedUser user() {
        return new AuthenticatedUser("user-1", "user@example.com", "user", "User", Set.of("ROLE_PRODUCT_MANAGER"));
    }
}
