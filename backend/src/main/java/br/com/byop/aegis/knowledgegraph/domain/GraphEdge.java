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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "graph_edges")
public class GraphEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "source_node_id", nullable = false)
    private UUID sourceNodeId;

    @Column(name = "target_node_id", nullable = false)
    private UUID targetNodeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "edge_type", nullable = false, length = 80)
    private GraphEdgeType edgeType;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal weight;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json", nullable = false, columnDefinition = "jsonb")
    private String metadataJson;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected GraphEdge() {
    }

    public GraphEdge(UUID tenantId, UUID productId, UUID sourceNodeId, UUID targetNodeId, GraphEdgeType edgeType) {
        this(tenantId, productId, sourceNodeId, targetNodeId, edgeType, BigDecimal.ONE, "{}");
    }

    public GraphEdge(UUID tenantId, UUID productId, UUID sourceNodeId, UUID targetNodeId, GraphEdgeType edgeType,
                     BigDecimal weight, String metadataJson) {
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId is required");
        this.productId = Objects.requireNonNull(productId, "productId is required");
        this.sourceNodeId = Objects.requireNonNull(sourceNodeId, "sourceNodeId is required");
        this.targetNodeId = Objects.requireNonNull(targetNodeId, "targetNodeId is required");
        this.edgeType = Objects.requireNonNull(edgeType, "edgeType is required");
        this.weight = Objects.requireNonNull(weight, "weight is required");
        this.metadataJson = Objects.requireNonNull(metadataJson, "metadataJson is required");
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

    public UUID getSourceNodeId() {
        return sourceNodeId;
    }

    public UUID getTargetNodeId() {
        return targetNodeId;
    }

    public GraphEdgeType getEdgeType() {
        return edgeType;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
