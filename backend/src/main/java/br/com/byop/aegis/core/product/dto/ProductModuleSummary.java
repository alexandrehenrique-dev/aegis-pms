package br.com.byop.aegis.core.product.dto;

import br.com.byop.aegis.core.product.ModuleKey;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProductModuleSummary(
        UUID id,
        UUID productId,
        ModuleKey moduleKey,
        boolean enabled,
        String settingsJson,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
