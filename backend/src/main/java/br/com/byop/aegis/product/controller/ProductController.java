package br.com.byop.aegis.product.controller;

import br.com.byop.aegis.product.contract.CreateProductRequest;
import br.com.byop.aegis.product.dto.ProductDetail;
import br.com.byop.aegis.product.dto.ProductModuleSummary;
import br.com.byop.aegis.product.dto.ProductSummary;
import br.com.byop.aegis.product.service.ProductModuleService;
import br.com.byop.aegis.product.service.ProductService;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.security.AuthenticatedUserProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class ProductController {

    private final ProductService productService;
    private final ProductModuleService productModuleService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public ProductController(ProductService productService, ProductModuleService productModuleService,
                             AuthenticatedUserProvider authenticatedUserProvider) {
        this.productService = productService;
        this.productModuleService = productModuleService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping("/api/v1/products")
    public List<ProductSummary> listProducts(Authentication authentication) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        return productService.listProducts(caller);
    }

    @PostMapping("/api/v1/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductSummary createProduct(@Valid @RequestBody CreateProductRequest request,
                                        Authentication authentication) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        return productService.createProduct(caller, request.toCommand());
    }

    @GetMapping("/api/v1/products/{productId}")
    public ProductDetail getProduct(@PathVariable("productId") UUID productId, Authentication authentication) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        return productService.getProductDetail(caller, productId);
    }

    @PostMapping("/api/v1/products/{productId}/modules/{moduleKey}/enable")
    public ProductModuleSummary enableModule(@PathVariable("productId") UUID productId,
                                             @PathVariable("moduleKey") String moduleKey) {
        return productModuleService.enableModule(productId, moduleKey);
    }

    @PostMapping("/api/v1/products/{productId}/modules/{moduleKey}/disable")
    public ProductModuleSummary disableModule(@PathVariable("productId") UUID productId,
                                              @PathVariable("moduleKey") String moduleKey) {
        return productModuleService.disableModule(productId, moduleKey);
    }
}
