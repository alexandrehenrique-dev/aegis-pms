package br.com.byop.aegis.knowledgegraph.contract;

import br.com.byop.aegis.knowledgegraph.domain.GraphNodeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateGraphNodeRequest(
        @NotNull GraphNodeType nodeType,
        @NotBlank String refType,
        @NotBlank String refId,
        @NotBlank String label,
        String slug,
        String metadataJson
) {
}
