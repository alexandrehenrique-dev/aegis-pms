package br.com.byop.aegis.settings.exception;

import br.com.byop.aegis.core.CoreErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class SettingsExceptionHandler {

    @ExceptionHandler(SettingsNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public CoreErrorResponse handleSettingsNotFound() {
        return new CoreErrorResponse("SETTINGS_NOT_FOUND");
    }

    @ExceptionHandler(InsufficientSettingsRoleException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public CoreErrorResponse handleInsufficientSettingsRole() {
        return new CoreErrorResponse("SETTINGS_FORBIDDEN");
    }

    @ExceptionHandler(InvalidSettingsRoleException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CoreErrorResponse handleInvalidSettingsRole() {
        return new CoreErrorResponse("INVALID_SETTINGS_ROLE");
    }
}
