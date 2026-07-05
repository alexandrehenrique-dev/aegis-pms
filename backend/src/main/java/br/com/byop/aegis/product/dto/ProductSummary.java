package br.com.byop.aegis.product.dto;

import br.com.byop.aegis.product.api.AssetStorageStrategy;
import br.com.byop.aegis.product.domain.ProductStatus;
import br.com.byop.aegis.product.domain.ProductTypeKey;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProductSummary(
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
        /** Número de módulos habilitados. Usado pelo frontend para determinar se o produto pode ser aberto. */
        int enabledModuleCount
) {
}
