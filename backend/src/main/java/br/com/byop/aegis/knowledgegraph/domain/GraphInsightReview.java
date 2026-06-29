package br.com.byop.aegis.knowledgegraph.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "graph_insight_reviews")
public class GraphInsightReview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "text_hash", nullable = false, length = 64)
    private String textHash;

    @Column(nullable = false, length = 1000)
    private String text;

    @Column(nullable = false)
    private boolean reviewed;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected GraphInsightReview() {
    }

    public GraphInsightReview(UUID tenantId, UUID productId, String textHash, String text) {
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId is required");
        this.productId = Objects.requireNonNull(productId, "productId is required");
        this.textHash = Objects.requireNonNull(textHash, "textHash is required");
        this.text = Objects.requireNonNull(text, "text is required");
        this.reviewed = true;
    }

    @PrePersist
    void prePersist() {
        createdAt = OffsetDateTime.now(ZoneOffset.UTC);
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

    public String getTextHash() {
        return textHash;
    }

    public String getText() {
        return text;
    }

    public boolean isReviewed() {
        return reviewed;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
