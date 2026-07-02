package br.com.byop.aegis.pages.service;

import br.com.byop.aegis.shared.markdown.SharedMarkdownSanitizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Sanitiza markdown com HTML inline embutido antes de persistir {@code
 * contentJson} de uma {@link br.com.byop.aegis.pages.domain.PageSection}. A
 * regra concreta vive em {@link SharedMarkdownSanitizer}, exposta pelo modulo
 * compartilhado para evitar imports diretos entre pacotes internos de dominios.
 */
@Component
public class PageMarkdownSanitizer {

    private final SharedMarkdownSanitizer sharedMarkdownSanitizer;

    public PageMarkdownSanitizer() {
        this(new SharedMarkdownSanitizer());
    }

    @Autowired
    public PageMarkdownSanitizer(SharedMarkdownSanitizer sharedMarkdownSanitizer) {
        this.sharedMarkdownSanitizer = sharedMarkdownSanitizer;
    }

    /**
     * Sanitiza o markdown recebido, removendo qualquer tag/atributo fora da
     * allowlist. Texto puro (sem HTML embutido) e devolvido inalterado.
     *
     * @param markdown markdown bruto, como recebido do cliente
     * @return markdown sanitizado, ou {@code null} se a entrada for {@code null}
     */
    public String sanitize(String markdown) {
        return sharedMarkdownSanitizer.sanitize(markdown);
    }
}
