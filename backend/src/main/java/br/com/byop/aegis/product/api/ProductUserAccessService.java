package br.com.byop.aegis.product.api;

import br.com.byop.aegis.product.domain.ProductAssignment;
import br.com.byop.aegis.product.domain.ProductAssignmentRole;
import br.com.byop.aegis.product.domain.ProductAssignmentStatus;
import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.repository.ProductAssignmentRepository;
import br.com.byop.aegis.product.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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
    public Set<UUID> listProductManagerProductIds(UUID tenantId, String userSubject) {
        log.debug("listProductManagerProductIds: tenantId='{}', userSubject='{}'", tenantId, userSubject);
        return assignmentRepository.findAllByTenantIdAndUserSubjectAndStatus(
                        tenantId,
                        userSubject,
                        ProductAssignmentStatus.ASSIGNED
                )
                .stream()
                .filter(assignment -> assignment.getRole() == ProductAssignmentRole.PRODUCT_MANAGER)
                .map(ProductAssignment::getProductId)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Transactional(readOnly = true)
    public long countDistinctAssignedUsers(Collection<UUID> productIds) {
        log.debug("countDistinctAssignedUsers: productCount='{}'", productIds.size());
        return resolveDistinctAssignedUserSubjects(productIds).size();
    }

    @Transactional(readOnly = true)
    public Set<String> listDistinctAssignedUserSubjects(Collection<UUID> productIds) {
        log.debug("listDistinctAssignedUserSubjects: productCount='{}'", productIds.size());
        return resolveDistinctAssignedUserSubjects(productIds);
    }

    @Transactional(readOnly = true)
    public long countDistinctProductManagers(Collection<UUID> productIds) {
        log.debug("countDistinctProductManagers: productCount='{}'", productIds.size());
        return resolveDistinctProductManagerSubjects(productIds).size();
    }

    @Transactional(readOnly = true)
    public Set<String> listDistinctProductManagerSubjects(Collection<UUID> productIds) {
        log.debug("listDistinctProductManagerSubjects: productCount='{}'", productIds.size());
        return resolveDistinctProductManagerSubjects(productIds);
    }

    private Set<String> resolveDistinctAssignedUserSubjects(Collection<UUID> productIds) {
        if (productIds.isEmpty()) {
            return Set.of();
        }
        return assignmentRepository.findAllByProductIdInAndStatus(productIds, ProductAssignmentStatus.ASSIGNED)
                .stream()
                .map(ProductAssignment::getUserSubject)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private Set<String> resolveDistinctProductManagerSubjects(Collection<UUID> productIds) {
        if (productIds.isEmpty()) {
            return Set.of();
        }
        return assignmentRepository.findAllByProductIdInAndStatus(productIds, ProductAssignmentStatus.ASSIGNED)
                .stream()
                .filter(assignment -> assignment.getRole() == ProductAssignmentRole.PRODUCT_MANAGER)
                .map(ProductAssignment::getUserSubject)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Transactional(readOnly = true)
    public Optional<String> findHighestAssignedRole(String userSubject) {
        log.debug("findHighestAssignedRole: userSubject='{}'", userSubject);
        return assignmentRepository.findAllByUserSubjectAndStatus(userSubject, ProductAssignmentStatus.ASSIGNED)
                .stream()
                .map(ProductAssignment::getRole)
                .min(this::compareProductRolePriority)
                .map(ProductAssignmentRole::name);
    }

    @Transactional
    public void inviteTenantAssignments(UUID tenantId, String userSubject, String role, List<UUID> productIds) {
        log.debug("inviteTenantAssignments: tenantId='{}', userSubject='{}', productCount='{}'",
                tenantId, userSubject, productIds.size());
        upsertTenantAssignments(tenantId, userSubject, role, productIds, ProductAssignmentStatus.INVITED);
    }

    @Transactional
    public void grantTenantAssignments(UUID tenantId, String userSubject, String role, List<UUID> productIds) {
        log.debug("grantTenantAssignments: tenantId='{}', userSubject='{}', productCount='{}'",
                tenantId, userSubject, productIds.size());
        upsertTenantAssignments(tenantId, userSubject, role, productIds, ProductAssignmentStatus.ASSIGNED);
    }

    private void upsertTenantAssignments(UUID tenantId, String userSubject, String role, List<UUID> productIds,
                                         ProductAssignmentStatus status) {
        if (productIds.isEmpty() || !isProductRole(role)) {
            return;
        }

        ProductAssignmentRole assignmentRole = ProductAssignmentRole.valueOf(role);
        productRepository.findAllById(productIds)
                .stream()
                .filter(product -> tenantId.equals(product.getTenantId()))
                .map(product -> assignmentRepository.findByProductIdAndUserSubject(product.getId(), userSubject)
                        .map(assignment -> updateAssignment(assignment, assignmentRole, status))
                        .orElseGet(() -> newAssignment(product, userSubject, assignmentRole, status)))
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

    private ProductAssignment newAssignment(Product product, String userSubject, ProductAssignmentRole role,
                                            ProductAssignmentStatus status) {
        ProductAssignment assignment = new ProductAssignment(product, userSubject, role);
        return updateAssignment(assignment, role, status);
    }

    private ProductAssignment updateAssignment(ProductAssignment assignment, ProductAssignmentRole role,
                                               ProductAssignmentStatus status) {
        assignment.changeRole(role);
        if (status == ProductAssignmentStatus.ASSIGNED) {
            assignment.assign();
        } else {
            assignment.revoke();
        }
        return assignment;
    }

    private boolean isProductRole(String role) {
        return Arrays.stream(ProductAssignmentRole.values()).anyMatch(productRole -> productRole.name().equals(role));
    }

    private int compareProductRolePriority(ProductAssignmentRole left, ProductAssignmentRole right) {
        return Integer.compare(productRolePriority(left), productRolePriority(right));
    }

    private int productRolePriority(ProductAssignmentRole role) {
        return switch (role) {
            case PRODUCT_MANAGER -> 0;
            case EDITOR -> 1;
            case VIEWER -> 2;
        };
    }
}
