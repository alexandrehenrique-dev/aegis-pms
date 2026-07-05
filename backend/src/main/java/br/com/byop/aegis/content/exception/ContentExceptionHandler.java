package br.com.byop.aegis.content.exception;

import br.com.byop.aegis.core.CoreErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ContentExceptionHandler {

    @ExceptionHandler(ContentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public CoreErrorResponse handleContentNotFound() {
        return new CoreErrorResponse("CONTENT_NOT_FOUND");
    }

    @ExceptionHandler(InvalidContentTransitionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CoreErrorResponse handleInvalidContentTransition() {
        return new CoreErrorResponse("INVALID_CONTENT_TRANSITION");
    }

    @ExceptionHandler(InsufficientContentRoleException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public CoreErrorResponse handleInsufficientContentRole() {
        return new CoreErrorResponse("CONTENT_PUBLISH_FORBIDDEN");
    }

    @ExceptionHandler(InvalidContentStatusException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CoreErrorResponse handleInvalidContentStatus() {
        return new CoreErrorResponse("INVALID_CONTENT_STATUS");
    }

    @ExceptionHandler(InvalidDifficultyLevelException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CoreErrorResponse handleInvalidDifficultyLevel() {
        return new CoreErrorResponse("INVALID_DIFFICULTY_LEVEL");
    }

    @ExceptionHandler(InvalidContentReferenceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CoreErrorResponse handleInvalidContentReference() {
        return new CoreErrorResponse("INVALID_KG_REFERENCE");
    }

    @ExceptionHandler(DuplicateContentTitleException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public CoreErrorResponse handleDuplicateContentTitle() {
        return new CoreErrorResponse("CONTENT_TITLE_ALREADY_EXISTS");
    }
}
