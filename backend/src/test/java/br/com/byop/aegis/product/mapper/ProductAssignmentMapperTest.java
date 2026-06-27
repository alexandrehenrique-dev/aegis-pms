package br.com.byop.aegis.product.mapper;

import br.com.byop.aegis.product.api.AssetStorageStrategy;
import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.domain.ProductAssignment;
import br.com.byop.aegis.product.domain.ProductAssignmentRole;
import br.com.byop.aegis.product.domain.ProductAssignmentStatus;
import br.com.byop.aegis.product.domain.ProductTypeKey;
import br.com.byop.aegis.product.dto.ProductAssignmentSummary;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class ProductAssignmentMapperTest {

    private final ProductAssignmentMapper mapper = Mappers.getMapper(ProductAssignmentMapper.class);

    @Test
    void shouldMapAssignedAssignmentToContractSummary() {
        UUID tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID productId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID assignmentId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-06-26T10:00:00-03:00");
        OffsetDateTime updatedAt = OffsetDateTime.parse("2026-06-26T10:10:00-03:00");
        Product product = product(tenantId, productId);
        ProductAssignment assignment = assignment(
                product,
                assignmentId,
                "subject-123",
                ProductAssignmentRole.PRODUCT_MANAGER,
                ProductAssignmentStatus.ASSIGNED,
                createdAt,
                updatedAt
        );

        ProductAssignmentSummary summary = mapper.toSummary(assignment, "Alexandre Silva", "alexandre@byop.dev");

        assertThat(summary).isNotNull();
        assertThat(summary.id()).isEqualTo(assignmentId);
        assertThat(summary.tenantId()).isEqualTo(tenantId);
        assertThat(summary.productId()).isEqualTo(productId);
        assertThat(summary.productName()).isEqualTo("Aegis PMS");
        assertThat(summary.userSubject()).isEqualTo("subject-123");
        assertThat(summary.userName()).isEqualTo("Alexandre Silva");
        assertThat(summary.userEmail()).isEqualTo("alexandre@byop.dev");
        assertThat(summary.role()).isEqualTo("PRODUCT_MANAGER");
        assertThat(summary.status()).isEqualTo("atribuido");
        assertThat(summary.createdAt()).isEqualTo(createdAt);
        assertThat(summary.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void shouldMapInvitedAssignmentToContractSummary() {
        Product product = product(
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                UUID.fromString("55555555-5555-5555-5555-555555555555")
        );
        ProductAssignment assignment = assignment(
                product,
                UUID.fromString("66666666-6666-6666-6666-666666666666"),
                "subject-invited",
                ProductAssignmentRole.VIEWER,
                ProductAssignmentStatus.INVITED,
                OffsetDateTime.parse("2026-06-26T11:00:00-03:00"),
                OffsetDateTime.parse("2026-06-26T11:10:00-03:00")
        );

        ProductAssignmentSummary summary = mapper.toSummary(assignment, "Invited User", "invited@byop.dev");

        assertThat(summary.status()).isEqualTo("convidado");
        assertThat(summary.role()).isEqualTo("VIEWER");
    }

    @Test
    void shouldMapNullRoleAndStatusToNullContractValues() {
        assertThat(mapper.toContractRole(null)).isNull();
        assertThat(mapper.toContractStatus(null)).isNull();
    }

    @Test
    void shouldReturnNullWhenAllSourcesAreNull() {
        assertThat(mapper.toSummary(null, null, null)).isNull();
    }

    @Test
    void shouldMapOnlyUserFieldsWhenAssignmentIsNull() {
        ProductAssignmentSummary summary = mapper.toSummary(null, "Only User", "only@byop.dev");

        assertThat(summary).isNotNull();
        assertThat(summary.id()).isNull();
        assertThat(summary.productName()).isNull();
        assertThat(summary.userName()).isEqualTo("Only User");
        assertThat(summary.userEmail()).isEqualTo("only@byop.dev");
        assertThat(summary.role()).isNull();
        assertThat(summary.status()).isNull();
    }

    @Test
    void shouldMapOnlyUserEmailWhenAssignmentAndUserNameAreNull() {
        ProductAssignmentSummary summary = mapper.toSummary(null, null, "only-email@byop.dev");

        assertThat(summary).isNotNull();
        assertThat(summary.userName()).isNull();
        assertThat(summary.userEmail()).isEqualTo("only-email@byop.dev");
    }

    @Test
    void shouldMapNullProductNameWhenProductIsNull() {
        Product product = product(
                UUID.fromString("77777777-7777-7777-7777-777777777777"),
                UUID.fromString("88888888-8888-8888-8888-888888888888")
        );
        ProductAssignment assignment = assignment(
                product,
                UUID.fromString("99999999-9999-9999-9999-999999999999"),
                "subject-null-product",
                ProductAssignmentRole.EDITOR,
                ProductAssignmentStatus.ASSIGNED,
                OffsetDateTime.parse("2026-06-26T12:00:00-03:00"),
                OffsetDateTime.parse("2026-06-26T12:10:00-03:00")
        );
        ReflectionTestUtils.setField(assignment, "product", null);

        ProductAssignmentSummary summary = mapper.toSummary(assignment, null, null);

        assertThat(summary.productName()).isNull();
    }

    private Product product(UUID tenantId, UUID productId) {
        Product product = new Product(
                tenantId,
                "aegis-pms",
                "Aegis PMS",
                ProductTypeKey.SITE_INSTITUCIONAL,
                "pt-BR",
                AssetStorageStrategy.LOCAL
        );
        ReflectionTestUtils.setField(product, "id", productId);
        return product;
    }

    private ProductAssignment assignment(Product product, UUID assignmentId, String userSubject,
                                         ProductAssignmentRole role, ProductAssignmentStatus status,
                                         OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        ProductAssignment assignment = new ProductAssignment(product, userSubject, ProductAssignmentRole.VIEWER);
        ReflectionTestUtils.setField(assignment, "id", assignmentId);
        ReflectionTestUtils.setField(assignment, "role", role);
        ReflectionTestUtils.setField(assignment, "status", status);
        ReflectionTestUtils.setField(assignment, "productId", product.getId());
        ReflectionTestUtils.setField(assignment, "createdAt", createdAt);
        ReflectionTestUtils.setField(assignment, "updatedAt", updatedAt);
        return assignment;
    }
}
