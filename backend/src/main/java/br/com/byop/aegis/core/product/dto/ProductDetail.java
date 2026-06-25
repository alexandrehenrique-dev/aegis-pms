package br.com.byop.aegis.core.product.dto;

import br.com.byop.aegis.core.product.AssetStorageStrategy;
import br.com.byop.aegis.core.product.ProductStatus;
import br.com.byop.aegis.core.product.ProductTypeKey;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ProductDetail(
        UUID id,
        UUID tenantId,
        String key,
        String name,
        ProductTypeKey type,
        ProductStatus status,
        String defaultLocale,
        AssetStorageStrategy assetStorageStrategy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<ProductModuleSummary> modules
) {
}
