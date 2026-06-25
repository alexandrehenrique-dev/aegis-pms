package br.com.byop.aegis.core.product.exception;

public class InvalidProductTypeException extends RuntimeException {

    public InvalidProductTypeException(String productType) {
        super("Invalid product type: " + productType);
    }
}
