package br.com.byop.aegis.product.user.contract;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public record InviteTenantUserRequest(
        @NotBlank String name,
        @Email @NotBlank String email,
        @NotBlank String role,
        @NotBlank String allowedProducts,
        List<UUID> allowedProductIds,
        String message
) {
    public InviteTenantUserRequest(String name, String email, String role, String allowedProducts, String message) {
        this(name, email, role, allowedProducts, List.of(), message);
    }
}
