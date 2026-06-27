package br.com.byop.aegis.asset.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssetFileContentTest {

    @Test
    void shouldBeEqualWhenContentArrayHasSameBytes() {
        AssetFileContent first = new AssetFileContent(new byte[]{1, 2, 3}, "application/pdf", "curriculo.pdf");
        AssetFileContent second = new AssetFileContent(new byte[]{1, 2, 3}, "application/pdf", "curriculo.pdf");

        assertThat(first)
                .isEqualTo(second)
                .isEqualTo(first)
                .hasSameHashCodeAs(second);
    }

    @Test
    void shouldNotBeEqualWhenContentArrayDiffers() {
        AssetFileContent first = new AssetFileContent(new byte[]{1, 2, 3}, "application/pdf", "curriculo.pdf");
        AssetFileContent withDifferentContent = new AssetFileContent(new byte[]{9, 9, 9}, "application/pdf", "curriculo.pdf");

        assertThat(first).isNotEqualTo(withDifferentContent);
    }

    @Test
    void shouldNotBeEqualWhenContentTypeDiffers() {
        AssetFileContent first = new AssetFileContent(new byte[]{1, 2, 3}, "application/pdf", "curriculo.pdf");
        AssetFileContent withDifferentContentType = new AssetFileContent(new byte[]{1, 2, 3}, "image/png", "curriculo.pdf");

        assertThat(first).isNotEqualTo(withDifferentContentType);
    }

    @Test
    void shouldNotBeEqualWhenFilenameDiffers() {
        AssetFileContent first = new AssetFileContent(new byte[]{1, 2, 3}, "application/pdf", "curriculo.pdf");
        AssetFileContent withDifferentFilename = new AssetFileContent(new byte[]{1, 2, 3}, "application/pdf", "outro.pdf");

        assertThat(first).isNotEqualTo(withDifferentFilename);
    }

    @Test
    void shouldNotBeEqualToNullOrDifferentType() {
        AssetFileContent first = new AssetFileContent(new byte[]{1}, "application/pdf", "curriculo.pdf");

        assertThat(first)
                .isNotEqualTo(null)
                .isNotEqualTo("not-an-asset-file-content");
    }

    @Test
    void shouldExposeArrayContentInToString() {
        AssetFileContent content = new AssetFileContent(new byte[]{1, 2}, "image/png", "foto.png");

        assertThat(content.toString())
                .contains("foto.png")
                .contains("image/png")
                .contains("[1, 2]");
    }
}
