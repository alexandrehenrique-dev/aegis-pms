package br.com.byop.aegis.asset.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AssetDetail(
        UUID id,
        String name,
        String friendlyName,
        String altText,
        String caption,
        String credit,
        String mimeType,
        String category,
        long sizeBytes,
        String status,
        List<String> tags,
        List<AssetUsageSummary> usage,
        String uploadedBySubject,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
