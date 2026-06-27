package br.com.byop.aegis.asset.service;

import br.com.byop.aegis.asset.config.AssetLimitsProperties;
import br.com.byop.aegis.asset.domain.AssetCategory;
import br.com.byop.aegis.asset.exception.AssetSizeLimitExceededException;
import br.com.byop.aegis.asset.exception.InvalidAssetMimeTypeException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssetUploadValidatorTest {

    private final AssetLimitsProperties limitsProperties = new AssetLimitsProperties(Map.of(
            "image", new AssetLimitsProperties.CategoryLimit(List.of("image/png", "image/jpeg"), 5_242_880L),
            "pdf", new AssetLimitsProperties.CategoryLimit(List.of("application/pdf"), 15_728_640L)
    ));

    private final AssetUploadValidator validator = new AssetUploadValidator(limitsProperties);

    @Test
    void shouldResolveCategoryForKnownMimeType() {
        assertThat(validator.validate("image/png", 1024L)).isEqualTo(AssetCategory.IMAGE);
    }

    @Test
    void shouldRejectUnknownMimeType() {
        assertThatThrownBy(() -> validator.validate("application/x-msdownload", 1024L))
                .isInstanceOf(InvalidAssetMimeTypeException.class);
    }

    @Test
    void shouldRejectFileAboveCategoryLimit() {
        assertThatThrownBy(() -> validator.validate("image/png", 6_000_000L))
                .isInstanceOf(AssetSizeLimitExceededException.class);
    }

    @Test
    void shouldAcceptFileAtExactLimit() {
        assertThat(validator.validate("image/png", 5_242_880L)).isEqualTo(AssetCategory.IMAGE);
    }

}
