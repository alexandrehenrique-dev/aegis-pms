package br.com.byop.aegis.identity.api;

import java.util.List;
import java.util.UUID;

public record IdentityActionInviteCommand(
        String keycloakId,
        String userEmail,
        String userName,
        UUID tenantId,
        String tenantName,
        List<String> productNames,
        String productSlug,
        String role,
        String inviterName
) {
}
