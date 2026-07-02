package br.com.byop.aegis.pages.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "page_sections")
public class PageSection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "page_id", nullable = false)
    private UUID pageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private BlockType type;

    @Column(length = 80)
    private String variant;

    @Column(name = "section_order", nullable = false)
    private int order;

    @Column(name = "content_json", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String contentJson;

    @Column(name = "settings_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String settingsJson;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected PageSection() {
    }

    public PageSection(UUID pageId, BlockType type, String variant, int order, String contentJson, String settingsJson) {
        this.pageId = Objects.requireNonNull(pageId, "pageId is required");
        this.type = Objects.requireNonNull(type, "type is required");
        this.variant = variant;
        this.order = order;
        this.contentJson = Objects.requireNonNull(contentJson, "contentJson is required");
        this.settingsJson = settingsJson;
    }

    public void applyEdit(Edit edit) {
        this.type = Objects.requireNonNull(edit.type(), "type is required");
        this.variant = edit.variant();
        this.order = edit.order();
        this.contentJson = Objects.requireNonNull(edit.contentJson(), "contentJson is required");
        this.settingsJson = edit.settingsJson();
    }

    public void reorderTo(int order) {
        this.order = order;
    }

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getId() {
        return id;
    }

    public UUID getPageId() {
        return pageId;
    }

    public BlockType getType() {
        return type;
    }

    public String getVariant() {
        return variant;
    }

    public int getOrder() {
        return order;
    }

    public String getContentJson() {
        return contentJson;
    }

    public String getSettingsJson() {
        return settingsJson;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Agrupa os campos editaveis de {@link PageSection} num unico parametro de
     * {@link #applyEdit(Edit)} — evita um metodo com lista longa de parametros
     * posicionais (java:S107).
     */
    public record Edit(
            BlockType type,
            String variant,
            int order,
            String contentJson,
            String settingsJson
    ) {
    }
}
