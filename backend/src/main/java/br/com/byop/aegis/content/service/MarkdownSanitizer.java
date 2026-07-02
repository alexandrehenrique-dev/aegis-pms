package br.com.byop.aegis.content.service;

import br.com.byop.aegis.shared.markdown.SharedMarkdownSanitizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

    private final SharedMarkdownSanitizer sharedMarkdownSanitizer;

    public MarkdownSanitizer() {
        this(new SharedMarkdownSanitizer());
    }

    @Autowired
    public MarkdownSanitizer(SharedMarkdownSanitizer sharedMarkdownSanitizer) {
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
