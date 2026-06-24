package br.com.byop.aegis.identity.auth.dto;

import br.com.byop.aegis.identity.auth.client.KeycloakTokenResponse;

public final class AuthTokenResponseMapper {

    private AuthTokenResponseMapper() {
    }

    public static AuthTokenResponse from(
            KeycloakTokenResponse source
    ) {

        return new AuthTokenResponse(
                source.accessToken(),
                source.refreshToken(),
                source.expiresIn(),
                source.tokenType()
        );
    }
}
