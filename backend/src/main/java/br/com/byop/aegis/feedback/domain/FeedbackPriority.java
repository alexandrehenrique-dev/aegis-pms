package br.com.byop.aegis.feedback.domain;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;

/**
 * Catalogo fechado de prioridades aceitas para feedbacks reportados por usuarios.
 */
public enum FeedbackPriority {
    LOW("baixa"),
    MEDIUM("média"),
    HIGH("alta"),
    CRITICAL("crítica");

    private final String contractValue;

    FeedbackPriority(String contractValue) {
        this.contractValue = contractValue;
    }

    public String contractValue() {
        return contractValue;
    }

    public static FeedbackPriority fromContractValue(String value) {
        String normalized = normalize(value);
        return Arrays.stream(values())
                .filter(priority -> normalize(priority.contractValue).equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported feedback priority: " + value));
    }

    private static String normalize(String value) {
        String withoutAccent = Normalizer.normalize(String.valueOf(value).trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccent.toLowerCase(Locale.ROOT);
    }
}
