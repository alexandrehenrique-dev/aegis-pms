package br.com.byop.aegis.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Fornece os dados do usuário autenticado a partir do JWT validado pelo Resource Server.
 */
@Component
public class AuthenticatedUserProvider {

    /**
     * Extrai o usuário autenticado a partir do contexto de autenticação atual.
     *
     * @param authentication autenticação resolvida pelo Spring Security
     * @return usuário autenticado
     */
    public AuthenticatedUser from(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("Usuário autenticado não encontrado.");
        }

        Set<String> authorities = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toUnmodifiableSet());

        return new AuthenticatedUser(
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("preferred_username"),
                jwt.getClaimAsString("name"),
                authorities
        );
    }
}
