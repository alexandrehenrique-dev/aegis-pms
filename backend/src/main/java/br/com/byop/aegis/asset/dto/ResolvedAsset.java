package br.com.byop.aegis.asset.dto;

import java.util.UUID;

public record ResolvedAsset(
        UUID id,
        String url,
        String expiresAt,
        String contentType
) {
}
