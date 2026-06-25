package br.com.byop.aegis.knowledgegraph.exception;

import br.com.byop.aegis.knowledgegraph.domain.GraphEdgeType;

import java.util.UUID;

public class DuplicateGraphEdgeException extends RuntimeException {

    public DuplicateGraphEdgeException(UUID sourceNodeId, UUID targetNodeId, GraphEdgeType edgeType) {
        super("Graph edge already exists: " + sourceNodeId + " -> " + targetNodeId + " (" + edgeType + ")");
    }
}
