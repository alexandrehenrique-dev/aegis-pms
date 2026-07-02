package br.com.byop.aegis.pages.exception;

public class DuplicatePageSlugException extends RuntimeException {

    public DuplicatePageSlugException(String slug) {
        super("Page slug already exists in this product: " + slug);
    }
}
