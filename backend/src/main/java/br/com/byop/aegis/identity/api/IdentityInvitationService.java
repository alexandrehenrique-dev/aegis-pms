package br.com.byop.aegis.identity.api;

import br.com.byop.aegis.identity.auth.client.KeycloakAdminClient;
import br.com.byop.aegis.identity.user.dto.UserResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class IdentityInvitationService {

    private final KeycloakAdminClient keycloakAdminClient;

    public IdentityInvitationService(KeycloakAdminClient keycloakAdminClient) {
        this.keycloakAdminClient = keycloakAdminClient;
    }

    public IdentityUser inviteByEmail(String email) {
        log.debug("inviteByEmail: email='{}'", email);
        return inviteByEmail(email, email);
    }

    public IdentityUser inviteByEmail(String email, String name) {
        log.debug("inviteByEmail: email='{}', name='{}'", email, name);
        UserResponse user = keycloakAdminClient.inviteUser(email, name);
        log.info("inviteByEmail: usuario convidado id='{}'", user.id());
        return new IdentityUser(
                user.id(),
                user.username(),
                user.email(),
                user.firstName(),
                user.lastName()
        );
    }
}
