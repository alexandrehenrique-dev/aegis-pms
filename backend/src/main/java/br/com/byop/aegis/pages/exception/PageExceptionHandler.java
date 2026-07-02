package br.com.byop.aegis.pages.exception;

import br.com.byop.aegis.core.CoreErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PageExceptionHandler {

    @ExceptionHandler(PageNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public CoreErrorResponse handlePageNotFound() {
        return new CoreErrorResponse("PAGE_NOT_FOUND");
    }

    @ExceptionHandler(PageSectionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public CoreErrorResponse handlePageSectionNotFound() {
        return new CoreErrorResponse("PAGE_SECTION_NOT_FOUND");
    }

    @ExceptionHandler(EventNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public CoreErrorResponse handleEventNotFound() {
        return new CoreErrorResponse("EVENT_NOT_FOUND");
    }

    @ExceptionHandler(DuplicatePageSlugException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public CoreErrorResponse handleDuplicatePageSlug() {
        return new CoreErrorResponse("PAGE_SLUG_ALREADY_EXISTS");
    }

    @ExceptionHandler(InvalidSectionReorderException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CoreErrorResponse handleInvalidSectionReorder() {
        return new CoreErrorResponse("INVALID_SECTION_REORDER");
    }

    @ExceptionHandler(UnknownBlockTypeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CoreErrorResponse handleUnknownBlockType() {
        return new CoreErrorResponse("UNKNOWN_BLOCK_TYPE");
    }

    @ExceptionHandler(InvalidPageStatusException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CoreErrorResponse handleInvalidPageStatus() {
        return new CoreErrorResponse("INVALID_PAGE_STATUS");
    }

    @ExceptionHandler(InvalidEventAccessTypeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CoreErrorResponse handleInvalidEventAccessType() {
        return new CoreErrorResponse("INVALID_EVENT_TYPE");
    }

    @ExceptionHandler(InvalidEventVisibilityException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CoreErrorResponse handleInvalidEventVisibility() {
        return new CoreErrorResponse("INVALID_EVENT_VISIBILITY");
    }

    @ExceptionHandler(InvalidSectionContentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CoreErrorResponse handleInvalidSectionContent(InvalidSectionContentException ex) {
        return new CoreErrorResponse(ex.getErrorCode());
    }
}
