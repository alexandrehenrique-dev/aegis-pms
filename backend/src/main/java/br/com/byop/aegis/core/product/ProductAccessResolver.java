package br.com.byop.aegis.core.product;

import br.com.byop.aegis.core.product.exception.ProductContentAccessDeniedException;
import br.com.byop.aegis.core.product.exception.ProductNotFoundException;
import br.com.byop.aegis.core.tenant.TenantMembershipRepository;
import br.com.byop.aegis.core.tenant.TenantMembershipStatus;
import br.com.byop.aegis.security.AuthenticatedUser;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class ProductAccessResolver {

    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String ROLE_TENANT_ADMIN = "ROLE_TENANT_ADMIN";
    private static final String ROLE_PRODUCT_MANAGER = "ROLE_PRODUCT_MANAGER";
    private static final String ROLE_EDITOR = "ROLE_EDITOR";
    private static final String ROLE_VIEWER = "ROLE_VIEWER";

    private final ProductRepository productRepository;
    private final TenantMembershipRepository membershipRepository;
    private final ProductAssignmentRepository assignmentRepository;

    public ProductAccessResolver(ProductRepository productRepository, TenantMembershipRepository membershipRepository,
                                 ProductAssignmentRepository assignmentRepository) {
        this.productRepository = productRepository;
        this.membershipRepository = membershipRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional(readOnly = true)
    public void assertContentAccess(AuthenticatedUser caller, UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        if (caller.authorities().contains(ROLE_SUPER_ADMIN)) {
            if (hasActiveAssignment(caller, productId)) {
                return;
            }
            throw new ProductContentAccessDeniedException(productId);
        }

        if (caller.authorities().contains(ROLE_TENANT_ADMIN)
                && membershipRepository.existsByTenantIdAndUserSubjectAndStatus(
                product.getTenant().getId(), caller.subject(), TenantMembershipStatus.ACTIVE)) {
            return;
        }

        if (hasProductRole(caller) && hasActiveAssignment(caller, productId)) {
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
