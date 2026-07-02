package br.com.byop.aegis.pages.dto;

import java.util.List;
import java.util.UUID;

public record NavbarResponse(
        UUID logoAssetId,
        List<NavLinkResponse> links
) {
}
