package br.com.byop.aegis.pages.contract;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record FooterRequest(
        String addressText,
        @NotNull List<@Valid NavLinkRequest> links
) {
}
