package br.com.byop.aegis.product.user.contract;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InviteTenantUserRequest(
        @NotBlank String name,
        @Email @NotBlank String email,
        @NotBlank String role,
        @NotBlank String allowedProducts,
        String message
) {
}
