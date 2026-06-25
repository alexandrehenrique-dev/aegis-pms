package br.com.byop.aegis.core.product.command;

import br.com.byop.aegis.core.product.AssetStorageStrategy;

import java.util.UUID;

public record CreateProductCommand(
        UUID tenantId,
        String key,
        String name,
        String type,
        String defaultLocale,
        AssetStorageStrategy assetStorageStrategy
) {
}
