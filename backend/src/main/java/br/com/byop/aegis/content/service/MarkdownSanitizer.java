package br.com.byop.aegis.content.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Sanitiza markdown com HTML inline embutido antes de persistir um {@link
 * br.com.byop.aegis.content.domain.Content}. O markdown em si nunca e convertido
 * para HTML — apenas a allowlist minima de tags/atributos documentada na Sprint 11
 * e aplicada, removendo qualquer tag fora dela (script, iframe, etc.), qualquer
 * atributo nao permitido (style, etc.) e qualquer link com protocolo perigoso
 * (javascript:, data:).
 */
@Component
public class MarkdownSanitizer {

    private static final Set<String> ALLOWED_SPAN_CLASSES = Set.of(
            "text-aegis-red",
            "text-aegis-blue",
            "text-aegis-green",
            "text-aegis-amber",
            "text-aegis-violet"
    );

    private static final Safelist SAFELIST = Safelist.none()
            .addTags("p", "strong", "em", "ul", "ol", "li", "blockquote", "h2", "h3", "a", "br", "span")
            .addAttributes("a", "href")
            .addAttributes("span", "class")
            .addProtocols("a", "href", "http", "https", "mailto")
            .preserveRelativeLinks(true);

    private static final Document.OutputSettings OUTPUT_SETTINGS = new Document.OutputSettings().prettyPrint(false);

    /**
     * Sanitiza o markdown recebido, removendo qualquer tag/atributo fora da
     * allowlist. Texto puro (sem HTML embutido) e devolvido inalterado.
     *
     * @param markdown markdown bruto, como recebido do cliente
     * @return markdown sanitizado, ou {@code null} se a entrada for {@code null}
     */
    public String sanitize(String markdown) {
        if (markdown == null) {
            return null;
        }
        String cleaned = Jsoup.clean(markdown, "", SAFELIST, OUTPUT_SETTINGS);
        return stripDisallowedSpanClasses(cleaned);
    }

    private String stripDisallowedSpanClasses(String html) {
        Document document = Jsoup.parseBodyFragment(html);
        document.outputSettings(OUTPUT_SETTINGS);
        List<Element> spans = document.body().select("span");
        for (Element span : spans) {
            if (!ALLOWED_SPAN_CLASSES.contains(span.attr("class"))) {
                span.unwrap();
            }
        }
        return document.body().html();
    }
}
