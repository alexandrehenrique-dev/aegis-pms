package br.com.byop.aegis.shared.markdown;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SharedMarkdownSanitizerTest {

    private final SharedMarkdownSanitizer sanitizer = new SharedMarkdownSanitizer();

    @Test
    void shouldSanitizeDangerousHtmlAndKeepAllowedInlineMarkdownHtml() {
        String markdown = "<p style=\"color:red\">ok</p><script>alert(1)</script>"
                + "<a href=\"javascript:alert(1)\">bad</a>"
                + "<span class=\"text-aegis-blue\">blue</span>"
                + "<span class=\"unknown\">plain</span>";

        String sanitized = sanitizer.sanitize(markdown);

        assertThat(sanitized)
                .doesNotContain("style", "script", "javascript:", "unknown")
                .contains("<p>ok</p>", "<span class=\"text-aegis-blue\">blue</span>", "plain");
    }

    @Test
    void shouldReturnNullWhenInputIsNull() {
        assertThat(sanitizer.sanitize(null)).isNull();
    }
}
