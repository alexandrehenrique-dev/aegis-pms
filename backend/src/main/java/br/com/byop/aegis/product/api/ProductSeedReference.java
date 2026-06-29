package br.com.byop.aegis.product.api;

import java.util.UUID;

/**
 * Referencia publica de produto garantido pelo seed local.
 */
public record ProductSeedReference(
        UUID id,
        UUID tenantId,
        String key,
        String name
) {
}
