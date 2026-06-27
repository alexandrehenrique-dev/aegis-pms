package br.com.byop.aegis.asset.exception;

public class InvalidAssetTagNameException extends RuntimeException {

    public InvalidAssetTagNameException() {
        super("Asset tag name must not be blank");
    }
}
