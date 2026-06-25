package br.com.byop.aegis.knowledgegraph.service;

import br.com.byop.aegis.knowledgegraph.domain.GraphNode;
import br.com.byop.aegis.knowledgegraph.exception.InvalidGraphEdgeException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class GraphConsistencyPolicy {

    public void validateEdgeNodes(UUID productId, GraphNode sourceNode, GraphNode targetNode) {
        if (sourceNode == null) {
            throw new InvalidGraphEdgeException("Graph edge source node is required");
        }
        if (targetNode == null) {
            throw new InvalidGraphEdgeException("Graph edge target node is required");
        }
        if (!sourceNode.getTenantId().equals(targetNode.getTenantId())) {
            throw new InvalidGraphEdgeException("Graph edge nodes must belong to the same tenant");
        }
        if (!sourceNode.getProductId().equals(targetNode.getProductId())) {
            throw new InvalidGraphEdgeException("Graph edge nodes must belong to the same product");
        }
        if (!sourceNode.getProductId().equals(productId)) {
            throw new InvalidGraphEdgeException("Graph edge nodes must belong to the requested product");
        }
    }
}
