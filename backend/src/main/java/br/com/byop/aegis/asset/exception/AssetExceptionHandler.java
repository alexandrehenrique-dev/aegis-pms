package br.com.byop.aegis.asset.exception;

import br.com.byop.aegis.core.CoreErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import software.amazon.awssdk.core.exception.SdkException;

@RestControllerAdvice
public class AssetExceptionHandler {

    @ExceptionHandler(AssetNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public CoreErrorResponse handleAssetNotFound() {
        return new CoreErrorResponse("ASSET_NOT_FOUND");
    }

    @ExceptionHandler(AssetTagNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public CoreErrorResponse handleAssetTagNotFound() {
        return new CoreErrorResponse("ASSET_TAG_NOT_FOUND");
    }

    @ExceptionHandler(AssetTagAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public CoreErrorResponse handleAssetTagAlreadyExists() {
        return new CoreErrorResponse("ASSET_TAG_ALREADY_EXISTS");
    }

    @ExceptionHandler(InvalidAssetTagNameException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CoreErrorResponse handleInvalidAssetTagName() {
        return new CoreErrorResponse("INVALID_ASSET_TAG_NAME");
    }

    @ExceptionHandler(InvalidAssetMimeTypeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CoreErrorResponse handleInvalidAssetMimeType() {
        return new CoreErrorResponse("INVALID_ASSET_MIME_TYPE");
    }

    @ExceptionHandler(InvalidAssetFilenameException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CoreErrorResponse handleInvalidAssetFilename() {
        return new CoreErrorResponse("INVALID_ASSET_FILENAME");
    }

    @ExceptionHandler({AssetSizeLimitExceededException.class, MaxUploadSizeExceededException.class})
    @ResponseStatus(HttpStatus.CONTENT_TOO_LARGE)
    public CoreErrorResponse handleAssetSizeLimitExceeded() {
        return new CoreErrorResponse("ASSET_SIZE_LIMIT_EXCEEDED");
    }

    @ExceptionHandler(AssetInUseException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public CoreErrorResponse handleAssetInUse() {
        return new CoreErrorResponse("ASSET_IN_USE");
    }

    /**
     * Falha de comunicacao com o provider S3 (bucket nao configurado,
     * credenciais invalidas, indisponibilidade do servico) — nunca deixar
     * vazar a stack trace do AWS SDK nem repassar a excecao sem tratamento
     * (o filtro de seguranca a interpretaria incorretamente como 403).
     */
    @ExceptionHandler(SdkException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public CoreErrorResponse handleS3Unavailable() {
        return new CoreErrorResponse("ASSET_STORAGE_UNAVAILABLE");
    }
}
