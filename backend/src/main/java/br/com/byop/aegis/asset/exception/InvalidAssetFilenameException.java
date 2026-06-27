package br.com.byop.aegis.asset.exception;

public class InvalidAssetFilenameException extends RuntimeException {

    public InvalidAssetFilenameException(String originalFilename) {
        super("Asset filename is invalid after sanitization: " + originalFilename);
    }
}
