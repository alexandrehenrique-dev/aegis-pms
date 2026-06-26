package br.com.byop.aegis.tenant.exception;

public class InvalidTenantStatusException extends RuntimeException {

    public InvalidTenantStatusException(String status) {
        super("Invalid tenant status: " + status);
    }
}
