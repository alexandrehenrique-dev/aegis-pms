package br.com.byop.aegis.knowledgegraph.dto;

import br.com.byop.aegis.knowledgegraph.domain.GraphNodeType;

import java.time.OffsetDateTime;
import java.util.List;
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
        String type,
        String status,
        double x,
        double y,
        List<GraphNodeProp> props,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public GraphNodeSummary(UUID id, UUID tenantId, UUID productId, GraphNodeType nodeType, String refType,
                            String refId, String label, String slug, OffsetDateTime createdAt,
                            OffsetDateTime updatedAt) {
        this(id, tenantId, productId, nodeType, refType, refId, label, slug, nodeType.name(), "ativo", 0.0, 0.0,
                List.of(), createdAt, updatedAt);
    }
}
