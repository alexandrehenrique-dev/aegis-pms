package br.com.byop.aegis.product.service;

import br.com.byop.aegis.product.api.ProductAccessPort;
import br.com.byop.aegis.product.domain.ProductAssignmentStatus;
import br.com.byop.aegis.product.exception.ProductContentAccessDeniedException;
import br.com.byop.aegis.product.exception.ProductNotFoundException;
import br.com.byop.aegis.product.repository.ProductAssignmentRepository;
import br.com.byop.aegis.product.repository.ProductRepository;
import br.com.byop.aegis.security.AuthenticatedUser;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class ProductAccessResolver implements ProductAccessPort {

    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";

    private final ProductRepository productRepository;
    private final ProductAssignmentRepository assignmentRepository;

    public ProductAccessResolver(ProductRepository productRepository, ProductAssignmentRepository assignmentRepository) {
        this.productRepository = productRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public void assertAccessible(UUID productId, AuthenticatedUser user) {
        productRepository.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));

        if (user.authorities().contains(ROLE_SUPER_ADMIN)) {
            if (hasActiveAssignment(user, productId)) {
                return;
            }
            throw new ProductContentAccessDeniedException(productId);
        }

        if (hasActiveAssignment(user, productId)) {
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
}
