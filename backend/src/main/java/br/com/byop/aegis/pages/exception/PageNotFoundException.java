package br.com.byop.aegis.pages.exception;

import java.util.UUID;

public class PageNotFoundException extends RuntimeException {

    public PageNotFoundException(UUID pageId) {
        super("Page not found: " + pageId);
    }
}
