package br.com.byop.aegis.identity.auth.dto;

import java.time.Instant;
import java.util.List;

public record AuthInviteValidationResponse(
        String userName,
        String userEmail,
        String tenantName,
        List<String> productNames,
        String role,
        String inviterName,
        Instant expiresAt
) {
}
