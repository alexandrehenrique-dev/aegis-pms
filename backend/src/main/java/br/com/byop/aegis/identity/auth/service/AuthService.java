package br.com.byop.aegis.identity.auth.service;

import br.com.byop.aegis.identity.auth.client.KeycloakTokenClient;
import br.com.byop.aegis.identity.auth.client.KeycloakTokenResponse;
import br.com.byop.aegis.identity.auth.dto.AuthMessageResponse;
import br.com.byop.aegis.identity.auth.dto.AuthTokenResponse;
import br.com.byop.aegis.identity.auth.dto.AuthTokenResponseMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthService {

    private static final String GENERIC_ACCOUNT_RECOVERY_MESSAGE =
            "Se o e-mail estiver cadastrado, você receberá as instruções em breve.";

    private final AuthActivationService authActivationService;
    private final KeycloakTokenClient keycloakTokenClient;

    public AuthService(
            KeycloakTokenClient keycloakTokenClient,
            AuthActivationService authActivationService
    ) {
        this.keycloakTokenClient = keycloakTokenClient;
        this.authActivationService = authActivationService;
    }

    public AuthTokenResponse login(String username, String password) {
        log.debug("login: username='{}'", username);
        try {
            KeycloakTokenResponse response = keycloakTokenClient.login(username, password);
            log.info("login: autenticacao bem-sucedida para username='{}'", username);
            return AuthTokenResponseMapper.from(response);
        } catch (RuntimeException ex) {
            log.warn("login: falha de autenticacao para username='{}'", username);
            throw ex;
        }
    }

    public AuthTokenResponse refresh(String refreshToken) {
        log.debug("refresh: solicitado");
        try {
            KeycloakTokenResponse response = keycloakTokenClient.refresh(refreshToken);
            log.info("refresh: token renovado com sucesso");
            return AuthTokenResponseMapper.from(response);
        } catch (RuntimeException ex) {
            log.warn("refresh: falha ao renovar token");
            throw ex;
        }
    }

    public void logout(String refreshToken) {
        log.debug("logout: solicitado");
        keycloakTokenClient.logout(refreshToken);
        log.info("logout: sessao encerrada");
    }

    public AuthMessageResponse forgotPassword(String email) {
        log.debug("forgotPassword: solicitado");
        authActivationService.requestPasswordReset(email);
        log.info("forgotPassword: solicitacao de recuperacao processada");
        return new AuthMessageResponse(GENERIC_ACCOUNT_RECOVERY_MESSAGE);
    }
}
