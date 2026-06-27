package br.com.byop.aegis.asset.exception;

import br.com.byop.aegis.asset.domain.AssetCategory;

public class AssetSizeLimitExceededException extends RuntimeException {

    public AssetSizeLimitExceededException(AssetCategory category, long sizeBytes, long maxSizeBytes) {
        super("Asset of category " + category + " (" + sizeBytes + " bytes) exceeds the limit of " + maxSizeBytes + " bytes");
    }
}
