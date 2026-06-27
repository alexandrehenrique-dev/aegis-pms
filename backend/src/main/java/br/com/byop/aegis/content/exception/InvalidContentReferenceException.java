package br.com.byop.aegis.content.exception;

public class InvalidContentReferenceException extends RuntimeException {

    public InvalidContentReferenceException(String nodeId) {
        super("Knowledge graph node referenced by kg-ref does not exist: " + nodeId);
    }
}
