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

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

/**
 * Evento de agenda de um produto — entidade propria (Secao A.1 da Sprint 23),
 * nunca resolvida como um "content type" generico. {@code datetime} e um unico
 * instante local (data e hora combinadas, ex. {@code 2026-07-12T16:00}), nunca
 * dois campos separados de data/hora.
 */
@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false, length = 240)
    private String title;

    @Column(nullable = false)
    private LocalDateTime datetime;

    @Column(nullable = false, length = 240)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventAccessType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventVisibility visibility;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "image_asset_id")
    private UUID imageAssetId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Event() {
    }

    public Event(UUID tenantId, UUID productId, String title, LocalDateTime datetime, String location,
                EventAccessType type, EventVisibility visibility) {
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId is required");
        this.productId = Objects.requireNonNull(productId, "productId is required");
        this.title = Objects.requireNonNull(title, "title is required");
        this.datetime = Objects.requireNonNull(datetime, "datetime is required");
        this.location = Objects.requireNonNull(location, "location is required");
        this.type = Objects.requireNonNull(type, "type is required");
        this.visibility = Objects.requireNonNull(visibility, "visibility is required");
    }

    public void applyEdit(Edit edit) {
        this.title = Objects.requireNonNull(edit.title(), "title is required");
        this.datetime = Objects.requireNonNull(edit.datetime(), "datetime is required");
        this.location = Objects.requireNonNull(edit.location(), "location is required");
        this.type = Objects.requireNonNull(edit.type(), "type is required");
        this.visibility = Objects.requireNonNull(edit.visibility(), "visibility is required");
        this.description = edit.description();
        this.imageAssetId = edit.imageAssetId();
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

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getTitle() {
        return title;
    }

    public LocalDateTime getDatetime() {
        return datetime;
    }

    public String getLocation() {
        return location;
    }

    public EventAccessType getType() {
        return type;
    }

    public EventVisibility getVisibility() {
        return visibility;
    }

    public String getDescription() {
        return description;
    }

    public UUID getImageAssetId() {
        return imageAssetId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Agrupa os campos editaveis de {@link Event} num unico parametro de
     * {@link #applyEdit(Edit)} — evita um metodo com lista longa de parametros
     * posicionais (java:S107).
     */
    public record Edit(
            String title,
            LocalDateTime datetime,
            String location,
            EventAccessType type,
            EventVisibility visibility,
            String description,
            UUID imageAssetId
    ) {
    }
}
