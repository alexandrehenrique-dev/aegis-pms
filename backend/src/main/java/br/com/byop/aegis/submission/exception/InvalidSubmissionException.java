package br.com.byop.aegis.submission.exception;

public class InvalidSubmissionException extends RuntimeException {

    private final String errorCode;

    public InvalidSubmissionException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
