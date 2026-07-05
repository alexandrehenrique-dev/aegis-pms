package br.com.byop.aegis.content.exception;

public class InsufficientContentDeleteRoleException extends RuntimeException {

    public InsufficientContentDeleteRoleException() {
        super("Only SUPER_ADMIN or TENANT_ADMIN can delete content");
    }
}
