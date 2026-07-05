package br.com.byop.aegis.product.service;

import br.com.byop.aegis.identity.api.IdentityUser;

import java.util.UUID;

public interface ProductAssignmentInvitePort {

    IdentityUser invite(UUID tenantId, UUID productId, String inviteEmail);

    default IdentityUser invite(UUID tenantId, UUID productId, String productKey, String productName,
                                String inviteEmail, String role, String inviterName) {
        return invite(tenantId, productId, inviteEmail);
    }
}
