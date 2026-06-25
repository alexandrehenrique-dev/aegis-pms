package br.com.byop.aegis.core.product.exception;

import java.util.UUID;

public class ProductContentAccessDeniedException extends RuntimeException {

    public ProductContentAccessDeniedException(UUID productId) {
        super("SUPER_ADMIN role requires explicit ProductAssignment to access product content: " + productId);
    }
}
