package br.com.byop.aegis.content.exception;

public class InsufficientContentRoleException extends RuntimeException {

    public InsufficientContentRoleException() {
        super("Only SUPER_ADMIN or PRODUCT_MANAGER can publish content");
    }
}
