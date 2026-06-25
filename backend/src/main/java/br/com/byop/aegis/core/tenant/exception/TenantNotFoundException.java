package br.com.byop.aegis.core.tenant.exception;

import java.util.UUID;

public class TenantNotFoundException extends RuntimeException {

    public TenantNotFoundException(UUID tenantId) {
        super("Tenant not found: " + tenantId);
    }
}
