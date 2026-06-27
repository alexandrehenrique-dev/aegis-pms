package br.com.byop.aegis.product.user.contract;

import jakarta.validation.constraints.NotBlank;

public record UpdateTenantUserRequest(
        @NotBlank String role,
        @NotBlank String allowedProducts,
        @NotBlank String status
) {
}
