package br.com.byop.aegis.tenant.contract;

import jakarta.validation.constraints.NotBlank;

public record UpdateTenantRequest(
        @NotBlank String name,
        @NotBlank String plan,
        @NotBlank String status
) {
}
