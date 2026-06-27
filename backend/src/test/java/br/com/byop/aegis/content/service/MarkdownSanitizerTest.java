package br.com.byop.aegis.content.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownSanitizerTest {

    private final MarkdownSanitizer sanitizer = new MarkdownSanitizer();

    @Test
    void shouldReturnNullWhenMarkdownIsNull() {
        assertThat(sanitizer.sanitize(null)).isNull();
    }

    @Test
    void shouldKeepPlainMarkdownTextUntouched() {
        String markdown = "# Titulo\n\nTexto **forte** e referencia {{kg-ref:11111111-1111-1111-1111-111111111111:Artigo}}.";

        assertThat(sanitizer.sanitize(markdown)).isEqualTo(markdown);
    }

    @Test
    void shouldKeepAllowedTags() {
        String markdown = "<p>paragrafo</p><strong>forte</strong><em>italico</em>"
                + "<ul><li>item</li></ul><ol><li>item</li></ol><blockquote>citacao</blockquote>"
                + "<h2>h2</h2><h3>h3</h3><br>";

        assertThat(sanitizer.sanitize(markdown)).isEqualTo(markdown);
    }

    @Test
    void shouldRemoveScriptTagButKeepInertText() {
        String sanitized = sanitizer.sanitize("<p>seguro</p><script>alert(1)</script>");

        assertThat(sanitized).doesNotContain("<script>").contains("<p>seguro</p>");
    }

    @Test
    void shouldRemoveIframeTag() {
        String sanitized = sanitizer.sanitize("<p>antes</p><iframe src=\"https://evil.example\"></iframe><p>depois</p>");

        assertThat(sanitized).doesNotContain("<iframe");
    }

    @Test
    void shouldBlockJavascriptProtocolInLink() {
        String sanitized = sanitizer.sanitize("<a href=\"javascript:alert(1)\">clique</a>");

        assertThat(sanitized).doesNotContain("javascript:");
    }

    @Test
    void shouldBlockDataProtocolInLink() {
        String sanitized = sanitizer.sanitize("<a href=\"data:text/html;base64,PHNjcmlwdD4=\">clique</a>");

        assertThat(sanitized).doesNotContain("data:");
    }

    @Test
    void shouldKeepHttpsLink() {
        String sanitized = sanitizer.sanitize("<a href=\"https://aegis.dev\">aegis</a>");

        assertThat(sanitized).isEqualTo("<a href=\"https://aegis.dev\">aegis</a>");
    }

    @Test
    void shouldRemoveStyleAttribute() {
        String sanitized = sanitizer.sanitize("<p style=\"color:red\">texto</p>");

        assertThat(sanitized).doesNotContain("style").isEqualTo("<p>texto</p>");
    }

    @Test
    void shouldKeepSpanWithAllowedClass() {
        String markdown = "<span class=\"text-aegis-red\">texto</span>";

        assertThat(sanitizer.sanitize(markdown)).isEqualTo(markdown);
    }

    @Test
    void shouldUnwrapSpanWithDisallowedClass() {
        String sanitized = sanitizer.sanitize("<span class=\"not-allowed\">texto</span>");

        assertThat(sanitized).isEqualTo("texto");
    }

    @Test
    void shouldUnwrapSpanWithoutClass() {
        String sanitized = sanitizer.sanitize("<span>texto</span>");

        assertThat(sanitized).isEqualTo("texto");
    }
}
