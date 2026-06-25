package br.com.byop.aegis.core;

public record CoreErrorResponse(
        String error,
        String moduleKey,
        String requires
) {

    public CoreErrorResponse(String error) {
        this(error, null, null);
    }
}
