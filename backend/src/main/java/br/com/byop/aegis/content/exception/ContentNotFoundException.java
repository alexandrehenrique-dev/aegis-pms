package br.com.byop.aegis.content.exception;

import java.util.UUID;

public class ContentNotFoundException extends RuntimeException {

    public ContentNotFoundException(UUID contentId) {
        super("Content not found: " + contentId);
    }
}
