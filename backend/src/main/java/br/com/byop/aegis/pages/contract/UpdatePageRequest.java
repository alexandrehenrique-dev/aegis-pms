package br.com.byop.aegis.pages.contract;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record UpdatePageRequest(
        @NotBlank String slug,
        @NotBlank String title,
        @NotBlank String locale,
        @NotBlank String status,
        @Valid PageSeoRequest seo
) {
}
