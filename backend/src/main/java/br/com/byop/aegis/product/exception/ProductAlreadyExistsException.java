package br.com.byop.aegis.product.exception;

import java.util.UUID;

public class ProductAlreadyExistsException extends RuntimeException {

    public ProductAlreadyExistsException(UUID tenantId, String key) {
        super("Product already exists with key '" + key + "' for tenant: " + tenantId);
    }
}
