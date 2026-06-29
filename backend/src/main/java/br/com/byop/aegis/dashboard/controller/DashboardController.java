package br.com.byop.aegis.dashboard.controller;

import br.com.byop.aegis.dashboard.dto.DashboardSummaryResponse;
import br.com.byop.aegis.dashboard.service.DashboardService;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.security.AuthenticatedUserProvider;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {

    private final DashboardService dashboardService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public DashboardController(DashboardService dashboardService, AuthenticatedUserProvider authenticatedUserProvider) {
        this.dashboardService = dashboardService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping("/api/v1/dashboard/summary")
    public DashboardSummaryResponse getSummary(Authentication authentication) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        return dashboardService.getSummary(caller);
    }
}
