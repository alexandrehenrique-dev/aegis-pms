package br.com.byop.aegis.pages.domain;

import java.util.Arrays;

/**
 * Catalogo fechado de estados de publicacao de uma {@link Page}.
 */
public enum PageStatus {
    DRAFT("draft"),
    REVIEW("review"),
    PUBLISHED("published"),
    ARCHIVED("archived");

    private final String contractValue;

    PageStatus(String contractValue) {
        this.contractValue = contractValue;
    }

    public String contractValue() {
        return contractValue;
    }

    public static PageStatus fromContractValue(String value) {
        return Arrays.stream(values())
                .filter(status -> status.contractValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported page status: " + value));
    }
}
