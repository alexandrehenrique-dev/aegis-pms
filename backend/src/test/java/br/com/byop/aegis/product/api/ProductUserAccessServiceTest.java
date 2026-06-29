package br.com.byop.aegis.product.api;

import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.domain.ProductAssignment;
import br.com.byop.aegis.product.domain.ProductAssignmentRole;
import br.com.byop.aegis.product.domain.ProductAssignmentStatus;
import br.com.byop.aegis.product.domain.ProductTypeKey;
import br.com.byop.aegis.product.repository.ProductAssignmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductUserAccessServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PRODUCT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private ProductAssignmentRepository assignmentRepository;

    @InjectMocks
    private ProductUserAccessService service;

    @Test
    void shouldListTenantAssignments() {
        ProductAssignment assignment = assignment("user-1", ProductAssignmentRole.EDITOR);
        when(assignmentRepository.findAllByTenantIdAndUserSubject(TENANT_ID, "user-1"))
                .thenReturn(List.of(assignment));

        ProductUserAccess access = service.listTenantAssignments(TENANT_ID, "user-1").getFirst();

        assertThat(access.productId()).isEqualTo(PRODUCT_ID);
        assertThat(access.productName()).isEqualTo("Aegis PMS");
        assertThat(access.status()).isEqualTo("atribuido");
    }

    @Test
    void shouldListSharedUserSubjects() {
        ProductAssignment caller = assignment("caller", ProductAssignmentRole.PRODUCT_MANAGER);
        ProductAssignment shared = assignment("user-1", ProductAssignmentRole.EDITOR);
        when(assignmentRepository.findAllByTenantIdAndUserSubjectAndStatus(
                TENANT_ID,
                "caller",
                ProductAssignmentStatus.ASSIGNED
        )).thenReturn(List.of(caller));
        when(assignmentRepository.findAllByTenantIdAndStatus(TENANT_ID, ProductAssignmentStatus.ASSIGNED))
                .thenReturn(List.of(caller, shared));

        Set<String> subjects = service.listSharedUserSubjects(TENANT_ID, "caller");

        assertThat(subjects).containsExactlyInAnyOrder("caller", "user-1");
    }

    @Test
    void shouldCountDistinctAssignedUsers() {
        ProductAssignment pm = assignment("pm-1", ProductAssignmentRole.PRODUCT_MANAGER);
        ProductAssignment editor = assignment("editor-1", ProductAssignmentRole.EDITOR);
        when(assignmentRepository.findAllByProductIdInAndStatus(List.of(PRODUCT_ID), ProductAssignmentStatus.ASSIGNED))
                .thenReturn(List.of(pm, editor));

        assertThat(service.countDistinctAssignedUsers(List.of(PRODUCT_ID))).isEqualTo(2);
    }

    @Test
    void shouldReturnZeroDistinctAssignedUsersWhenNoProductIds() {
        assertThat(service.countDistinctAssignedUsers(List.of())).isZero();
    }

    @Test
    void shouldCountDistinctProductManagers() {
        ProductAssignment pm = assignment("pm-1", ProductAssignmentRole.PRODUCT_MANAGER);
        ProductAssignment editor = assignment("editor-1", ProductAssignmentRole.EDITOR);
        when(assignmentRepository.findAllByProductIdInAndStatus(List.of(PRODUCT_ID), ProductAssignmentStatus.ASSIGNED))
                .thenReturn(List.of(pm, editor));

        assertThat(service.countDistinctProductManagers(List.of(PRODUCT_ID))).isEqualTo(1);
    }

    @Test
    void shouldReturnZeroDistinctProductManagersWhenNoProductIds() {
        assertThat(service.countDistinctProductManagers(List.of())).isZero();
    }

    @Test
    void shouldRemoveTenantAssignments() {
        ProductAssignment assignment = assignment("user-1", ProductAssignmentRole.EDITOR);
        when(assignmentRepository.findAllByTenantIdAndUserSubjectAndStatus(
                TENANT_ID,
                "user-1",
                ProductAssignmentStatus.ASSIGNED
        )).thenReturn(List.of(assignment));

        service.removeTenantAssignments(TENANT_ID, "user-1");

        assertThat(assignment.getStatus()).isEqualTo(ProductAssignmentStatus.REMOVED);
    }

    private ProductAssignment assignment(String subject, ProductAssignmentRole role) {
        Product product = new Product(
                TENANT_ID,
                "aegis-pms",
                "Aegis PMS",
                ProductTypeKey.SITE_INSTITUCIONAL,
                "pt-BR",
                AssetStorageStrategy.LOCAL
        );
        ReflectionTestUtils.setField(product, "id", PRODUCT_ID);
        ProductAssignment assignment = new ProductAssignment(product, subject, role);
        ReflectionTestUtils.setField(assignment, "id", UUID.nameUUIDFromBytes(subject.getBytes()));
        return assignment;
    }
}
