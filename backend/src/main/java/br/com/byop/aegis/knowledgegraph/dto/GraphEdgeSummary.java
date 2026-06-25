package br.com.byop.aegis.knowledgegraph.dto;

import br.com.byop.aegis.knowledgegraph.domain.GraphEdgeType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record GraphEdgeSummary(
        UUID id,
        UUID tenantId,
        UUID productId,
        UUID sourceNodeId,
        UUID targetNodeId,
        GraphEdgeType edgeType,
        BigDecimal weight,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
