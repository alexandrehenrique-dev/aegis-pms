package br.com.byop.aegis.core.product;

import br.com.byop.aegis.core.RepositoryTestSupport;
import br.com.byop.aegis.core.tenant.Tenant;
import br.com.byop.aegis.core.tenant.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductAssignmentRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductAssignmentRepository assignmentRepository;

    @Test
    void shouldSaveAssignment() {
        Product product = saveProduct("assignment-save");

        ProductAssignment saved = assignmentRepository.saveAndFlush(
                new ProductAssignment(product, "subject-save", ProductAssignmentRole.PRODUCT_MANAGER)
        );

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getProduct()).isEqualTo(product);
        assertThat(saved.getUserSubject()).isEqualTo("subject-save");
        assertThat(saved.getRole()).isEqualTo(ProductAssignmentRole.PRODUCT_MANAGER);
        assertThat(saved.getStatus()).isEqualTo(ProductAssignmentStatus.ASSIGNED);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldFindByProductIdAndUserSubject() {
        Product product = saveProduct("assignment-find");
        ProductAssignment saved = assignmentRepository.saveAndFlush(
                new ProductAssignment(product, "subject-find", ProductAssignmentRole.EDITOR)
        );

        assertThat(assignmentRepository.findByProductIdAndUserSubject(product.getId(), "subject-find"))
                .contains(saved);
        assertThat(assignmentRepository.existsByProductIdAndUserSubjectAndStatus(
                product.getId(), "subject-find", ProductAssignmentStatus.ASSIGNED
        )).isTrue();
    }

    @Test
    void shouldFindProductsByUserSubjectForFutureAccessResolution() {
        Product first = saveProduct("assignment-list-a");
        Product second = saveProduct("assignment-list-b");
        ProductAssignment firstAssignment = assignmentRepository.saveAndFlush(
                new ProductAssignment(first, "subject-list", ProductAssignmentRole.EDITOR)
        );
        ProductAssignment secondAssignment = assignmentRepository.saveAndFlush(
                new ProductAssignment(second, "subject-list", ProductAssignmentRole.VIEWER)
        );

        assertThat(assignmentRepository.findAllByUserSubjectAndStatus("subject-list", ProductAssignmentStatus.ASSIGNED))
                .containsExactlyInAnyOrder(firstAssignment, secondAssignment);
    }

    @Test
    void shouldRejectDuplicateProductAndUserSubject() {
        Product product = saveProduct("assignment-duplicate");
        assignmentRepository.saveAndFlush(
                new ProductAssignment(product, "subject-duplicate", ProductAssignmentRole.EDITOR)
        );
        ProductAssignment duplicate = new ProductAssignment(product, "subject-duplicate", ProductAssignmentRole.VIEWER);

        assertThatThrownBy(() -> assignmentRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Product saveProduct(String suffix) {
        Tenant tenant = tenantRepository.saveAndFlush(tenant(suffix));
        return productRepository.saveAndFlush(product(tenant, suffix));
    }
}
