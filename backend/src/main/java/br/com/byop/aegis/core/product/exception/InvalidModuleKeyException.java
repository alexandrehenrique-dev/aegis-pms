package br.com.byop.aegis.core.product.exception;

public class InvalidModuleKeyException extends RuntimeException {

    public InvalidModuleKeyException(String moduleKey) {
        super("Invalid module key: " + moduleKey);
    }
}
