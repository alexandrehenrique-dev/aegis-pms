package br.com.byop.aegis.product.exception;

public class InvalidModuleKeyException extends RuntimeException {

    public InvalidModuleKeyException(String moduleKey) {
        super("Invalid module key: " + moduleKey);
    }
}
