package br.com.byop.aegis.knowledgegraph.dto;

public record GraphNeighborSummary(
        GraphNodeSummary node,
        GraphEdgeSummary edge
) {
}
