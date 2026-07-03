package br.com.byop.aegis.identity.api;

import br.com.byop.aegis.identity.auth.client.KeycloakAdminClient;
import br.com.byop.aegis.identity.user.dto.UserResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class IdentityUserLifecycleService {

    private final KeycloakAdminClient keycloakAdminClient;

    public IdentityUserLifecycleService(KeycloakAdminClient keycloakAdminClient) {
        this.keycloakAdminClient = keycloakAdminClient;
    }

    public IdentityUser getRequiredUser(String userId) {
        return toIdentityUser(keycloakAdminClient.findUserById(userId));
    }

    public Optional<IdentityUser> findByEmail(String email) {
        return keycloakAdminClient.findUserByEmail(email).map(this::toIdentityUser);
    }

    public IdentityUser invite(String email, String name) {
        return toIdentityUser(keycloakAdminClient.inviteUser(email, name));
    }

    public void setUserEnabled(String userId, boolean enabled) {
        keycloakAdminClient.setUserEnabled(userId, enabled);
    }

    public void executeActionsEmail(String userId, List<String> requiredActions) {
        keycloakAdminClient.executeActionsEmail(userId, requiredActions);
    }

    private IdentityUser toIdentityUser(UserResponse user) {
        return new IdentityUser(user.id(), user.username(), user.email(), user.firstName(), user.lastName());
    }
}
