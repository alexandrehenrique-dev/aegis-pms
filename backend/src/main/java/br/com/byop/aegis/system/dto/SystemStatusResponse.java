package br.com.byop.aegis.system.dto;

import java.util.List;

public record SystemStatusResponse(
        String applicationStatus,
        String databaseStatus,
        String databaseMessage,
        boolean keycloakIssuerConfigured,
        String keycloakIssuer,
        String knowledgeGraphStatus,
        List<String> activeProfiles,
        String buildVersion
) {
}
