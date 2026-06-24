package br.com.byop.aegis.identity.auth.client;

import br.com.byop.aegis.identity.auth.config.KeycloakProperties;
import br.com.byop.aegis.identity.auth.exception.KeycloakAuthenticationException;
import br.com.byop.aegis.identity.user.dto.UserResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class KeycloakAdminClient {

    private static final String CLIENT_ID = "client_id";
    private static final String GRANT_TYPE = "grant_type";
    private static final String PASSWORD = "password";
    private static final String USERNAME = "username";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String ADMIN_API_ERROR = "Error communicating with Keycloak Admin API";

    private final RestClient restClient;
    private final KeycloakProperties properties;

    public KeycloakAdminClient(RestClient restClient, KeycloakProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public void sendResetPasswordEmail(String email) {
        try {
            String accessToken = adminAccessToken();

            findUserIdByEmail(email, accessToken)
                    .ifPresent(userId -> executeResetPasswordEmail(userId, accessToken));

        } catch (RestClientException exception) {
            throw new KeycloakAuthenticationException(
                    ADMIN_API_ERROR + ": " + exception.getMessage(),
                    exception
            );
        }
    }

    public List<UserResponse> findUsers() {
        try {
            String accessToken = adminAccessToken();

            List<KeycloakUserResponse> users = restClient.get()
                    .uri(usersEndpoint())
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            return users.stream()
                    .map(KeycloakAdminClient::toUserResponse)
                    .toList();

        } catch (RestClientException exception) {
            throw new KeycloakAuthenticationException(ADMIN_API_ERROR, exception);
        }
    }

    public UserResponse findUserById(String id) {
        try {
            String accessToken = adminAccessToken();

            KeycloakUserResponse user = restClient.get()
                    .uri(userByIdEndpoint(id))
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(KeycloakUserResponse.class);

            return toUserResponse(user);

        } catch (RestClientException exception) {
            throw new KeycloakAuthenticationException(ADMIN_API_ERROR, exception);
        }
    }

    private String usersEndpoint() {
        return properties.internalBaseUrl()
                + "/admin/realms/"
                + properties.realm()
                + "/users";
    }

    private String userByIdEndpoint(String id) {
        return usersEndpoint() + "/" + id;
    }

    private static UserResponse toUserResponse(KeycloakUserResponse user) {
        return new UserResponse(
                user.id(),
                user.username(),
                user.email(),
                user.firstName(),
                user.lastName(),
                user.enabled()
        );
    }

    private String adminAccessToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add(CLIENT_ID, properties.adminClientId());
        form.add(GRANT_TYPE, PASSWORD);
        form.add(USERNAME, properties.adminUsername());
        form.add(PASSWORD, properties.adminPassword());

        KeycloakAdminTokenResponse response = restClient.post()
                .uri(tokenEndpoint())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(KeycloakAdminTokenResponse.class);

        return response.accessToken();
    }

    private Optional<String> findUserIdByEmail(String email, String accessToken) {
        KeycloakUserResponse[] users = restClient.get()
                .uri(usersEndpoint(email))
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(KeycloakUserResponse[].class);

        if (users == null) {
            return Optional.empty();
        }

        return Arrays.stream(users)
                .findFirst()
                .map(KeycloakUserResponse::id);
    }

    private void executeResetPasswordEmail(String userId, String accessToken) {
        restClient.put()
                .uri(executeActionsEmailEndpoint(userId))
                .headers(headers -> headers.setBearerAuth(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of("UPDATE_PASSWORD"))
                .retrieve()
                .toBodilessEntity();
    }

    private String tokenEndpoint() {
        return properties.internalBaseUrl()
                + "/realms/master/protocol/openid-connect/token";
    }

    private String usersEndpoint(String email) {
        return properties.internalBaseUrl()
                + "/admin/realms/"
                + properties.realm()
                + "/users?email="
                + email
                + "&exact=true";
    }

    private String executeActionsEmailEndpoint(String userId) {
        return properties.internalBaseUrl()
                + "/admin/realms/"
                + properties.realm()
                + "/users/"
                + userId
                + "/execute-actions-email?client_id="
                + properties.webClientId();
    }

    private record KeycloakAdminTokenResponse(
            @com.fasterxml.jackson.annotation.JsonProperty(ACCESS_TOKEN)
            String accessToken
    ) {
    }

    private record KeycloakUserResponse(
            String id,
            String username,
            String email,
            String firstName,
            String lastName,
            boolean enabled
    ) {
    }
}