package br.com.byop.aegis.submission.exception;

import br.com.byop.aegis.core.CoreErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class SubmissionExceptionHandler {

    @ExceptionHandler(SubmissionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public CoreErrorResponse handleSubmissionNotFound() {
        return new CoreErrorResponse("SUBMISSION_NOT_FOUND");
    }

    @ExceptionHandler(InvalidSubmissionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CoreErrorResponse handleInvalidSubmission(InvalidSubmissionException exception) {
        return new CoreErrorResponse(exception.getErrorCode());
    }
}
