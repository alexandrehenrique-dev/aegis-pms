package br.com.byop.aegis.knowledgegraph.exception;

import java.util.UUID;

public class DuplicateGraphNodeException extends RuntimeException {

    public DuplicateGraphNodeException(UUID productId, String refType, String refId) {
        super("Graph node already exists for product " + productId + " and reference " + refType + "/" + refId);
    }
}
