package br.com.byop.aegis.product.service;

import br.com.byop.aegis.audit.api.AuditRecordCommand;
import br.com.byop.aegis.audit.api.AuditService;
import br.com.byop.aegis.identity.api.IdentityUser;
import br.com.byop.aegis.identity.api.IdentityUserDirectory;
import br.com.byop.aegis.product.contract.AssignProductUserRequest;
import br.com.byop.aegis.product.api.AssetStorageStrategy;
import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.domain.ProductAssignment;
import br.com.byop.aegis.product.domain.ProductAssignmentRole;
import br.com.byop.aegis.product.domain.ProductAssignmentStatus;
import br.com.byop.aegis.product.domain.ProductTypeKey;
import br.com.byop.aegis.product.dto.ProductAssignmentSummary;
import br.com.byop.aegis.product.exception.InvalidProductAssignmentException;
import br.com.byop.aegis.product.exception.ProductAssignmentNotFoundException;
import br.com.byop.aegis.product.exception.ProductNotFoundException;
import br.com.byop.aegis.product.mapper.ProductAssignmentMapper;
import br.com.byop.aegis.product.repository.ProductAssignmentRepository;
import br.com.byop.aegis.product.repository.ProductRepository;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.tenant.api.TenantAccessService;
import br.com.byop.aegis.tenant.api.TenantReference;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductAssignmentServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductAssignmentRepository assignmentRepository;

    @Mock
    private TenantAccessService tenantAccessService;

    @Mock
    private IdentityUserDirectory userDirectory;

    @Mock
    private ProductAssignmentInvitePort invitePort;

    @Mock
    private ProductAssignmentEmailPort emailPort;

    @Mock
    private ProductAssignmentNotificationPort notificationPort;

    @Mock
    private ProductAssignmentMapper assignmentMapper;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ProductAssignmentService service;

    @Test
    void shouldListAssignmentsForProduct() {
        Product product = product();
        ProductAssignment assignment = assignment(product, "user-1", ProductAssignmentRole.EDITOR);
        ProductAssignmentSummary summary = summary(product, "user-1", "Editor", "editor@byop.dev");
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(assignmentRepository.findAllByProductId(product.getId())).thenReturn(List.of(assignment));
        when(assignmentMapper.toSummary(assignment, null, null)).thenReturn(summary);

        List<ProductAssignmentSummary> result = service.listAssignments(product.getId());

        assertThat(result).containsExactly(summary);
    }

    @Test
    void shouldAssignExistingUser() {
        Product product = product();
        IdentityUser user = new IdentityUser("user-1", "editor", "editor@byop.dev", "Editor", "User");
        ProductAssignment saved = assignment(product, "user-1", ProductAssignmentRole.EDITOR);
        ProductAssignmentSummary summary = summary(product, "user-1", "Editor User", "editor@byop.dev");
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(tenantAccessService.hasActiveMembership(product.getTenantId(), "user-1")).thenReturn(true);
        when(tenantAccessService.getRequiredReference(product.getTenantId()))
                .thenReturn(new TenantReference(product.getTenantId(), "Tenant Aegis"));
        when(userDirectory.getRequiredUser("user-1")).thenReturn(user);
        when(assignmentRepository.save(any(ProductAssignment.class))).thenReturn(saved);
        when(assignmentMapper.toSummary(saved, "Editor User", "editor@byop.dev")).thenReturn(summary);

        ProductAssignmentSummary result = service.assignUser(
                caller(),
                product.getId(),
                request(product, "user-1", null)
        );

        assertThat(result).isEqualTo(summary);
        ArgumentCaptor<ProductAssignment> captor = ArgumentCaptor.forClass(ProductAssignment.class);
        verify(assignmentRepository).save(captor.capture());
        assertThat(captor.getValue().getProduct()).isEqualTo(product);
        assertThat(captor.getValue().getUserSubject()).isEqualTo("user-1");
        assertThat(captor.getValue().getRole()).isEqualTo(ProductAssignmentRole.EDITOR);
        assertThat(captor.getValue().getStatus()).isEqualTo(ProductAssignmentStatus.ASSIGNED);
        ArgumentCaptor<ProductAssignmentEmailCommand> emailCaptor =
                ArgumentCaptor.forClass(ProductAssignmentEmailCommand.class);
        verify(emailPort).notifyAssignment(emailCaptor.capture());
        assertThat(emailCaptor.getValue().tenantName()).isEqualTo("Tenant Aegis");
        assertThat(emailCaptor.getValue().productName()).isEqualTo("Aegis PMS");
        assertThat(emailCaptor.getValue().recipientEmail()).isEqualTo("editor@byop.dev");
        assertThat(emailCaptor.getValue().recipientName()).isEqualTo("Editor User");
        verify(notificationPort).notifyAssignment(product.getTenantId(), product.getId(), "user-1");
        ArgumentCaptor<AuditRecordCommand> auditCaptor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService).recordEvent(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo("PRODUCT_ASSIGNMENT_CREATED");
        assertThat(auditCaptor.getValue().tenantId()).isEqualTo(product.getTenantId());
        assertThat(auditCaptor.getValue().productId()).isEqualTo(product.getId());
        assertThat(auditCaptor.getValue().targetId()).isEqualTo("user-1");
    }

    @Test
    void shouldInviteUserByEmail() {
        Product product = product();
        IdentityUser invitedUser = new IdentityUser("keycloak-guest-id", "guest@byop.dev", "guest@byop.dev", null, null);
        ProductAssignment saved = assignment(product, "keycloak-guest-id", ProductAssignmentRole.VIEWER);
        saved.revoke();
        ProductAssignmentSummary summary = summary(product, "keycloak-guest-id", "guest@byop.dev", "guest@byop.dev");
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(invitePort.invite(product.getTenantId(), product.getId(), "guest@byop.dev"))
                .thenReturn(invitedUser);
        when(assignmentRepository.save(any(ProductAssignment.class))).thenReturn(saved);
        when(assignmentMapper.toSummary(saved, "guest@byop.dev", "guest@byop.dev")).thenReturn(summary);

        ProductAssignmentSummary result = service.assignUser(
                caller(),
                product.getId(),
                request(product, null, "guest@byop.dev")
        );

        assertThat(result).isEqualTo(summary);
        ArgumentCaptor<ProductAssignment> captor = ArgumentCaptor.forClass(ProductAssignment.class);
        verify(assignmentRepository).save(captor.capture());
        assertThat(captor.getValue().getUserSubject()).isEqualTo("keycloak-guest-id");
        assertThat(captor.getValue().getStatus()).isEqualTo(ProductAssignmentStatus.INVITED);
        verify(emailPort, never()).notifyAssignment(any(ProductAssignmentEmailCommand.class));
        verify(notificationPort).notifyAssignment(product.getTenantId(), product.getId(), "keycloak-guest-id");
    }

    @Test
    void shouldRejectUserIdAndInviteEmailTogether() {
        Product product = product();
        UUID productId = product.getId();
        AssignProductUserRequest request = request(product, "user-1", "guest@byop.dev");
        AuthenticatedUser caller = caller();

        assertThatThrownBy(() -> service.assignUser(caller, productId, request))
                .isInstanceOf(InvalidProductAssignmentException.class)
                .hasMessage("Exactly one of userId or inviteEmail is required");

        verify(productRepository, never()).findById(any(UUID.class));
    }

    @Test
    void shouldRejectMissingUserIdAndInviteEmail() {
        Product product = product();
        UUID productId = product.getId();
        AssignProductUserRequest request = request(product, null, null);
        AuthenticatedUser caller = caller();

        assertThatThrownBy(() -> service.assignUser(caller, productId, request))
                .isInstanceOf(InvalidProductAssignmentException.class)
                .hasMessage("Exactly one of userId or inviteEmail is required");

        verify(productRepository, never()).findById(any(UUID.class));
    }

    @Test
    void shouldRejectDifferentPathAndBodyProductId() {
        Product product = product();
        UUID pathProductId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        AssignProductUserRequest request = request(product, "user-1", null);
        AuthenticatedUser caller = caller();

        assertThatThrownBy(() -> service.assignUser(caller, pathProductId, request))
                .isInstanceOf(InvalidProductAssignmentException.class)
                .hasMessage("Path productId does not match request productId");
    }

    @Test
    void shouldRejectDifferentTenantId() {
        Product product = product();
        UUID productId = product.getId();
        AssignProductUserRequest request = new AssignProductUserRequest(
                productId,
                UUID.fromString("88888888-8888-8888-8888-888888888888"),
                "user-1",
                null,
                "EDITOR",
                null
        );
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        AuthenticatedUser caller = caller();

        assertThatThrownBy(() -> service.assignUser(caller, productId, request))
                .isInstanceOf(InvalidProductAssignmentException.class)
                .hasMessage("Request tenantId does not match product tenantId");
    }

    @Test
    void shouldRejectUserOutsideTenant() {
        Product product = product();
        UUID productId = product.getId();
        AssignProductUserRequest request = request(product, "user-1", null);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(tenantAccessService.hasActiveMembership(product.getTenantId(), "user-1")).thenReturn(false);
        AuthenticatedUser caller = caller();

        assertThatThrownBy(() -> service.assignUser(caller, productId, request))
                .isInstanceOf(InvalidProductAssignmentException.class)
                .hasMessage("User does not have active membership in product tenant");

        verify(assignmentRepository, never()).save(any(ProductAssignment.class));
    }

    @Test
    void shouldRejectInvalidRole() {
        Product product = product();
        UUID productId = product.getId();
        AssignProductUserRequest request = new AssignProductUserRequest(
                productId,
                product.getTenantId(),
                "user-1",
                null,
                "invalid",
                null
        );
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        AuthenticatedUser caller = caller();

        assertThatThrownBy(() -> service.assignUser(caller, productId, request))
                .isInstanceOf(InvalidProductAssignmentException.class)
                .hasMessage("Invalid product assignment role: invalid");

        verify(assignmentRepository, never()).save(any(ProductAssignment.class));
    }

    @Test
    void shouldRejectAssignmentWhenProductDoesNotExist() {
        Product product = product();
        UUID productId = product.getId();
        AssignProductUserRequest request = request(product, "user-1", null);
        when(productRepository.findById(productId)).thenReturn(Optional.empty());
        AuthenticatedUser caller = caller();

        assertThatThrownBy(() -> service.assignUser(caller, productId, request))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product not found: " + productId);
    }

    @Test
    void shouldRejectListWhenProductDoesNotExist() {
        UUID productId = UUID.fromString("77777777-7777-7777-7777-777777777777");
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listAssignments(productId))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product not found: " + productId);
    }

    @Test
    void shouldDeleteAssignmentWithoutDeletingMembership() {
        Product product = product();
        IdentityUser user = new IdentityUser("user-1", "editor", "editor@byop.dev", "Editor", "User");
        ProductAssignment assignment = assignment(product, "user-1", ProductAssignmentRole.EDITOR);
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(assignmentRepository.findByProductIdAndUserSubject(product.getId(), "user-1"))
                .thenReturn(Optional.of(assignment));
        when(userDirectory.getRequiredUser("user-1")).thenReturn(user);
        when(tenantAccessService.getRequiredReference(product.getTenantId()))
                .thenReturn(new TenantReference(product.getTenantId(), "Tenant Aegis"));

        service.removeAssignment(caller(), product.getId(), "user-1");

        ArgumentCaptor<ProductAssignmentEmailCommand> emailCaptor =
                ArgumentCaptor.forClass(ProductAssignmentEmailCommand.class);
        verify(emailPort).notifyRevocation(emailCaptor.capture());
        assertThat(emailCaptor.getValue().recipientEmail()).isEqualTo("editor@byop.dev");
        assertThat(emailCaptor.getValue().recipientName()).isEqualTo("Editor User");
        verify(notificationPort).notifyRevocation(product.getTenantId(), product.getId(), "user-1");
        verify(assignmentRepository).delete(assignment);
        verifyNoTenantMembershipRemovalDependency();
        ArgumentCaptor<AuditRecordCommand> auditCaptor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService).recordEvent(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo("PRODUCT_ASSIGNMENT_REMOVED");
    }

    @Test
    void shouldRejectDeleteWhenAssignmentDoesNotExist() {
        Product product = product();
        UUID productId = product.getId();
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(assignmentRepository.findByProductIdAndUserSubject(productId, "user-1"))
                .thenReturn(Optional.empty());
        AuthenticatedUser caller = caller();

        assertThatThrownBy(() -> service.removeAssignment(caller, productId, "user-1"))
                .isInstanceOf(ProductAssignmentNotFoundException.class)
                .hasMessage("Product assignment not found for product " + productId + " and user user-1");

        verify(assignmentRepository, never()).delete(any(ProductAssignment.class));
    }

    private AuthenticatedUser caller() {
        return new AuthenticatedUser("admin-subject", "admin@byop.dev", "admin", "Admin", Set.of("ROLE_SUPER_ADMIN"));
    }

    private void verifyNoTenantMembershipRemovalDependency() {
        verify(tenantAccessService, never()).hasActiveMembership(any(UUID.class), any(String.class));
    }

    private AssignProductUserRequest request(Product product, String userId, String inviteEmail) {
        return new AssignProductUserRequest(product.getId(), product.getTenantId(), userId, inviteEmail, "EDITOR", null);
    }

    private Product product() {
        UUID tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID productId = UUID.fromString("22222222-2222-2222-2222-222222222222");
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

    private ProductAssignment assignment(Product product, String userSubject, ProductAssignmentRole role) {
        ProductAssignment assignment = new ProductAssignment(product, userSubject, role);
        ReflectionTestUtils.setField(assignment, "id", UUID.nameUUIDFromBytes(userSubject.getBytes()));
        ReflectionTestUtils.setField(assignment, "createdAt", OffsetDateTime.parse("2026-06-26T10:00:00-03:00"));
        ReflectionTestUtils.setField(assignment, "updatedAt", OffsetDateTime.parse("2026-06-26T10:10:00-03:00"));
        return assignment;
    }

    private ProductAssignmentSummary summary(Product product, String userSubject, String userName, String userEmail) {
        return new ProductAssignmentSummary(
                UUID.nameUUIDFromBytes(userSubject.getBytes()),
                product.getTenantId(),
                product.getId(),
                product.getName(),
                userSubject,
                userName,
                userEmail,
                "EDITOR",
                "atribuido",
                OffsetDateTime.parse("2026-06-26T10:00:00-03:00"),
                OffsetDateTime.parse("2026-06-26T10:10:00-03:00")
        );
    }
}
