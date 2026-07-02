package br.com.byop.aegis.pages.domain;

import java.util.Arrays;

/**
 * Catalogo fechado de visibilidade de {@link Event} — mesmos valores exatos de
 * {@code domains/pages/contracts/events.ts} no frontend (Sprint 23), nunca um
 * catalogo novo inventado no backend.
 */
public enum EventVisibility {
    PUBLIC("public"),
    PUBLIC_SUMMARY("public-summary"),
    PRIVATE("private");

    private final String contractValue;

    EventVisibility(String contractValue) {
        this.contractValue = contractValue;
    }

    public String contractValue() {
        return contractValue;
    }

    public static EventVisibility fromContractValue(String value) {
        return Arrays.stream(values())
                .filter(visibility -> visibility.contractValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported event visibility: " + value));
    }
}
