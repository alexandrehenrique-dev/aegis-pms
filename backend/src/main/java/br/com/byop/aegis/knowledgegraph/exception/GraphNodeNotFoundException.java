package br.com.byop.aegis.knowledgegraph.exception;

import java.util.UUID;

public class GraphNodeNotFoundException extends RuntimeException {

    public GraphNodeNotFoundException(UUID nodeId) {
        super("Graph node not found: " + nodeId);
    }
}
