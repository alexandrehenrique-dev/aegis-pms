package br.com.byop.aegis.pages.exception;

public class InvalidPageStatusException extends RuntimeException {

    public InvalidPageStatusException(String status) {
        super("Invalid page status: " + status);
    }
}
