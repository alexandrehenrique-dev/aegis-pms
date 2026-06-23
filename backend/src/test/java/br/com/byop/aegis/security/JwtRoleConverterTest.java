package br.com.byop.aegis.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

class JwtRoleConverterTest {

    private final JwtRoleConverter converter = new JwtRoleConverter();

    @Test
    void shouldConvertOnlyAegisRolesToSpringAuthorities() {
        Jwt jwt = jwtWithRoles("AEGIS_SUPER_ADMIN", "offline_access", "uma_authorization");

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_SUPER_ADMIN");
    }

    @Test
    void shouldConvertMultipleAegisRoles() {
        Jwt jwt = jwtWithRoles("AEGIS_TENANT_ADMIN", "AEGIS_EDITOR", "AEGIS_VIEWER");

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_TENANT_ADMIN", "ROLE_EDITOR", "ROLE_VIEWER");
    }

    @Test
    void shouldReturnEmptyAuthoritiesWhenRealmAccessIsMissing() {
        Jwt jwt = jwtWithoutRealmAccess();

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }

    @Test
    void shouldReturnEmptyAuthoritiesWhenRolesAreMissing() {
        Jwt jwt = jwtWithRealmAccess(Map.of());

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }

    @Test
    void shouldReturnEmptyAuthoritiesWhenRolesClaimIsNotACollection() {
        Jwt jwt = jwtWithRealmAccess(Map.of("roles", "AEGIS_SUPER_ADMIN"));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }

    @Test
    void shouldIgnoreNullRoles() {
        Jwt jwt = jwtWithRealmAccess(Map.of("roles", List.of("AEGIS_VIEWER", new ArrayList<>(asList("AEGIS_VIEWER", null))
        )));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_VIEWER");
    }

    private Jwt jwtWithRoles(String... roles) {
        return jwtWithRealmAccess(Map.of("roles", List.of(roles)));
    }

    private Jwt jwtWithoutRealmAccess() {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("subject-123")
                .issuedAt(Instant.parse("2026-06-23T00:00:00Z"))
                .expiresAt(Instant.parse("2026-06-23T01:00:00Z"))
                .build();
    }

    private Jwt jwtWithRealmAccess(Map<String, Object> realmAccess) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("subject-123")
                .claim("realm_access", realmAccess)
                .issuedAt(Instant.parse("2026-06-23T00:00:00Z"))
                .expiresAt(Instant.parse("2026-06-23T01:00:00Z"))
                .build();
    }
}