package br.com.byop.aegis.pages.exception;

import java.util.UUID;

public class PageSectionNotFoundException extends RuntimeException {

    public PageSectionNotFoundException(UUID sectionId) {
        super("Page section not found: " + sectionId);
    }
}
