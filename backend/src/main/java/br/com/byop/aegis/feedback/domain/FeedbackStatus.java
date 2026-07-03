package br.com.byop.aegis.feedback.domain;

import java.util.Arrays;

/**
 * Catalogo fechado de status operacionais de um feedback.
 */
public enum FeedbackStatus {
    OPEN("aberto"),
    IN_REVIEW("em_analise"),
    RESOLVED("resolvido");

    private final String contractValue;

    FeedbackStatus(String contractValue) {
        this.contractValue = contractValue;
    }

    public String contractValue() {
        return contractValue;
    }

    public static FeedbackStatus fromContractValue(String value) {
        return Arrays.stream(values())
                .filter(status -> status.contractValue.equalsIgnoreCase(String.valueOf(value).trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported feedback status: " + value));
    }
}
