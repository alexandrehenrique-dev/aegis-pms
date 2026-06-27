package br.com.byop.aegis.content.exception;

public class InvalidContentStatusException extends RuntimeException {

    public InvalidContentStatusException(String value) {
        super("Invalid content status: " + value);
    }
}
