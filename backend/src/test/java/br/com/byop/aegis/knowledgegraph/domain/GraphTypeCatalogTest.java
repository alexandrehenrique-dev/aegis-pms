package br.com.byop.aegis.knowledgegraph.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GraphTypeCatalogTest {

    @Test
    void shouldExposeHomologationNodeTypes() {
        assertThat(GraphNodeType.values())
                .contains(GraphNodeType.TOPIC, GraphNodeType.POEM, GraphNodeType.MUSIC_REF,
                        GraphNodeType.MANIFEST, GraphNodeType.BOOK, GraphNodeType.PLAYLIST, GraphNodeType.TENANT);
    }

    @Test
    void shouldExposeHomologationEdgeTypes() {
        assertThat(GraphEdgeType.values())
                .contains(GraphEdgeType.INTEGRATES_WITH, GraphEdgeType.PACKAGED_BY, GraphEdgeType.WRITTEN_BY);
    }
}
