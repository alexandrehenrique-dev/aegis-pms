package br.com.byop.aegis.knowledgegraph.dto;

public record GraphRelatedSummary(
        GraphNodeSummary node,
        GraphEdgeSummary edge
) {
}
