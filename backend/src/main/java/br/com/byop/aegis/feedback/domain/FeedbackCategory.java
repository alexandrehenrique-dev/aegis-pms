package br.com.byop.aegis.feedback.domain;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;

/**
 * Catalogo fechado de categorias aceitas para feedbacks reportados por usuarios.
 */
public enum FeedbackCategory {
    BUG("Bug"),
    CONFUSING_UX("UX confusa"),
    VISUAL_ERROR("Erro visual"),
    INCORRECT_PERMISSION("Permissão incorreta"),
    WRONG_INFORMATION("Informação errada"),
    SUGGESTION("Sugestão");

    private final String contractValue;

    FeedbackCategory(String contractValue) {
        this.contractValue = contractValue;
    }

    public String contractValue() {
        return contractValue;
    }

    public static FeedbackCategory fromContractValue(String value) {
        String normalized = normalize(value);
        return Arrays.stream(values())
                .filter(category -> normalize(category.contractValue).equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported feedback category: " + value));
    }

    private static String normalize(String value) {
        String withoutAccent = Normalizer.normalize(String.valueOf(value).trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccent.toLowerCase(Locale.ROOT);
    }
}
