package br.com.byop.aegis.product.user.dto;

public record TenantUserSummary(
        String userId,
        String name,
        String email,
        String role,
        String products,
        String status,
        String lastAccess,
        String inviteStatus
) {
}
