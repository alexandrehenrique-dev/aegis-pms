package br.com.byop.aegis.asset.exception;

public class AssetTagNotFoundException extends RuntimeException {

    public AssetTagNotFoundException(String name) {
        super("Asset tag not found: " + name);
    }
}
