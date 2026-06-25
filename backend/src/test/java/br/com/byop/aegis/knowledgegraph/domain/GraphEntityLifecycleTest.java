package br.com.byop.aegis.knowledgegraph.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GraphEntityLifecycleTest {

    @Test
    void shouldRunGraphNodeJpaLifecycleCallbacks() {
        GraphNode node = new GraphNode();

        node.prePersist();
        assertThat(node.getCreatedAt()).isNotNull();
        assertThat(node.getUpdatedAt()).isNotNull();

        node.preUpdate();
        assertThat(node.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldRunGraphEdgeJpaLifecycleCallbacks() {
        GraphEdge edge = new GraphEdge();

        edge.prePersist();
        assertThat(edge.getCreatedAt()).isNotNull();
        assertThat(edge.getUpdatedAt()).isNotNull();

        edge.preUpdate();
        assertThat(edge.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldExposeDefaultMetadataAndWeightCreationDefaults() {
        UUID tenantId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID sourceNodeId = UUID.randomUUID();
        UUID targetNodeId = UUID.randomUUID();

        GraphNode node = new GraphNode(new GraphNode.Creation(
                tenantId,
                productId,
                GraphNodeType.CONTENT,
                "CONTENT",
                "content-1",
                "Content 1",
                "content-1",
                "{}"
        ));
        GraphEdge edge = new GraphEdge(
                tenantId,
                productId,
                sourceNodeId,
                targetNodeId,
                GraphEdgeType.RELATED_TO
        );

        assertThat(node.getMetadataJson()).isEqualTo("{}");
        assertThat(edge.getWeight()).isEqualByComparingTo("1");
        assertThat(edge.getMetadataJson()).isEqualTo("{}");
    }
}
