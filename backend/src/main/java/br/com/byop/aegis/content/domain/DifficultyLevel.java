package br.com.byop.aegis.content.domain;

import java.util.Arrays;

/**
 * Nivel de dificuldade opcional de um {@link Content} (caso WikiDev — filtragem/ordenacao
 * de artigos de uma Knowledge Base).
 *
 * <p>Cada valor corresponde ao literal exato (minusculo) esperado pelo contrato publico.
 */
public enum DifficultyLevel {
    BEGINNER("beginner"),
    INTERMEDIATE("intermediate"),
    ADVANCED("advanced");

    private final String contractValue;

    DifficultyLevel(String contractValue) {
        this.contractValue = contractValue;
    }

    public String contractValue() {
        return contractValue;
    }

    public static DifficultyLevel fromContractValue(String value) {
        return Arrays.stream(values())
                .filter(level -> level.contractValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported difficulty level: " + value));
    }
}
