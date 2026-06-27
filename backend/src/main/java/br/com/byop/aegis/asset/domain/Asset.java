package br.com.byop.aegis.asset.domain;

import br.com.byop.aegis.product.api.AssetStorageStrategy;
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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "assets")
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "friendly_name", length = 255)
    private String friendlyName;

    @Column(name = "alt_text", length = 480)
    private String altText;

    @Column(length = 480)
    private String caption;

    @Column(length = 255)
    private String credit;

    @Column(name = "mime_type", nullable = false, length = 120)
    private String mimeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssetCategory category;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssetStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_provider", nullable = false, length = 20)
    private AssetStorageStrategy storageProvider;

    @Column(name = "storage_key", nullable = false, length = 1024)
    private String storageKey;

    @Column(name = "uploaded_by_subject", nullable = false, length = 160)
    private String uploadedBySubject;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Asset() {
    }

    public Asset(Creation creation) {
        this.tenantId = Objects.requireNonNull(creation.tenantId(), "tenantId is required");
        this.productId = Objects.requireNonNull(creation.productId(), "productId is required");
        this.name = Objects.requireNonNull(creation.name(), "name is required");
        this.mimeType = Objects.requireNonNull(creation.mimeType(), "mimeType is required");
        this.category = Objects.requireNonNull(creation.category(), "category is required");
        this.sizeBytes = creation.sizeBytes();
        this.storageProvider = Objects.requireNonNull(creation.storageProvider(), "storageProvider is required");
        this.storageKey = Objects.requireNonNull(creation.storageKey(), "storageKey is required");
        this.uploadedBySubject = Objects.requireNonNull(creation.uploadedBySubject(), "uploadedBySubject is required");
        this.friendlyName = creation.name();
        this.status = AssetStatus.ACTIVE;
    }

    public void applyMetadata(Metadata metadata) {
        this.friendlyName = metadata.friendlyName() == null ? this.friendlyName : metadata.friendlyName();
        this.altText = metadata.altText();
        this.caption = metadata.caption();
        this.credit = metadata.credit();
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

    public String getName() {
        return name;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public String getAltText() {
        return altText;
    }

    public String getCaption() {
        return caption;
    }

    public String getCredit() {
        return credit;
    }

    public String getMimeType() {
        return mimeType;
    }

    public AssetCategory getCategory() {
        return category;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public AssetStatus getStatus() {
        return status;
    }

    public AssetStorageStrategy getStorageProvider() {
        return storageProvider;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getUploadedBySubject() {
        return uploadedBySubject;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Agrupa os campos editaveis de metadados num unico parametro de
     * {@link #applyMetadata(Metadata)} — evita um metodo com lista longa de
     * parametros posicionais (java:S107).
     */
    public record Metadata(
            String friendlyName,
            String altText,
            String caption,
            String credit
    ) {
    }

    /**
     * Agrupa os campos de criacao num unico parametro do construtor — evita
     * um construtor com lista longa de parametros posicionais (java:S107),
     * mesmo padrao de {@code Content.Edit} (Sprint 11).
     */
    public record Creation(
            UUID tenantId,
            UUID productId,
            String name,
            String mimeType,
            AssetCategory category,
            long sizeBytes,
            AssetStorageStrategy storageProvider,
            String storageKey,
            String uploadedBySubject
    ) {
    }
}
