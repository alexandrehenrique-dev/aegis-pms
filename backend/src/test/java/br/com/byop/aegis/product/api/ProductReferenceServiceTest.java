package br.com.byop.aegis.product.api;

import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.domain.ProductTypeKey;
import br.com.byop.aegis.product.exception.ProductNotFoundException;
import br.com.byop.aegis.product.repository.ProductRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductReferenceServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PRODUCT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private ProductRepository productRepository;

    private ProductReferenceService service;

    @BeforeEach
    void setUp() {
        service = new ProductReferenceService(productRepository);
    }

    @Test
    void shouldReturnProductReference() {
        Product product = product();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        ProductReference reference = service.getRequiredReference(PRODUCT_ID);

        assertThat(reference.productId()).isEqualTo(PRODUCT_ID);
        assertThat(reference.tenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    void shouldRejectMissingProduct() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRequiredReference(PRODUCT_ID))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product not found: " + PRODUCT_ID);
    }

    @Test
    void shouldReturnAssetStorageStrategy() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product()));

        assertThat(service.getRequiredAssetStorageStrategy(PRODUCT_ID)).isEqualTo(AssetStorageStrategy.LOCAL);
    }

    @Test
    void shouldRejectAssetStorageStrategyForMissingProduct() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRequiredAssetStorageStrategy(PRODUCT_ID))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product not found: " + PRODUCT_ID);
    }

    @Test
    void shouldRenameProduct() {
        Product product = product();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        service.renameProduct(PRODUCT_ID, "Novo Nome");

        assertThat(product.getName()).isEqualTo("Novo Nome");
    }

    @Test
    void shouldRejectRenameForMissingProduct() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.renameProduct(PRODUCT_ID, "Novo Nome"))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product not found: " + PRODUCT_ID);
    }

    private Product product() {
        Product product = new Product(
                TENANT_ID,
                "product-ref",
                "Product Reference",
                ProductTypeKey.KNOWLEDGE_BASE,
                "pt-BR",
                AssetStorageStrategy.LOCAL
        );
        ReflectionTestUtils.setField(product, "id", PRODUCT_ID);
        return product;
    }
}
