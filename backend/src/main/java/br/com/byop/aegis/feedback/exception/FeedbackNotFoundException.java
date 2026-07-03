package br.com.byop.aegis.feedback.exception;

public class FeedbackNotFoundException extends RuntimeException {

    public FeedbackNotFoundException(String feedbackId) {
        super("Feedback not found: " + feedbackId);
    }
}
