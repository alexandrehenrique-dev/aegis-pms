package br.com.byop.aegis.identity.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AuthPasswordActionRequest(
        @NotNull UUID token,
        @NotBlank String password
) {
}
