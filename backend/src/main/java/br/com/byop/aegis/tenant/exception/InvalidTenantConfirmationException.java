package br.com.byop.aegis.tenant.exception;

public class InvalidTenantConfirmationException extends RuntimeException {

    public InvalidTenantConfirmationException() {
        super("Tenant deletion confirmation text does not match tenant name");
    }
}
