package br.com.byop.aegis.knowledgegraph.dto;

import java.util.UUID;

public record GraphNodePreview(
        UUID id,
        String label,
        String type,
        String summary,
        String difficulty,
        String thumbnail
) {
}
