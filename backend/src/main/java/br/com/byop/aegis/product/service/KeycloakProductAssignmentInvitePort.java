package br.com.byop.aegis.product.service;

import br.com.byop.aegis.identity.api.IdentityInvitationService;
import br.com.byop.aegis.identity.api.IdentityUser;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class KeycloakProductAssignmentInvitePort implements ProductAssignmentInvitePort {

    private final IdentityInvitationService invitationService;

    public KeycloakProductAssignmentInvitePort(IdentityInvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @Override
    public IdentityUser invite(UUID tenantId, UUID productId, String inviteEmail) {
        return invitationService.inviteByEmail(inviteEmail);
    }
}
