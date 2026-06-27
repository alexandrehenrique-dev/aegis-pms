package br.com.byop.aegis.form.exception;

import br.com.byop.aegis.core.CoreErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class FormExceptionHandler {

    @ExceptionHandler(FormNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public CoreErrorResponse handleFormNotFound() {
        return new CoreErrorResponse("FORM_NOT_FOUND");
    }

    @ExceptionHandler(InvalidFormPublicationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CoreErrorResponse handleInvalidPublication(InvalidFormPublicationException exception) {
        return new CoreErrorResponse(exception.getErrorCode());
    }

    @ExceptionHandler(InvalidFormDeliveryException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CoreErrorResponse handleInvalidDelivery() {
        return new CoreErrorResponse("INVALID_FORM_DELIVERY");
    }
}
