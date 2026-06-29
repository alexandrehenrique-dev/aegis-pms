package br.com.byop.aegis.knowledgegraph.api;

import java.util.UUID;

/**
 * Referencia publica de node garantido pelo seed local.
 */
public record GraphSeedNodeReference(
        UUID id,
        UUID productId,
        String refType,
        String refId,
        String label
) {
}
