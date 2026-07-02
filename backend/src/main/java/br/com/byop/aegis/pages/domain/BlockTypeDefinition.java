package br.com.byop.aegis.pages.domain;

import java.util.List;

/**
 * Definicao catalogada de um {@link BlockType}: quais chaves de {@code contentJson}
 * (se houver) guardam sub-blocos ({@code childSlots}) e, quando houver, quais
 * {@link BlockType} sao aceitos como filho nessas chaves ({@code acceptsChildren}).
 * Bloco sem sub-blocos tem as duas listas vazias — o mecanismo de validacao de
 * filhos (ver {@code SectionContentValidationService}) e sempre generico sobre
 * esta tabela, nunca condicional a um {@link BlockType} especifico.
 */
public record BlockTypeDefinition(
        BlockType type,
        List<String> childSlots,
        List<BlockType> acceptsChildren
) {

    public boolean acceptsChildBlocks() {
        return !childSlots.isEmpty();
    }
}
