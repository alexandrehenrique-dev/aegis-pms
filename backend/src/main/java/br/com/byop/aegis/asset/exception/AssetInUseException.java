package br.com.byop.aegis.asset.exception;

import java.util.UUID;

public class AssetInUseException extends RuntimeException {

    public AssetInUseException(UUID assetId) {
        super("Asset is in use and requires force=true to be deleted: " + assetId);
    }
}
