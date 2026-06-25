package br.com.byop.aegis.product.exception;

import br.com.byop.aegis.core.CoreErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ProductExceptionHandler {

    @ExceptionHandler(ProductAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public CoreErrorResponse handleProductAlreadyExists() {
        return new CoreErrorResponse("PRODUCT_ALREADY_EXISTS");
    }

    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public CoreErrorResponse handleProductNotFound() {
        return new CoreErrorResponse("PRODUCT_NOT_FOUND");
    }

    @ExceptionHandler(InvalidProductTypeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CoreErrorResponse handleInvalidProductType() {
        return new CoreErrorResponse("INVALID_PRODUCT_TYPE");
    }

    @ExceptionHandler(InvalidModuleKeyException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CoreErrorResponse handleInvalidModuleKey() {
        return new CoreErrorResponse("INVALID_MODULE_KEY");
    }

    @ExceptionHandler(ModuleDependencyMissingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CoreErrorResponse handleModuleDependencyMissing(ModuleDependencyMissingException exception) {
        return new CoreErrorResponse(
                "MODULE_DEPENDENCY_MISSING",
                exception.getModuleKey().name(),
                exception.getRequires().name()
        );
    }

    @ExceptionHandler(ModuleDisabledException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public CoreErrorResponse handleModuleDisabled(ModuleDisabledException exception) {
        return new CoreErrorResponse("MODULE_DISABLED", exception.getModuleKey().name(), null);
    }

    @ExceptionHandler(ProductContentAccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public CoreErrorResponse handleProductContentAccessDenied() {
        return new CoreErrorResponse("PRODUCT_CONTENT_ACCESS_DENIED");
    }

    @ExceptionHandler(ModuleProductIdMissingException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public CoreErrorResponse handleModuleProductIdMissing() {
        return new CoreErrorResponse("MODULE_PRODUCT_ID_MISSING");
    }
}
