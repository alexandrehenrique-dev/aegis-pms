package br.com.byop.aegis.audit.exception;

import br.com.byop.aegis.core.CoreErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuditEventExceptionHandler {

    @ExceptionHandler(AuditEventNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public CoreErrorResponse handleAuditEventNotFound() {
        return new CoreErrorResponse("AUDIT_EVENT_NOT_FOUND");
    }

    @ExceptionHandler(InvalidAuditRiskFilterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CoreErrorResponse handleInvalidAuditRiskFilter() {
        return new CoreErrorResponse("INVALID_AUDIT_RISK_FILTER");
    }
}
