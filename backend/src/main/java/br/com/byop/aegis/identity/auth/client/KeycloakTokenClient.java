package br.com.byop.aegis.identity.auth.client;

import br.com.byop.aegis.identity.auth.config.KeycloakProperties;
import br.com.byop.aegis.identity.auth.exception.AccountDisabledException;
import br.com.byop.aegis.identity.auth.exception.InvalidCredentialsException;
import br.com.byop.aegis.identity.auth.exception.KeycloakAuthenticationException;
import br.com.byop.aegis.identity.auth.exception.RefreshTokenExpiredException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.regex.Pattern;

@Component
public class KeycloakTokenClient {

    private static final String CLIENT_ID = "client_id";
    private static final String GRANT_TYPE = "grant_type";
    private static final String REFRESH_TOKEN = "refresh_token";
    private static final String PASSWORD = "password";
    private static final String REFRESH_TOKEN_GRANT = "refresh_token";
    private static final String USERNAME = "username";
    private static final String KEYCLOAK_COMMUNICATION_ERROR = "Error communicating with Keycloak";

    private static final Pattern ACCOUNT_DISABLED_PATTERN =
            Pattern.compile("disabled|not fully set up", Pattern.CASE_INSENSITIVE);

    private final RestClient restClient;
    private final KeycloakProperties properties;

    public KeycloakTokenClient(RestClient restClient, KeycloakProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public KeycloakTokenResponse login(String username, String password) {
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add(CLIENT_ID, properties.webClientId());
            form.add(GRANT_TYPE, PASSWORD);
            form.add(USERNAME, username);
            form.add(PASSWORD, password);

            return restClient.post()
                    .uri(tokenEndpoint())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(KeycloakTokenResponse.class);

        } catch (HttpClientErrorException.Unauthorized exception) {
            if (isAccountDisabled(exception.getResponseBodyAsString())) {
                throw new AccountDisabledException();
            }
            throw new InvalidCredentialsException();

        } catch (HttpClientErrorException.BadRequest exception) {
            // Keycloak retorna 400 com invalid_grant quando a conta existe mas tem
            // required actions pendentes (ex.: "Account is not fully set up").
            // Tratamos como conta bloqueada/pendente — mensagem ao usuário é mais
            // precisa do que "Erro no servidor".
            if (isAccountDisabled(exception.getResponseBodyAsString())) {
                throw new AccountDisabledException();
            }
            throw new KeycloakAuthenticationException(KEYCLOAK_COMMUNICATION_ERROR, exception);

        } catch (HttpServerErrorException | HttpClientErrorException exception) {
            throw new KeycloakAuthenticationException(KEYCLOAK_COMMUNICATION_ERROR, exception);
        }
    }

    public KeycloakTokenResponse refresh(String refreshToken) {
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add(CLIENT_ID, properties.webClientId());
            form.add(GRANT_TYPE, REFRESH_TOKEN_GRANT);
            form.add(REFRESH_TOKEN, refreshToken);

            return restClient.post()
                    .uri(tokenEndpoint())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(KeycloakTokenResponse.class);

        } catch (HttpClientErrorException.Unauthorized _) {
            throw new RefreshTokenExpiredException();

        } catch (HttpServerErrorException | HttpClientErrorException exception) {
            throw new KeycloakAuthenticationException(KEYCLOAK_COMMUNICATION_ERROR, exception);
        }
    }

    public void logout(String refreshToken) {
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add(CLIENT_ID, properties.webClientId());
            form.add(REFRESH_TOKEN, refreshToken);

            restClient.post()
                    .uri(logoutEndpoint())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();

        } catch (HttpServerErrorException | HttpClientErrorException exception) {
            throw new KeycloakAuthenticationException(KEYCLOAK_COMMUNICATION_ERROR, exception);
        }
    }

    private String tokenEndpoint() {
        return properties.internalBaseUrl()
                + "/realms/"
                + properties.realm()
                + "/protocol/openid-connect/token";
    }

    private String logoutEndpoint() {
        return properties.internalBaseUrl()
                + "/realms/"
                + properties.realm()
                + "/protocol/openid-connect/logout";
    }

    private boolean isAccountDisabled(String responseBody) {
        return ACCOUNT_DISABLED_PATTERN
                .matcher(String.valueOf(responseBody))
                .find();
    }
}
