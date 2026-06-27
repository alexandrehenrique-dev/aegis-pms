package br.com.byop.aegis.asset.storage;

import br.com.byop.aegis.asset.exception.InvalidAssetFilenameException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssetFilenameSanitizerTest {

    @Test
    void shouldKeepAsciiLettersAndDigits() {
        assertThat(AssetFilenameSanitizer.sanitize("foto123.png")).isEqualTo("foto123.png");
    }

    @Test
    void shouldStripAccentsViaNormalization() {
        assertThat(AssetFilenameSanitizer.sanitize("café-ção.png")).isEqualTo("cafe-cao.png");
    }

    @Test
    void shouldDropInvalidCharactersFromExtension() {
        assertThat(AssetFilenameSanitizer.sanitize("foto.p&ng")).isEqualTo("foto.png");
    }

    @Test
    void shouldCoverFullLetterDigitBoundaryChecks() {
        assertThat(AssetFilenameSanitizer.sanitize("a5{-.png")).isEqualTo("a5.png");
    }

    @Test
    void shouldRejectWhenNormalizedBaseIsBlank() {
        assertThatThrownBy(() -> AssetFilenameSanitizer.sanitize("###.png"))
                .isInstanceOf(InvalidAssetFilenameException.class);
    }
}
