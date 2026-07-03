package br.com.byop.aegis.product.export.exception;

import br.com.byop.aegis.core.CoreErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ProductExportExceptionHandler {

    @ExceptionHandler(InvalidProductDeleteConfirmationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CoreErrorResponse handleInvalidConfirmation() {
        return new CoreErrorResponse("INVALID_PRODUCT_DELETE_CONFIRMATION");
    }

    @ExceptionHandler(ProductDeleteForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public CoreErrorResponse handleForbiddenDelete() {
        return new CoreErrorResponse("PRODUCT_DELETE_FORBIDDEN");
    }

    @ExceptionHandler(ExportAlreadyInProgressException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public CoreErrorResponse handleExportAlreadyInProgress() {
        return new CoreErrorResponse("EXPORT_ALREADY_IN_PROGRESS");
    }

    @ExceptionHandler(ExportDownloadNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public CoreErrorResponse handleDownloadNotFound() {
        return new CoreErrorResponse("EXPORT_DOWNLOAD_NOT_FOUND");
    }

    @ExceptionHandler(ExportLinkExpiredException.class)
    @ResponseStatus(HttpStatus.GONE)
    public CoreErrorResponse handleExpiredLink() {
        return new CoreErrorResponse("EXPORT_LINK_EXPIRED");
    }
}
