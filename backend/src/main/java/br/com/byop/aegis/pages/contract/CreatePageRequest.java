package br.com.byop.aegis.pages.contract;

import jakarta.validation.constraints.NotBlank;

public record CreatePageRequest(
        @NotBlank String slug,
        @NotBlank String title,
        @NotBlank String locale
) {
}
