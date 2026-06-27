package br.com.byop.aegis.asset.dto;

import java.util.UUID;

public record AssetSummary(
        UUID id,
        String name,
        String type,
        String size,
        String status,
        String tags,
        String usage,
        String uploadedAt
) {
}
