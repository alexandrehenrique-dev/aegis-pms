package br.com.byop.aegis.content.exception;

import br.com.byop.aegis.content.domain.ContentStatus;

public class InvalidContentTransitionException extends RuntimeException {

    public InvalidContentTransitionException(ContentStatus from, ContentStatus to) {
        super("Invalid content transition from " + from + " to " + to);
    }
}
