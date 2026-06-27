package br.com.byop.aegis.product.controller;

import br.com.byop.aegis.product.contract.AssignProductUserRequest;
import br.com.byop.aegis.product.dto.ProductAssignmentSummary;
import br.com.byop.aegis.product.service.ProductAssignmentService;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.security.AuthenticatedUserProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class ProductAssignmentController {

    private final ProductAssignmentService productAssignmentService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public ProductAssignmentController(ProductAssignmentService productAssignmentService,
                                       AuthenticatedUserProvider authenticatedUserProvider) {
        this.productAssignmentService = productAssignmentService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping("/api/v1/products/{productId}/users")
    public List<ProductAssignmentSummary> listProductUsers(@PathVariable("productId") UUID productId) {
        return productAssignmentService.listAssignments(productId);
    }

    @PostMapping("/api/v1/products/{productId}/users")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductAssignmentSummary assignProductUser(@PathVariable("productId") UUID productId,
                                                      @Valid @RequestBody AssignProductUserRequest request,
                                                      Authentication authentication) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        return productAssignmentService.assignUser(caller, productId, request);
    }

    @DeleteMapping("/api/v1/products/{productId}/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeProductUser(@PathVariable("productId") UUID productId,
                                  @PathVariable("userId") String userId,
                                  Authentication authentication) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        productAssignmentService.removeAssignment(caller, productId, userId);
    }
}
