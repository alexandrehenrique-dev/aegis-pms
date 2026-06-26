package br.com.byop.aegis.tenant.contract;

import jakarta.validation.constraints.NotBlank;

public record DeleteTenantRequest(
        @NotBlank String confirmationText
) {
}
