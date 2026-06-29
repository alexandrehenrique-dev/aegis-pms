package br.com.byop.aegis.knowledgegraph.api;

import java.util.UUID;

/**
 * Comando publico usado pelo seed local para garantir uma edge do Knowledge Graph.
 */
public record GraphSeedEdgeCommand(
        UUID productId,
        UUID sourceNodeId,
        UUID targetNodeId,
        String edgeType
) {
}
