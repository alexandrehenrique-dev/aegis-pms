package br.com.byop.aegis.asset.exception;

import br.com.byop.aegis.asset.domain.AssetCategory;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import software.amazon.awssdk.core.exception.SdkClientException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AssetExceptionHandlerTest {

    private final AssetExceptionHandler handler = new AssetExceptionHandler();

    @Test
    void shouldReturnAssetErrorCodes() {
        assertThat(handler.handleAssetNotFound().error()).isEqualTo("ASSET_NOT_FOUND");
        assertThat(handler.handleAssetTagNotFound().error()).isEqualTo("ASSET_TAG_NOT_FOUND");
        assertThat(handler.handleAssetTagAlreadyExists().error()).isEqualTo("ASSET_TAG_ALREADY_EXISTS");
        assertThat(handler.handleInvalidAssetTagName().error()).isEqualTo("INVALID_ASSET_TAG_NAME");
        assertThat(handler.handleInvalidAssetMimeType().error()).isEqualTo("INVALID_ASSET_MIME_TYPE");
        assertThat(handler.handleInvalidAssetFilename().error()).isEqualTo("INVALID_ASSET_FILENAME");
        assertThat(handler.handleAssetSizeLimitExceeded().error()).isEqualTo("ASSET_SIZE_LIMIT_EXCEEDED");
        assertThat(handler.handleAssetInUse().error()).isEqualTo("ASSET_IN_USE");
        assertThat(handler.handleS3Unavailable().error()).isEqualTo("ASSET_STORAGE_UNAVAILABLE");
    }

    @Test
    void shouldExposeExceptionMessages() {
        UUID assetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        assertThat(new AssetNotFoundException(assetId)).hasMessage("Asset not found: " + assetId);
        assertThat(new AssetTagNotFoundException("blog")).hasMessage("Asset tag not found: blog");
        assertThat(new AssetTagAlreadyExistsException(productId, "blog"))
                .hasMessage("Asset tag already exists for product " + productId + ": blog");
        assertThat(new InvalidAssetTagNameException()).hasMessage("Asset tag name must not be blank");
        assertThat(new InvalidAssetMimeTypeException("application/x-msdownload"))
                .hasMessage("Unsupported asset mime type: application/x-msdownload");
        assertThat(new InvalidAssetFilenameException("###.png"))
                .hasMessage("Asset filename is invalid after sanitization: ###.png");
        assertThat(new AssetSizeLimitExceededException(AssetCategory.IMAGE, 999L, 100L))
                .hasMessage("Asset of category IMAGE (999 bytes) exceeds the limit of 100 bytes");
        assertThat(new AssetInUseException(assetId))
                .hasMessage("Asset is in use and requires force=true to be deleted: " + assetId);
        Exception cause = new Exception("disk full");
        assertThat(new AssetStorageException("Failed", cause)).hasMessage("Failed").hasCause(cause);
    }

    @Test
    void shouldMapMaxUploadSizeExceededToSameErrorCode() {
        assertThat(handler.handleAssetSizeLimitExceeded().error()).isEqualTo("ASSET_SIZE_LIMIT_EXCEEDED");
        assertThat(new MaxUploadSizeExceededException(1024L)).hasMessageContaining("1024");
    }

    @Test
    void shouldHandleSdkClientExceptionAsStorageUnavailable() {
        assertThat(handler.handleS3Unavailable().error()).isEqualTo("ASSET_STORAGE_UNAVAILABLE");
        assertThat(SdkClientException.builder().message("Bucket cannot be empty.").build())
                .hasMessage("Bucket cannot be empty.");
    }
}
