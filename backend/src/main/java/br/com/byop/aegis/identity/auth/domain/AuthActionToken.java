package br.com.byop.aegis.identity.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "auth_action_tokens")
public class AuthActionToken {

    @Id
    private UUID id;

    @Column(name = "keycloak_id", nullable = false)
    private String keycloakId;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "user_name")
    private String userName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthActionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthActionStatus status;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "tenant_name")
    private String tenantName;

    @Column(name = "product_names", columnDefinition = "TEXT")
    private String productNames;

    @Column(length = 50)
    private String role;

    @Column(name = "inviter_name")
    private String inviterName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    protected AuthActionToken() {
    }

    public AuthActionToken(String keycloakId, String userEmail, String userName, AuthActionType type,
                           Instant createdAt, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.keycloakId = Objects.requireNonNull(keycloakId, "keycloakId is required");
        this.userEmail = Objects.requireNonNull(userEmail, "userEmail is required");
        this.userName = userName;
        this.type = Objects.requireNonNull(type, "type is required");
        this.status = AuthActionStatus.PENDING;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt is required");
    }

    @PrePersist
    void prePersist() {
        createdAt = createdAt == null ? Instant.now() : createdAt;
        status = status == null ? AuthActionStatus.PENDING : status;
    }

    public void addInviteContext(UUID tenantId, String tenantName, String productNames, String role, String inviterName) {
        this.tenantId = tenantId;
        this.tenantName = tenantName;
        this.productNames = productNames;
        this.role = role;
        this.inviterName = inviterName;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public void markUsed(Instant usedAt) {
        this.status = AuthActionStatus.USED;
        this.usedAt = Objects.requireNonNull(usedAt, "usedAt is required");
    }

    public void markExpired() {
        this.status = AuthActionStatus.EXPIRED;
    }

    public UUID getId() {
        return id;
    }

    public String getKeycloakId() {
        return keycloakId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getUserName() {
        return userName;
    }

    public AuthActionType getType() {
        return type;
    }

    public AuthActionStatus getStatus() {
        return status;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getTenantName() {
        return tenantName;
    }

    public String getProductNames() {
        return productNames;
    }

    public String getRole() {
        return role;
    }

    public String getInviterName() {
        return inviterName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }
}
