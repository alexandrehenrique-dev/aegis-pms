package br.com.byop.aegis.asset.api;

import java.time.OffsetDateTime;

public record AssetAnalyticsResponse(
        long total,
        long recent,
        long missingAltText,
        OffsetDateTime lastUploadedAt
) {
}
