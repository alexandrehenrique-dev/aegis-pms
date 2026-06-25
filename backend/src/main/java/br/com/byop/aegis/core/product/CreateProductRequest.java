package br.com.byop.aegis.core.product;

import br.com.byop.aegis.core.product.command.CreateProductCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateProductRequest(
        @NotNull UUID tenantId,
        @NotBlank String key,
        @NotBlank String name,
        @NotBlank String type,
        @NotBlank String defaultLocale,
        AssetStorageStrategy assetStorageStrategy
) {

    public CreateProductCommand toCommand() {
        return new CreateProductCommand(tenantId, key, name, type, defaultLocale, assetStorageStrategy);
    }
}
