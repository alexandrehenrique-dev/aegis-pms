package br.com.byop.aegis.settings.contract;

import jakarta.validation.constraints.NotBlank;

/**
 * Corpo de {@code POST /tenants/{tenantId}/permission-matrix/preview}.
 * Simulacao somente leitura — devolve a linha da matriz do papel informado,
 * sem persistir nada.
 */
public record PermissionMatrixPreviewRequest(
        @NotBlank String role
) {
}
