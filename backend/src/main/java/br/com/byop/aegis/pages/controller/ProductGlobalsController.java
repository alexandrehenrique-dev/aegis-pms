package br.com.byop.aegis.pages.controller;

import br.com.byop.aegis.pages.contract.UpdateProductGlobalsRequest;
import br.com.byop.aegis.pages.dto.ProductGlobalsResponse;
import br.com.byop.aegis.pages.service.ProductGlobalsService;
import br.com.byop.aegis.product.api.ProductAccessPort;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.security.AuthenticatedUserProvider;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Navbar, footer e redes sociais de um produto (ADR-0013) — estrutura basica
 * do produto, nunca gateada por modulo opcional (Secao E da Sprint 23):
 * {@code ProductGlobalsController} nao tem {@code @RequireModule}.
 */
@RestController
public class ProductGlobalsController {

    private final ProductGlobalsService globalsService;
    private final ProductAccessPort productAccessPort;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public ProductGlobalsController(ProductGlobalsService globalsService, ProductAccessPort productAccessPort,
                                    AuthenticatedUserProvider authenticatedUserProvider) {
        this.globalsService = globalsService;
        this.productAccessPort = productAccessPort;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping("/api/v1/products/{productId}/globals")
    public ProductGlobalsResponse getGlobals(@PathVariable("productId") UUID productId, Authentication authentication) {
        assertProductAccess(authentication, productId);
        return globalsService.getGlobals(productId);
    }

    @PutMapping("/api/v1/products/{productId}/globals")
    public ProductGlobalsResponse updateGlobals(@PathVariable("productId") UUID productId,
                                                @Valid @RequestBody UpdateProductGlobalsRequest request,
                                                Authentication authentication) {
        AuthenticatedUser caller = assertProductAccess(authentication, productId);
        return globalsService.updateGlobals(productId, request, caller);
    }

    private AuthenticatedUser assertProductAccess(Authentication authentication, UUID productId) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        productAccessPort.assertAccessible(productId, caller);
        return caller;
    }
}
