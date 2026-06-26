package br.com.byop.aegis.product.service;

import br.com.byop.aegis.identity.api.IdentityUser;

import java.util.UUID;

public class StubProductAssignmentInvitePort implements ProductAssignmentInvitePort {

    @Override
    public IdentityUser invite(UUID tenantId, UUID productId, String inviteEmail) {
        // TODO Sprint futura: usar apenas em testes/ambientes sem Keycloak oficial configurado.
        return new IdentityUser("invite:" + inviteEmail, inviteEmail, inviteEmail, null, null);
    }
}
