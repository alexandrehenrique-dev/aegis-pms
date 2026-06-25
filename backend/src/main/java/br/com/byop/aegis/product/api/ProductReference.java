package br.com.byop.aegis.product.api;

import java.util.UUID;

public record ProductReference(
        UUID productId,
        UUID tenantId
) {
}
