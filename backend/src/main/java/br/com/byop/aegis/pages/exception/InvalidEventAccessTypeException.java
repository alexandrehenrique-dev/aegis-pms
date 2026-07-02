package br.com.byop.aegis.pages.exception;

public class InvalidEventAccessTypeException extends RuntimeException {

    public InvalidEventAccessTypeException(String type) {
        super("Invalid event type: " + type);
    }
}
