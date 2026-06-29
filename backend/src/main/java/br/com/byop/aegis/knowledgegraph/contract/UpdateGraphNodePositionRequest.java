package br.com.byop.aegis.knowledgegraph.contract;

import jakarta.validation.constraints.NotNull;

public record UpdateGraphNodePositionRequest(
        @NotNull Double x,
        @NotNull Double y
) {
}
