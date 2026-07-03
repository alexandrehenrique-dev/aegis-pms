package br.com.byop.aegis.feedback.exception;

public class FeedbackContextRequiredException extends RuntimeException {

    public FeedbackContextRequiredException() {
        super("Feedback requires an unambiguous tenant or product context.");
    }
}
