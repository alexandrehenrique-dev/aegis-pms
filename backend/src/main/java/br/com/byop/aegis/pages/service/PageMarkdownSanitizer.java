package br.com.byop.aegis.pages.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Sanitiza markdown com HTML inline embutido antes de persistir {@code
 * contentJson} de uma {@link br.com.byop.aegis.pages.domain.PageSection} — mesma
 * allowlist e mesma regra da Sprint 11 ({@link
 * br.com.byop.aegis.content.service.MarkdownSanitizer}), duplicada aqui porque
 * a classe original vive no pacote interno de {@code content} (nao exportado
 * via {@code content.api}) e o Spring Modulith proibe import direto entre
 * pacotes internos de modulos diferentes. Nomeada {@code PageMarkdownSanitizer}
 * (nao {@code MarkdownSanitizer}) porque o component-scan do Spring gera o nome
 * do bean a partir do simple name da classe — duas classes homonimas em pacotes
 * diferentes colidem em {@code ConflictingBeanDefinitionException} (bug real
 * encontrado na validacao manual desta sprint).
 */
@Component
public class PageMarkdownSanitizer {

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
