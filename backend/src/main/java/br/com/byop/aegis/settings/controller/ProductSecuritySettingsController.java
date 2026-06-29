package br.com.byop.aegis.settings.controller;

import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.security.AuthenticatedUserProvider;
import br.com.byop.aegis.settings.contract.UpdateProductSecuritySettingsRequest;
import br.com.byop.aegis.settings.dto.ProductSecuritySettingsResponse;
import br.com.byop.aegis.settings.service.ProductSecuritySettingsService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class ProductSecuritySettingsController {

    private final ProductSecuritySettingsService settingsService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public ProductSecuritySettingsController(ProductSecuritySettingsService settingsService,
                                             AuthenticatedUserProvider authenticatedUserProvider) {
        this.settingsService = settingsService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping("/api/v1/products/{productId}/settings/security")
    public ProductSecuritySettingsResponse getSecuritySettings(@PathVariable("productId") UUID productId,
                                                               Authentication authentication) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        return settingsService.getSecuritySettings(caller, productId);
    }

    @PutMapping("/api/v1/products/{productId}/settings/security")
    public ProductSecuritySettingsResponse updateSecuritySettings(@PathVariable("productId") UUID productId,
                                                                  @RequestBody UpdateProductSecuritySettingsRequest request,
                                                                  Authentication authentication) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        return settingsService.updateSecuritySettings(caller, productId, request);
    }
}
