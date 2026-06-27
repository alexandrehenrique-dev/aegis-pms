package br.com.byop.aegis.audit.domain;

import java.util.Arrays;

/**
 * Nivel de risco de um {@link AuditEvent}, calculado pela acao auditada.
 *
 * <p>O dominio Java preserva o padrao de enums em ingles usado pelo backend.
 * Na fronteira de contrato, {@link #BAIXO}/{@link #MEDIO}/{@link #ALTO}
 * correspondem literalmente a {@code baixo}/{@code medio}/{@code alto}.
 */
public enum AuditRisk {
    BAIXO("baixo"),
    MEDIO("medio"),
    ALTO("alto");

    private final String contractValue;

    AuditRisk(String contractValue) {
        this.contractValue = contractValue;
    }

    public String contractValue() {
        return contractValue;
    }

    public static AuditRisk fromContractValue(String value) {
        return Arrays.stream(values())
                .filter(risk -> risk.contractValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported audit risk: " + value));
    }
}
