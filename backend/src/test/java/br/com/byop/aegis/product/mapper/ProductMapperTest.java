package br.com.byop.aegis.product.mapper;

import br.com.byop.aegis.product.api.ModuleKey;
import br.com.byop.aegis.product.api.AssetStorageStrategy;
import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.domain.ProductStatus;
import br.com.byop.aegis.product.domain.ProductTypeKey;
import br.com.byop.aegis.product.dto.ProductDetail;
import br.com.byop.aegis.product.dto.ProductModuleSummary;
import br.com.byop.aegis.product.dto.ProductSummary;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductMapperTest {

    private final ProductMapper mapper = Mappers.getMapper(ProductMapper.class);

    @Test
    void shouldMapEntityToSummary() {
        UUID tenantId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        UUID productId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-06-25T12:00:00-03:00");
        OffsetDateTime updatedAt = OffsetDateTime.parse("2026-06-25T12:10:00-03:00");
        Product product = product(tenantId, productId, createdAt, updatedAt);

        ProductSummary summary = mapper.toSummary(product, 3, List.of("CONTENT", "ASSETS", "FORMS"), "EDITOR");

        assertThat(summary).isNotNull();
        assertThat(summary.id()).isEqualTo(productId);
        assertThat(summary.tenantId()).isEqualTo(tenantId);
        assertThat(summary.key()).isEqualTo("maestro-beton");
        assertThat(summary.name()).isEqualTo("Maestro Beton");
        assertThat(summary.type()).isEqualTo(ProductTypeKey.SITE_INSTITUCIONAL);
        assertThat(summary.status()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(summary.defaultLocale()).isEqualTo("pt-BR");
        assertThat(summary.assetStorageStrategy()).isEqualTo(AssetStorageStrategy.S3);
        assertThat(summary.createdAt()).isEqualTo(createdAt);
        assertThat(summary.updatedAt()).isEqualTo(updatedAt);
        assertThat(summary.enabledModuleCount()).isEqualTo(3);
        assertThat(summary.enabledModules()).containsExactly("CONTENT", "ASSETS", "FORMS");
        assertThat(summary.callerAssignedRole()).isEqualTo("EDITOR");
    }

    @Test
    void shouldReturnNullSummaryWhenProductIsNull() {
        assertThat(mapper.toSummary(null, 0, List.of(), null)).isNull();
    }

    @Test
    void shouldMapNullTenantIdWhenProductHasNoTenant() {
        Product product = mock(Product.class);
        when(product.getId()).thenReturn(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        when(product.getTenantId()).thenReturn(null);
        when(product.getKey()).thenReturn("custom");
        when(product.getName()).thenReturn("Custom");
        when(product.getType()).thenReturn(ProductTypeKey.CUSTOM);
        when(product.getStatus()).thenReturn(ProductStatus.ACTIVE);
        when(product.getDefaultLocale()).thenReturn("pt-BR");
        when(product.getAssetStorageStrategy()).thenReturn(AssetStorageStrategy.LOCAL);

        ProductSummary summary = mapper.toSummary(product, 0, List.of(), null);

        assertThat(summary).isNotNull();
        assertThat(summary.id()).isEqualTo(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        assertThat(summary.tenantId()).isNull();
        assertThat(summary.type()).isEqualTo(ProductTypeKey.CUSTOM);
        assertThat(summary.assetStorageStrategy()).isEqualTo(AssetStorageStrategy.LOCAL);
        assertThat(summary.enabledModuleCount()).isZero();
        assertThat(summary.callerAssignedRole()).isNull();
    }

    @Test
    void shouldMapEntityAndModulesToDetail() {
        UUID tenantId = UUID.fromString("66666666-6666-6666-6666-666666666666");
        UUID productId = UUID.fromString("77777777-7777-7777-7777-777777777777");
        UUID contentModuleId = UUID.fromString("88888888-8888-8888-8888-888888888888");
        UUID graphModuleId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-06-25T13:00:00-03:00");
        OffsetDateTime updatedAt = OffsetDateTime.parse("2026-06-25T13:10:00-03:00");
        OffsetDateTime moduleCreatedAt = OffsetDateTime.parse("2026-06-25T13:20:00-03:00");
        OffsetDateTime moduleUpdatedAt = OffsetDateTime.parse("2026-06-25T13:30:00-03:00");
        Product product = product(tenantId, productId, createdAt, updatedAt);
        List<ProductModuleSummary> modules = List.of(
                new ProductModuleSummary(
                        contentModuleId,
                        productId,
                        ModuleKey.CONTENT,
                        true,
                        "{}",
                        moduleCreatedAt,
                        moduleUpdatedAt
                ),
                new ProductModuleSummary(
                        graphModuleId,
                        productId,
                        ModuleKey.KNOWLEDGE_GRAPH,
                        false,
                        "{\"layout\":\"force\"}",
                        moduleCreatedAt.plusMinutes(1),
                        moduleUpdatedAt.plusMinutes(1)
                )
        );

        ProductDetail detail = mapper.toDetail(product, modules);

        assertThat(detail).isNotNull();
        assertThat(detail.id()).isEqualTo(productId);
        assertThat(detail.tenantId()).isEqualTo(tenantId);
        assertThat(detail.key()).isEqualTo("maestro-beton");
        assertThat(detail.name()).isEqualTo("Maestro Beton");
        assertThat(detail.type()).isEqualTo(ProductTypeKey.SITE_INSTITUCIONAL);
        assertThat(detail.status()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(detail.defaultLocale()).isEqualTo("pt-BR");
        assertThat(detail.assetStorageStrategy()).isEqualTo(AssetStorageStrategy.S3);
        assertThat(detail.createdAt()).isEqualTo(createdAt);
        assertThat(detail.updatedAt()).isEqualTo(updatedAt);
        assertThat(detail.modules()).containsExactlyElementsOf(modules);
        assertThat(detail.modules())
                .extracting(ProductModuleSummary::moduleKey)
                .containsExactly(ModuleKey.CONTENT, ModuleKey.KNOWLEDGE_GRAPH);
    }

    @Test
    void shouldReturnNullDetailWhenProductAndModulesAreNull() {
        assertThat(mapper.toDetail(null, null)).isNull();
    }

    @Test
    void shouldMapDetailWithOnlyModules() {
        UUID productId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        ProductModuleSummary module = new ProductModuleSummary(
                UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"),
                productId,
                ModuleKey.ASSETS,
                true,
                "{}",
                OffsetDateTime.parse("2026-06-25T14:00:00-03:00"),
                OffsetDateTime.parse("2026-06-25T14:10:00-03:00")
        );

        ProductDetail detail = mapper.toDetail(null, List.of(module));

        assertThat(detail).isNotNull();
        assertThat(detail.id()).isNull();
        assertThat(detail.tenantId()).isNull();
        assertThat(detail.modules()).containsExactly(module);
    }

    @Test
    void shouldMapDetailWithNullModules() {
        Product product = product(
                UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"),
                UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee"),
                OffsetDateTime.parse("2026-06-25T15:00:00-03:00"),
                OffsetDateTime.parse("2026-06-25T15:10:00-03:00")
        );

        ProductDetail detail = mapper.toDetail(product, null);

        assertThat(detail).isNotNull();
        assertThat(detail.id()).isEqualTo(UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee"));
        assertThat(detail.tenantId()).isEqualTo(UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"));
        assertThat(detail.modules()).isNull();
    }

    private Product product(UUID tenantId, UUID productId, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        Product product = new Product(
                tenantId,
                "maestro-beton",
                "Maestro Beton",
                ProductTypeKey.SITE_INSTITUCIONAL,
                "pt-BR",
                AssetStorageStrategy.S3
        );
        ReflectionTestUtils.setField(product, "id", productId);
        ReflectionTestUtils.setField(product, "createdAt", createdAt);
        ReflectionTestUtils.setField(product, "updatedAt", updatedAt);
        return product;
    }
}
