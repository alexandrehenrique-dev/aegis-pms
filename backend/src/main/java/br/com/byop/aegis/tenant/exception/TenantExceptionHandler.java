package br.com.byop.aegis.tenant.exception;

import br.com.byop.aegis.core.CoreErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TenantExceptionHandler {

    @ExceptionHandler(TenantAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public CoreErrorResponse handleTenantAlreadyExists() {
        return new CoreErrorResponse("TENANT_ALREADY_EXISTS");
    }

    @ExceptionHandler(TenantNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public CoreErrorResponse handleTenantNotFound() {
        return new CoreErrorResponse("TENANT_NOT_FOUND");
    }

    @ExceptionHandler(InvalidTenantConfirmationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CoreErrorResponse handleInvalidTenantConfirmation() {
        return new CoreErrorResponse("INVALID_TENANT_CONFIRMATION");
    }

    @ExceptionHandler(InvalidTenantStatusException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CoreErrorResponse handleInvalidTenantStatus() {
        return new CoreErrorResponse("INVALID_TENANT_STATUS");
    }
}
