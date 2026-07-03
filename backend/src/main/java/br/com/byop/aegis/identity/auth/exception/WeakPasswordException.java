package br.com.byop.aegis.identity.auth.exception;

public class WeakPasswordException extends RuntimeException {

    public WeakPasswordException(String message) {
        super(message);
    }
}
