package br.com.byop.aegis.identity.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "keycloak")
public record KeycloakProperties(
        String issuerUri,
        String internalBaseUrl,
        String realm,
        String webClientId,
        String adminClientId,
        String adminUsername,
        String adminPassword
) {
}
