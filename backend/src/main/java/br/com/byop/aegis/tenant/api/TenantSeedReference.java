package br.com.byop.aegis.tenant.api;

import java.util.UUID;

/**
 * Referencia publica de tenant garantido pelo seed local.
 */
public record TenantSeedReference(
        UUID id,
        String key,
        String name
) {
}
