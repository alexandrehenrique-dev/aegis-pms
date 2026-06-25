package br.com.byop.aegis.product.exception;

public class InvalidProductTypeException extends RuntimeException {

    public InvalidProductTypeException(String productType) {
        super("Invalid product type: " + productType);
    }
}
