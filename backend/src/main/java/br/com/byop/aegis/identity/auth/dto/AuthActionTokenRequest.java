package br.com.byop.aegis.identity.auth.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AuthActionTokenRequest(@NotNull UUID token) {
}
