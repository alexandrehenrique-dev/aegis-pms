package br.com.byop.aegis.product.export.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "export_tokens")
public class ExportToken {

    private static final long DEFAULT_EXPIRATION_DAYS = 7;

    @Id
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_key", nullable = false, length = 120)
    private String productKey;

    @Column(name = "zip_path", nullable = false, length = 1024)
    private String zipPath;

    @Column(name = "storage_provider", nullable = false, length = 20)
    private String storageProvider;

    @Column(name = "recipient_email", nullable = false, length = 255)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExportTokenStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "downloaded_at")
    private Instant downloadedAt;

    protected ExportToken() {
    }

    public ExportToken(UUID productId, String productKey, String zipPath, String storageProvider, String recipientEmail) {
        this.id = UUID.randomUUID();
        this.productId = Objects.requireNonNull(productId, "productId is required");
        this.productKey = Objects.requireNonNull(productKey, "productKey is required");
        this.zipPath = Objects.requireNonNull(zipPath, "zipPath is required");
        this.storageProvider = Objects.requireNonNull(storageProvider, "storageProvider is required");
        this.recipientEmail = Objects.requireNonNull(recipientEmail, "recipientEmail is required");
        this.status = ExportTokenStatus.PENDING;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = createdAt == null ? now : createdAt;
        expiresAt = expiresAt == null ? createdAt.plus(DEFAULT_EXPIRATION_DAYS, ChronoUnit.DAYS) : expiresAt;
    }

    public void markAvailable(String zipPath) {
        this.zipPath = Objects.requireNonNull(zipPath, "zipPath is required");
        this.status = ExportTokenStatus.AVAILABLE;
    }

    public void markExpired() {
        this.status = ExportTokenStatus.EXPIRED;
    }

    public void markDownloaded(Instant downloadedAt) {
        if (this.downloadedAt == null) {
            this.downloadedAt = Objects.requireNonNull(downloadedAt, "downloadedAt is required");
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getProductKey() {
        return productKey;
    }

    public String getZipPath() {
        return zipPath;
    }

    public String getStorageProvider() {
        return storageProvider;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public ExportTokenStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getDownloadedAt() {
        return downloadedAt;
    }
}
