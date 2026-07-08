package br.com.byop.aegis.identity.auth.client;

import br.com.byop.aegis.identity.auth.config.KeycloakProperties;
import br.com.byop.aegis.identity.auth.exception.KeycloakAuthenticationException;
import br.com.byop.aegis.identity.user.dto.UserResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class KeycloakAdminClient {

    private static final String CLIENT_ID = "client_id";
    private static final String GRANT_TYPE = "grant_type";
    private static final String PASSWORD = "password";
    private static final String USERNAME = "username";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String ADMIN_API_ERROR = "Error communicating with Keycloak Admin API";
    private static final String EMPTY_ADMIN_RESPONSE = "Empty response from Keycloak Admin API";
    private static final String PASSWORD_CREDENTIAL_TYPE = "password";
    private static final String UPDATE_PASSWORD_ACTION = "UPDATE_PASSWORD";

    private final RestClient restClient;
    private final KeycloakProperties properties;

    public KeycloakAdminClient(RestClient restClient, KeycloakProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public void sendResetPasswordEmail(String email) {
        try {
            String accessToken = adminAccessToken();

            findUserByEmail(email, accessToken)
                    .map(KeycloakUserResponse::id)
                    .ifPresent(userId -> executeResetPasswordEmail(userId, accessToken));

        } catch (RestClientException exception) {
            throw new KeycloakAuthenticationException(
                    ADMIN_API_ERROR + ": " + exception.getMessage(),
                    exception
            );
        }
    }

    public UserResponse inviteUserByEmail(String email) {
        return inviteUser(email, email);
    }

    public UserResponse inviteUser(String email, String name) {
        try {
            String accessToken = adminAccessToken();
            KeycloakUserResponse user = findUserByEmail(email, accessToken)
                    .orElseGet(() -> createUserForInvitation(email, name, accessToken));

            return toUserResponse(user);

        } catch (RestClientException exception) {
            throw new KeycloakAuthenticationException(ADMIN_API_ERROR, exception);
        }
    }

    public UserResponse ensureDemoUser(String email, String name, String password, String realmRoleName) {
        try {
            String accessToken = adminAccessToken();
            KeycloakUserResponse user = findUserByEmail(email, accessToken)
                    .orElseGet(() -> createDemoUser(email, name, password, accessToken));

            setUserEnabled(user.id(), true, accessToken);
            resetPassword(user.id(), password, accessToken);
            assignRealmRole(user.id(), realmRoleName, accessToken);

            return toUserResponse(user);

        } catch (RestClientException exception) {
            throw new KeycloakAuthenticationException(ADMIN_API_ERROR, exception);
        }
    }

    public Optional<UserResponse> findUserByEmail(String email) {
        try {
            String accessToken = adminAccessToken();
            return findUserByEmail(email, accessToken).map(KeycloakAdminClient::toUserResponse);
        } catch (RestClientException exception) {
            throw new KeycloakAuthenticationException(ADMIN_API_ERROR, exception);
        }
    }

    public void executeActionsEmail(String userId, List<String> requiredActions) {
        try {
            String accessToken = adminAccessToken();
            executeActionsEmail(userId, requiredActions, accessToken);
        } catch (RestClientException exception) {
            throw new KeycloakAuthenticationException(ADMIN_API_ERROR, exception);
        }
    }

    public void setUserEnabled(String userId, boolean enabled) {
        try {
            String accessToken = adminAccessToken();
            setUserEnabled(userId, enabled, accessToken);
        } catch (RestClientException exception) {
            throw new KeycloakAuthenticationException(ADMIN_API_ERROR, exception);
        }
    }

    public void resetPassword(String userId, String password) {
        try {
            String accessToken = adminAccessToken();
            resetPassword(userId, password, accessToken);
        } catch (RestClientException exception) {
            throw new KeycloakAuthenticationException(ADMIN_API_ERROR, exception);
        }
    }

    public void clearRequiredActions(String userId) {
        try {
            String accessToken = adminAccessToken();
            clearRequiredActions(userId, accessToken);
        } catch (RestClientException exception) {
            throw new KeycloakAuthenticationException(ADMIN_API_ERROR, exception);
        }
    }

    public void assignRealmRole(String userId, String realmRoleName) {
        try {
            String accessToken = adminAccessToken();
            assignRealmRole(userId, realmRoleName, accessToken);
        } catch (RestClientException exception) {
            throw new KeycloakAuthenticationException(ADMIN_API_ERROR, exception);
        }
    }

    /**
     * O.1 (BUG-SPRINT-05) — ativacao de convite antes nunca atualizava
     * firstName/lastName no Keycloak; um convite criado com {@code name}
     * vazio ou de uma so palavra (ver {@link #firstName(String)}/{@link
     * #lastName(String)}) deixava o usuario com perfil incompleto, e o realm
     * bloqueia login ("Account is not fully set up") para perfil incompleto
     * mesmo com {@code enabled=true}. Chamado em {@code activate()} antes de
     * habilitar a conta, com o nome/sobrenome coletados explicitamente no
     * formulario de ativacao (nunca mais derivados de uma unica string).
     */
    public void updateUserProfile(String userId, String firstName, String lastName) {
        try {
            String accessToken = adminAccessToken();
            updateUserProfile(userId, firstName, lastName, accessToken);
        } catch (RestClientException exception) {
            throw new KeycloakAuthenticationException(ADMIN_API_ERROR, exception);
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

            return Optional.ofNullable(users)
                    .orElseGet(Collections::emptyList)
                    .stream()
                    .filter(Objects::nonNull)
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

            return toUserResponse(requireAdminResponse(user));

        } catch (RestClientException exception) {
            throw new KeycloakAuthenticationException(ADMIN_API_ERROR, exception);
        }
    }

    /**
     * {@code true} quando o usuário ainda tem a required action pendente no Keycloak
     * (ex.: {@code UPDATE_PASSWORD} para quem nunca definiu senha própria). Usado para
     * distinguir, na validação de um convite, se o destinatário é um usuário novo
     * (precisa definir senha) ou já tem conta ativa.
     */
    public boolean hasRequiredAction(String keycloakId, String action) {
        try {
            String accessToken = adminAccessToken();

            KeycloakUserResponse user = restClient.get()
                    .uri(userByIdEndpoint(keycloakId))
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(KeycloakUserResponse.class);

            List<String> requiredActions = requireAdminResponse(user).requiredActions();
            return requiredActions != null && requiredActions.contains(action);

        } catch (RestClientException exception) {
            throw new KeycloakAuthenticationException(ADMIN_API_ERROR, exception);
        }
    }

    private String usersEndpoint() {
        return properties.internalBaseUrl()
                + normalizedAdminRealmsPath()
                + properties.realm()
                + "/users";
    }

    private String userByIdEndpoint(String id) {
        return usersEndpoint() + "/" + id;
    }

    private static UserResponse toUserResponse(KeycloakUserResponse user) {
        Objects.requireNonNull(user, "user is required");
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

        return requireAdminAccessToken(requireAdminResponse(response));
    }

    private Optional<KeycloakUserResponse> findUserByEmail(String email, String accessToken) {
        KeycloakUserResponse[] users = restClient.get()
                .uri(usersEndpoint(email))
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(KeycloakUserResponse[].class);

        if (users == null) {
            return Optional.empty();
        }

        return Arrays.stream(users)
                .filter(Objects::nonNull)
                .findFirst();
    }

    private KeycloakUserResponse createUserForInvitation(String email, String name, String accessToken) {
        ResponseEntity<Void> response = restClient.post()
                .uri(usersEndpoint())
                .headers(headers -> headers.setBearerAuth(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new KeycloakCreateUserRequest(
                        email,
                        email,
                        firstName(name),
                        lastName(name),
                        true,
                        List.of(UPDATE_PASSWORD_ACTION)
                ))
                .retrieve()
                .toBodilessEntity();

        return findUserByEmail(email, accessToken)
                .orElseThrow(() -> new KeycloakAuthenticationException(
                        "Created Keycloak user was not returned by lookup: " + email
                                + " (" + response.getStatusCode() + ")"
                ));
    }

    private KeycloakUserResponse createDemoUser(String email, String name, String password, String accessToken) {
        ResponseEntity<Void> response = restClient.post()
                .uri(usersEndpoint())
                .headers(headers -> headers.setBearerAuth(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new KeycloakCreateUserRequest(
                        email,
                        email,
                        firstName(name),
                        lastName(name),
                        true,
                        List.of(),
                        List.of(new KeycloakCredentialRequest(PASSWORD_CREDENTIAL_TYPE, password, false))
                ))
                .retrieve()
                .toBodilessEntity();

        return findUserByEmail(email, accessToken)
                .orElseThrow(() -> new KeycloakAuthenticationException(
                        "Created Keycloak demo user was not returned by lookup: " + email
                                + " (" + response.getStatusCode() + ")"
                ));
    }

    private void setUserEnabled(String userId, boolean enabled, String accessToken) {
        restClient.put()
                .uri(userByIdEndpoint(userId))
                .headers(headers -> headers.setBearerAuth(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"enabled\":" + enabled + "}")
                .retrieve()
                .toBodilessEntity();
    }

    private void resetPassword(String userId, String password, String accessToken) {
        restClient.put()
                .uri(resetPasswordEndpoint(userId))
                .headers(headers -> headers.setBearerAuth(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new KeycloakCredentialRequest(PASSWORD_CREDENTIAL_TYPE, password, false))
                .retrieve()
                .toBodilessEntity();
    }

    private void clearRequiredActions(String userId, String accessToken) {
        restClient.put()
                .uri(userByIdEndpoint(userId))
                .headers(headers -> headers.setBearerAuth(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"requiredActions\":[]}")
                .retrieve()
                .toBodilessEntity();
    }

    private void updateUserProfile(String userId, String firstName, String lastName, String accessToken) {
        restClient.put()
                .uri(userByIdEndpoint(userId))
                .headers(headers -> headers.setBearerAuth(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"firstName\":" + jsonString(firstName) + ",\"lastName\":" + jsonString(lastName) + "}")
                .retrieve()
                .toBodilessEntity();
    }

    private void assignRealmRole(String userId, String realmRoleName, String accessToken) {
        KeycloakRealmRoleResponse role = restClient.get()
                .uri(realmRoleEndpoint(realmRoleName))
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(KeycloakRealmRoleResponse.class);

        restClient.post()
                .uri(realmRoleMappingEndpoint(userId))
                .headers(headers -> headers.setBearerAuth(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(requireAdminResponse(role)))
                .retrieve()
                .toBodilessEntity();
    }

    private void executeResetPasswordEmail(String userId, String accessToken) {
        executeActionsEmail(userId, List.of(UPDATE_PASSWORD_ACTION), accessToken);
    }

    private void executeActionsEmail(String userId, List<String> requiredActions, String accessToken) {
        restClient.put()
                .uri(executeActionsEmailEndpoint(userId))
                .headers(headers -> headers.setBearerAuth(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(actionsJson(requiredActions))
                .retrieve()
                .toBodilessEntity();
    }

    private String tokenEndpoint() {
        return properties.internalBaseUrl()
                + "/realms/master/protocol/openid-connect/token";
    }

    private String usersEndpoint(String email) {
        return properties.internalBaseUrl()
                + normalizedAdminRealmsPath()
                + properties.realm()
                + "/users?email="
                + email
                + "&exact=true";
    }

    private String executeActionsEmailEndpoint(String userId) {
        return properties.internalBaseUrl()
                + normalizedAdminRealmsPath()
                + properties.realm()
                + "/users/"
                + userId
                + "/execute-actions-email?client_id="
                + properties.webClientId();
    }

    private String resetPasswordEndpoint(String userId) {
        return userByIdEndpoint(userId) + "/reset-password";
    }

    private String realmRoleEndpoint(String realmRoleName) {
        return properties.internalBaseUrl()
                + normalizedAdminRealmsPath()
                + properties.realm()
                + "/roles/"
                + realmRoleName;
    }

    private String realmRoleMappingEndpoint(String userId) {
        return userByIdEndpoint(userId) + "/role-mappings/realm";
    }

    private String normalizedAdminRealmsPath() {
        String path = requireAdminResponse(properties.adminRealmsPath());
        String withLeadingSlash = path.startsWith("/") ? path : "/" + path;
        return withLeadingSlash.endsWith("/") ? withLeadingSlash : withLeadingSlash + "/";
    }

    private static <T> T requireAdminResponse(T response) {
        if (response == null) {
            throw new KeycloakAuthenticationException(EMPTY_ADMIN_RESPONSE);
        }
        return response;
    }

    private static String requireAdminAccessToken(KeycloakAdminTokenResponse response) {
        if (response.accessToken() == null) {
            throw new KeycloakAuthenticationException(EMPTY_ADMIN_RESPONSE);
        }
        return response.accessToken();
    }

    /**
     * Escapa uma string para uso como valor JSON literal — usado para montar
     * o corpo de {@link #updateUserProfile(String, String, String, String)}
     * como JSON string bruto (mesmo padrão de {@link #actionsJson(List)} e
     * do corpo de {@code enabled}/{@code requiredActions} acima), em vez de
     * serializar via Jackson/record: nome e sobrenome vêm de input do
     * usuário e precisam de escape real (aspas, barra invertida), diferente
     * das actions do catálogo fechado do Keycloak.
     */
    private static String jsonString(String value) {
        String escaped = Objects.toString(value, "")
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
        return "\"" + escaped + "\"";
    }

    private static String actionsJson(List<String> requiredActions) {
        return requiredActions.stream()
                .map(action -> "\"" + action + "\"")
                .reduce((left, right) -> left + "," + right)
                .map(actions -> "[" + actions + "]")
                .orElse("[]");
    }

    private static String firstName(String name) {
        String normalized = Objects.toString(name, "").trim();
        if (normalized.isBlank()) {
            return null;
        }
        int separator = normalized.indexOf(' ');
        return separator < 0 ? normalized : normalized.substring(0, separator);
    }

    private static String lastName(String name) {
        String normalized = Objects.toString(name, "").trim();
        int separator = normalized.indexOf(' ');
        if (separator < 0) {
            return null;
        }
        return normalized.substring(separator + 1).trim();
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
            boolean enabled,
            List<String> requiredActions
    ) {
    }

    private record KeycloakCreateUserRequest(
            String username,
            String email,
            String firstName,
            String lastName,
            boolean enabled,
            List<String> requiredActions,
            List<KeycloakCredentialRequest> credentials
    ) {
        private KeycloakCreateUserRequest(String username, String email, String firstName, String lastName,
                                          boolean enabled, List<String> requiredActions) {
            this(username, email, firstName, lastName, enabled, requiredActions, null);
        }
    }

    private record KeycloakCredentialRequest(
            String type,
            String value,
            boolean temporary
    ) {
    }

    private record KeycloakRealmRoleResponse(
            String id,
            String name
    ) {
    }
}
