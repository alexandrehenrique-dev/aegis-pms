package br.com.byop.aegis.pages.exception;

/**
 * Lancada quando {@code ReorderSectionsRequest.sectionIds} nao corresponde
 * exatamente ao conjunto de secoes existentes da pagina — reordenar e sempre
 * uma substituicao completa da lista (Secao E da Sprint 23), nunca parcial.
 */
public class InvalidSectionReorderException extends RuntimeException {

    public InvalidSectionReorderException() {
        super("sectionIds must contain exactly the current sections of the page");
    }
}
