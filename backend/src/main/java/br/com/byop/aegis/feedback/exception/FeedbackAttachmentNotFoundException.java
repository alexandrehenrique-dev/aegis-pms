package br.com.byop.aegis.feedback.exception;

import java.util.UUID;

public class FeedbackAttachmentNotFoundException extends RuntimeException {

    public FeedbackAttachmentNotFoundException(UUID attachmentAssetId) {
        super("Feedback attachment not found: " + attachmentAssetId);
    }
}
