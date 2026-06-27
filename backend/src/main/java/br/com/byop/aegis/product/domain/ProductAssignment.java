package br.com.byop.aegis.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "product_assignments")
public class ProductAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "product_id", insertable = false, updatable = false)
    private UUID productId;

    @Column(name = "user_subject", nullable = false, length = 160)
    private String userSubject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    private ProductAssignmentRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ProductAssignmentStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ProductAssignment() {
    }

    public ProductAssignment(Product product, String userSubject, ProductAssignmentRole role) {
        this.product = Objects.requireNonNull(product, "product is required");
        this.tenantId = Objects.requireNonNull(product.getTenantId(), "product tenantId is required");
        this.userSubject = Objects.requireNonNull(userSubject, "userSubject is required");
        this.role = Objects.requireNonNull(role, "role is required");
        this.status = ProductAssignmentStatus.ASSIGNED;
    }

    public void changeRole(ProductAssignmentRole role) {
        this.role = Objects.requireNonNull(role, "role is required");
    }

    public void revoke() {
        this.status = ProductAssignmentStatus.INVITED;
    }

    public void remove() {
        this.status = ProductAssignmentStatus.REMOVED;
    }

    public void assign() {
        this.status = ProductAssignmentStatus.ASSIGNED;
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

    public Product getProduct() {
        return product;
    }

    public UUID getProductId() {
        if (productId != null) {
            return productId;
        }
        return product == null ? null : product.getId();
    }

    public String getUserSubject() {
        return userSubject;
    }

    public ProductAssignmentRole getRole() {
        return role;
    }

    public ProductAssignmentStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

}
