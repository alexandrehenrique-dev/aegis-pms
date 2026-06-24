package br.com.byop.aegis.identity.auth.dto;

import br.com.byop.aegis.identity.auth.client.KeycloakTokenResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthTokenResponseMapperTest {

    @Test
    void shouldMapKeycloakTokenResponseToAuthTokenResponse() {

        KeycloakTokenResponse source = new KeycloakTokenResponse(
                "access-token",
                "refresh-token",
                300L,
                "Bearer"
        );

        AuthTokenResponse response =
                AuthTokenResponseMapper.from(source);

        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals(300L, response.expiresIn());
        assertEquals("Bearer", response.tokenType());
    }
}
