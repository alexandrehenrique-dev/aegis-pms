package br.com.byop.aegis.pages.exception;

/**
 * Lancada por {@code SectionContentValidationService} quando {@code contentJson}
 * de uma {@link br.com.byop.aegis.pages.domain.PageSection} viola a regra minima
 * de validacao do seu {@link br.com.byop.aegis.pages.domain.BlockType} (Secao C
 * da Sprint 23). {@code errorCode} identifica a regra violada — traduzido 1:1
 * para o corpo {@code {"error": errorCode}} pelo exception handler.
 */
public class InvalidSectionContentException extends RuntimeException {

    private final String errorCode;

    public InvalidSectionContentException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
