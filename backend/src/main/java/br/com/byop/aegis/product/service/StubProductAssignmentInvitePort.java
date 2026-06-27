package br.com.byop.aegis.product.service;

import br.com.byop.aegis.identity.api.IdentityUser;

import java.util.UUID;

/**
 * Implementacao no-op de {@link ProductAssignmentInvitePort}, usada apenas em
 * testes/ambientes sem Keycloak oficial configurado — nunca bean principal em
 * producao (ver {@link KeycloakProductAssignmentInvitePort}).
 */
public class StubProductAssignmentInvitePort implements ProductAssignmentInvitePort {

    @Override
    public IdentityUser invite(UUID tenantId, UUID productId, String inviteEmail) {
        return new IdentityUser("invite:" + inviteEmail, inviteEmail, inviteEmail, null, null);
    }
}
