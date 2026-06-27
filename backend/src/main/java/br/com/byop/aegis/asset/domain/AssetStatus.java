package br.com.byop.aegis.asset.domain;

/**
 * Status de ciclo de vida de um {@link Asset}.
 *
 * <p>Esta sprint nao expoe nenhuma transicao de status (sem endpoint de
 * arquivamento) — qualquer {@link Asset} criado permanece {@link #ACTIVE} ate ser
 * excluido. Reservado para uma sprint futura de arquivamento.
 */
public enum AssetStatus {
    ACTIVE("ativo");

    private final String contractValue;

    AssetStatus(String contractValue) {
        this.contractValue = contractValue;
    }

    public String contractValue() {
        return contractValue;
    }
}
