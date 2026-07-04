package br.com.byop.aegis.identity.api;

import br.com.byop.aegis.identity.auth.client.KeycloakAdminClient;
import br.com.byop.aegis.identity.user.dto.UserResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Porta publica de identidade para garantir usuarios de demonstracao no Keycloak.
 */
@Slf4j
@Service
public class IdentityDemoUserService {

    private static final String KEYCLOAK_ROLE_PREFIX = "AEGIS_";

    private final KeycloakAdminClient keycloakAdminClient;

    public IdentityDemoUserService(KeycloakAdminClient keycloakAdminClient) {
        this.keycloakAdminClient = keycloakAdminClient;
    }

    public IdentityUser ensureDemoUser(String email, String name, String password, String role) {
        log.debug("ensureDemoUser: email='{}', role='{}'", email, role);
        UserResponse user = keycloakAdminClient.ensureDemoUser(
                email,
                name,
                password,
                KEYCLOAK_ROLE_PREFIX + role
        );
        log.info("ensureDemoUser: usuario de demonstracao garantido id='{}', role='{}'", user.id(), role);
        return new IdentityUser(
                user.id(),
                user.username(),
                user.email(),
                user.firstName(),
                user.lastName()
        );
    }
}
