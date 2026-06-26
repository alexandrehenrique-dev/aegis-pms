package br.com.byop.aegis.product.service;

import java.util.UUID;

public record ProductAssignmentEmailCommand(
        UUID tenantId,
        String tenantName,
        UUID productId,
        String productName,
        String recipientEmail,
        String recipientName
) {
}
