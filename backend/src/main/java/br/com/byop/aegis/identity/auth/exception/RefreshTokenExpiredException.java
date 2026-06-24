package br.com.byop.aegis.identity.auth.exception;

public class RefreshTokenExpiredException extends RuntimeException {

    public RefreshTokenExpiredException() {
        super("Refresh token expired");
    }
}
