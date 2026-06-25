package br.com.byop.aegis.product.service;

import br.com.byop.aegis.product.api.ProductAccessPort;
import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.domain.ProductAssignmentStatus;
import br.com.byop.aegis.product.exception.ProductContentAccessDeniedException;
import br.com.byop.aegis.product.exception.ProductNotFoundException;
import br.com.byop.aegis.product.repository.ProductAssignmentRepository;
import br.com.byop.aegis.product.repository.ProductRepository;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.tenant.api.TenantAccessService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class ProductAccessResolver implements ProductAccessPort {

    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String ROLE_TENANT_ADMIN = "ROLE_TENANT_ADMIN";
    private static final String ROLE_PRODUCT_MANAGER = "ROLE_PRODUCT_MANAGER";
    private static final String ROLE_EDITOR = "ROLE_EDITOR";
    private static final String ROLE_VIEWER = "ROLE_VIEWER";

    private final ProductRepository productRepository;
    private final TenantAccessService tenantAccessService;
    private final ProductAssignmentRepository assignmentRepository;

    public ProductAccessResolver(ProductRepository productRepository, TenantAccessService tenantAccessService,
                                 ProductAssignmentRepository assignmentRepository) {
        this.productRepository = productRepository;
        this.tenantAccessService = tenantAccessService;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public void assertAccessible(UUID productId, AuthenticatedUser user) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        if (user.authorities().contains(ROLE_SUPER_ADMIN)) {
            if (hasActiveAssignment(user, productId)) {
                return;
            }
            throw new ProductContentAccessDeniedException(productId);
        }

        if (user.authorities().contains(ROLE_TENANT_ADMIN)
                && tenantAccessService.hasActiveMembership(product.getTenantId(), user.subject())) {
            return;
        }

        if (hasProductRole(user) && hasActiveAssignment(user, productId)) {
            return;
        }

        throw new ProductNotFoundException(productId);
    }

    private boolean hasActiveAssignment(AuthenticatedUser caller, UUID productId) {
        return assignmentRepository.existsByProductIdAndUserSubjectAndStatus(
                productId,
                caller.subject(),
                ProductAssignmentStatus.ASSIGNED
        );
    }

    private boolean hasProductRole(AuthenticatedUser caller) {
        return caller.authorities().contains(ROLE_PRODUCT_MANAGER)
                || caller.authorities().contains(ROLE_EDITOR)
                || caller.authorities().contains(ROLE_VIEWER);
    }
}
