package br.com.byop.aegis.product.api;

import java.util.UUID;

/**
 * Um produto visivel ao caller (ADR-0019), na forma minima necessaria para
 * agregadores de outros modulos (ex.: {@code dashboard}) — nunca expoe o
 * {@code ProductSummary} interno do modulo {@code product}.
 */
public record ProductAccessScope(
        UUID productId,
        UUID tenantId,
        String status
) {
}
