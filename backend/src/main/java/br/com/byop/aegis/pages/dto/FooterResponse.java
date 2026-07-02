package br.com.byop.aegis.pages.dto;

import java.util.List;

public record FooterResponse(
        String addressText,
        List<NavLinkResponse> links
) {
}
