package br.com.byop.aegis.pages.domain;

import java.util.Arrays;

/**
 * Catalogo fechado de tipos de bloco de uma {@link PageSection}. Um {@code type}
 * fora deste catalogo e sempre rejeitado com 400 na criacao/edicao de uma secao —
 * nunca aceito como string livre vinda do cliente.
 */
public enum BlockType {
    HERO("hero"),
    TEXT("text"),
    RICH_TEXT("rich-text"),
    TWO_COLUMN("two-column"),
    IMAGE("image"),
    IMAGE_TEXT("image-text"),
    FEATURE_GRID("feature-grid"),
    CARD_LIST("card-list"),
    GALLERY("gallery"),
    TIMELINE("timeline"),
    EVENT_LIST("event-list"),
    CTA_SECTION("cta-section"),
    FAQ("faq"),
    CONTACT("contact"),
    FORM("form"),
    DOWNLOAD("download"),
    AUDIO("audio"),
    SOCIAL_LINKS("social-links"),
    VIDEO("video"),
    VIDEO_GALLERY("video-gallery");

    private final String contractValue;

    BlockType(String contractValue) {
        this.contractValue = contractValue;
    }

    public String contractValue() {
        return contractValue;
    }

    public static BlockType fromContractValue(String value) {
        return Arrays.stream(values())
                .filter(type -> type.contractValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported block type: " + value));
    }
}
