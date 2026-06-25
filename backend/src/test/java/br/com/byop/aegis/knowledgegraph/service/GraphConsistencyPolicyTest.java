package br.com.byop.aegis.knowledgegraph.service;

import br.com.byop.aegis.knowledgegraph.domain.GraphNode;
import br.com.byop.aegis.knowledgegraph.domain.GraphNodeType;
import br.com.byop.aegis.knowledgegraph.exception.InvalidGraphEdgeException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GraphConsistencyPolicyTest {

    private final GraphConsistencyPolicy policy = new GraphConsistencyPolicy();

    @Test
    void shouldAcceptEdgeNodesFromSameTenantAndProduct() {
        UUID productId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        GraphNode source = node(tenantId, productId, "source");
        GraphNode target = node(tenantId, productId, "target");

        assertThatCode(() -> policy.validateEdgeNodes(productId, source, target))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectEdgeWithoutSourceNode() {
        UUID productId = UUID.randomUUID();
        GraphNode target = node(UUID.randomUUID(), productId, "target");

        assertThatThrownBy(() -> policy.validateEdgeNodes(productId, null, target))
                .isInstanceOf(InvalidGraphEdgeException.class)
                .hasMessage("Graph edge source node is required");
    }

    @Test
    void shouldRejectEdgeWithoutTargetNode() {
        UUID productId = UUID.randomUUID();
        GraphNode source = node(UUID.randomUUID(), productId, "source");

        assertThatThrownBy(() -> policy.validateEdgeNodes(productId, source, null))
                .isInstanceOf(InvalidGraphEdgeException.class)
                .hasMessage("Graph edge target node is required");
    }

    @Test
    void shouldRejectEdgeAcrossTenants() {
        UUID productId = UUID.randomUUID();
        GraphNode source = node(UUID.randomUUID(), productId, "source");
        GraphNode target = node(UUID.randomUUID(), productId, "target");

        assertThatThrownBy(() -> policy.validateEdgeNodes(productId, source, target))
                .isInstanceOf(InvalidGraphEdgeException.class)
                .hasMessage("Graph edge nodes must belong to the same tenant");
    }

    @Test
    void shouldRejectEdgeAcrossProducts() {
        UUID tenantId = UUID.randomUUID();
        GraphNode source = node(tenantId, UUID.randomUUID(), "source");
        GraphNode target = node(tenantId, UUID.randomUUID(), "target");
        UUID productId = source.getProductId();

        assertThatThrownBy(() -> policy.validateEdgeNodes(productId, source, target))
                .isInstanceOf(InvalidGraphEdgeException.class)
                .hasMessage("Graph edge nodes must belong to the same product");
    }

    @Test
    void shouldRejectEdgeWhenNodesDoNotBelongToRequestedProduct() {
        UUID tenantId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID requestedProductId = UUID.randomUUID();
        GraphNode source = node(tenantId, productId, "source");
        GraphNode target = node(tenantId, productId, "target");

        assertThatThrownBy(() -> policy.validateEdgeNodes(requestedProductId, source, target))
                .isInstanceOf(InvalidGraphEdgeException.class)
                .hasMessage("Graph edge nodes must belong to the requested product");
    }

    private GraphNode node(UUID tenantId, UUID productId, String refId) {
        return new GraphNode(new GraphNode.Creation(
                tenantId,
                productId,
                GraphNodeType.ARTICLE,
                "ARTICLE",
                refId,
                "Label " + refId,
                refId,
                "{}"
        ));
    }
}
