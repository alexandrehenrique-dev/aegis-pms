package br.com.byop.aegis.product.api;

import java.util.UUID;

public record ProductUserAccess(
        UUID tenantId,
        UUID productId,
        String productName,
        String userSubject,
        String role,
        String status
) {
}
