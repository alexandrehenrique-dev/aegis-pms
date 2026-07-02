package br.com.byop.aegis.pages.contract;

import jakarta.validation.constraints.NotBlank;

public record SocialLinkRequest(
        @NotBlank String platform,
        @NotBlank String href
) {
}
