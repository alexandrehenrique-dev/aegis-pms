package br.com.byop.aegis.core.product;

import br.com.byop.aegis.core.product.exception.ProductContentAccessDeniedException;
import br.com.byop.aegis.core.product.exception.ProductNotFoundException;
import br.com.byop.aegis.core.tenant.Tenant;
import br.com.byop.aegis.core.tenant.TenantMembershipRepository;
import br.com.byop.aegis.core.tenant.TenantMembershipStatus;
import br.com.byop.aegis.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductAccessResolverTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private TenantMembershipRepository membershipRepository;

    @Mock
    private ProductAssignmentRepository assignmentRepository;

    @InjectMocks
    private ProductAccessResolver accessResolver;

    @Test
    void shouldRejectSuperAdminWithoutAssignment() {
        Product product = product();
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(assignmentRepository.existsByProductIdAndUserSubjectAndStatus(
                product.getId(), "super-subject", ProductAssignmentStatus.ASSIGNED
        )).thenReturn(false);
        AuthenticatedUser caller = user("super-subject", "ROLE_SUPER_ADMIN");
        UUID productId = product.getId();

        assertThatThrownBy(() -> accessResolver.assertContentAccess(caller, productId))
                .isInstanceOf(ProductContentAccessDeniedException.class)
                .hasMessage("SUPER_ADMIN role requires explicit ProductAssignment to access product content: " + product.getId());
    }

    @Test
    void shouldAllowSuperAdminWithActiveAssignment() {
        Product product = product();
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(assignmentRepository.existsByProductIdAndUserSubjectAndStatus(
                product.getId(), "super-subject", ProductAssignmentStatus.ASSIGNED
        )).thenReturn(true);
        AuthenticatedUser caller = user("super-subject", "ROLE_SUPER_ADMIN");
        UUID productId = product.getId();

        assertThatCode(() -> accessResolver.assertContentAccess(caller, productId))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAllowTenantAdminWithActiveMembership() {
        Product product = product();
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(membershipRepository.existsByTenantIdAndUserSubjectAndStatus(
                product.getTenant().getId(), "tenant-subject", TenantMembershipStatus.ACTIVE
        )).thenReturn(true);
        AuthenticatedUser caller = user("tenant-subject", "ROLE_TENANT_ADMIN");
        UUID productId = product.getId();

        assertThatCode(() -> accessResolver.assertContentAccess(caller, productId))
                .doesNotThrowAnyException();

        verify(assignmentRepository, never()).existsByProductIdAndUserSubjectAndStatus(
                product.getId(), "tenant-subject", ProductAssignmentStatus.ASSIGNED
        );
    }

    @Test
    void shouldRejectTenantAdminWithoutActiveMembership() {
        Product product = product();
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(membershipRepository.existsByTenantIdAndUserSubjectAndStatus(
                product.getTenant().getId(), "tenant-subject", TenantMembershipStatus.ACTIVE
        )).thenReturn(false);
        AuthenticatedUser caller = user("tenant-subject", "ROLE_TENANT_ADMIN");
        UUID productId = product.getId();

        assertThatThrownBy(() -> accessResolver.assertContentAccess(caller, productId))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product not found: " + product.getId());
    }

    @Test
    void shouldAllowProductRolesWithActiveAssignment() {
        assertProductRolePasses("ROLE_PRODUCT_MANAGER");
        assertProductRolePasses("ROLE_EDITOR");
        assertProductRolePasses("ROLE_VIEWER");
    }

    @Test
    void shouldRejectEditorWithoutAssignment() {
        Product product = product();
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(assignmentRepository.existsByProductIdAndUserSubjectAndStatus(
                product.getId(), "editor-subject", ProductAssignmentStatus.ASSIGNED
        )).thenReturn(false);
        AuthenticatedUser caller = user("editor-subject", "ROLE_EDITOR");
        UUID productId = product.getId();

        assertThatThrownBy(() -> accessResolver.assertContentAccess(caller, productId))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product not found: " + product.getId());
    }

    @Test
    void shouldRejectUnknownRole() {
        Product product = product();
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        AuthenticatedUser caller = user("unknown-subject", "ROLE_UNKNOWN");
        UUID productId = product.getId();

        assertThatThrownBy(() -> accessResolver.assertContentAccess(caller, productId))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product not found: " + product.getId());
    }

    @Test
    void shouldRejectMissingProduct() {
        UUID productId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(productRepository.findById(productId)).thenReturn(Optional.empty());
        AuthenticatedUser caller = user("editor-subject", "ROLE_EDITOR");

        assertThatThrownBy(() -> accessResolver.assertContentAccess(caller, productId))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product not found: " + productId);
    }

    private void assertProductRolePasses(String authority) {
        Product product = productForAuthority(authority);
        String subject = authority.toLowerCase().replace("role_", "") + "-subject";
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(assignmentRepository.existsByProductIdAndUserSubjectAndStatus(
                product.getId(), subject, ProductAssignmentStatus.ASSIGNED
        )).thenReturn(true);
        AuthenticatedUser caller = user(subject, authority);
        UUID productId = product.getId();

        assertThatCode(() -> accessResolver.assertContentAccess(caller, productId))
                .doesNotThrowAnyException();
    }

    private Product product() {
        return productForAuthority("default");
    }

    private Product productForAuthority(String authority) {
        Tenant tenant = new Tenant("byop-" + authority.toLowerCase(), "BYOP " + authority);
        ReflectionTestUtils.setField(tenant, "id", UUID.nameUUIDFromBytes(("tenant-" + authority).getBytes()));
        Product product = new Product(
                tenant,
                "product-" + authority.toLowerCase(),
                "Product " + authority,
                ProductTypeKey.SITE_INSTITUCIONAL,
                "pt-BR",
                AssetStorageStrategy.LOCAL
        );
        ReflectionTestUtils.setField(product, "id", UUID.nameUUIDFromBytes(("product-" + authority).getBytes()));
        return product;
    }

    private AuthenticatedUser user(String subject, String authority) {
        return new AuthenticatedUser(subject, subject + "@byop.dev", subject, subject, Set.of(authority));
    }
}
