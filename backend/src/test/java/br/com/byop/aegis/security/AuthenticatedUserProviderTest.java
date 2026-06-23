package br.com.byop.aegis.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticatedUserProviderTest {

    private final AuthenticatedUserProvider provider = new AuthenticatedUserProvider();

    @Test
    void shouldExtractAuthenticatedUserFromJwt() {

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("subject-123")
                .claim("email", "loki@byop.dev")
                .claim("preferred_username", "loki")
                .claim("name", "Loki")
                .issuedAt(Instant.parse("2026-06-23T00:00:00Z"))
                .expiresAt(Instant.parse("2026-06-23T01:00:00Z"))
                .build();

        var authentication = new UsernamePasswordAuthenticationToken(
                jwt,
                null,
                List.of(
                        new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")
                )
        );

        AuthenticatedUser user = provider.from(authentication);

        assertThat(user.subject()).isEqualTo("subject-123");
        assertThat(user.email()).isEqualTo("loki@byop.dev");
        assertThat(user.username()).isEqualTo("loki");
        assertThat(user.name()).isEqualTo("Loki");
        assertThat(user.authorities())
                .containsExactly("ROLE_SUPER_ADMIN");
    }

    @Test
    void shouldThrowWhenAuthenticationIsNull() {

        assertThatThrownBy(() -> provider.from(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Usuário autenticado não encontrado.");
    }

    @Test
    void shouldThrowWhenPrincipalIsNotJwt() {

        var authentication = new UsernamePasswordAuthenticationToken(
                "loki",
                null
        );

        assertThatThrownBy(() -> provider.from(authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Usuário autenticado não encontrado.");
    }
}