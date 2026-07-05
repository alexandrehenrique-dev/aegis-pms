package br.com.byop.aegis.product.api;

import br.com.byop.aegis.product.domain.ProductAssignment;
import br.com.byop.aegis.product.domain.ProductAssignmentRole;
import br.com.byop.aegis.product.domain.ProductAssignmentStatus;
import br.com.byop.aegis.product.repository.ProductAssignmentRepository;
import br.com.byop.aegis.product.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class ProductUserAccessService {

    private final ProductAssignmentRepository assignmentRepository;
    private final ProductRepository productRepository;

    public ProductUserAccessService(ProductAssignmentRepository assignmentRepository, ProductRepository productRepository) {
        this.assignmentRepository = assignmentRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductUserAccess> listTenantAssignments(UUID tenantId, String userSubject) {
        log.debug("listTenantAssignments: tenantId='{}', userSubject='{}'", tenantId, userSubject);
        return assignmentRepository.findAllByTenantIdAndUserSubject(tenantId, userSubject)
                .stream()
                .map(this::toAccess)
                .toList();
    }

    @Transactional(readOnly = true)
    public Set<String> listSharedUserSubjects(UUID tenantId, String callerSubject) {
        log.debug("listSharedUserSubjects: tenantId='{}', callerSubject='{}'", tenantId, callerSubject);
        Set<UUID> callerProductIds = new HashSet<>();
        assignmentRepository.findAllByTenantIdAndUserSubjectAndStatus(
                        tenantId,
                        callerSubject,
                        ProductAssignmentStatus.ASSIGNED
                )
                .forEach(assignment -> callerProductIds.add(assignment.getProductId()));

        Set<String> sharedSubjects = new HashSet<>();
        assignmentRepository.findAllByTenantIdAndStatus(tenantId, ProductAssignmentStatus.ASSIGNED)
                .stream()
                .filter(assignment -> callerProductIds.contains(assignment.getProductId()))
                .forEach(assignment -> sharedSubjects.add(assignment.getUserSubject()));
        return Set.copyOf(sharedSubjects);
    }

    @Transactional(readOnly = true)
    public long countDistinctAssignedUsers(Collection<UUID> productIds) {
        log.debug("countDistinctAssignedUsers: productCount='{}'", productIds.size());
        if (productIds.isEmpty()) {
            return 0;
        }
        return assignmentRepository.findAllByProductIdInAndStatus(productIds, ProductAssignmentStatus.ASSIGNED)
                .stream()
                .map(ProductAssignment::getUserSubject)
                .distinct()
                .count();
    }

    @Transactional(readOnly = true)
    public long countDistinctProductManagers(Collection<UUID> productIds) {
        log.debug("countDistinctProductManagers: productCount='{}'", productIds.size());
        if (productIds.isEmpty()) {
            return 0;
        }
        return assignmentRepository.findAllByProductIdInAndStatus(productIds, ProductAssignmentStatus.ASSIGNED)
                .stream()
                .filter(assignment -> assignment.getRole() == ProductAssignmentRole.PRODUCT_MANAGER)
                .map(ProductAssignment::getUserSubject)
                .distinct()
                .count();
    }

    @Transactional
    public void inviteTenantAssignments(UUID tenantId, String userSubject, String role, List<UUID> productIds) {
        log.debug("inviteTenantAssignments: tenantId='{}', userSubject='{}', productCount='{}'",
                tenantId, userSubject, productIds.size());
        if (productIds.isEmpty() || !isProductRole(role)) {
            return;
        }

        ProductAssignmentRole assignmentRole = ProductAssignmentRole.valueOf(role);
        productRepository.findAllById(productIds)
                .stream()
                .filter(product -> tenantId.equals(product.getTenantId()))
                .filter(product -> assignmentRepository.findByProductIdAndUserSubject(product.getId(), userSubject).isEmpty())
                .map(product -> {
                    ProductAssignment assignment = new ProductAssignment(product, userSubject, assignmentRole);
                    assignment.revoke();
                    return assignment;
                })
                .forEach(assignmentRepository::save);
    }

    @Transactional
    public void removeTenantAssignments(UUID tenantId, String userSubject) {
        log.debug("removeTenantAssignments: tenantId='{}', userSubject='{}'", tenantId, userSubject);
        List<ProductAssignment> assignments = assignmentRepository.findAllByTenantIdAndUserSubjectAndStatus(
                        tenantId,
                        userSubject,
                        ProductAssignmentStatus.ASSIGNED
                );
        assignments.forEach(ProductAssignment::remove);
        log.info("removeTenantAssignments: assignments removidos tenantId='{}', userSubject='{}', count='{}'",
                tenantId, userSubject, assignments.size());
    }

    private ProductUserAccess toAccess(ProductAssignment assignment) {
        return new ProductUserAccess(
                assignment.getTenantId(),
                assignment.getProductId(),
                assignment.getProduct().getName(),
                assignment.getUserSubject(),
                assignment.getRole().name(),
                assignment.getStatus().contractValue()
        );
    }

    private boolean isProductRole(String role) {
        return Arrays.stream(ProductAssignmentRole.values()).anyMatch(productRole -> productRole.name().equals(role));
    }
}
