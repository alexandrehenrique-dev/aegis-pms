package br.com.byop.aegis.security;

import java.util.Set;

/**
 * Representa o usuário autenticado extraído do JWT emitido pelo Keycloak.
 */
public record AuthenticatedUser(
        String subject,
        String email,
        String username,
        String name,
        Set<String> authorities
) {
}
