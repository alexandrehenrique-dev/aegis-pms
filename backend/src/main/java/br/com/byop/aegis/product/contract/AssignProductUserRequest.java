package br.com.byop.aegis.product.contract;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignProductUserRequest(
        @NotNull UUID productId,
        @NotNull UUID tenantId,
        String userId,
        @Email String inviteEmail,
        @NotBlank String role,
        String status
) {
}
