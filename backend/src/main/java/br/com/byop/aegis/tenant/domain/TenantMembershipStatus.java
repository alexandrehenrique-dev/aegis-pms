package br.com.byop.aegis.tenant.domain;

/**
 * Status do vinculo de um usuario autenticado a um tenant.
 */
public enum TenantMembershipStatus {
    ACTIVE,
    INVITED,
    SUSPENDED,
    REVOKED,
    REMOVED
}
