package br.com.byop.aegis.product.exception;

import java.util.UUID;

public class ProductAssignmentNotFoundException extends RuntimeException {

    public ProductAssignmentNotFoundException(UUID productId, String userSubject) {
        super("Product assignment not found for product " + productId + " and user " + userSubject);
    }
}
