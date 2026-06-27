package br.com.byop.aegis.asset.api;

import java.util.UUID;

public record AssetReference(
        UUID id,
        UUID productId,
        String mimeType
) {
}
