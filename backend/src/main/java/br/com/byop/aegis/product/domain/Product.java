package br.com.byop.aegis.product.domain;

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
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "key", nullable = false, length = 120)
    private String key;

    @Column(nullable = false, length = 160)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    private ProductTypeKey type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ProductStatus status;

    @Column(name = "default_locale", nullable = false, length = 16)
    private String defaultLocale;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_storage_strategy", nullable = false, length = 20)
    private AssetStorageStrategy assetStorageStrategy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Product() {
    }

    public Product(UUID tenantId, String key, String name, ProductTypeKey type, String defaultLocale) {
        this(tenantId, key, name, type, defaultLocale, AssetStorageStrategy.LOCAL);
    }

    public Product(UUID tenantId, String key, String name, ProductTypeKey type, String defaultLocale,
                   AssetStorageStrategy assetStorageStrategy) {
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId is required");
        this.key = Objects.requireNonNull(key, "key is required");
        this.name = Objects.requireNonNull(name, "name is required");
        this.type = Objects.requireNonNull(type, "type is required");
        this.defaultLocale = Objects.requireNonNull(defaultLocale, "defaultLocale is required");
        this.assetStorageStrategy = Objects.requireNonNull(assetStorageStrategy, "assetStorageStrategy is required");
        this.status = ProductStatus.ACTIVE;
    }

    public void rename(String name) {
        this.name = Objects.requireNonNull(name, "name is required");
    }

    public void suspend() {
        this.status = ProductStatus.SUSPENDED;
    }

    public void activate() {
        this.status = ProductStatus.ACTIVE;
    }

    public void archive() {
        this.status = ProductStatus.ARCHIVED;
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

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public ProductTypeKey getType() {
        return type;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public AssetStorageStrategy getAssetStorageStrategy() {
        return assetStorageStrategy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

}
