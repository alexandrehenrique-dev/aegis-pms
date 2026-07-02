package br.com.byop.aegis.pages.exception;

/**
 * Lancada quando o {@code type} de uma {@link br.com.byop.aegis.pages.domain.PageSection}
 * nao pertence ao catalogo fechado de {@link br.com.byop.aegis.pages.domain.BlockType}
 * (Secao B da Sprint 23) — inclui os casos de {@code footer}/{@code navbar}, removidos
 * do catalogo em favor de {@code ProductGlobals}.
 */
public class UnknownBlockTypeException extends RuntimeException {

    public UnknownBlockTypeException(String type) {
        super("Unknown block type: " + type);
    }
}
