package br.com.byop.aegis.content.exception;

/**
 * Lancada quando a exclusao definitiva de um {@link br.com.byop.aegis.content.domain.Content}
 * e solicitada fora da unica janela permitida: rascunho ({@code DRAFT}) que
 * nunca foi publicado ({@code currentVersion == 1}). Qualquer outro estado
 * deve seguir a transicao normal para {@code ARCHIVED}.
 */
public class ContentDeletionNotAllowedException extends RuntimeException {

    public ContentDeletionNotAllowedException() {
        super("Only a never-published draft (currentVersion == 1) can be permanently deleted; use the Archived transition instead");
    }
}
