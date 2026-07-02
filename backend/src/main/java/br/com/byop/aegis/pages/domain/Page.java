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
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "pages", uniqueConstraints = @UniqueConstraint(
        name = "uq_pages_product_slug",
        columnNames = {"product_id", "slug"}
))
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false, length = 160)
    private String slug;

    @Column(nullable = false, length = 240)
    private String title;

    @Column(nullable = false, length = 16)
    private String locale;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PageStatus status;

    @Column(nullable = false)
    private int version;

    @Column(name = "seo_title", length = 240)
    private String seoTitle;

    @Column(name = "seo_description", length = 480)
    private String seoDescription;

    @Column(name = "seo_canonical", length = 480)
    private String seoCanonical;

    @Column(name = "seo_og_image_asset_id")
    private UUID seoOgImageAssetId;

    @Column(name = "seo_no_index", nullable = false)
    private boolean seoNoIndex;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Page() {
    }

    public Page(UUID tenantId, UUID productId, String slug, String title, String locale) {
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId is required");
        this.productId = Objects.requireNonNull(productId, "productId is required");
        this.slug = Objects.requireNonNull(slug, "slug is required");
        this.title = Objects.requireNonNull(title, "title is required");
        this.locale = Objects.requireNonNull(locale, "locale is required");
        this.status = PageStatus.DRAFT;
        this.version = 1;
        this.seoNoIndex = false;
    }

    public void applyEdit(Edit edit) {
        this.slug = Objects.requireNonNull(edit.slug(), "slug is required");
        this.title = Objects.requireNonNull(edit.title(), "title is required");
        this.locale = Objects.requireNonNull(edit.locale(), "locale is required");
        this.status = Objects.requireNonNull(edit.status(), "status is required");
        this.seoTitle = edit.seoTitle();
        this.seoDescription = edit.seoDescription();
        this.seoCanonical = edit.seoCanonical();
        this.seoOgImageAssetId = edit.seoOgImageAssetId();
        this.seoNoIndex = edit.seoNoIndex();
        this.version = this.version + 1;
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

    public String getSlug() {
        return slug;
    }

    public String getTitle() {
        return title;
    }

    public String getLocale() {
        return locale;
    }

    public PageStatus getStatus() {
        return status;
    }

    public int getVersion() {
        return version;
    }

    public String getSeoTitle() {
        return seoTitle;
    }

    public String getSeoDescription() {
        return seoDescription;
    }

    public String getSeoCanonical() {
        return seoCanonical;
    }

    public UUID getSeoOgImageAssetId() {
        return seoOgImageAssetId;
    }

    public boolean isSeoNoIndex() {
        return seoNoIndex;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Agrupa os campos editaveis de {@link Page} num unico parametro de
     * {@link #applyEdit(Edit)} — evita um metodo com lista longa de parametros
     * posicionais (java:S107).
     */
    public record Edit(
            String slug,
            String title,
            String locale,
            PageStatus status,
            String seoTitle,
            String seoDescription,
            String seoCanonical,
            UUID seoOgImageAssetId,
            boolean seoNoIndex
    ) {
    }
}
