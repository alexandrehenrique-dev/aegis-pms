package br.com.byop.aegis.content.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record ContentSummary(
        UUID id,
        String title,
        String type,
        String lang,
        String author,
        String status,
        OffsetDateTime updatedAt,
        String publication,
        String version,
        String summary,
        String difficultyLevel,
        String body,
        String category,
        String topic,
        Map<String, Object> metadata
) {
}
