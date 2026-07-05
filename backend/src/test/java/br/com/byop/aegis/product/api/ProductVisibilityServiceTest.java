package br.com.byop.aegis.product.api;

import br.com.byop.aegis.product.domain.ProductStatus;
import br.com.byop.aegis.product.domain.ProductTypeKey;
import br.com.byop.aegis.product.dto.ProductSummary;
import br.com.byop.aegis.product.service.ProductService;
import br.com.byop.aegis.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductVisibilityServiceTest {

    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TENANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2026-06-29T10:00:00Z");

    @Mock
    private ProductService productService;

    @Test
    void shouldAdaptProductSummariesToAccessScope() {
        AuthenticatedUser caller = new AuthenticatedUser("user-1", "user@aegis.app", "user", "User", Set.of("ROLE_TENANT_ADMIN"));
        when(productService.listProducts(caller)).thenReturn(List.of(summary(ProductStatus.ACTIVE)));

        ProductVisibilityService service = new ProductVisibilityService(productService);
        List<ProductAccessScope> scopes = service.listVisibleProducts(caller);

        assertThat(scopes).hasSize(1);
        assertThat(scopes.getFirst().productId()).isEqualTo(PRODUCT_ID);
        assertThat(scopes.getFirst().tenantId()).isEqualTo(TENANT_ID);
        assertThat(scopes.getFirst().status()).isEqualTo("ACTIVE");
    }

    @Test
    void shouldReturnEmptyWhenCallerHasNoVisibleProducts() {
        AuthenticatedUser caller = new AuthenticatedUser("user-1", "user@aegis.app", "user", "User", Set.of());
        when(productService.listProducts(caller)).thenReturn(List.of());

        ProductVisibilityService service = new ProductVisibilityService(productService);

        assertThat(service.listVisibleProducts(caller)).isEmpty();
    }

    private ProductSummary summary(ProductStatus status) {
        return new ProductSummary(
                PRODUCT_ID,
                TENANT_ID,
                "product-key",
                "Product",
                ProductTypeKey.SITE_INSTITUCIONAL,
                status,
                "pt-BR",
                AssetStorageStrategy.LOCAL,
                FIXED_TIMESTAMP,
                FIXED_TIMESTAMP,
                0,
                null
        );
    }
}
