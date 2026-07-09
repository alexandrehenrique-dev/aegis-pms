package br.com.byop.aegis.product.api;

import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.domain.ProductAssignment;
import br.com.byop.aegis.product.domain.ProductAssignmentRole;
import br.com.byop.aegis.product.domain.ProductAssignmentStatus;
import br.com.byop.aegis.product.domain.ProductTypeKey;
import br.com.byop.aegis.product.repository.ProductAssignmentRepository;
import br.com.byop.aegis.product.repository.ProductRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductUserAccessServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PRODUCT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private ProductAssignmentRepository assignmentRepository;

    @Mock
    private ProductRepository productRepository;

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
    void shouldListOnlyProductManagerProductIds() {
        ProductAssignment manager = assignment("manager", ProductAssignmentRole.PRODUCT_MANAGER);
        ProductAssignment editor = assignment("editor", ProductAssignmentRole.EDITOR);
        when(assignmentRepository.findAllByTenantIdAndUserSubjectAndStatus(
                TENANT_ID,
                "manager",
                ProductAssignmentStatus.ASSIGNED
        )).thenReturn(List.of(manager, editor));

        Set<UUID> productIds = service.listProductManagerProductIds(TENANT_ID, "manager");

        assertThat(productIds).containsExactly(PRODUCT_ID);
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
    void shouldListDistinctAssignedUserSubjects() {
        ProductAssignment pm = assignment("pm-1", ProductAssignmentRole.PRODUCT_MANAGER);
        ProductAssignment duplicatedPm = assignment("pm-1", ProductAssignmentRole.EDITOR);
        ProductAssignment editor = assignment("editor-1", ProductAssignmentRole.EDITOR);
        when(assignmentRepository.findAllByProductIdInAndStatus(List.of(PRODUCT_ID), ProductAssignmentStatus.ASSIGNED))
                .thenReturn(List.of(pm, duplicatedPm, editor));

        Set<String> subjects = service.listDistinctAssignedUserSubjects(List.of(PRODUCT_ID));

        assertThat(subjects).containsExactlyInAnyOrder("pm-1", "editor-1");
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
    void shouldListDistinctProductManagerSubjects() {
        ProductAssignment pm = assignment("pm-1", ProductAssignmentRole.PRODUCT_MANAGER);
        ProductAssignment duplicatedPm = assignment("pm-1", ProductAssignmentRole.PRODUCT_MANAGER);
        ProductAssignment editor = assignment("editor-1", ProductAssignmentRole.EDITOR);
        when(assignmentRepository.findAllByProductIdInAndStatus(List.of(PRODUCT_ID), ProductAssignmentStatus.ASSIGNED))
                .thenReturn(List.of(pm, duplicatedPm, editor));

        Set<String> subjects = service.listDistinctProductManagerSubjects(List.of(PRODUCT_ID));

        assertThat(subjects).containsExactly("pm-1");
    }

    @Test
    void shouldReturnZeroDistinctProductManagersWhenNoProductIds() {
        assertThat(service.countDistinctProductManagers(List.of())).isZero();
    }

    @Test
    void shouldFindHighestAssignedRoleByProductRolePriority() {
        ProductAssignment viewer = assignment("user-1-viewer", ProductAssignmentRole.VIEWER);
        ProductAssignment editor = assignment("user-1-editor", ProductAssignmentRole.EDITOR);
        ProductAssignment manager = assignment("user-1-manager", ProductAssignmentRole.PRODUCT_MANAGER);
        when(assignmentRepository.findAllByUserSubjectAndStatus("user-1", ProductAssignmentStatus.ASSIGNED))
                .thenReturn(List.of(viewer, editor, manager));

        assertThat(service.findHighestAssignedRole("user-1")).contains("PRODUCT_MANAGER");
    }

    @Test
    void shouldReturnEmptyHighestAssignedRoleWhenUserHasNoAssignedProducts() {
        when(assignmentRepository.findAllByUserSubjectAndStatus("user-1", ProductAssignmentStatus.ASSIGNED))
                .thenReturn(List.of());

        assertThat(service.findHighestAssignedRole("user-1")).isEmpty();
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

    @Test
    void shouldCreateInvitedAssignmentsForTenantInvite() {
        Product product = product();
        when(productRepository.findAllById(List.of(PRODUCT_ID))).thenReturn(List.of(product));
        when(assignmentRepository.findByProductIdAndUserSubject(PRODUCT_ID, "user-1")).thenReturn(java.util.Optional.empty());

        service.inviteTenantAssignments(TENANT_ID, "user-1", "EDITOR", List.of(PRODUCT_ID));

        org.mockito.ArgumentCaptor<ProductAssignment> assignmentCaptor =
                org.mockito.ArgumentCaptor.forClass(ProductAssignment.class);
        verify(assignmentRepository).save(assignmentCaptor.capture());
        ProductAssignment saved = assignmentCaptor.getValue();
        assertThat(saved.getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(saved.getUserSubject()).isEqualTo("user-1");
        assertThat(saved.getRole()).isEqualTo(ProductAssignmentRole.EDITOR);
        assertThat(saved.getStatus()).isEqualTo(ProductAssignmentStatus.INVITED);
    }

    @Test
    void shouldCreateAssignedAssignmentsForExistingTenantUser() {
        Product product = product();
        when(productRepository.findAllById(List.of(PRODUCT_ID))).thenReturn(List.of(product));
        when(assignmentRepository.findByProductIdAndUserSubject(PRODUCT_ID, "user-1")).thenReturn(java.util.Optional.empty());

        service.grantTenantAssignments(TENANT_ID, "user-1", "EDITOR", List.of(PRODUCT_ID));

        org.mockito.ArgumentCaptor<ProductAssignment> assignmentCaptor =
                org.mockito.ArgumentCaptor.forClass(ProductAssignment.class);
        verify(assignmentRepository).save(assignmentCaptor.capture());
        ProductAssignment saved = assignmentCaptor.getValue();
        assertThat(saved.getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(saved.getUserSubject()).isEqualTo("user-1");
        assertThat(saved.getRole()).isEqualTo(ProductAssignmentRole.EDITOR);
        assertThat(saved.getStatus()).isEqualTo(ProductAssignmentStatus.ASSIGNED);
    }

    @Test
    void shouldUpdateExistingProductAssignmentWithoutDuplicatingUser() {
        Product product = product();
        ProductAssignment removed = assignment("user-1", ProductAssignmentRole.VIEWER);
        removed.remove();
        when(productRepository.findAllById(List.of(PRODUCT_ID))).thenReturn(List.of(product));
        when(assignmentRepository.findByProductIdAndUserSubject(PRODUCT_ID, "user-1"))
                .thenReturn(java.util.Optional.of(removed));

        service.grantTenantAssignments(TENANT_ID, "user-1", "EDITOR", List.of(PRODUCT_ID));

        assertThat(removed.getRole()).isEqualTo(ProductAssignmentRole.EDITOR);
        assertThat(removed.getStatus()).isEqualTo(ProductAssignmentStatus.ASSIGNED);
        verify(assignmentRepository).save(removed);
    }

    @Test
    void shouldSkipTenantInviteAssignmentsForTenantRole() {
        service.inviteTenantAssignments(TENANT_ID, "user-1", "TENANT_ADMIN", List.of(PRODUCT_ID));

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void shouldSkipTenantInviteAssignmentsWhenNoProductsAllowed() {
        service.inviteTenantAssignments(TENANT_ID, "user-1", "EDITOR", List.of());

        verify(assignmentRepository, never()).save(any());
        verify(productRepository, never()).findAllById(any());
    }

    private ProductAssignment assignment(String subject, ProductAssignmentRole role) {
        Product product = product();
        ProductAssignment assignment = new ProductAssignment(product, subject, role);
        ReflectionTestUtils.setField(assignment, "id", UUID.nameUUIDFromBytes(subject.getBytes()));
        return assignment;
    }

    private Product product() {
        Product product = new Product(
                TENANT_ID,
                "aegis-pms",
                "Aegis PMS",
                ProductTypeKey.SITE_INSTITUCIONAL,
                "pt-BR",
                AssetStorageStrategy.LOCAL
        );
        ReflectionTestUtils.setField(product, "id", PRODUCT_ID);
        return product;
    }
}
