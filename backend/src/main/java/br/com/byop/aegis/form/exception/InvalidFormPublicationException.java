package br.com.byop.aegis.form.exception;

public class InvalidFormPublicationException extends RuntimeException {

    private final String errorCode;

    public InvalidFormPublicationException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
