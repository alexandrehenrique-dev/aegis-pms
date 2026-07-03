package br.com.byop.aegis.product.export.controller;

import br.com.byop.aegis.product.export.contract.DeleteProductRequest;
import br.com.byop.aegis.product.export.dto.DeleteAcceptedResponse;
import br.com.byop.aegis.product.export.service.ProductDeleteService;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.security.AuthenticatedUserProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class ProductDeleteController {

    private final ProductDeleteService productDeleteService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public ProductDeleteController(ProductDeleteService productDeleteService, AuthenticatedUserProvider authenticatedUserProvider) {
        this.productDeleteService = productDeleteService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @DeleteMapping("/api/v1/products/{productId}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public DeleteAcceptedResponse deleteProduct(@PathVariable("productId") UUID productId,
                                                @Valid @RequestBody DeleteProductRequest request,
                                                Authentication authentication) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        return productDeleteService.deleteProduct(caller, productId, request);
    }
}
