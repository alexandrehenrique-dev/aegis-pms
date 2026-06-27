package br.com.byop.aegis.product.user.exception;

public class InvalidTenantUserOperationException extends RuntimeException {

    public InvalidTenantUserOperationException(String message) {
        super(message);
    }
}
