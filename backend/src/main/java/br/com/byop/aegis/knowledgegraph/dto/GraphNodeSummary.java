package br.com.byop.aegis.knowledgegraph.dto;

import br.com.byop.aegis.knowledgegraph.domain.GraphNodeType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record GraphNodeSummary(
        UUID id,
        UUID tenantId,
        UUID productId,
        GraphNodeType nodeType,
        String refType,
        String refId,
        String label,
        String slug,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
