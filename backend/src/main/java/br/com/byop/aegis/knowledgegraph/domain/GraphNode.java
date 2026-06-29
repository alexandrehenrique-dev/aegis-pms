package br.com.byop.aegis.knowledgegraph.domain;

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
@Table(name = "graph_nodes")
public class GraphNode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "node_type", nullable = false, length = 80)
    private GraphNodeType nodeType;

    @Column(name = "ref_type", nullable = false, length = 120)
    private String refType;

    @Column(name = "ref_id", nullable = false, length = 160)
    private String refId;

    @Column(nullable = false, length = 240)
    private String label;

    @Column(nullable = false, length = 180)
    private String slug;

    @Column(nullable = false)
    private double x;

    @Column(nullable = false)
    private double y;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json", nullable = false, columnDefinition = "jsonb")
    private String metadataJson;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected GraphNode() {
    }

    public GraphNode(Creation creation) {
        this.tenantId = Objects.requireNonNull(creation.tenantId(), "tenantId is required");
        this.productId = Objects.requireNonNull(creation.productId(), "productId is required");
        this.nodeType = Objects.requireNonNull(creation.nodeType(), "nodeType is required");
        this.refType = Objects.requireNonNull(creation.refType(), "refType is required");
        this.refId = Objects.requireNonNull(creation.refId(), "refId is required");
        this.label = Objects.requireNonNull(creation.label(), "label is required");
        this.slug = Objects.requireNonNull(creation.slug(), "slug is required");
        this.metadataJson = Objects.requireNonNull(creation.metadataJson(), "metadataJson is required");
        this.x = creation.x();
        this.y = creation.y();
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

    public GraphNodeType getNodeType() {
        return nodeType;
    }

    public String getRefType() {
        return refType;
    }

    public String getRefId() {
        return refId;
    }

    public String getLabel() {
        return label;
    }

    public String getSlug() {
        return slug;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void reposition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void replaceMetadataJson(String metadataJson) {
        this.metadataJson = Objects.requireNonNull(metadataJson, "metadataJson is required");
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public record Creation(
            UUID tenantId,
            UUID productId,
            GraphNodeType nodeType,
            String refType,
            String refId,
            String label,
            String slug,
            String metadataJson,
            double x,
            double y
    ) {
        public Creation(UUID tenantId, UUID productId, GraphNodeType nodeType, String refType, String refId,
                        String label, String slug, String metadataJson) {
            this(tenantId, productId, nodeType, refType, refId, label, slug, metadataJson, 0.0, 0.0);
        }
    }
}
