package br.com.byop.aegis.identity.api;

import br.com.byop.aegis.identity.auth.client.KeycloakAdminClient;
import br.com.byop.aegis.identity.user.dto.UserResponse;
import org.springframework.stereotype.Service;

@Service
public class IdentityInvitationService {

    private final KeycloakAdminClient keycloakAdminClient;

    public IdentityInvitationService(KeycloakAdminClient keycloakAdminClient) {
        this.keycloakAdminClient = keycloakAdminClient;
    }

    public IdentityUser inviteByEmail(String email) {
        UserResponse user = keycloakAdminClient.inviteUserByEmail(email);
        return new IdentityUser(
                user.id(),
                user.username(),
                user.email(),
                user.firstName(),
                user.lastName()
        );
    }
}
