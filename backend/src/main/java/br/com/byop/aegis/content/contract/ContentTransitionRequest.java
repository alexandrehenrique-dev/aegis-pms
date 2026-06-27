package br.com.byop.aegis.content.contract;

import jakarta.validation.constraints.NotBlank;

public record ContentTransitionRequest(
        @NotBlank String from,
        @NotBlank String to,
        String comment
) {
}
