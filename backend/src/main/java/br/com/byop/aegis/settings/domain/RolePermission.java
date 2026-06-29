package br.com.byop.aegis.settings.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.Objects;
import java.util.UUID;

/**
 * Permissao de um papel canonico (ADR-0014) sobre uma chave de permissao do
 * tenant, persistida por {@code br.com.byop.aegis.settings.service.SettingsService}.
 * A chave de permissao reaproveita exatamente as strings ja usadas pelo
 * frontend em {@code core/permissions/roles.ts} (navegacao/rotas), nunca uma
 * nomenclatura nova.
 */
@Entity
@Table(name = "role_permissions", uniqueConstraints = @UniqueConstraint(
        name = "uq_role_permissions_tenant_role_key",
        columnNames = {"tenant_id", "role", "permission_key"}
))
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 40)
    private String role;

    @Column(name = "permission_key", nullable = false, length = 160)
    private String permissionKey;

    @Column(nullable = false)
    private boolean allowed;

    protected RolePermission() {
    }

    public RolePermission(UUID tenantId, String role, String permissionKey, boolean allowed) {
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId is required");
        this.role = Objects.requireNonNull(role, "role is required");
        this.permissionKey = Objects.requireNonNull(permissionKey, "permissionKey is required");
        this.allowed = allowed;
    }

    public void changeAllowed(boolean allowed) {
        this.allowed = allowed;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getRole() {
        return role;
    }

    public String getPermissionKey() {
        return permissionKey;
    }

    public boolean isAllowed() {
        return allowed;
    }
}
