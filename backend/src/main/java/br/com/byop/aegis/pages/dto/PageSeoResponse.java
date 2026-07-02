package br.com.byop.aegis.pages.dto;

import java.util.UUID;

public record PageSeoResponse(
        String title,
        String description,
        String canonical,
        UUID ogImageAssetId,
        boolean noIndex
) {
}
