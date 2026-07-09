package br.com.byop.aegis.product.exception;

public class InvalidProductStatusException extends RuntimeException {

    public InvalidProductStatusException(String status) {
        super("Invalid product status: " + status);
    }
}
