package br.com.byop.aegis.pages.contract;

import java.util.UUID;

public record PageSeoRequest(
        String title,
        String description,
        String canonical,
        UUID ogImageAssetId,
        boolean noIndex
) {
}
