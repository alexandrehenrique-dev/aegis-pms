package br.com.byop.aegis.identity.auth.exception;

public class AccountDisabledException extends RuntimeException {

    public AccountDisabledException() {
        super("Account disabled");
    }
}
