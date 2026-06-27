package br.com.byop.aegis.asset.domain;

import java.util.Arrays;

/**
 * Categoria de um {@link Asset}, derivada do mime type no momento do upload.
 *
 * <p>Define a subpasta usada no storage local e o limite de tamanho aplicado
 * (ver {@code aegis.assets.limits} em {@code application.yml}).
 */
public enum AssetCategory {
    IMAGE("image"),
    PDF("pdf"),
    AUDIO("audio"),
    VIDEO("video"),
    DOCUMENT("document");

    private final String contractValue;

    AssetCategory(String contractValue) {
        this.contractValue = contractValue;
    }

    public String contractValue() {
        return contractValue;
    }

    public static AssetCategory fromContractValue(String value) {
        return Arrays.stream(values())
                .filter(category -> category.contractValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported asset category: " + value));
    }
}
