package br.com.byop.aegis.identity.auth.dto;

public record AuthTokenResponse(
        String accessToken,
        String refreshToken,
        Long expiresIn,
        String tokenType
) {
}
