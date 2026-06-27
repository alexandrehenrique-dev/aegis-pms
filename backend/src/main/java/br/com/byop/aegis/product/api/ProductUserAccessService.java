package br.com.byop.aegis.product.api;

import br.com.byop.aegis.product.domain.ProductAssignment;
import br.com.byop.aegis.product.domain.ProductAssignmentStatus;
import br.com.byop.aegis.product.repository.ProductAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ProductUserAccessService {

    private final ProductAssignmentRepository assignmentRepository;

    public ProductUserAccessService(ProductAssignmentRepository assignmentRepository) {
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductUserAccess> listTenantAssignments(UUID tenantId, String userSubject) {
        return assignmentRepository.findAllByTenantIdAndUserSubject(tenantId, userSubject)
                .stream()
                .map(this::toAccess)
                .toList();
    }

    @Transactional(readOnly = true)
    public Set<String> listSharedUserSubjects(UUID tenantId, String callerSubject) {
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

    @Transactional
    public void removeTenantAssignments(UUID tenantId, String userSubject) {
        assignmentRepository.findAllByTenantIdAndUserSubjectAndStatus(
                        tenantId,
                        userSubject,
                        ProductAssignmentStatus.ASSIGNED
                )
                .forEach(ProductAssignment::remove);
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
}
