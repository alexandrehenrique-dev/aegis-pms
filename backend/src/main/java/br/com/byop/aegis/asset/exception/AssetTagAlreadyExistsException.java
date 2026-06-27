package br.com.byop.aegis.asset.exception;

import java.util.UUID;

public class AssetTagAlreadyExistsException extends RuntimeException {

    public AssetTagAlreadyExistsException(UUID productId, String name) {
        super("Asset tag already exists for product " + productId + ": " + name);
    }
}
