package br.com.byop.aegis.feedback.exception;

public class FeedbackForbiddenException extends RuntimeException {

    public FeedbackForbiddenException() {
        super("Feedback operation forbidden.");
    }
}
