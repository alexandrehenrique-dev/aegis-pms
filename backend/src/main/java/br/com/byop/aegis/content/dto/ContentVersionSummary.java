package br.com.byop.aegis.content.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ContentVersionSummary(
        UUID id,
        String versionLabel,
        String createdByName,
        OffsetDateTime createdAt,
        String snapshotJson
) {
}
