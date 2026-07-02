package br.com.byop.aegis.pages.domain;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Tabela estatica de {@link BlockTypeDefinition} por {@link BlockType} — a unica
 * fonte de verdade sobre quais tipos aceitam sub-blocos e em quais chaves.
 * Hoje apenas {@link BlockType#TWO_COLUMN} tem {@code childSlots} preenchido
 * ({@code left}/{@code right}), mas um {@link BlockType} futuro pode ganhar
 * sub-blocos apenas adicionando uma entrada aqui — nunca exige um {@code if}
 * novo em {@code SectionContentValidationService}.
 */
public final class BlockTypeCatalog {

    private static final Map<BlockType, BlockTypeDefinition> DEFINITIONS = buildDefinitions();

    private BlockTypeCatalog() {
    }

    public static BlockTypeDefinition definitionOf(BlockType type) {
        return DEFINITIONS.get(type);
    }

    private static Map<BlockType, BlockTypeDefinition> buildDefinitions() {
        Map<BlockType, BlockTypeDefinition> definitions = new EnumMap<>(BlockType.class);
        for (BlockType type : BlockType.values()) {
            definitions.put(type, new BlockTypeDefinition(type, List.of(), List.of()));
        }
        definitions.put(BlockType.TWO_COLUMN, new BlockTypeDefinition(
                BlockType.TWO_COLUMN,
                List.of("left", "right"),
                List.of(BlockType.TEXT, BlockType.RICH_TEXT, BlockType.IMAGE, BlockType.CTA_SECTION)
        ));
        return Map.copyOf(definitions);
    }
}
