package br.com.byop.aegis.feedback.domain;

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
@Table(name = "feedback")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "public_id", nullable = false, unique = true, length = 16)
    private String publicId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "created_by_subject", nullable = false, length = 160)
    private String createdBySubject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private FeedbackCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedbackPriority priority;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "screen_name", length = 240)
    private String screenName;

    @Column(name = "attachment_asset_id")
    private UUID attachmentAssetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedbackStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Feedback() {
    }

    public Feedback(Creation creation) {
        this.publicId = Objects.requireNonNull(creation.publicId(), "publicId is required");
        this.tenantId = Objects.requireNonNull(creation.tenantId(), "tenantId is required");
        this.productId = creation.productId();
        this.createdBySubject = Objects.requireNonNull(creation.createdBySubject(), "createdBySubject is required");
        this.category = Objects.requireNonNull(creation.category(), "category is required");
        this.priority = Objects.requireNonNull(creation.priority(), "priority is required");
        this.description = Objects.requireNonNull(creation.description(), "description is required");
        this.screenName = creation.screenName();
        this.attachmentAssetId = creation.attachmentAssetId();
        this.status = FeedbackStatus.OPEN;
    }

    public void changeStatus(FeedbackStatus nextStatus) {
        this.status = Objects.requireNonNull(nextStatus, "nextStatus is required");
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

    public String getPublicId() {
        return publicId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getCreatedBySubject() {
        return createdBySubject;
    }

    public FeedbackCategory getCategory() {
        return category;
    }

    public FeedbackPriority getPriority() {
        return priority;
    }

    public String getDescription() {
        return description;
    }

    public String getScreenName() {
        return screenName;
    }

    public UUID getAttachmentAssetId() {
        return attachmentAssetId;
    }

    public FeedbackStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Agrupa os dados obrigatorios de criacao de {@link Feedback}, evitando
     * construtor com lista longa de parametros posicionais.
     */
    public record Creation(
            String publicId,
            UUID tenantId,
            UUID productId,
            String createdBySubject,
            FeedbackCategory category,
            FeedbackPriority priority,
            String description,
            String screenName,
            UUID attachmentAssetId
    ) {
    }
}
