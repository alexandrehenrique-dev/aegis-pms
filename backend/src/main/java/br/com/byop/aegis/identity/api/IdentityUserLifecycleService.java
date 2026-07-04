package br.com.byop.aegis.identity.api;

import br.com.byop.aegis.identity.auth.client.KeycloakAdminClient;
import br.com.byop.aegis.identity.user.dto.UserResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class IdentityUserLifecycleService {

    private final KeycloakAdminClient keycloakAdminClient;

    public IdentityUserLifecycleService(KeycloakAdminClient keycloakAdminClient) {
        this.keycloakAdminClient = keycloakAdminClient;
    }

    public IdentityUser getRequiredUser(String userId) {
        log.debug("getRequiredUser: userId='{}'", userId);
        return toIdentityUser(keycloakAdminClient.findUserById(userId));
    }

    public Optional<IdentityUser> findByEmail(String email) {
        log.debug("findByEmail: email='{}'", email);
        return keycloakAdminClient.findUserByEmail(email).map(this::toIdentityUser);
    }

    public IdentityUser invite(String email, String name) {
        log.debug("invite: email='{}', name='{}'", email, name);
        IdentityUser user = toIdentityUser(keycloakAdminClient.inviteUser(email, name));
        log.info("invite: usuario convidado id='{}'", user.id());
        return user;
    }

    public void setUserEnabled(String userId, boolean enabled) {
        log.debug("setUserEnabled: userId='{}', enabled='{}'", userId, enabled);
        keycloakAdminClient.setUserEnabled(userId, enabled);
        log.info("setUserEnabled: usuario id='{}' enabled='{}'", userId, enabled);
    }

    public void executeActionsEmail(String userId, List<String> requiredActions) {
        log.debug("executeActionsEmail: userId='{}', requiredActions='{}'", userId, requiredActions);
        keycloakAdminClient.executeActionsEmail(userId, requiredActions);
        log.info("executeActionsEmail: acoes obrigatorias enviadas para userId='{}'", userId);
    }

    private IdentityUser toIdentityUser(UserResponse user) {
        return new IdentityUser(user.id(), user.username(), user.email(), user.firstName(), user.lastName());
    }
}
