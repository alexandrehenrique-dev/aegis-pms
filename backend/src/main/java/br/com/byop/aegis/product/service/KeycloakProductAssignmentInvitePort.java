package br.com.byop.aegis.product.service;

import br.com.byop.aegis.identity.api.IdentityActionInviteCommand;
import br.com.byop.aegis.identity.api.IdentityActionTokenService;
import br.com.byop.aegis.identity.api.IdentityInvitationService;
import br.com.byop.aegis.identity.api.IdentityUser;
import br.com.byop.aegis.tenant.api.TenantAccessService;
import br.com.byop.aegis.tenant.api.TenantReference;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class KeycloakProductAssignmentInvitePort implements ProductAssignmentInvitePort {

    private final IdentityInvitationService invitationService;
    private final IdentityActionTokenService actionTokenService;
    private final TenantAccessService tenantAccessService;

    public KeycloakProductAssignmentInvitePort(IdentityInvitationService invitationService,
                                               IdentityActionTokenService actionTokenService,
                                               TenantAccessService tenantAccessService) {
        this.invitationService = invitationService;
        this.actionTokenService = actionTokenService;
        this.tenantAccessService = tenantAccessService;
    }

    @Override
    public IdentityUser invite(UUID tenantId, UUID productId, String inviteEmail) {
        return invitationService.inviteByEmail(inviteEmail);
    }

    @Override
    public IdentityUser invite(UUID tenantId, UUID productId, String productKey, String productName,
                               String inviteEmail, String role, String inviterName) {
        IdentityUser invited = invitationService.inviteByEmail(inviteEmail);
        TenantReference tenant = tenantAccessService.getRequiredReference(tenantId);
        actionTokenService.sendInviteActivation(new IdentityActionInviteCommand(
                invited.id(),
                invited.email(),
                invited.displayName(),
                tenant.tenantId(),
                tenant.name(),
                List.of(productName),
                productKey,
                role,
                inviterName
        ));
        return invited;
    }
}
