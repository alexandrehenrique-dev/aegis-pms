package br.com.byop.aegis.identity.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthLogoutRequest(

        @NotBlank
        String refreshToken

) {
}
