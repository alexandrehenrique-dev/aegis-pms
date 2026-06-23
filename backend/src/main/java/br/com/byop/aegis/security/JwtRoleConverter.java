package br.com.byop.aegis.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Converte roles canônicas do Keycloak em authorities Spring Security.
 */
public final class JwtRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String AEGIS_ROLE_PREFIX = "AEGIS_";
    private static final String SPRING_ROLE_PREFIX = "ROLE_";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");

        if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> roles)) {
            return List.of();
        }

        return roles.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .filter(role -> role.startsWith(AEGIS_ROLE_PREFIX))
                .map(role -> role.substring(AEGIS_ROLE_PREFIX.length()))
                .map(role -> SPRING_ROLE_PREFIX + role)
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }
}
