package br.com.byop.aegis.product.domain;

import java.util.Arrays;

/**
 * Status de uma atribuicao de acesso a produto.
 *
 * <p>O dominio Java preserva o padrao de enums em ingles usado pelo backend.
 * Na fronteira de contrato, {@link #ASSIGNED} corresponde a {@code atribuido}
 * e {@link #INVITED} corresponde a {@code convidado}.
 */
public enum ProductAssignmentStatus {
    ASSIGNED("atribuido"),
    INVITED("convidado"),
    REMOVED("removido");

    private final String contractValue;

    ProductAssignmentStatus(String contractValue) {
        this.contractValue = contractValue;
    }

    public String contractValue() {
        return contractValue;
    }

    public static ProductAssignmentStatus fromContractValue(String value) {
        return Arrays.stream(values())
                .filter(status -> status.contractValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported product assignment status: " + value));
    }
}
