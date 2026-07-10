package br.com.byop.aegis.product.service;

import br.com.byop.aegis.product.api.AssetStorageStrategy;
import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.domain.ProductAssignmentStatus;
import br.com.byop.aegis.product.domain.ProductTypeKey;
import br.com.byop.aegis.product.exception.ProductContentAccessDeniedException;
import br.com.byop.aegis.product.exception.ProductNotFoundException;
import br.com.byop.aegis.product.repository.ProductAssignmentRepository;
import br.com.byop.aegis.product.repository.ProductRepository;
import br.com.byop.aegis.security.AuthenticatedUser;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductAccessResolverTest {

    @Mock
    private ProductRepository productRepository;

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

        assertThatThrownBy(() -> accessResolver.assertAccessible(productId, caller))
                .isInstanceOf(ProductContentAccessDeniedException.class)
                .hasMessage("SUPER_ADMIN role requires explicit ProductAssignment to access product content: " + product.getId());
    }

    @Test
    void shouldRejectTenantAdminWithoutProductAssignment() {
        Product product = product();
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(assignmentRepository.existsByProductIdAndUserSubjectAndStatus(
                product.getId(), "tenant-subject", ProductAssignmentStatus.ASSIGNED
        )).thenReturn(false);
        AuthenticatedUser caller = user("tenant-subject", "ROLE_TENANT_ADMIN");
        UUID productId = product.getId();

        assertThatThrownBy(() -> accessResolver.assertAccessible(productId, caller))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product not found: " + product.getId());
    }

    @ParameterizedTest(name = "{0} com ProductAssignment ativo acessa conteudo do produto")
    @CsvSource({
            "super-subject,ROLE_SUPER_ADMIN",
            "tenant-subject,ROLE_TENANT_ADMIN",
            "product-manager-subject,ROLE_PRODUCT_MANAGER",
            "editor-subject,ROLE_EDITOR",
            "viewer-subject,ROLE_VIEWER",
            "unknown-subject,ROLE_UNKNOWN"
    })
    void shouldAllowUsersWithActiveProductAssignment(String subject, String authority) {
        Product product = productForAuthority(authority);
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(assignmentRepository.existsByProductIdAndUserSubjectAndStatus(
                product.getId(), subject, ProductAssignmentStatus.ASSIGNED
        )).thenReturn(true);
        AuthenticatedUser caller = user(subject, authority);
        UUID productId = product.getId();

        assertThatCode(() -> accessResolver.assertAccessible(productId, caller))
                .doesNotThrowAnyException();
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

        assertThatThrownBy(() -> accessResolver.assertAccessible(productId, caller))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product not found: " + product.getId());
    }

    @Test
    void shouldRejectMissingProduct() {
        UUID productId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(productRepository.findById(productId)).thenReturn(Optional.empty());
        AuthenticatedUser caller = user("editor-subject", "ROLE_EDITOR");

        assertThatThrownBy(() -> accessResolver.assertAccessible(productId, caller))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product not found: " + productId);
    }

    private Product product() {
        return productForAuthority("default");
    }

    private Product productForAuthority(String authority) {
        Product product = new Product(
                UUID.nameUUIDFromBytes(("tenant-" + authority).getBytes()),
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
