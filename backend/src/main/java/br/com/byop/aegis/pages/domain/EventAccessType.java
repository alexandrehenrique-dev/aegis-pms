package br.com.byop.aegis.pages.domain;

import java.util.Arrays;

/**
 * Catalogo fechado do campo {@code type} de {@link Event} — publico ou privado.
 * Distinto de {@link EventVisibility}, que controla o nivel de exposicao do
 * evento (inclui o valor intermediario {@code public-summary}).
 */
public enum EventAccessType {
    PUBLIC("public"),
    PRIVATE("private");

    private final String contractValue;

    EventAccessType(String contractValue) {
        this.contractValue = contractValue;
    }

    public String contractValue() {
        return contractValue;
    }

    public static EventAccessType fromContractValue(String value) {
        return Arrays.stream(values())
                .filter(type -> type.contractValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported event type: " + value));
    }
}
