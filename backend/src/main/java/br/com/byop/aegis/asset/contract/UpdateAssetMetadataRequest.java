package br.com.byop.aegis.asset.contract;

import jakarta.validation.constraints.NotBlank;

public record UpdateAssetMetadataRequest(
        @NotBlank String friendlyName,
        String altText,
        String caption,
        String credit,
        String tags
) {
}
