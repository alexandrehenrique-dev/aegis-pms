package br.com.byop.aegis.knowledgegraph.mapper;

import br.com.byop.aegis.knowledgegraph.domain.GraphEdgeType;
import br.com.byop.aegis.knowledgegraph.domain.GraphNodeType;
import br.com.byop.aegis.knowledgegraph.dto.GraphEdgeSummary;
import br.com.byop.aegis.knowledgegraph.dto.GraphNeighborSummary;
import br.com.byop.aegis.knowledgegraph.dto.GraphNodeSummary;
import br.com.byop.aegis.knowledgegraph.dto.GraphRelatedSummary;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GraphRelationSummaryTest {

    @Test
    void shouldExposeNeighborSummaryParts() {
        GraphNodeSummary node = nodeSummary();
        GraphEdgeSummary edge = edgeSummary();

        GraphNeighborSummary summary = new GraphNeighborSummary(node, edge);

        assertThat(summary.node()).isEqualTo(node);
        assertThat(summary.edge()).isEqualTo(edge);
    }

    @Test
    void shouldExposeRelatedSummaryParts() {
        GraphNodeSummary node = nodeSummary();
        GraphEdgeSummary edge = edgeSummary();

        GraphRelatedSummary summary = new GraphRelatedSummary(node, edge);

        assertThat(summary.node()).isEqualTo(node);
        assertThat(summary.edge()).isEqualTo(edge);
    }

    private GraphNodeSummary nodeSummary() {
        return new GraphNodeSummary(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                GraphNodeType.TOPIC,
                "TOPIC",
                "spring",
                "Spring",
                "spring",
                OffsetDateTime.parse("2026-06-25T16:00:00-03:00"),
                OffsetDateTime.parse("2026-06-25T16:10:00-03:00")
        );
    }

    private GraphEdgeSummary edgeSummary() {
        return new GraphEdgeSummary(
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                UUID.fromString("55555555-5555-5555-5555-555555555555"),
                UUID.fromString("66666666-6666-6666-6666-666666666666"),
                GraphEdgeType.RELATED_TO,
                BigDecimal.ONE,
                OffsetDateTime.parse("2026-06-25T16:20:00-03:00"),
                OffsetDateTime.parse("2026-06-25T16:30:00-03:00")
        );
    }
}
