package br.com.byop.aegis.identity.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthRefreshRequest(

        @NotBlank
        String refreshToken

) {
}
