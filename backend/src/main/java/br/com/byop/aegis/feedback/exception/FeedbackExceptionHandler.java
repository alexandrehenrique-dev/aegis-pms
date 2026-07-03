package br.com.byop.aegis.feedback.exception;

import br.com.byop.aegis.core.CoreErrorResponse;
import br.com.byop.aegis.feedback.controller.FeedbackController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = FeedbackController.class)
public class FeedbackExceptionHandler {

    @ExceptionHandler(FeedbackNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public CoreErrorResponse handleFeedbackNotFound() {
        return new CoreErrorResponse("FEEDBACK_NOT_FOUND");
    }

    @ExceptionHandler(FeedbackAttachmentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public CoreErrorResponse handleFeedbackAttachmentNotFound() {
        return new CoreErrorResponse("FEEDBACK_ATTACHMENT_NOT_FOUND");
    }

    @ExceptionHandler(FeedbackForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public CoreErrorResponse handleFeedbackForbidden() {
        return new CoreErrorResponse("FEEDBACK_FORBIDDEN");
    }

    @ExceptionHandler(FeedbackContextRequiredException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CoreErrorResponse handleFeedbackContextRequired() {
        return new CoreErrorResponse("FEEDBACK_CONTEXT_REQUIRED");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CoreErrorResponse handleIllegalArgument() {
        return new CoreErrorResponse("INVALID_FEEDBACK_REQUEST");
    }
}
