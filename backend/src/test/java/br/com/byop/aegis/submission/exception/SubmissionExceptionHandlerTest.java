package br.com.byop.aegis.submission.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SubmissionExceptionHandlerTest {

    private final SubmissionExceptionHandler handler = new SubmissionExceptionHandler();

    @Test
    void shouldMapMissingSubmissionError() {
        assertThat(handler.handleSubmissionNotFound().error()).isEqualTo("SUBMISSION_NOT_FOUND");
    }

    @Test
    void shouldMapInvalidSubmissionError() {
        InvalidSubmissionException exception = new InvalidSubmissionException("INVALID_SUBMISSION_UPLOAD_ASSET");

        assertThat(exception.getErrorCode()).isEqualTo("INVALID_SUBMISSION_UPLOAD_ASSET");
        assertThat(handler.handleInvalidSubmission(exception).error()).isEqualTo("INVALID_SUBMISSION_UPLOAD_ASSET");
    }
}
