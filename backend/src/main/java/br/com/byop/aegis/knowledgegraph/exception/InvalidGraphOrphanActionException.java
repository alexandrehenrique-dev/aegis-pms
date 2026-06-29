package br.com.byop.aegis.knowledgegraph.exception;

public class InvalidGraphOrphanActionException extends RuntimeException {

    public InvalidGraphOrphanActionException(String action) {
        super("Invalid graph orphan action: " + action);
    }
}
