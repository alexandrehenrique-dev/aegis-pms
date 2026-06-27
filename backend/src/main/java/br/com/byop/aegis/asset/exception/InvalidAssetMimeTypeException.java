package br.com.byop.aegis.asset.exception;

public class InvalidAssetMimeTypeException extends RuntimeException {

    public InvalidAssetMimeTypeException(String mimeType) {
        super("Unsupported asset mime type: " + mimeType);
    }
}
