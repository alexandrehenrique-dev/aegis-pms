package br.com.byop.aegis.product.user.exception;

import br.com.byop.aegis.core.CoreErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TenantUserExceptionHandler {

    @ExceptionHandler(TenantUserAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public CoreErrorResponse handleTenantUserAlreadyExists() {
        return new CoreErrorResponse("USER_ALREADY_EXISTS_IN_TENANT");
    }

    @ExceptionHandler(TenantUserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public CoreErrorResponse handleTenantUserNotFound() {
        return new CoreErrorResponse("USER_NOT_FOUND");
    }

    @ExceptionHandler(InvalidTenantUserOperationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CoreErrorResponse handleInvalidTenantUserOperation() {
        return new CoreErrorResponse("INVALID_USER_OPERATION");
    }

    @ExceptionHandler(LastTenantAdminException.class)
    public CoreErrorResponse handleLastTenantAdmin(HttpServletResponse response) {
        response.setStatus(422);
        return handleLastTenantAdmin();
    }

    public CoreErrorResponse handleLastTenantAdmin() {
        return new CoreErrorResponse("LAST_TENANT_ADMIN");
    }
}
