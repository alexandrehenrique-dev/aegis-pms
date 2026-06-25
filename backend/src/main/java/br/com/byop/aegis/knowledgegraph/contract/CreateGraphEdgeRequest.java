package br.com.byop.aegis.knowledgegraph.contract;

import br.com.byop.aegis.knowledgegraph.domain.GraphEdgeType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateGraphEdgeRequest(
        @NotNull UUID sourceNodeId,
        @NotNull UUID targetNodeId,
        @NotNull GraphEdgeType edgeType,
        BigDecimal weight,
        String metadataJson
) {
}
