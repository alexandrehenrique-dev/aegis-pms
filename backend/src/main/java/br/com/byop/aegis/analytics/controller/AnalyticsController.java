package br.com.byop.aegis.analytics.controller;

import br.com.byop.aegis.analytics.dto.AnalyticsKpiResponse;
import br.com.byop.aegis.analytics.dto.AnalyticsReportResponse;
import br.com.byop.aegis.analytics.dto.ChannelRowResponse;
import br.com.byop.aegis.analytics.dto.HealthSignalResponse;
import br.com.byop.aegis.analytics.dto.TrendCardResponse;
import br.com.byop.aegis.analytics.service.AnalyticsService;
import br.com.byop.aegis.product.api.ModuleKey;
import br.com.byop.aegis.product.api.ProductAccessPort;
import br.com.byop.aegis.product.api.RequireModule;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.security.AuthenticatedUserProvider;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequireModule(ModuleKey.ANALYTICS)
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final ProductAccessPort productAccessPort;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public AnalyticsController(AnalyticsService analyticsService, ProductAccessPort productAccessPort,
                               AuthenticatedUserProvider authenticatedUserProvider) {
        this.analyticsService = analyticsService;
        this.productAccessPort = productAccessPort;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping("/api/v1/products/{productId}/analytics/kpis")
    public List<AnalyticsKpiResponse> listKpis(@PathVariable("productId") UUID productId,
                                               Authentication authentication) {
        assertProductAccess(authentication, productId);
        return analyticsService.listKpis(productId);
    }

    @GetMapping("/api/v1/products/{productId}/analytics/health")
    public List<HealthSignalResponse> listHealth(@PathVariable("productId") UUID productId,
                                                 Authentication authentication) {
        assertProductAccess(authentication, productId);
        return analyticsService.listHealth(productId);
    }

    @GetMapping("/api/v1/products/{productId}/analytics/channels")
    public List<ChannelRowResponse> listChannels(@PathVariable("productId") UUID productId,
                                                 Authentication authentication) {
        assertProductAccess(authentication, productId);
        return analyticsService.listChannels(productId);
    }

    @GetMapping("/api/v1/products/{productId}/analytics/trends")
    public List<TrendCardResponse> listTrends(@PathVariable("productId") UUID productId,
                                              Authentication authentication) {
        assertProductAccess(authentication, productId);
        return analyticsService.listTrends(productId);
    }

    @GetMapping("/api/v1/products/{productId}/analytics/reports")
    public List<AnalyticsReportResponse> listReports(@PathVariable("productId") UUID productId,
                                                     Authentication authentication) {
        assertProductAccess(authentication, productId);
        return analyticsService.listReports(productId);
    }

    private void assertProductAccess(Authentication authentication, UUID productId) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        productAccessPort.assertAccessible(productId, caller);
    }
}
