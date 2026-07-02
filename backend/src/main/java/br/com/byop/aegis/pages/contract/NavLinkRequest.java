package br.com.byop.aegis.pages.contract;

import jakarta.validation.constraints.NotBlank;

public record NavLinkRequest(
        @NotBlank String label,
        @NotBlank String href
) {
}
