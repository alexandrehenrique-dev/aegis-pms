package br.com.byop.aegis.identity.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AuthPasswordResetRequest(@Email @NotBlank String email) {
}
