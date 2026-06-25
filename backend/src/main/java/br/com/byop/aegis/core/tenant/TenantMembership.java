package br.com.byop.aegis.core.tenant;

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
@Table(name = "tenant_memberships")
public class TenantMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "user_subject", nullable = false, length = 160)
    private String userSubject;

    @Column(nullable = false, length = 80)
    private String role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TenantMembershipStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected TenantMembership() {
    }

    public TenantMembership(Tenant tenant, String userSubject, String role) {
        this.tenant = Objects.requireNonNull(tenant, "tenant is required");
        this.userSubject = Objects.requireNonNull(userSubject, "userSubject is required");
        this.role = Objects.requireNonNull(role, "role is required");
        this.status = TenantMembershipStatus.ACTIVE;
    }

    public void suspend() {
        this.status = TenantMembershipStatus.SUSPENDED;
    }

    public void revoke() {
        this.status = TenantMembershipStatus.REVOKED;
    }

    public void activate() {
        this.status = TenantMembershipStatus.ACTIVE;
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

    public Tenant getTenant() {
        return tenant;
    }

    public String getUserSubject() {
        return userSubject;
    }

    public String getRole() {
        return role;
    }

    public TenantMembershipStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

}
