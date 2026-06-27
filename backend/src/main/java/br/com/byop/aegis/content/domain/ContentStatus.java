package br.com.byop.aegis.content.domain;

import java.util.Arrays;

/**
 * Status do workflow editorial de um {@link Content}.
 *
 * <p>O dominio Java preserva o padrao de enums em ingles usado pelo backend.
 * Na fronteira de contrato, cada valor corresponde ao literal exato esperado
 * pelo frontend (ex.: {@link #IN_REVIEW} corresponde a {@code "In Review"}).
 */
public enum ContentStatus {
    DRAFT("Draft"),
    IN_REVIEW("In Review"),
    PUBLISHED("Published"),
    ARCHIVED("Archived");

    private final String contractValue;

    ContentStatus(String contractValue) {
        this.contractValue = contractValue;
    }

    public String contractValue() {
        return contractValue;
    }

    public static ContentStatus fromContractValue(String value) {
        return Arrays.stream(values())
                .filter(status -> status.contractValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported content status: " + value));
    }
}
