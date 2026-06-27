package br.com.byop.aegis.submission.exception;

import java.util.UUID;

public class SubmissionNotFoundException extends RuntimeException {

    public SubmissionNotFoundException(UUID submissionId) {
        super("Submission not found: " + submissionId);
    }
}
