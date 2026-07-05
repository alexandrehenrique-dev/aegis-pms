package br.com.byop.aegis.content.exception;

public class DuplicateContentTitleException extends RuntimeException {

    public DuplicateContentTitleException(String title) {
        super("Content title already exists in this product: " + title);
    }
}
