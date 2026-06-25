package br.com.byop.aegis.core.tenant;

/**
 * Status do vinculo de um usuario autenticado a um tenant.
 */
public enum TenantMembershipStatus {
    ACTIVE,
    SUSPENDED,
    REVOKED
}
