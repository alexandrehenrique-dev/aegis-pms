package br.com.byop.aegis.core.product;

import br.com.byop.aegis.core.product.dto.ProductModuleSummary;
import br.com.byop.aegis.core.tenant.Tenant;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProductModuleMapperTest {

    private final ProductModuleMapper mapper = Mappers.getMapper(ProductModuleMapper.class);

    @Test
    void shouldMapEntityToSummary() {
        UUID moduleId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID productId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-06-25T11:00:00-03:00");
        OffsetDateTime updatedAt = OffsetDateTime.parse("2026-06-25T11:10:00-03:00");
        Product product = product();
        ReflectionTestUtils.setField(product, "id", productId);
        ProductModule module = new ProductModule(product, ModuleKey.KNOWLEDGE_GRAPH);
        module.enable();
        module.updateSettings("{\"layout\":\"force\"}");
        ReflectionTestUtils.setField(module, "id", moduleId);
        ReflectionTestUtils.setField(module, "createdAt", createdAt);
        ReflectionTestUtils.setField(module, "updatedAt", updatedAt);

        ProductModuleSummary summary = mapper.toSummary(module);

        assertThat(summary).isNotNull();
        assertThat(summary.id()).isEqualTo(moduleId);
        assertThat(summary.productId()).isEqualTo(productId);
        assertThat(summary.moduleKey()).isEqualTo(ModuleKey.KNOWLEDGE_GRAPH);
        assertThat(summary.enabled()).isTrue();
        assertThat(summary.settingsJson()).isEqualTo("{\"layout\":\"force\"}");
        assertThat(summary.createdAt()).isEqualTo(createdAt);
        assertThat(summary.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void shouldReturnNullSummaryWhenProductModuleIsNull() {
        assertThat(mapper.toSummary(null)).isNull();
    }

    @Test
    void shouldMapNullProductIdWhenProductModuleHasNoProduct() {
        ProductModule module = new ProductModule();
        ReflectionTestUtils.setField(module, "moduleKey", ModuleKey.CONTENT);

        ProductModuleSummary summary = mapper.toSummary(module);

        assertThat(summary).isNotNull();
        assertThat(summary.productId()).isNull();
        assertThat(summary.moduleKey()).isEqualTo(ModuleKey.CONTENT);
        assertThat(summary.enabled()).isFalse();
    }

    private Product product() {
        return new Product(
                new Tenant("tenant-module", "Tenant Module"),
                "product-module",
                "Product Module",
                ProductTypeKey.KNOWLEDGE_BASE,
                "pt-BR",
                AssetStorageStrategy.LOCAL
        );
    }
}
