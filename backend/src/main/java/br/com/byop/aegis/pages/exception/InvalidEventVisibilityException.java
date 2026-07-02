package br.com.byop.aegis.pages.exception;

public class InvalidEventVisibilityException extends RuntimeException {

    public InvalidEventVisibilityException(String visibility) {
        super("Invalid event visibility: " + visibility);
    }
}
