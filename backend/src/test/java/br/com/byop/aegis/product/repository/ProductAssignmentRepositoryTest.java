package br.com.byop.aegis.product.repository;

import br.com.byop.aegis.core.RepositoryTestSupport;
import br.com.byop.aegis.product.api.ModuleKey;
import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.domain.ProductAssignment;
import br.com.byop.aegis.product.domain.ProductAssignmentRole;
import br.com.byop.aegis.product.domain.ProductAssignmentStatus;
import br.com.byop.aegis.product.domain.ProductModule;
import br.com.byop.aegis.tenant.domain.Tenant;
import br.com.byop.aegis.tenant.domain.TenantMembership;
import br.com.byop.aegis.tenant.repository.TenantMembershipRepository;
import br.com.byop.aegis.tenant.repository.TenantRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductAssignmentRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductModuleRepository moduleRepository;

    @Autowired
    private ProductAssignmentRepository assignmentRepository;

    @Autowired
    private TenantMembershipRepository membershipRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldSaveAssignment() {
        Product product = saveProduct("assignment-save");

        ProductAssignment saved = assignmentRepository.saveAndFlush(
                new ProductAssignment(product, "subject-save", ProductAssignmentRole.PRODUCT_MANAGER)
        );
        entityManager.clear();
        ProductAssignment reloaded = assignmentRepository.findById(saved.getId()).orElseThrow();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTenantId()).isEqualTo(product.getTenantId());
        assertThat(reloaded.getProductId()).isEqualTo(product.getId());
        assertThat(saved.getProduct()).isEqualTo(product);
        assertThat(saved.getUserSubject()).isEqualTo("subject-save");
        assertThat(saved.getRole()).isEqualTo(ProductAssignmentRole.PRODUCT_MANAGER);
        assertThat(saved.getStatus()).isEqualTo(ProductAssignmentStatus.ASSIGNED);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldSupportAssignedAndInvitedContractStatuses() {
        Product product = saveProduct("assignment-status");
        ProductAssignment assigned = new ProductAssignment(product, "subject-assigned", ProductAssignmentRole.EDITOR);
        ProductAssignment invited = new ProductAssignment(product, "subject-invited", ProductAssignmentRole.VIEWER);
        invited.revoke();

        assignmentRepository.saveAndFlush(assigned);
        assignmentRepository.saveAndFlush(invited);

        assertThat(assigned.getStatus()).isEqualTo(ProductAssignmentStatus.fromContractValue("atribuido"));
        assertThat(assigned.getStatus().contractValue()).isEqualTo("atribuido");
        assertThat(invited.getStatus()).isEqualTo(ProductAssignmentStatus.fromContractValue("convidado"));
        assertThat(invited.getStatus().contractValue()).isEqualTo("convidado");
        assertThatThrownBy(() -> ProductAssignmentStatus.fromContractValue("revogado"))
                .isInstanceOf(IllegalArgumentException.class);
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

    @Test
    void shouldCascadeDeleteAssignmentsAndModulesWhenProductIsDeleted() {
        Product product = saveProduct("assignment-product-cascade");
        moduleRepository.saveAndFlush(new ProductModule(product, ModuleKey.CONTENT));
        assignmentRepository.saveAndFlush(
                new ProductAssignment(product, "subject-product-cascade", ProductAssignmentRole.EDITOR)
        );

        jdbcTemplate.update("DELETE FROM products WHERE id = ?", product.getId());

        assertThat(countRows("products", "id", product.getId())).isZero();
        assertThat(countRows("product_modules", "product_id", product.getId())).isZero();
        assertThat(countRows("product_assignments", "product_id", product.getId())).isZero();
    }

    @Test
    void shouldCascadeDeleteTenantChildrenWhenTenantIsDeleted() {
        Tenant tenant = tenantRepository.saveAndFlush(tenant("assignment-tenant-cascade"));
        Product product = productRepository.saveAndFlush(product(tenant, "assignment-tenant-cascade"));
        membershipRepository.saveAndFlush(new TenantMembership(tenant, "subject-tenant-cascade", "TENANT_ADMIN"));
        moduleRepository.saveAndFlush(new ProductModule(product, ModuleKey.CONTENT));
        assignmentRepository.saveAndFlush(
                new ProductAssignment(product, "subject-tenant-cascade", ProductAssignmentRole.PRODUCT_MANAGER)
        );

        jdbcTemplate.update("DELETE FROM tenants WHERE id = ?", tenant.getId());

        assertThat(countRows("tenants", "id", tenant.getId())).isZero();
        assertThat(countRows("products", "id", product.getId())).isZero();
        assertThat(countRows("product_modules", "product_id", product.getId())).isZero();
        assertThat(countRows("tenant_memberships", "tenant_id", tenant.getId())).isZero();
        assertThat(countRows("product_assignments", "product_id", product.getId())).isZero();
    }

    private Product saveProduct(String suffix) {
        Tenant tenant = tenantRepository.saveAndFlush(tenant(suffix));
        return productRepository.saveAndFlush(product(tenant, suffix));
    }

    private Long countRows(String tableName, String columnName, Object value) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName + " WHERE " + columnName + " = ?",
                Long.class,
                value
        );
    }
}
