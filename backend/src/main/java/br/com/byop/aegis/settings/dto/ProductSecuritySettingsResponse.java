package br.com.byop.aegis.settings.dto;

import java.time.OffsetDateTime;

public record ProductSecuritySettingsResponse(
        String webhookUrl,
        boolean analyticsEnabled,
        String analyticsProviderKey,
        boolean emailDeliveryEnabled,
        OffsetDateTime updatedAt,
        String webhookStatus,
        String analyticsStatus,
        String emailStatus
) {
}
