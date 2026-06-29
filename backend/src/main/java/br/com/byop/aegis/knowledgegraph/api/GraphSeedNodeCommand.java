package br.com.byop.aegis.knowledgegraph.api;

import java.util.UUID;

/**
 * Comando publico usado pelo seed local para garantir um node do Knowledge Graph.
 */
public record GraphSeedNodeCommand(
        UUID productId,
        String nodeType,
        String refType,
        String refId,
        String label,
        String slug,
        String metadataJson,
        double x,
        double y
) {
}
