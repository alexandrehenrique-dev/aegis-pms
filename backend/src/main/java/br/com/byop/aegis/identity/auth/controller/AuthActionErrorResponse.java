package br.com.byop.aegis.identity.auth.controller;

public record AuthActionErrorResponse(
        String error,
        String message
) {
    public AuthActionErrorResponse(String error) {
        this(error, null);
    }
}
