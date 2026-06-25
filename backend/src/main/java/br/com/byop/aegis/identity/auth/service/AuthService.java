package br.com.byop.aegis.identity.auth.service;

import br.com.byop.aegis.identity.auth.client.KeycloakAdminClient;
import br.com.byop.aegis.identity.auth.client.KeycloakTokenClient;
import br.com.byop.aegis.identity.auth.client.KeycloakTokenResponse;
import br.com.byop.aegis.identity.auth.dto.AuthMessageResponse;
import br.com.byop.aegis.identity.auth.dto.AuthTokenResponse;
import br.com.byop.aegis.identity.auth.dto.AuthTokenResponseMapper;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final String GENERIC_ACCOUNT_RECOVERY_MESSAGE =
            "Se o e-mail estiver cadastrado, você receberá as instruções em breve.";

    private final KeycloakAdminClient keycloakAdminClient;
    private final KeycloakTokenClient keycloakTokenClient;

    public AuthService(
            KeycloakTokenClient keycloakTokenClient,
            KeycloakAdminClient keycloakAdminClient
    ) {
        this.keycloakTokenClient = keycloakTokenClient;
        this.keycloakAdminClient = keycloakAdminClient;
    }

    public AuthTokenResponse login(String username, String password) {
        KeycloakTokenResponse response = keycloakTokenClient.login(username, password);
        return AuthTokenResponseMapper.from(response);
    }

    public AuthTokenResponse refresh(String refreshToken) {
        KeycloakTokenResponse response = keycloakTokenClient.refresh(refreshToken);
        return AuthTokenResponseMapper.from(response);
    }

    public void logout(String refreshToken) {
        keycloakTokenClient.logout(refreshToken);
    }

    public AuthMessageResponse forgotPassword(String email) {
        keycloakAdminClient.sendResetPasswordEmail(email);
        return new AuthMessageResponse(GENERIC_ACCOUNT_RECOVERY_MESSAGE);
    }
}