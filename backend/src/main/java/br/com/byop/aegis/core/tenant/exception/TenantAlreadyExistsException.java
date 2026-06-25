package br.com.byop.aegis.core.tenant.exception;

public class TenantAlreadyExistsException extends RuntimeException {

    public TenantAlreadyExistsException(String key) {
        super("Tenant already exists with key: " + key);
    }
}
