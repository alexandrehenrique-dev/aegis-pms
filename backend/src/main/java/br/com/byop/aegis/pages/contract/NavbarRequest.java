package br.com.byop.aegis.pages.contract;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record NavbarRequest(
        UUID logoAssetId,
        @NotNull List<@Valid NavLinkRequest> links
) {
}
